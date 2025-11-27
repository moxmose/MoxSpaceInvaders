package com.moxmose.moxspaceinvaders.ui

import android.util.Log
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

data class AlienState(
    val position: Offset,
    val size: Size = Size(80f, 80f),
    val color: Color
)
data class ProjectileState(val position: Offset, val color: Color = Color.White, val size: Size = Size(10f, 20f))
data class MotherShipState(
    val position: Offset,
    val size: Size = Size(150f, 70f),
    val speed: Float = 5f
)

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
    val isPlayerInvincible = mutableStateOf(false)

    // --- STATI DEGLI OGGETTI DI GIOCO ---
    val playerPositionX = mutableFloatStateOf(0f)
    private val movementInput = mutableFloatStateOf(0f)
    val projectiles = mutableStateOf<List<ProjectileState>>(emptyList())
    val alienProjectiles = mutableStateOf<List<ProjectileState>>(emptyList())
    val aliens = mutableStateOf<List<AlienState>>(emptyList())
    val motherShip = mutableStateOf<MotherShipState?>(null)

    private val playerSpeed = 15f 
    private val projectileSpeed = 20f
    private val alienProjectileSpeed = 10f
    private var alienDirection = 1f
    private val baseAlienSpeed = 2f
    private var initialAlienCount = 0
    private val alienShootProbability = 4
    private val motherShipSpawnProbability = 200
    private var gameLoopJob: Job? = null
    private var isReady = false
    
    private var lastShotTime = 0L
    private val shotCooldown = 500L
    private val invincibilityDuration = 2500L

    private var screenWidthPx = 0f
    private var screenHeightPx = 0f
    private var playerMovementBoundsDp = 0f
    private var playerSizePx = 0f
    private var playerOffsetYPx = 0f

    init {
        startGame()
    }

    fun updateScreenDimensions(widthPx: Float, heightPx: Float, boundsDp: Float, pSizePx: Float, pOffsetYpx: Float) {
        if (isReady) return
        screenWidthPx = widthPx
        screenHeightPx = heightPx
        playerMovementBoundsDp = boundsDp
        playerSizePx = pSizePx
        playerOffsetYPx = pOffsetYpx
        Log.d("GameViewModel", "Screen dimensions updated: screenWidthPx = $screenWidthPx")
        isReady = true
    }

    private fun startGame() {
        gameLoopJob?.cancel()
        viewModelScope.launch {
            lives.intValue = 3
            score.intValue = 0
            resetLevel(true)
            timerViewModel.resetTimer()
            timerViewModel.startTimer()
            gameLoopJob = gameLoop()
        }
    }

    private fun resetLevel(isNewGame: Boolean = false) {
        if(!isNewGame) isReady = false
        playerPositionX.floatValue = 0f
        movementInput.floatValue = 0f
        projectiles.value = emptyList()
        alienProjectiles.value = emptyList()
        motherShip.value = null
        gameState.value = GameStatus.Playing
        isPlayerInvincible.value = false
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
        initialAlienCount = newAliens.size
    }

    private fun gameLoop() = viewModelScope.launch(ioDispatcher) {
        while (!isReady) {
            delay(100) 
        }

        while (isActive && gameState.value == GameStatus.Playing) {
            delay(16)
            
            movePlayer()
            updateProjectiles()
            moveAliens()
            handleMotherShipLogic()
            handleAlienShooting()
            checkCollisions()
            checkGameStatus()
        }
    }

    private fun movePlayer() {
        if (movementInput.floatValue != 0f) {
            val newPosition = playerPositionX.floatValue + (movementInput.floatValue * playerSpeed)
            playerPositionX.floatValue = newPosition.coerceIn(-playerMovementBoundsDp, playerMovementBoundsDp)
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
        if (aliens.value.isEmpty()) return
        val aliensDestroyed = initialAlienCount - aliens.value.size
        val speedMultiplier = 1 + (aliensDestroyed.toFloat() / initialAlienCount.toFloat()) * 2
        val currentAlienSpeed = baseAlienSpeed * speedMultiplier

        var boundaryReached = false
        var nextAliens = aliens.value.map { alien ->
            val newX = alien.position.x + (currentAlienSpeed * alienDirection)
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

    private fun handleMotherShipLogic() {
        if (screenWidthPx == 0f) return

        if (motherShip.value == null) {
            val randomValue = Random.nextInt(0, 1000)
            if (randomValue < motherShipSpawnProbability) {
                Log.d("MotherShip", "Spawning MotherShip! Rolled $randomValue")
                motherShip.value = MotherShipState(position = Offset(-150f, 80f))
            }
        } else {
            val newPosition = motherShip.value!!.position.x + motherShip.value!!.speed
            if (newPosition > screenWidthPx) {
                Log.d("MotherShip", "Despawning MotherShip off-screen.")
                motherShip.value = null
            } else {
                motherShip.value = motherShip.value!!.copy(position = Offset(newPosition, motherShip.value!!.position.y))
            }
        }
    }

    private fun triggerInvincibility() {
        viewModelScope.launch {
            isPlayerInvincible.value = true
            delay(invincibilityDuration)
            isPlayerInvincible.value = false
        }
    }

    private fun checkCollisions() {
        if (isPlayerInvincible.value) return // Salta tutti i controlli di collisione del giocatore

        val playerProjectilesToRemove = mutableSetOf<ProjectileState>()
        val alienProjectilesToRemove = mutableSetOf<ProjectileState>()
        val aliensToRemove = mutableSetOf<AlienState>()

        val playerOffsetXPx = screenWidthPx * (playerPositionX.floatValue / (playerMovementBoundsDp * 2))
        val playerRect = Rect(
            left = (screenWidthPx / 2) + playerOffsetXPx - (playerSizePx / 2),
            top = screenHeightPx - playerOffsetYPx - playerSizePx,
            right = (screenWidthPx / 2) + playerOffsetXPx + (playerSizePx / 2),
            bottom = screenHeightPx - playerOffsetYPx
        )

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

            motherShip.value?.let {
                val motherShipRect = Rect(it.position, it.size)
                if (projectileRect.overlaps(motherShipRect)) {
                    playerProjectilesToRemove.add(projectile)
                    motherShip.value = null
                    score.intValue += 100
                }
            }
        }

        alienProjectiles.value.forEach { projectile ->
            val projectileRect = Rect(projectile.position, projectile.size)
            if (projectileRect.overlaps(playerRect)) {
                alienProjectilesToRemove.add(projectile)
                handlePlayerLightHit()
            }
        }

        val alienCollidingWithPlayer = aliens.value.find { alien ->
            val alienRect = Rect(alien.position, alien.size)
            alienRect.overlaps(playerRect)
        }

        if (alienCollidingWithPlayer != null) {
            handlePlayerGraveHit()
            return
        }

        if (playerProjectilesToRemove.isNotEmpty() || aliensToRemove.isNotEmpty() || alienProjectilesToRemove.isNotEmpty()) {
            projectiles.value = projectiles.value - playerProjectilesToRemove
            aliens.value = aliens.value - aliensToRemove
            alienProjectiles.value = alienProjectiles.value - alienProjectilesToRemove
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
        if (isPlayerInvincible.value) return
        lives.intValue--
        triggerInvincibility()
        if (lives.intValue <= 0) {
            endGame(GameStatus.GameOver)
        } else {
            resetLevel()
        }
    }

    private fun handlePlayerLightHit() {
        if (isPlayerInvincible.value) return
        lives.intValue--
        triggerInvincibility()
        if (lives.intValue <= 0) {
            endGame(GameStatus.GameOver)
        }
    }

    fun onGameEvent(event: GameEvent) {
        if (gameState.value != GameStatus.Playing && event !is GameEvent.Reset && event !is GameEvent.BackToMenu) return

        when (event) {
            is GameEvent.UpdateMovement -> {
                movementInput.floatValue = event.direction
            }
            is GameEvent.PlayerShoot -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShotTime > shotCooldown) {
                    val newProjectile = ProjectileState(position = event.startPositionPx)
                    projectiles.value = projectiles.value + newProjectile
                    lastShotTime = currentTime
                }
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
    data class UpdateMovement(val direction: Float) : GameEvent()
    data class PlayerShoot(val startPositionPx: Offset) : GameEvent()
    object Pause : GameEvent()
    object Reset : GameEvent()
    object BackToMenu : GameEvent()
}