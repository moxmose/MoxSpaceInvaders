package com.moxmose.moxspaceinvaders.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.moxmose.moxspaceinvaders.R
import com.moxmose.moxspaceinvaders.data.local.FakeAppSettingsDataStore
import com.moxmose.moxspaceinvaders.ui.BackgroundMusicManager
import com.moxmose.moxspaceinvaders.ui.PreferencesViewModel
import com.moxmose.moxspaceinvaders.ui.Screen
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class PreferencesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeDataStore: FakeAppSettingsDataStore
    private lateinit var navController: TestNavHostController
    private lateinit var fakeMusicManager: BackgroundMusicManager

    @Before
    fun setup() {
        fakeDataStore = FakeAppSettingsDataStore()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        fakeMusicManager = BackgroundMusicManager(
            context = ApplicationProvider.getApplicationContext(),
            appSettingsDataStore = fakeDataStore,
            externalScope = CoroutineScope(Dispatchers.IO)
        )
    }

    private fun createViewModel() = PreferencesViewModel(
        navController = navController,
        appSettingsDataStore = fakeDataStore,
        backgroundMusicManager = fakeMusicManager
    )

    @Test
    fun backButton_navigatesToOpeningMenu() = runTest {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            navController.setLifecycleOwner(LocalLifecycleOwner.current)
            NavHost(
                navController = navController,
                startDestination = Screen.OpeningMenuScreen.route
            ) {
                composable(Screen.OpeningMenuScreen.route) { Text("Opening Menu Screen") }
                composable(Screen.PreferencesScreen.route) {
                    PreferencesScreen(
                        preferencesViewModel = viewModel,
                        innerPadding = PaddingValues(0.dp)
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(Screen.PreferencesScreen.route)
        }
        composeTestRule.waitForIdle()

        val backButtonText = ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.preferences_button_back_to_main_menu)
        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(backButtonText, ignoreCase = true))
        composeTestRule.onNodeWithText(backButtonText, ignoreCase = true).performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Opening Menu Screen").assertIsDisplayed()
    }

    @Test
    fun playerName_canBeUpdated() = runTest {
        val newName = "Mox"
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val saveButtonText = context.getString(R.string.preferences_button_save_player_name)

        val viewModel = createViewModel()

        composeTestRule.setContent {
            PreferencesScreen(
                preferencesViewModel = viewModel,
                innerPadding = PaddingValues(0.dp)
            )
        }
        composeTestRule.waitForIdle()

        val initialName = viewModel.playerName.value

        composeTestRule.onNodeWithText(initialName).performTextClearance()
        composeTestRule.onNodeWithText("").performTextInput(newName)
        composeTestRule.onNodeWithText(saveButtonText, ignoreCase = true).performClick()

        assertThat(fakeDataStore.playerName.value).isEqualTo(newName)
    }

    @Test
    fun backgroundSelection_canBeUpdated() = runTest {
        val initialSelection = setOf("background_01", "background_03")
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        val viewModel = createViewModel()

        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val selectButtonText = context.getString(R.string.preferences_button_select_backgrounds, initialSelection.size)
        val dialogTitle = context.getString(R.string.background_selection_dialog_title)
        val okButtonText = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(selectButtonText, ignoreCase = true))
        composeTestRule.onNodeWithText(selectButtonText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText("Background 01").performClick() // Deselect
        composeTestRule.onNodeWithText("Background 02").performClick() // Select
        composeTestRule.onNodeWithText(okButtonText, ignoreCase = true).performClick()

        composeTestRule.waitForIdle()

        val expectedSelection = setOf("background_03", "background_02")
        assertThat(fakeDataStore.selectedBackgrounds.value).isEqualTo(expectedSelection)
    }

    @Test
    fun playerShipSelection_canBeUpdated() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val buttonText = context.getString(R.string.preferences_button_select_player_ship)
        val dialogTitle = context.getString(R.string.ship_selection_dialog_title_player)
        val okButtonText = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(buttonText, ignoreCase = true))
        composeTestRule.onNodeWithText(buttonText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Astro pl 2").performClick()
        composeTestRule.onNodeWithText(okButtonText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeDataStore.playerShip.value).isEqualTo("astro_pl_2")
    }

    @Test
    fun enemyShipSelection_canBeUpdated() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val buttonText = context.getString(R.string.preferences_button_select_enemy_ship)
        val dialogTitle = context.getString(R.string.ship_selection_dialog_title_enemy)
        val okButtonText = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(buttonText, ignoreCase = true))
        composeTestRule.onNodeWithText(buttonText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Astro al 3").performClick()
        composeTestRule.onNodeWithText(okButtonText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeDataStore.enemyShip.value).isEqualTo("astro_al_3")
    }

    @Test
    fun motherShipSelection_canBeUpdated() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val buttonText = context.getString(R.string.preferences_button_select_mother_ship)
        val dialogTitle = context.getString(R.string.ship_selection_dialog_title_mother)
        val okButtonText = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(buttonText, ignoreCase = true))
        composeTestRule.onNodeWithText(buttonText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Astro mo 2").performClick()
        composeTestRule.onNodeWithText(okButtonText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeDataStore.motherShip.value).isEqualTo("astro_mo_2")
    }

    @Test
    fun music_canBeToggledAndVolumeChanged() = runTest {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val preferencesListTag = "PreferencesList"
        val musicSwitchTag = "MusicSwitch"
        val musicSliderTag = "MusicVolumeSlider"

        val initialVolume = fakeDataStore.musicVolume.value
        assertThat(fakeDataStore.isMusicEnabled.value).isTrue()

        composeTestRule.onNodeWithTag(preferencesListTag).performScrollToNode(hasTestTag(musicSwitchTag))
        composeTestRule.onNodeWithTag(musicSwitchTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.isMusicEnabled.value).isFalse()

        composeTestRule.onNodeWithTag(preferencesListTag).performScrollToNode(hasTestTag(musicSliderTag))
        composeTestRule.onNodeWithTag(musicSliderTag).assertIsNotEnabled()

        composeTestRule.onNodeWithTag(preferencesListTag).performScrollToNode(hasTestTag(musicSwitchTag))
        composeTestRule.onNodeWithTag(musicSwitchTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.isMusicEnabled.value).isTrue()

        composeTestRule.onNodeWithTag(preferencesListTag).performScrollToNode(hasTestTag(musicSliderTag))
        composeTestRule.onNodeWithTag(musicSliderTag).assertIsEnabled()

        composeTestRule.onNodeWithTag(musicSliderTag).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.musicVolume.value).isGreaterThan(initialVolume)
    }

    @Test
    fun musicSelectionDialog_canSelectNone() = runTest {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val initialCount = viewModel.selectedMusicTrackNames.value.size
        val selectMusicTracksText = context.getString(R.string.preferences_button_select_music_tracks, initialCount)
        val noneButtonText = context.getString(R.string.preferences_music_selection_none)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(selectMusicTracksText, ignoreCase = true))
        composeTestRule.onNodeWithText(selectMusicTracksText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(noneButtonText, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeDataStore.selectedMusicTrackNames.value).isEmpty()
        composeTestRule.onNodeWithText(context.getString(R.string.preferences_music_selection_dialog_title)).assertDoesNotExist()
    }

    @Test
    fun soundEffects_canBeToggledAndVolumeChanged() = runTest {
        val viewModel = createViewModel()
        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val preferencesListTag = "PreferencesList"
        val sfxSwitchTag = "SfxSwitch"
        val sfxSliderTag = "SfxVolumeSlider"
        val listNode = composeTestRule.onNodeWithTag(preferencesListTag)

        val initialVolume = fakeDataStore.soundEffectsVolume.value
        assertThat(fakeDataStore.areSoundEffectsEnabled.value).isTrue()

        listNode.performScrollToNode(hasTestTag(sfxSwitchTag))
        composeTestRule.onNodeWithTag(sfxSwitchTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.areSoundEffectsEnabled.value).isFalse()

        listNode.performScrollToNode(hasTestTag(sfxSliderTag))
        composeTestRule.onNodeWithTag(sfxSliderTag).assertIsNotEnabled()

        listNode.performScrollToNode(hasTestTag(sfxSwitchTag))
        composeTestRule.onNodeWithTag(sfxSwitchTag).performClick()
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.areSoundEffectsEnabled.value).isTrue()

        listNode.performScrollToNode(hasTestTag(sfxSliderTag))
        composeTestRule.onNodeWithTag(sfxSliderTag).assertIsEnabled()

        listNode.performScrollToNode(hasTestTag(sfxSliderTag))
        composeTestRule.onNodeWithTag(sfxSliderTag).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(fakeDataStore.soundEffectsVolume.value).isGreaterThan(initialVolume)
    }

    @Test
    fun backgroundSelection_selectAll_worksAsExpected() = runTest {
        val initialSelection = setOf("background_00") // Start with one
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        val viewModel = createViewModel()
        val allBackgrounds = viewModel.availableBackgrounds.toSet()

        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val selectButtonText = context.getString(R.string.preferences_button_select_backgrounds, initialSelection.size)
        val selectAllButtonText = context.getString(R.string.dialog_select_deselect_all)
        val okButtonText = context.getString(R.string.button_ok)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(selectButtonText, ignoreCase = true))
        composeTestRule.onNodeWithText(selectButtonText, ignoreCase = true).performClick()

        composeTestRule.onNodeWithText(selectAllButtonText).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(okButtonText).performClick()
        composeTestRule.waitForIdle()

        assertThat(fakeDataStore.selectedBackgrounds.value).isEqualTo(allBackgrounds)
    }
    @Test
    fun backgroundSelection_isNotSavedOnBackPress() = runTest {
        val initialSelection = setOf("background_01")
        fakeDataStore.saveSelectedBackgrounds(initialSelection)

        val viewModel = createViewModel()

        composeTestRule.setContent {
            PreferencesScreen(preferencesViewModel = viewModel, innerPadding = PaddingValues(0.dp))
        }
        composeTestRule.waitForIdle()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val selectButtonText = context.getString(R.string.preferences_button_select_backgrounds, initialSelection.size)
        val dialogTitle = context.getString(R.string.background_selection_dialog_title)

        composeTestRule.onNodeWithTag("PreferencesList").performScrollToNode(hasText(selectButtonText, ignoreCase = true))
        composeTestRule.onNodeWithText(selectButtonText, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Background 02").performClick()

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

        assertThat(fakeDataStore.selectedBackgrounds.value).isEqualTo(initialSelection)
    }
}