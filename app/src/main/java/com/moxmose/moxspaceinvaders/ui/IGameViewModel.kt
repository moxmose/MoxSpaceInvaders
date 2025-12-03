package com.moxmose.moxspaceinvaders.ui

import androidx.compose.runtime.State
import com.moxmose.moxspaceinvaders.model.SoundEvent
import kotlinx.coroutines.flow.StateFlow

interface IGameViewModel {
    val playerName: StateFlow<String>
    val score: State<Int>
    val lives: State<Int>
    val currentTime: StateFlow<Long>
    val gameState: State<GameStatus>
    val isPlayerInvincible: State<Boolean>
    val selectedBackgrounds: StateFlow<Set<String>>

    fun onGameEvent(event: GameEvent)
}
