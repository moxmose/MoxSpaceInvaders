package com.moxmose.moxspaceinvaders.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moxmose.moxspaceinvaders.R
import com.moxmose.moxspaceinvaders.data.local.FakeAppSettingsDataStore
import com.moxmose.moxspaceinvaders.ui.GameViewModel
import com.moxmose.moxspaceinvaders.ui.SoundUtils
import com.moxmose.moxspaceinvaders.ui.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: GameViewModel
    private lateinit var fakeDataStore: FakeAppSettingsDataStore
    private lateinit var navController: TestNavHostController
    private lateinit var soundUtils: SoundUtils
    private lateinit var fakeTimerViewModel: TimerViewModel

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeDataStore = FakeAppSettingsDataStore()
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        fakeTimerViewModel = TimerViewModel()
        soundUtils = SoundUtils(
            context = ApplicationProvider.getApplicationContext(),
            appSettingsDataStore = fakeDataStore,
            externalScope = CoroutineScope(Dispatchers.IO)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GameViewModel {
        return GameViewModel(
            navController = navController,
            timerViewModel = fakeTimerViewModel,
            appSettingsDataStore = fakeDataStore,
            soundUtils = soundUtils,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun gameScreen_displaysInitialState() = runTest {
        // Arrange
        viewModel = createViewModel()
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            GameScreen(innerPadding = PaddingValues(0.dp), gameViewModel = viewModel)
        }

        // Act
        composeTestRule.mainClock.advanceTimeByFrame()

        // Assert
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scoreText = context.getString(R.string.game_head_score, 0)

        composeTestRule.onNodeWithText(scoreText).assertIsDisplayed()
        composeTestRule.onNodeWithText("x3").assertIsDisplayed() // Initial lives
    }
}