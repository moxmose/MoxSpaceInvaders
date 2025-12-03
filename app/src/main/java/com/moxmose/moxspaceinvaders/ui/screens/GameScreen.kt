package com.moxmose.moxspaceinvaders.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moxmose.moxspaceinvaders.R
import com.moxmose.moxspaceinvaders.ui.GameEvent
import com.moxmose.moxspaceinvaders.ui.GameStatus
import com.moxmose.moxspaceinvaders.ui.GameViewModel
import com.moxmose.moxspaceinvaders.ui.composables.BackgroundImg
import com.moxmose.moxspaceinvaders.ui.formatDuration
import org.koin.androidx.compose.koinViewModel

@Composable
fun GameScreen(
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = koinViewModel(),
) {
    val score by gameViewModel.score
    val lives by gameViewModel.lives
    val timeGame by gameViewModel.currentTime.collectAsState()
    val timeGameString = timeGame.formatDuration()
    val playerPositionX by gameViewModel.playerPositionX
    val projectiles by gameViewModel.projectiles
    val alienProjectiles by gameViewModel.alienProjectiles
    val aliens by gameViewModel.aliens
    val motherShipState by gameViewModel.motherShipState
    val gameState by gameViewModel.gameState
    val isPlayerInvincible by gameViewModel.isPlayerInvincible
    val playerShip by gameViewModel.playerShip.collectAsState()
    val enemyShip by gameViewModel.enemyShip.collectAsState()
    val motherShip by gameViewModel.motherShip.collectAsState()

    val invincibilityAlpha = remember { Animatable(1f) }

    LaunchedEffect(isPlayerInvincible) {
        if (isPlayerInvincible) {
            invincibilityAlpha.animateTo(
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 150),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            invincibilityAlpha.snapTo(1f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        val screenWidthDp = maxWidth
        val playerSizeDp = 48.dp
        val playerOffsetYdp = 100.dp
        val playerMovementBoundsDp = (screenWidthDp / 2) - (playerSizeDp / 2)

        val density = LocalDensity.current
        val screenWidthPx = with(density) { screenWidthDp.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val playerSizePx = with(density) { playerSizeDp.toPx() }
        val playerOffsetYPx = with(density) { playerOffsetYdp.toPx() }

        LaunchedEffect(Unit) {
            gameViewModel.updateScreenDimensions(screenWidthPx, screenHeightPx, playerMovementBoundsDp.value, playerSizePx, playerOffsetYPx)
        }

        BackgroundImg(
            selectedBackgrounds = gameViewModel.selectedBackgrounds,
            modifier = Modifier.fillMaxSize()
        )

        val context = LocalContext.current
        val enemyShipId = remember(enemyShip) {
            context.resources.getIdentifier(enemyShip, "mipmap", context.packageName)
        }
        val motherShipId = remember(motherShip) {
            context.resources.getIdentifier(motherShip, "mipmap", context.packageName)
        }

        // --- DISEGNO DEGLI OGGETTI DI GIOCO ---
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            projectiles.forEach { projectile ->
                drawRect(
                    color = projectile.color,
                    topLeft = projectile.position,
                    size = projectile.size
                )
            }
            alienProjectiles.forEach { projectile ->
                drawRect(
                    color = projectile.color,
                    topLeft = projectile.position,
                    size = projectile.size
                )
            }
        }

        if (enemyShipId != 0) {
            aliens.forEach { alien ->
                val xDp = with(density) { alien.position.x.toDp() }
                val yDp = with(density) { alien.position.y.toDp() }
                val sizeDp = with(density) { alien.size.width.toDp() }

                Box(modifier = Modifier.offset(x = xDp, y = yDp).size(sizeDp)) {
                    Image(
                        painter = painterResource(id = enemyShipId),
                        contentDescription = "Alien",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = enemyShipId),
                        contentDescription = "Alien Color Overlay",
                        colorFilter = ColorFilter.tint(alien.color.copy(alpha = 0.03f), blendMode = BlendMode.SrcAtop),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (motherShipId != 0) {
            motherShipState?.let {
                val xDp = with(density) { it.position.x.toDp() }
                val yDp = with(density) { it.position.y.toDp() }
                val sizeWidthDp = with(density) { it.size.width.toDp() }
                val sizeHeightDp = with(density) { it.size.height.toDp() }

                Image(
                    painter = painterResource(id = motherShipId),
                    contentDescription = "Mother Ship",
                    modifier = Modifier
                        .offset(x = xDp, y = yDp)
                        .size(sizeWidthDp, sizeHeightDp)
                )
            }
        }
        
        // --- UI E CONTROLLI ---

        Column(modifier = Modifier.fillMaxSize()) {
            GameHeader(
                score = score,
                lives = lives,
                time = timeGameString,
                onReset = { gameViewModel.onGameEvent(GameEvent.Reset) },
                onBack = { gameViewModel.onGameEvent(GameEvent.BackToMenu) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- Pulsante Sinistra ---
                Icon(
                    imageVector = Icons.Default.ArrowLeft, 
                    contentDescription = "Move Left", 
                    tint = Color.White, 
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { 
                                    gameViewModel.onGameEvent(GameEvent.UpdateMovement(-1f))
                                    tryAwaitRelease()
                                    gameViewModel.onGameEvent(GameEvent.UpdateMovement(0f))
                                }
                            )
                        }
                )

                IconButton(onClick = {
                    val playerOffsetXpx = with(density) { playerPositionX.dp.toPx() }
                    val projectileStartX = (screenWidthPx / 2) + playerOffsetXpx - 5f
                    val projectileStartY = screenHeightPx - with(density) { 150.dp.toPx() }
                    
                    gameViewModel.onGameEvent(GameEvent.PlayerShoot(Offset(projectileStartX, projectileStartY)))
                }) {
                    Icon(Icons.Default.Star, contentDescription = "Shoot", tint = Color.Yellow, modifier = Modifier.size(64.dp))
                }

                // --- Pulsante Destra ---
                Icon(
                    imageVector = Icons.Default.ArrowRight, 
                    contentDescription = "Move Right", 
                    tint = Color.White, 
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { 
                                    gameViewModel.onGameEvent(GameEvent.UpdateMovement(1f))
                                    tryAwaitRelease()
                                    gameViewModel.onGameEvent(GameEvent.UpdateMovement(0f))
                                }
                            )
                        }
                )
            }
        }

        val playerShipId = remember(playerShip) {
            context.resources.getIdentifier(playerShip, "mipmap", context.packageName)
        }

        if (playerShipId != 0) {
            Image(
                painter = painterResource(id = playerShipId),
                contentDescription = "Player Ship",
                alpha = if(isPlayerInvincible) invincibilityAlpha.value else 1f,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = playerPositionX.dp, y = -playerOffsetYdp)
                    .size(playerSizeDp)
            )
        }
        
        if (gameState != GameStatus.Playing) {
            val message = if (gameState == GameStatus.Victory) "YOU WIN!" else "GAME OVER"
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = message, fontSize = 48.sp, color = Color.Yellow)
            }
        }
    }
}

@Composable
fun GameHeader(
    score: Int,
    lives: Int,
    time: String,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.preferences_button_back_to_main_menu))
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, contentDescription = "Lives", tint = Color.Red, modifier = Modifier.size(24.dp))
            Text(
                text = "x$lives",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Text(
            text = stringResource(R.string.game_head_score, score),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        IconButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.game_tail_button_reset))
        }
    }
}
