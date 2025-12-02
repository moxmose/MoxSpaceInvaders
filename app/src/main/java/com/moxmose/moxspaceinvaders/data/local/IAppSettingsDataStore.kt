
package com.moxmose.moxspaceinvaders.data.local

import com.moxmose.moxspaceinvaders.model.ScoreEntry
import kotlinx.coroutines.flow.StateFlow

interface IAppSettingsDataStore {

    val isDataLoaded: StateFlow<Boolean>
    val playerName: StateFlow<String>
    val playerShip: StateFlow<String>
    val enemyShip: StateFlow<String>
    val motherShip: StateFlow<String>
    val selectedBackgrounds: StateFlow<Set<String>>
    val selectedCards: StateFlow<Set<String>>
    val selectedBoardWidth: StateFlow<Int>
    val selectedBoardHeight: StateFlow<Int>
    val topRanking: StateFlow<List<ScoreEntry>>
    val lastPlayedEntry: StateFlow<ScoreEntry?>
    val isFirstTimeLaunch: StateFlow<Boolean>

    val isMusicEnabled: StateFlow<Boolean>
    val musicVolume: StateFlow<Float>
    val selectedMusicTrackNames: StateFlow<Set<String>>

    val areSoundEffectsEnabled: StateFlow<Boolean>
    val soundEffectsVolume: StateFlow<Float>

    suspend fun savePlayerName(name: String)
    suspend fun savePlayerShip(ship: String)
    suspend fun saveEnemyShip(ship: String)
    suspend fun saveMotherShip(ship: String)
    suspend fun saveSelectedBackgrounds(backgrounds: Set<String>)
    suspend fun saveSelectedCards(cards: Set<String>)
    suspend fun saveBoardDimensions(width: Int, height: Int)
    suspend fun saveScore(playerName: String, score: Int)
    suspend fun saveIsFirstTimeLaunch(isFirstTime: Boolean)
    suspend fun saveIsMusicEnabled(isEnabled: Boolean)
    suspend fun saveMusicVolume(volume: Float)
    suspend fun saveSelectedMusicTracks(trackNames: Set<String>)
    suspend fun saveAreSoundEffectsEnabled(isEnabled: Boolean)
    suspend fun saveSoundEffectsVolume(volume: Float)

    companion object {
        const val DEFAULT_PLAYER_NAME = "Player"
        const val DEFAULT_PLAYER_SHIP = "astro_pl_1"
        const val DEFAULT_ENEMY_SHIP = "astro_al_1"
        const val DEFAULT_MOTHER_SHIP = "astro_mo_1"
        val DEFAULT_SELECTED_BACKGROUNDS = setOf("background_00")
        val DEFAULT_SELECTED_CARDS = setOf("img_c_00", "img_c_01", "img_c_02", "img_c_03", "img_c_04", "img_c_05")
        const val DEFAULT_BOARD_WIDTH = 4
        const val DEFAULT_BOARD_HEIGHT = 4
        val DEFAULT_MUSIC_TRACKS: Set<String> = emptySet()
        const val DEFAULT_IS_MUSIC_ENABLED = true
        const val DEFAULT_MUSIC_VOLUME = 0.5f
        const val DEFAULT_ARE_SOUND_EFFECTS_ENABLED = true
        const val DEFAULT_SOUND_EFFECTS_VOLUME = 0.8f
    }
}
