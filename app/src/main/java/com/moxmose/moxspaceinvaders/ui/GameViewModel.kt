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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameStatus { Playing, Victory, GameOver }

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
    val lives = mutableIntStateOf(3)
    val score = mutableIntStateOf(0)
    val gameState = mutableStateOf(GameStatus.Playing)
    val currentTime = timerViewModel.elapsedSeconds
    val playerName: StateFlow<String> = appSettingsDataStore.playerName
    val selectedBackgrounds: StateFlow<Set<String>> = appSettingsDataStore.selectedBackgrounds

    // --- STATI DEGLI OGGETTI DI GIOCO ---
    val playerPositionX = mutableFloatStateOf(0f) // Offset orizzontale dal centro in DP
    val projectiles = mutableStateOf<List<ProjectileState>>(emptyList())
    val alienProjectiles = mutableStateOf<List<ProjectileState>>(emptyList())
    val aliens = mutableStateOf<List<AlienState>>(emptyList())

    private val playerSpeed = 15f // in DP
    private val projectileSpeed = 20f // in PX
    private val alienProjectileSpeed = 10f // in PX
    private var alienDirection = 1f
    private val alienSpeed = 2f
    private val alienShootProbability = 4 // Percentuale (0-100)
    private var gameLoopJob: Job? = null
    private var isReady = false

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
        if (!isReady) {
            isReady = true
            gameLoopJob = gameLoop()
        }
    }

    private fun startGame() {
        gameLoopJob?.cancel()
        isReady = false
        viewModelScope.launch {
            lives.intValue = 3
            score.intValue = 0
            resetLevel()
            timerViewModel.resetTimer()
            timerViewModel.startTimer()
        }
    }

    private fun resetLevel() {
        playerPositionX.floatValue = 0f
        projectiles.value = emptyList()
        alienProjectiles.value = emptyList()
        gameState.value = GameStatus.Playing
        initializeAliens()
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

    private fun gameLoop() = viewModelScope.launch(ioDispatcher) {
        while (isActive && gameState.value == GameStatus.Playing) {
            if (!isReady) {
                delayProvider(100)
                continue
            }
            delayProvider(16)
            
            // Aggiorna proiettili (giocatore e alieni)
            updateProjectiles()

            // Muovi gli alieni
            moveAliens()

            // Gestisci lo sparo alieno
            handleAlienShooting()
            
            // Controlla collisioni e stato del gioco
            checkCollisions()
            checkGameStatus()
        }
    }

    private fun updateProjectiles() {
        projectiles.value = projectiles.value.map {
            it.copy(position = it.position.copy(y = it.position.y - projectileSpeed))
        }.filter { it.position.y > 0 }

        alienProjectiles.value = alienProjectiles.value.map {
            it.copy(position = it.position.copy(y = it.position.y + alienProjectileSpeed))
        }.filter { it.position.y < screenHeightPx }
    }

    private fun moveAliens() {
        var boundaryReached = false
        var nextAliens = aliens.value.map { alien ->
            val newX = alien.position.x + (alienSpeed * alienDirection)
            if (screenWidthPx > 0 && (newX < 0 || newX + alien.size.width > screenWidthPx)) {
                boundaryReached = true
            }
            alien.copy(position = alien.position.copy(x = newX))
        }

        if (boundaryReached) {
            alienDirection *= -1
            nextAliens = nextAliens.map { alien ->
                alien.copy(position = alien.position.copy(y = alien.position.y + 40f))
            }
        }
        aliens.value = nextAliens
    }

    private fun handleAlienShooting() {
        if (aliens.value.isNotEmpty() && Random.nextInt(0, 100) < alienShootProbability) {
            val randomAlien = aliens.value.random()
            val projectileStartPos = Offset(randomAlien.position.x + randomAlien.size.width / 2, randomAlien.position.y + randomAlien.size.height)
            val newProjectile = ProjectileState(position = projectileStartPos, color = Color.Red, size = Size(10f, 30f))
            alienProjectiles.value = alienProjectiles.value + newProjectile
        }
    }

    private fun checkGameStatus() {
        if (aliens.value.isEmpty() && gameState.value == GameStatus.Playing) {
            endGame(GameStatus.Victory)
        }
    }

    private fun endGame(status: GameStatus) {
        if (gameState.value == GameStatus.Playing) {
            gameState.value = status
            timerViewModel.stopTimer()
            viewModelScope.launch {
                appSettingsDataStore.saveScore(playerName.value, score.value)
            }
        }
    }

    private fun handlePlayerGraveHit() {
        lives.intValue--
        if (lives.intValue <= 0) {
            endGame(GameStatus.GameOver)
        } else {
            resetLevel()
        }
    }

    private fun handlePlayerLightHit() {
        lives.intValue--
        if (lives.intValue <= 0) {
            endGame(GameStatus.GameOver)
        }
    }

    private fun checkCollisions() {
        val playerProjectilesToRemove = mutableSetOf<ProjectileState>()
        val alienProjectilesToRemove = mutableSetOf<ProjectileState>()
        val aliensToRemove = mutableSetOf<AlienState>()

        val playerWidthPx = 120f 
        val playerHeightPx = 120f
        val playerOffsetYPx = 250f

        val playerOffsetXPx = screenWidthPx * (playerPositionX.floatValue / (playerMovementBoundsDp * 2))
        val playerRect = Rect(
            left = (screenWidthPx / 2) + playerOffsetXPx - (playerWidthPx / 2),
            top = screenHeightPx - playerOffsetYPx - playerHeightPx,
            right = (screenWidthPx / 2) + playerOffsetXPx + (playerWidthPx / 2),
            bottom = screenHeightPx - playerOffsetYPx
        )

        // Collisione Proiettili Giocatore vs Alieni
        projectiles.value.forEach { projectile ->
            val projectileRect = Rect(projectile.position, projectile.size)
            aliens.value.forEach { alien ->
                val alienRect = Rect(alien.position, alien.size)
                if (projectileRect.overlaps(alienRect)) {
                    playerProjectilesToRemove.add(projectile)
                    aliensToRemove.add(alien)
                    score.intValue += 10
                }
            }
        }

        // Collisione Proiettili Alieni vs Giocatore
        alienProjectiles.value.forEach { projectile ->
            val projectileRect = Rect(projectile.position, projectile.size)
            if (projectileRect.overlaps(playerRect)) {
                alienProjectilesToRemove.add(projectile)
                handlePlayerLightHit()
            }
        }

        // Collisione Alieni vs Giocatore (Morte Grave)
        val alienCollidingWithPlayer = aliens.value.find { alien ->
            val alienRect = Rect(alien.position, alien.size)
            alienRect.overlaps(playerRect)
        }

        if (alienCollidingWithPlayer != null) {
            handlePlayerGraveHit()
            return // Esce per processare il reset del livello
        }

        // Rimuovi gli oggetti colpiti
        if (playerProjectilesToRemove.isNotEmpty() || aliensToRemove.isNotEmpty() || alienProjectilesToRemove.isNotEmpty()) {
            projectiles.value = projectiles.value - playerProjectilesToRemove
            aliens.value = aliens.value - aliensToRemove
            alienProjectiles.value = alienProjectiles.value - alienProjectilesToRemove
        }
    }

    fun onGameEvent(event: GameEvent) {
        if (gameState.value != GameStatus.Playing && event !is GameEvent.Reset && event !is GameEvent.BackToMenu) return

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
        gameLoopJob?.cancel()
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