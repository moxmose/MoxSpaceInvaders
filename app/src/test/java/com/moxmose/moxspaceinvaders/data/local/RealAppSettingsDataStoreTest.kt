package com.moxmose.moxspaceinvaders.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.moxmose.moxspaceinvaders.model.ScoreEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RealAppSettingsDataStoreTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var testScope: TestScope
    private lateinit var testContext: Context
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var appSettingsDataStore: RealAppSettingsDataStore

    private val testDatastoreName = "test_datastore"

    private object Keys {
        val PLAYER_NAME = stringPreferencesKey("player_name")
        val PLAYER_SHIP = stringPreferencesKey("player_ship")
        val ENEMY_SHIP = stringPreferencesKey("enemy_ship")
        val MOTHER_SHIP = stringPreferencesKey("mother_ship")
        val SELECTED_BACKGROUNDS = stringSetPreferencesKey("selected_backgrounds")
        val IS_FIRST_TIME_LAUNCH = booleanPreferencesKey("is_first_time_launch")
        val TOP_RANKING = stringPreferencesKey("top_ranking")
        val LAST_PLAYED_ENTRY = stringPreferencesKey("last_played_entry")
    }

    @Before
    fun setup() {
        testScope = TestScope(testDispatcher + Job())
        testContext = ApplicationProvider.getApplicationContext()
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testContext.preferencesDataStoreFile(testDatastoreName) }
        )
        appSettingsDataStore = RealAppSettingsDataStore(testDataStore, testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        stopKoin()
        val datastoreFile = testContext.filesDir.resolve("datastore/$testDatastoreName.preferences_pb")
        if (datastoreFile.exists()) {
            datastoreFile.delete()
        }
    }

    @Test
    fun savePlayerName_savesNameCorrectly() = testScope.runTest {
        val playerName = "TestPlayer"
        appSettingsDataStore.savePlayerName(playerName)
        advanceUntilIdle()
        val savedName = testDataStore.data.map { it[Keys.PLAYER_NAME] }.first()
        assertThat(savedName).isEqualTo(playerName)
    }

    @Test
    fun savePlayerShip_savesShipCorrectly() = testScope.runTest {
        val ship = "astro_pl_2"
        appSettingsDataStore.savePlayerShip(ship)
        advanceUntilIdle()
        val savedShip = testDataStore.data.map { it[Keys.PLAYER_SHIP] }.first()
        assertThat(savedShip).isEqualTo(ship)
    }

    @Test
    fun saveEnemyShip_savesShipCorrectly() = testScope.runTest {
        val ship = "astro_al_3"
        appSettingsDataStore.saveEnemyShip(ship)
        advanceUntilIdle()
        val savedShip = testDataStore.data.map { it[Keys.ENEMY_SHIP] }.first()
        assertThat(savedShip).isEqualTo(ship)
    }

    @Test
    fun saveMotherShip_savesShipCorrectly() = testScope.runTest {
        val ship = "astro_mo_1"
        appSettingsDataStore.saveMotherShip(ship)
        advanceUntilIdle()
        val savedShip = testDataStore.data.map { it[Keys.MOTHER_SHIP] }.first()
        assertThat(savedShip).isEqualTo(ship)
    }

    @Test
    fun saveSelectedBackgrounds_savesBackgroundSetCorrectly() = testScope.runTest {
        val backgrounds = setOf("background_01", "background_03")
        appSettingsDataStore.saveSelectedBackgrounds(backgrounds)
        advanceUntilIdle()
        val savedBackgrounds = testDataStore.data.map { it[Keys.SELECTED_BACKGROUNDS] }.first()
        assertThat(savedBackgrounds).isEqualTo(backgrounds)
    }

    @Test
    fun saveIsFirstTimeLaunch_savesBooleanFlagCorrectly() = testScope.runTest {
        val isFirstTime = false
        appSettingsDataStore.saveIsFirstTimeLaunch(isFirstTime)
        advanceUntilIdle()
        val savedFlag = testDataStore.data.map { it[Keys.IS_FIRST_TIME_LAUNCH] }.first()
        assertThat(savedFlag).isEqualTo(isFirstTime)
    }

    @Test
    fun saveScore_savesLastEntryAndUpdatesRanking() = testScope.runTest {
        val player1 = "Player1"
        val score1 = 100
        val player2 = "Player2"
        val score2 = 200

        appSettingsDataStore.saveScore(player1, score1)
        advanceUntilIdle()

        val lastEntryJson1 = testDataStore.data.map { it[Keys.LAST_PLAYED_ENTRY] }.first()
        val rankingJson1 = testDataStore.data.map { it[Keys.TOP_RANKING] }.first()
        val lastEntry1 = Json.decodeFromString<ScoreEntry>(lastEntryJson1!!)
        val ranking1 = Json.decodeFromString<List<ScoreEntry>>(rankingJson1!!)

        assertThat(lastEntry1.playerName).isEqualTo(player1)
        assertThat(lastEntry1.score).isEqualTo(score1)
        assertThat(ranking1).hasSize(1)

        appSettingsDataStore.saveScore(player2, score2)
        advanceUntilIdle()

        val lastEntryJson2 = testDataStore.data.map { it[Keys.LAST_PLAYED_ENTRY] }.first()
        val rankingJson2 = testDataStore.data.map { it[Keys.TOP_RANKING] }.first()
        val lastEntry2 = Json.decodeFromString<ScoreEntry>(lastEntryJson2!!)
        val ranking2 = Json.decodeFromString<List<ScoreEntry>>(rankingJson2!!)

        assertThat(lastEntry2.playerName).isEqualTo(player2)
        assertThat(lastEntry2.score).isEqualTo(score2)
        assertThat(ranking2).hasSize(2)
        assertThat(ranking2.first().score).isEqualTo(score2)
    }
}
