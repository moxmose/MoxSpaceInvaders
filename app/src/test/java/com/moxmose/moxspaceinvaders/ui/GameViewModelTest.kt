package com.moxmose.moxspaceinvaders.ui

import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.moxmose.moxspaceinvaders.data.local.FakeAppSettingsDataStore
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import com.moxmose.moxspaceinvaders.model.GameCard
import com.moxmose.moxspaceinvaders.model.SoundEvent
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q]) // Specify the SDK to simulate
class GameViewModelTest {

    private lateinit var testDispatcher: TestDispatcher

    private lateinit var viewModel: GameViewModel
    private lateinit var fakeDataStore: FakeAppSettingsDataStore
    private lateinit var testNavController: TestNavHostController
    private lateinit var fakeTimerViewModel: FakeTimerViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeDataStore = FakeAppSettingsDataStore()
        fakeTimerViewModel = FakeTimerViewModel()

        // Use the TestNavHostController
        testNavController = TestNavHostController(ApplicationProvider.getApplicationContext())
        testNavController.navigatorProvider.addNavigator(ComposeNavigator())
        testNavController.graph = testNavController.createGraph(startDestination = "game_screen") {
            composable("game_screen") { }
            composable(Screen.OpeningMenuScreen.route) { }
        }
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = GameViewModel(
            navController = testNavController,
            timerViewModel = fakeTimerViewModel,
            appSettingsDataStore = fakeDataStore,
            resourceNameToId = { 0 },
            ioDispatcher = testDispatcher, // Use the test dispatcher for IO operations
            delayProvider = { _ -> } // No delay in tests
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun initialStateIsCorrect() = runTest(testDispatcher) {
/*        advanceUntilIdle()

        val expectedWidth = IAppSettingsDataStore.DEFAULT_BOARD_WIDTH
        val expectedHeight = IAppSettingsDataStore.DEFAULT_BOARD_HEIGHT

        assertThat(viewModel.isBoardInitialized.value).isTrue()
        assertThat(viewModel.moves.intValue).isEqualTo(0)
        assertThat(viewModel.score.intValue).isEqualTo(0)
        assertThat(viewModel.tablePlay).isNotNull()
        assertThat(viewModel.tablePlay!!.boardWidth).isEqualTo(expectedWidth)
        assertThat(viewModel.tablePlay!!.boardHeight).isEqualTo(expectedHeight)
        assertThat(viewModel.tablePlay!!.cardsArray.sumOf { it.size }).isEqualTo(expectedWidth * expectedHeight)
        assertThat(viewModel.playResetSound.value).isTrue() // Check that reset sound is requested*/
    }

}