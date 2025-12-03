package com.moxmose.moxspaceinvaders.model

import androidx.annotation.RawRes
import com.moxmose.moxspaceinvaders.R

/**
 * Represents all possible sound events that can occur during the game.
 * Each event holds a direct reference to its corresponding raw sound resource ID.
 * This keeps the sound mapping logic co-located with the event definition.
 */
sealed class SoundEvent(@RawRes val resId: Int) {
    object PlayerLaser : SoundEvent(R.raw.fx_player_laser_shot)
    object PlayerExplosion : SoundEvent(R.raw.fx_player_ship_explosion)
    object EnemyLaser : SoundEvent(R.raw.fx_enemy_laser_shot)
    object EnemyExplosion : SoundEvent(R.raw.fx_enemy_explosion)
    object MotherShipExplosion : SoundEvent(R.raw.fx_enemy_mo_explosion)

    companion object {
        // A list of all sound events, used to preload them into the SoundPool.
        val allEvents = listOf(
            PlayerLaser, PlayerExplosion, EnemyLaser, EnemyExplosion, MotherShipExplosion
        )
    }
}
