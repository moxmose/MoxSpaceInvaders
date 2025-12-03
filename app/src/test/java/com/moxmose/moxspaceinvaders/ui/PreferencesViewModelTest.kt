package com.moxmose.moxspaceinvaders.ui

import android.os.Build
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.moxmose.moxspaceinvaders.data.local.FakeAppSettingsDataStore
import com.moxmose.moxspaceinvaders.model.BackgroundMusic
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PreferencesViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: PreferencesViewModel
    private lateinit var fakeDataStore: FakeAppSettingsDataStore
    private lateinit var testNavController: TestNavHostController
    private lateinit var mockMusicManager: BackgroundMusicManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeDataStore = FakeAppSettingsDataStore()
        testNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
        testNavController.navigatorProvider.addNavigator(ComposeNavigator())
        testNavController.graph = testNavController.createGraph(startDestination = "preferences") {
            composable("preferences") { }
            composable(Screen.OpeningMenuScreen.route) { }
        }

        mockMusicManager = mock(BackgroundMusicManager::class.java)
    }

    private fun initViewModel() {
        viewModel = PreferencesViewModel(
            navController = testNavController,
            appSettingsDataStore = fakeDataStore,
            backgroundMusicManager = mockMusicManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun updatePlayerName_whenNameIsValid_savesToDataStore() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialName = "Player1"
        fakeDataStore.savePlayerName(initialName)
        advanceUntilIdle()

        // 2. Act
        val newName = "NewPlayer"
        viewModel.updatePlayerName(newName)
        advanceUntilIdle()

        // 3. Assert
        assertThat(fakeDataStore.playerName.value).isEqualTo(newName)
    }

    @Test
    fun updatePlayerName_whenNameIsTooLong_isIgnored() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialName = "Player1"
        fakeDataStore.savePlayerName(initialName)
        advanceUntilIdle()

        // 2. Act
        val longName = "a".repeat(PreferencesViewModel.PLAYERNAME_MAX_LENGTH + 1)
        viewModel.updatePlayerName(longName)
        advanceUntilIdle()

        // 3. Assert
        assertThat(fakeDataStore.playerName.value).isEqualTo(initialName)
    }

    @Test
    fun confirmPlayerShipSelection_savesToDataStore() = runTest(testDispatcher) {
        initViewModel()
        advanceUntilIdle()
        val newShip = "astro_pl_2"
        viewModel.updatePlayerShipSelection(newShip)
        viewModel.confirmPlayerShipSelection()
        advanceUntilIdle()
        assertThat(fakeDataStore.playerShip.value).isEqualTo(newShip)
    }

    @Test
    fun confirmEnemyShipSelection_savesToDataStore() = runTest(testDispatcher) {
        initViewModel()
        advanceUntilIdle()
        val newShip = "astro_al_3"
        viewModel.updateEnemyShipSelection(newShip)
        viewModel.confirmEnemyShipSelection()
        advanceUntilIdle()
        assertThat(fakeDataStore.enemyShip.value).isEqualTo(newShip)
    }

    @Test
    fun confirmMotherShipSelection_savesToDataStore() = runTest(testDispatcher) {
        initViewModel()
        advanceUntilIdle()
        val newShip = "astro_mo_2"
        viewModel.updateMotherShipSelection(newShip)
        viewModel.confirmMotherShipSelection()
        advanceUntilIdle()
        assertThat(fakeDataStore.motherShip.value).isEqualTo(newShip)
    }

    @Test
    fun saveIsMusicEnabled_savesToDataStore() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        val initialValue = fakeDataStore.isMusicEnabled.value
        val newValue = !initialValue

        // Act
        viewModel.saveIsMusicEnabled(newValue)
        advanceUntilIdle()

        // Assert
        assertThat(fakeDataStore.isMusicEnabled.value).isEqualTo(newValue)
    }

    @Test
    fun saveMusicVolume_savesToDataStore() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        val newVolume = 0.75f

        // Act
        viewModel.saveMusicVolume(newVolume)
        advanceUntilIdle()

        // Assert
        assertThat(fakeDataStore.musicVolume.value).isEqualTo(newVolume)
    }

    @Test
    fun saveSelectedMusicTracks_savesToDataStore() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        val newTracks = setOf("track1", "track2")

        // Act
        viewModel.saveSelectedMusicTracks(newTracks)
        advanceUntilIdle()

        // Assert
        assertThat(fakeDataStore.selectedMusicTrackNames.value).isEqualTo(newTracks)
    }

    @Test
    fun saveAreSoundEffectsEnabled_savesToDataStore() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        val initialValue = fakeDataStore.areSoundEffectsEnabled.value
        val newValue = !initialValue

        // Act
        viewModel.saveAreSoundEffectsEnabled(newValue)
        advanceUntilIdle()

        // Assert
        assertThat(fakeDataStore.areSoundEffectsEnabled.value).isEqualTo(newValue)
    }

    @Test
    fun saveSoundEffectsVolume_savesToDataStore() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        val newVolume = 0.88f

        // Act
        viewModel.saveSoundEffectsVolume(newVolume)
        advanceUntilIdle()

        // Assert
        assertThat(fakeDataStore.soundEffectsVolume.value).isEqualTo(newVolume)
    }

    @Test
    fun onBackToMainMenuClicked_waitsForSaveJobs_thenNavigates() = runTest(testDispatcher) {
        // Arrange
        initViewModel()
        fakeDataStore.setSaveDelay(2000) // Mock a long save operation
        testNavController.setCurrentDestination("preferences")

        // Act
        viewModel.confirmBackgroundSelections() // This will start a long-running save job
        viewModel.onBackToMainMenuClicked()

        // Assert
        // Immediately after calling, we should still be on the same screen because the save is in progress
        assertThat(testNavController.currentDestination?.route).isEqualTo("preferences")

        // Advance time to allow the save to complete
        advanceUntilIdle()

        // Now, we expect navigation to have occurred
        assertThat(testNavController.currentDestination?.route).isNotEqualTo("preferences")
    }

    @Test
    fun toggleSelectAllBackgrounds_whenDeselectingAll_fallsBackToFirstSelected() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialSelection = setOf("background_02", "background_04", "background_00")
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        advanceUntilIdle()

        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(initialSelection)

        // 2. Act
        viewModel.prepareForBackgroundSelection()
        viewModel.toggleSelectAllBackgrounds(selectAll = false)

        // 3. Assert
        val expectedFallback = "background_00"
        assertThat(viewModel.selectedBackgrounds.value).containsExactly(expectedFallback)
    }

    @Test
    fun toggleSelectAllBackgrounds_whenSelectingAll_selectsAll() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialSelection = setOf("background_00")
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        advanceUntilIdle()

        // 2. Act
        viewModel.toggleSelectAllBackgrounds(selectAll = true)

        // 3. Assert
        val allBackgrounds = viewModel.availableBackgrounds.toSet()
        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(allBackgrounds)
    }

    @Test
    fun updateBackgroundSelection_whenDeselectingLastOne_isIgnored() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialSelection = setOf("background_01")
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        advanceUntilIdle()

        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(initialSelection)

        // 2. Act
        viewModel.updateBackgroundSelection("background_01", isSelected = false)

        // 3. Assert
        // The selection should not have changed because it was the last one
        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(initialSelection)
    }

    @Test
    fun confirmBackgroundSelections_savesToDataStore() = runTest(testDispatcher) {
        // 1. Arrange
        initViewModel()
        val initialSelection = setOf("background_00")
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        advanceUntilIdle()

        assertThat(fakeDataStore.selectedBackgrounds.value).isEqualTo(initialSelection)
        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(initialSelection)

        // 2. Act
        val newSelection = setOf("background_02", "background_05")
        // First add, then remove, to avoid deselecting the last one
        viewModel.updateBackgroundSelection("background_02", true)
        viewModel.updateBackgroundSelection("background_05", true)
        viewModel.updateBackgroundSelection("background_00", false)

        assertThat(viewModel.selectedBackgrounds.value).isEqualTo(newSelection)
    }

    // --- Interaction tests with mocked BackgroundMusicManager ---

    @Test
    fun playMusicPreview_callsManager() = runTest(testDispatcher) {
        initViewModel()
        val mockTrack = mock(BackgroundMusic::class.java)
        viewModel.playMusicPreview(mockTrack)
        verify(mockMusicManager).playPreview(mockTrack)
    }

    @Test
    fun stopMusicPreview_callsManager() = runTest(testDispatcher) {
        initViewModel()
        viewModel.stopMusicPreview()
        verify(mockMusicManager).stopPreview()
    }

}
