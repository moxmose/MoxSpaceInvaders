package com.moxmose.moxspaceinvaders.ui

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Stati degli oggetti di gioco
data class AlienState(
    val position: Offset,
    val size: Size = Size(80f, 80f),
    val color: Color
)
data class ProjectileState(val position: Offset, val color: Color = Color.White, val size: Size = Size(10f, 20f))

class GameViewModel(
    private val navController: NavHostController,
    private val timerViewModel: TimerViewModel,
    private val appSettingsDataStore: IAppSettingsDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) : ViewModel() {

    // --- STATI DI GIOCO ---
    val playerName: StateFlow<String> = appSettingsDataStore.playerName
    val selectedBackgrounds: StateFlow<Set<String>> = appSettingsDataStore.selectedBackgrounds
    val score = mutableIntStateOf(0)
    val currentTime = timerViewModel.elapsedSeconds

    // --- STATI DEGLI OGGETTI DI GIOCO ---
    val playerPositionX = mutableFloatStateOf(0f) // Offset orizzontale dal centro in DP
    val projectiles = mutableStateOf<List<ProjectileState>>(emptyList())
    val aliens = mutableStateOf<List<AlienState>>(emptyList())

    private val playerSpeed = 15f // in DP
    private val projectileSpeed = 20f // in PX
    private var alienDirection = 1f // 1f for right, -1f for left
    private val alienSpeed = 2f

    private var screenWidthPx = 0f
    private var screenHeightPx = 0f
    private var playerMovementBoundsDp = 0f

    init {
        startGame()
    }

    fun updateScreenDimensions(widthPx: Float, heightPx: Float, boundsDp: Float) {
        screenWidthPx = widthPx
        screenHeightPx = heightPx
        playerMovementBoundsDp = boundsDp
    }

    private fun startGame() {
        viewModelScope.launch {
            score.intValue = 0
            playerPositionX.floatValue = 0f
            projectiles.value = emptyList()
            initializeAliens()

            timerViewModel.resetTimer()
            timerViewModel.startTimer()

            gameLoop()
        }
    }

    private fun initializeAliens() {
        val newAliens = mutableListOf<AlienState>()
        val alienRows = 5
        val alienCols = 8
        val alienColors = listOf(Color.Red, Color.Yellow, Color.Cyan, Color.Magenta, Color.Green)
        val startX = 100f
        val startY = 150f
        val spacing = 100f

        for (row in 0 until alienRows) {
            for (col in 0 until alienCols) {
                newAliens.add(
                    AlienState(
                        position = Offset(startX + col * spacing, startY + row * spacing),
                        color = alienColors[row % alienColors.size]
                    )
                )
            }
        }
        aliens.value = newAliens
    }

    private fun gameLoop() {
        viewModelScope.launch(ioDispatcher) {
            while (isActive) {
                // --- AGGIORNAMENTO PROIETTILI ---
                projectiles.value = projectiles.value.map {
                    it.copy(position = it.position.copy(y = it.position.y - projectileSpeed))
                }.filter { it.position.y > 0 }

                // --- AGGIORNAMENTO ALIENI ---
                var boundaryReached = false
                var nextAliens = aliens.value.map { alien ->
                    val newX = alien.position.x + (alienSpeed * alienDirection)
                    if (newX < 0 || newX + alien.size.width > screenWidthPx) {
                        boundaryReached = true
                    }
                    alien.copy(position = alien.position.copy(x = newX))
                }

                if (boundaryReached) {
                    alienDirection *= -1 // Inverti direzione
                    nextAliens = nextAliens.map { alien ->
                        alien.copy(position = alien.position.copy(y = alien.position.y + 40f))
                    }
                }
                aliens.value = nextAliens
                
                // --- CONTROLLO COLLISIONI ---
                checkCollisions()

                delayProvider(16)
            }
        }
    }

    private fun checkCollisions() {
        val projectilesToRemove = mutableSetOf<ProjectileState>()
        val aliensToRemove = mutableSetOf<AlienState>()

        projectiles.value.forEach { projectile ->
            val projectileRect = Rect(projectile.position, projectile.size)

            aliens.value.forEach { alien ->
                val alienRect = Rect(alien.position, alien.size)

                if (projectileRect.overlaps(alienRect)) {
                    projectilesToRemove.add(projectile)
                    aliensToRemove.add(alien)
                    score.intValue += 10
                }
            }
        }

        if (projectilesToRemove.isNotEmpty() || aliensToRemove.isNotEmpty()) {
            projectiles.value = projectiles.value - projectilesToRemove
            aliens.value = aliens.value - aliensToRemove
        }
    }

    fun onGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.MovePlayer -> {
                val newPosition = playerPositionX.floatValue + (event.direction * playerSpeed)
                playerPositionX.floatValue = newPosition.coerceIn(-playerMovementBoundsDp, playerMovementBoundsDp)
            }
            is GameEvent.PlayerShoot -> {
                val newProjectile = ProjectileState(position = event.startPositionPx)
                projectiles.value = projectiles.value + newProjectile
            }
            GameEvent.Pause -> { /* Logica pausa */ }
            GameEvent.Reset -> startGame()
            GameEvent.BackToMenu -> {
                navController.popBackStack()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            timerViewModel.stopAndAwaitTimerCompletion()
        }
    }
}

sealed class GameEvent {
    data class MovePlayer(val direction: Float) : GameEvent()
    data class PlayerShoot(val startPositionPx: Offset) : GameEvent()
    object Pause : GameEvent()
    object Reset : GameEvent()
    object BackToMenu : GameEvent()
}