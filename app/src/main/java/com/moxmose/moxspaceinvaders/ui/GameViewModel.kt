package com.moxmose.moxspaceinvaders.ui

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
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

// Placeholder per gli stati degli oggetti di gioco
data class PlayerState(val position: Offset)
data class AlienState(val position: Offset, val color: Color)
data class ProjectileState(val position: Offset, val color: Color = Color.White, val size: Size = Size(10f, 20f))

class GameViewModel(
    private val navController: NavHostController,
    private val timerViewModel: TimerViewModel,
    private val appSettingsDataStore: IAppSettingsDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) : ViewModel() {

    // --- STATI DI GIOCO ESSENZIALI ---
    val playerName: StateFlow<String> = appSettingsDataStore.playerName
    val selectedBackgrounds: StateFlow<Set<String>> = appSettingsDataStore.selectedBackgrounds
    val score = mutableIntStateOf(0)
    val currentTime = timerViewModel.elapsedSeconds

    // --- STATI DEGLI OGGETTI DI GIOCO ---
    val playerPositionX = mutableFloatStateOf(0f) // Offset orizzontale dal centro in DP
    val projectiles = mutableStateOf<List<ProjectileState>>(emptyList())

    private val playerSpeed = 15f // in DP
    private val projectileSpeed = 20f // in PX

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

            timerViewModel.resetTimer()
            timerViewModel.startTimer()

            gameLoop()
        }
    }

    private fun gameLoop() {
        viewModelScope.launch(ioDispatcher) {
            while (isActive) {
                val updatedProjectiles = projectiles.value.map {
                    it.copy(position = it.position.copy(y = it.position.y - projectileSpeed))
                }.filter { it.position.y > 0 }

                projectiles.value = updatedProjectiles

                delayProvider(16)
            }
        }
    }

    fun onGameEvent(event: GameEvent) {
        when (event) {
            is GameEvent.MovePlayer -> {
                val newPosition = playerPositionX.floatValue + (event.direction * playerSpeed)
                // Usa i limiti corretti in DP forniti dalla UI
                playerPositionX.floatValue = newPosition.coerceIn(-playerMovementBoundsDp, playerMovementBoundsDp)
            }
            GameEvent.PlayerShoot -> {
                // Usa la larghezza dello schermo in PX per un calcolo corretto
                // Devo convertire il mio offset in DP in un offset in PX
                // Questa è un'approssimazione che funziona perché il centro è sempre il centro
                val playerOffsetPx = screenWidthPx * (playerPositionX.floatValue / (playerMovementBoundsDp * 2))

                val newProjectile = ProjectileState(
                    position = Offset(
                        x = (screenWidthPx / 2) + playerOffsetPx - 5f, // 5f è metà larghezza proiettile
                        y = screenHeightPx - 300f // Posizione Y di partenza sopra la nave
                    )
                )
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
    object PlayerShoot : GameEvent()
    object Pause : GameEvent()
    object Reset : GameEvent()
    object BackToMenu : GameEvent()
}