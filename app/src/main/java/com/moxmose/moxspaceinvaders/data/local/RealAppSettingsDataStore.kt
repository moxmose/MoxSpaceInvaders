package com.moxmose.moxspaceinvaders.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moxmose.moxspaceinvaders.model.ScoreEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

class RealAppSettingsDataStore(
    private val dataStore: DataStore<Preferences>,
    private val externalScope: CoroutineScope
) : IAppSettingsDataStore {

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val SELECTED_BACKGROUNDS = stringSetPreferencesKey("selected_backgrounds")
        val SELECTED_CARDS = stringSetPreferencesKey("selected_cards")
        val SELECTED_BOARD_WIDTH = intPreferencesKey("selected_board_width")
        val SELECTED_BOARD_HEIGHT = intPreferencesKey("selected_board_height")
        val TOP_RANKING = stringPreferencesKey("top_ranking")
        val LAST_PLAYED_ENTRY = stringPreferencesKey("last_played_entry")
        val IS_FIRST_TIME_LAUNCH = booleanPreferencesKey("is_first_time_launch")

        val IS_MUSIC_ENABLED = booleanPreferencesKey("is_music_enabled")
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SELECTED_MUSIC_TRACK_NAMES = stringSetPreferencesKey("selected_music_track_names")

        val ARE_SOUND_EFFECTS_ENABLED = booleanPreferencesKey("are_sound_effects_enabled")
        val SOUND_EFFECTS_VOLUME = floatPreferencesKey("sound_effects_volume")
    }

    private val flow = dataStore.data
        .catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }

    override val isDataLoaded: StateFlow<Boolean> = flow.map { true }.stateIn(externalScope, SharingStarted.Eagerly, false)

    override val playerName: StateFlow<String> = flow.map { it[Keys.PLAYER_NAME] ?: IAppSettingsDataStore.DEFAULT_PLAYER_NAME }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_PLAYER_NAME)
    override val selectedBackgrounds: StateFlow<Set<String>> = flow.map { it[Keys.SELECTED_BACKGROUNDS] ?: IAppSettingsDataStore.DEFAULT_SELECTED_BACKGROUNDS }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_SELECTED_BACKGROUNDS)
    override val selectedCards: StateFlow<Set<String>> = flow.map { it[Keys.SELECTED_CARDS] ?: IAppSettingsDataStore.DEFAULT_SELECTED_CARDS }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_SELECTED_CARDS)
    override val selectedBoardWidth: StateFlow<Int> = flow.map { it[Keys.SELECTED_BOARD_WIDTH] ?: IAppSettingsDataStore.DEFAULT_BOARD_WIDTH }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_BOARD_WIDTH)
    override val selectedBoardHeight: StateFlow<Int> = flow.map { it[Keys.SELECTED_BOARD_HEIGHT] ?: IAppSettingsDataStore.DEFAULT_BOARD_HEIGHT }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_BOARD_HEIGHT)
    override val topRanking: StateFlow<List<ScoreEntry>> = flow.map { it[Keys.TOP_RANKING]?.let { json -> Json.decodeFromString<List<ScoreEntry>>(json) } ?: emptyList() }.stateIn(externalScope, SharingStarted.Eagerly, emptyList())
    override val lastPlayedEntry: StateFlow<ScoreEntry?> = flow.map { it[Keys.LAST_PLAYED_ENTRY]?.let { json -> Json.decodeFromString<ScoreEntry?>(json) } }.stateIn(externalScope, SharingStarted.Eagerly, null)
    override val isFirstTimeLaunch: StateFlow<Boolean> = flow.map { it[Keys.IS_FIRST_TIME_LAUNCH] ?: true }.stateIn(externalScope, SharingStarted.Eagerly, true)

    override val isMusicEnabled: StateFlow<Boolean> = flow.map { it[Keys.IS_MUSIC_ENABLED] ?: IAppSettingsDataStore.DEFAULT_IS_MUSIC_ENABLED }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_IS_MUSIC_ENABLED)
    override val musicVolume: StateFlow<Float> = flow.map { it[Keys.MUSIC_VOLUME] ?: IAppSettingsDataStore.DEFAULT_MUSIC_VOLUME }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_MUSIC_VOLUME)
    override val selectedMusicTrackNames: StateFlow<Set<String>> = flow.map { it[Keys.SELECTED_MUSIC_TRACK_NAMES] ?: IAppSettingsDataStore.DEFAULT_MUSIC_TRACKS }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_MUSIC_TRACKS)

    override val areSoundEffectsEnabled: StateFlow<Boolean> = flow.map { it[Keys.ARE_SOUND_EFFECTS_ENABLED] ?: IAppSettingsDataStore.DEFAULT_ARE_SOUND_EFFECTS_ENABLED }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_ARE_SOUND_EFFECTS_ENABLED)
    override val soundEffectsVolume: StateFlow<Float> = flow.map { it[Keys.SOUND_EFFECTS_VOLUME] ?: IAppSettingsDataStore.DEFAULT_SOUND_EFFECTS_VOLUME }.stateIn(externalScope, SharingStarted.Eagerly, IAppSettingsDataStore.DEFAULT_SOUND_EFFECTS_VOLUME)

    override suspend fun savePlayerName(name: String) {
        dataStore.edit { it[Keys.PLAYER_NAME] = name }
    }

    override suspend fun saveSelectedBackgrounds(backgrounds: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_BACKGROUNDS] = backgrounds }
    }

    override suspend fun saveSelectedCards(cards: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_CARDS] = cards }
    }

    override suspend fun saveBoardDimensions(width: Int, height: Int) {
        dataStore.edit {
            it[Keys.SELECTED_BOARD_WIDTH] = width
            it[Keys.SELECTED_BOARD_HEIGHT] = height
        }
    }

    override suspend fun saveScore(playerName: String, score: Int) {
        val newEntry = ScoreEntry(playerName, score, System.currentTimeMillis())
        val currentRanking = topRanking.value
        val updatedRanking = (currentRanking + newEntry).sortedByDescending { it.score }.take(10)
        dataStore.edit {
            it[Keys.TOP_RANKING] = Json.encodeToString(updatedRanking)
            it[Keys.LAST_PLAYED_ENTRY] = Json.encodeToString(newEntry)
        }
    }

    override suspend fun saveIsFirstTimeLaunch(isFirstTime: Boolean) {
        dataStore.edit { it[Keys.IS_FIRST_TIME_LAUNCH] = isFirstTime }
    }

    override suspend fun saveIsMusicEnabled(isEnabled: Boolean) {
        dataStore.edit { it[Keys.IS_MUSIC_ENABLED] = isEnabled }
    }

    override suspend fun saveMusicVolume(volume: Float) {
        dataStore.edit { it[Keys.MUSIC_VOLUME] = volume }
    }

    override suspend fun saveSelectedMusicTracks(trackNames: Set<String>) {
        dataStore.edit { it[Keys.SELECTED_MUSIC_TRACK_NAMES] = trackNames }
    }

    override suspend fun saveAreSoundEffectsEnabled(isEnabled: Boolean) {
        dataStore.edit { it[Keys.ARE_SOUND_EFFECTS_ENABLED] = isEnabled }
    }

    override suspend fun saveSoundEffectsVolume(volume: Float) {
        dataStore.edit { it[Keys.SOUND_EFFECTS_VOLUME] = volume }
    }
}