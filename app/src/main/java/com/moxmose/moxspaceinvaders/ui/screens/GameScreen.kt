package com.moxmose.moxspaceinvaders.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moxmose.moxspaceinvaders.R
import com.moxmose.moxspaceinvaders.ui.GameEvent
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
    val timeGame by gameViewModel.currentTime.collectAsState()
    val timeGameString = timeGame.formatDuration()
    val playerPositionX by gameViewModel.playerPositionX
    val projectiles by gameViewModel.projectiles

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight
        val playerSizeDp = 48.dp
        val playerMovementBoundsDp = (screenWidthDp / 2) - (playerSizeDp / 2)

        val screenWidthPx = with(LocalDensity.current) { screenWidthDp.toPx() }
        val screenHeightPx = with(LocalDensity.current) { screenHeightDp.toPx() }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            gameViewModel.updateScreenDimensions(screenWidthPx, screenHeightPx, playerMovementBoundsDp.value)
        }

        BackgroundImg(
            selectedBackgrounds = gameViewModel.selectedBackgrounds,
            modifier = Modifier.fillMaxSize()
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            projectiles.forEach { projectile ->
                drawRect(
                    color = projectile.color,
                    topLeft = projectile.position,
                    size = projectile.size
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            GameHeader(
                score = score,
                time = timeGameString,
                onPause = { gameViewModel.onGameEvent(GameEvent.Pause) },
                onReset = { gameViewModel.onGameEvent(GameEvent.Reset) },
                onBack = { gameViewModel.onGameEvent(GameEvent.BackToMenu) }
            )

            // --- AREA DI GIOCO ---
            Spacer(modifier = Modifier.weight(1f))

            // --- CONTROLLI DEL GIOCATORE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { gameViewModel.onGameEvent(GameEvent.MovePlayer(-1f)) }) {
                    Icon(Icons.Default.ArrowLeft, contentDescription = "Move Left", tint = Color.White, modifier = Modifier.size(48.dp))
                }
                IconButton(onClick = { gameViewModel.onGameEvent(GameEvent.PlayerShoot) }) {
                    Icon(Icons.Default.Star, contentDescription = "Shoot", tint = Color.Yellow, modifier = Modifier.size(64.dp))
                }
                IconButton(onClick = { gameViewModel.onGameEvent(GameEvent.MovePlayer(1f)) }) {
                    Icon(Icons.Default.ArrowRight, contentDescription = "Move Right", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            }
        }

        // --- PLAYER ---
        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = "Player Ship",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = playerPositionX.dp, y = (-100).dp)
                .size(playerSizeDp)
        )
    }
}

@Composable
fun GameHeader(
    score: Int,
    time: String,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.preferences_button_back_to_main_menu))
        }
        Text(
            text = stringResource(R.string.game_head_score, score),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.game_head_time, time),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.weight(0.5f)) {
            IconButton(onClick = onPause) {
                Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.game_tail_button_pause))
            }
            IconButton(onClick = onReset) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.game_tail_button_reset))
            }
        }
    }
}
