package com.moxmose.moxspaceinvaders.ui

import android.os.Build
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.moxmose.moxspaceinvaders.data.local.FakeAppSettingsDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GameViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: GameViewModel
    private lateinit var fakeDataStore: FakeAppSettingsDataStore
    private lateinit var testNavController: TestNavHostController
    private lateinit var mockSoundUtils: SoundUtils
    private lateinit var fakeTimerViewModel: TimerViewModel // Assuming a fake or real one is available

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeDataStore = FakeAppSettingsDataStore()
        testNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
        testNavController.navigatorProvider.addNavigator(ComposeNavigator())
        testNavController.graph = testNavController.createGraph(startDestination = "game_screen") {
            composable("game_screen") { }
            composable(Screen.OpeningMenuScreen.route) { }
        }

        mockSoundUtils = Mockito.mock(SoundUtils::class.java)
        fakeTimerViewModel = TimerViewModel() // Or a fake implementation if needed
    }

    private fun initViewModel() {
        viewModel = GameViewModel(
            navController = testNavController,
            timerViewModel = fakeTimerViewModel,
            appSettingsDataStore = fakeDataStore,
            soundUtils = mockSoundUtils,
            ioDispatcher = testDispatcher, // Use the test dispatcher for IO operations
            delayProvider = { } // No delay in tests
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun initialStateIsCorrect() = runTest {
        // Arrange
        initViewModel()
        viewModel.updateScreenDimensions(1080f, 1920f, 540f, 100f, 100f)

        // Act
        // Run only the tasks scheduled for the current virtual time.
        // This executes the ViewModel's init block but prevents the game loop
        // from running and changing the state before assertions.
        runCurrent()

        // Assert
        assertThat(viewModel.lives.intValue).isEqualTo(3)
        assertThat(viewModel.score.intValue).isEqualTo(0)
        assertThat(viewModel.gameState.value).isEqualTo(GameStatus.Playing)
        assertThat(viewModel.aliens.value).isNotEmpty()
    }

    @Test
    fun onGameEvent_BackToMenu_navigatesBack() {
        initViewModel()
        testNavController.setCurrentDestination("game_screen")
        viewModel.onGameEvent(GameEvent.BackToMenu)
        assertThat(testNavController.currentDestination?.route).isNotEqualTo("game_screen")
    }
}
