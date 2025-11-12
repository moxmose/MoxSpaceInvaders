package com.moxmose.moxspaceinvaders.ui

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import com.moxmose.moxspaceinvaders.model.GameBoard
import com.moxmose.moxspaceinvaders.model.GameCard
import com.moxmose.moxspaceinvaders.model.SoundEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class GameViewModel(
    private val navController: NavHostController,
    private val timerViewModel: TimerViewModel,
    private val appSettingsDataStore: IAppSettingsDataStore,
    private val resourceNameToId: (String) -> Int,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
): ViewModel(), IGameViewModel {
    override val playerName: StateFlow<String> = appSettingsDataStore.playerName
    override val selectedBackgrounds: StateFlow<Set<String>> = appSettingsDataStore.selectedBackgrounds

    private val _score = mutableIntStateOf(0)
    override val score get() = _score

    private val _moves = mutableIntStateOf(0)
    override val moves get() = _moves

    private var lastMove: Pair<Int, Int> = Pair(0,0)
    private val noLastMove = Pair(-1, -1)
    private var cardInPlay: Boolean = false

    private var _tablePlay: GameBoard? = null
    override val tablePlay: GameBoard? get() = _tablePlay

    @DrawableRes
    private lateinit var _gameCardImages: List<Int>
    override val gameCardImages get() = _gameCardImages

    override val currentTime = timerViewModel.elapsedSeconds
    private var timeOfLastMove = 0L

    override var gamePaused: MutableState<Boolean> = mutableStateOf(false)
    override var gameResetRequest: MutableState<Boolean> = mutableStateOf(false)
    override var gameWon: MutableState<Boolean> = mutableStateOf(false)

    private val _isBoardInitialized = mutableStateOf(false)
    override val isBoardInitialized: State<Boolean> = _isBoardInitialized

    private val _playResetSound = MutableStateFlow(false)
    override val playResetSound: StateFlow<Boolean> = _playResetSound.asStateFlow()

    init {
        Log.d("GameVM", "init - Calling resetGame()")
        resetGame()
    }

    private fun resetGame() {
        Log.d("GameVM", "resetGame - Starting game reset process")
        viewModelScope.launch {
            _isBoardInitialized.value = false 
            _tablePlay = null 
            Log.d("GameVM", "resetGame - _isBoardInitialized set to false, _tablePlay set to null. Calling loadAndShuffleCards()")
            loadAndShuffleCards()
            
            Log.d("GameVM", "resetGame - loadAndShuffleCards() completed, proceeding with main thread state reset for UI elements")
            withContext(Dispatchers.Main) {
                _score.intValue = 0 
                _moves.intValue = 0
                lastMove = noLastMove
                cardInPlay = false
                _playResetSound.value = true // Signal the UI to play the sound
                // These states are crucial for dialogs, make sure they are clean after a full reset.
                gamePaused.value = false
                gameResetRequest.value = false
                gameWon.value = false 
                timerViewModel.resetTimer()
                timerViewModel.startTimer()
                timeOfLastMove = 0L
                Log.d("GameVM", "resetGame - Main thread UI state reset completed")
            }
        }
    }

    override fun onResetSoundPlayed() {
        _playResetSound.value = false
    }

    private suspend fun loadAndShuffleCards() {
        Log.d("GameVM", "loadAndShuffleCards - Waiting for DataStore to be loaded...")
        appSettingsDataStore.isDataLoaded.filter { it }.first() 
        Log.d("GameVM", "loadAndShuffleCards - DataStore is loaded. Proceeding to load cards.")

        val boardWidth = appSettingsDataStore.selectedBoardWidth.first()
        val boardHeight = appSettingsDataStore.selectedBoardHeight.first()
        Log.d("GameVM", "loadAndShuffleCards - Fetched board dimensions: ${boardWidth}x${boardHeight}")

        _tablePlay = GameBoard(boardWidth, boardHeight)
        Log.d("GameVM", "loadAndShuffleCards - _tablePlay instance created with new dimensions.")

        val uniqueCardsNeeded = (boardWidth * boardHeight) / 2
        var userSelectedResourceNames = appSettingsDataStore.selectedCards.first()

        if (userSelectedResourceNames.size < uniqueCardsNeeded) {
            Log.w("GameVM", "loadAndShuffleCards - User selected cards insufficient. Falling back to default.")
            userSelectedResourceNames = IAppSettingsDataStore.DEFAULT_SELECTED_CARDS
        }
        
        val actualCardResourceNamesForGame = userSelectedResourceNames.shuffled().take(uniqueCardsNeeded)
        _gameCardImages = actualCardResourceNamesForGame.map { resourceName -> resourceNameToId(resourceName) }
        Log.d("GameVM", "loadAndShuffleCards - Card images prepared.")

        val logicalCardIds = (0 until uniqueCardsNeeded).toList()
        val gameCardLogicalIdsForBoard = (logicalCardIds + logicalCardIds).shuffled()

        withContext(Dispatchers.Main) {
            val board = _tablePlay
            if (board == null) {
                Log.e("GameVM", "loadAndShuffleCards - CRITICAL: _tablePlay is null before populating on Main thread. Aborting population.")
                _isBoardInitialized.value = false 
                return@withContext
            }

            var i = 0
            for(x in (0 until boardWidth)) {
                for(y in (0 until boardHeight)) {
                     if (i < gameCardLogicalIdsForBoard.size) { 
                        board.cardsArray[x][y].value = GameCard(
                            id = gameCardLogicalIdsForBoard[i++],
                            turned = false,
                            coupled = false
                        )
                    } else {
                        Log.e("GameVM", "loadAndShuffleCards - Error: Not enough logical card IDs for board size!") 
                    }
                }
            }
            Log.d("GameVM", "loadAndShuffleCards - Finished populating _tablePlay on Main thread.")
            _isBoardInitialized.value = true 
            Log.d("GameVM", "loadAndShuffleCards - _isBoardInitialized set to true.")
        }
    }

    override fun checkGamePlayCardTurned(x: Int, y: Int, onSoundEvent: (SoundEvent) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            if (cardInPlay) {
                Log.d("GameVM_Play", "Card in play, ignoring tap")
                return@launch
            }

            val board = _tablePlay ?: return@launch
            val card = board.cardsArray[x][y].value ?: return@launch

            if (card.turned || card.coupled) {
                Log.d("GameVM_Play", "Card already turned or coupled, ignoring tap")
                return@launch
            }

            // --- First part of the move: Turn the card ---
            withContext(Dispatchers.Main) {
                board.cardsArray[x][y].value = card.copy(turned = true)
                _moves.intValue++
                onSoundEvent(SoundEvent.Flip)
            }

            val lastClicked = lastMove
            if (lastClicked == noLastMove) {
                // This is the first card of a potential pair
                lastMove = Pair(x, y)
                timeOfLastMove = currentTime.value
                Log.d("GameVM_Play", "First card selected at ($x, $y)")
            } else {
                // This is the second card, let's check for a match
                cardInPlay = true
                val lastCardState = board.cardsArray[lastClicked.first][lastClicked.second]
                val lastCardValue = lastCardState.value

                if (lastCardValue == null) {
                    Log.e("GameVM_Play", "CRITICAL: lastCardValue is null. Resetting lastMove.")
                    lastMove = noLastMove
                    cardInPlay = false
                    return@launch
                }


                val timeDelta = currentTime.value - timeOfLastMove

                if (lastCardValue.id == card.id) {
                    // --- MATCH ---
                    Log.d("GameVM_Play", "Match found for cards at $lastClicked and ($x, $y)")
                    withContext(Dispatchers.Main) {
                        refreshPointsRightCouple(timeDelta)
                        board.cardsArray[x][y].value = card.copy(turned = true, coupled = true)
                        lastCardState.value = lastCardValue.copy(turned = true, coupled = true)
                        onSoundEvent(SoundEvent.Success)
                    }

                    val gameHasBeenWon = checkAllCardsCoupled()
                    if (gameHasBeenWon) {
                        Log.d("GameVM_Play", "Game won!")
                        withContext(Dispatchers.Main) {
                            gameWon.value = true
                            gamePaused.value = true // Use pause state to show win dialog
                            timerViewModel.stopTimer()
                            onSoundEvent(SoundEvent.Win)
                            viewModelScope.launch {
                                appSettingsDataStore.saveScore(playerName.value, score.value)
                            }
                        }
                    }
                } else {
                    // --- NO MATCH ---
                    Log.d("GameVM_Play", "No match for cards at $lastClicked and ($x, $y)")
                    withContext(Dispatchers.Main) {
                        refreshPointsWrongCouple(timeDelta)
                        onSoundEvent(SoundEvent.Fail)
                    }
                    delayProvider(1000) // wait a second
                    withContext(Dispatchers.Main) {
                        board.cardsArray[x][y].value = card.copy(turned = false)
                        lastCardState.value = lastCardValue.copy(turned = false)
                    }
                }
                // Reset for the next turn
                lastMove = noLastMove
                cardInPlay = false
            }
        }
    }

    // Called from GameWonDialog and ResetDialog confirmation to go back to the menu
    override fun navigateToOpeningMenuAndCleanupDialogStates() {
        Log.d("GameVM", "navigateToOpeningMenuAndCleanupDialogStates - Cleaning dialog states and navigating.")
        gamePaused.value = false
        gameResetRequest.value = false
        gameWon.value = false // Ensures the win state is reset
        navController.navigate(Screen.OpeningMenuScreen.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }



    // Called by the Pause button or when the game is won
    override fun requestPauseDialog() { 
        Log.d("GameVM", "requestPauseDialog - Setting gamePaused = true")
        gamePaused.value = true 
    }

    // Called by the Reset button
    override fun requestResetDialog() { 
        Log.d("GameVM", "requestResetDialog - Setting gamePaused = true, gameResetRequest = true")
        gamePaused.value = true
        gameResetRequest.value = true 
    }

    // Called to close the PauseDialog
    override fun dismissPauseDialog() {
        Log.d("GameVM", "dismissPauseDialog - Setting gamePaused = false")
        gamePaused.value = false
        // gameResetRequest should already be false, but just in case:
        if (gameResetRequest.value && !gameWon.value) { // Don't reset if it's a confirmed or won reset
            Log.w("GameVM", "dismissPauseDialog - gameResetRequest was true when dismissing pause. Check logic.")
            // gameResetRequest.value = false // Optional: depends on the desired logic
        }
    }

    // Called to cancel the ResetDialog
    override fun cancelResetDialog() {
        Log.d("GameVM", "cancelResetDialog - Setting gamePaused = false, gameResetRequest = false")
        gamePaused.value = false
        gameResetRequest.value = false
    }
    
    // Called from the "Reset" button in the game menu to reset the current match
    fun resetCurrentGame() { 
        Log.d("GameVM", "resetCurrentGame - Calling resetGame() to restart current match.")
        resetGame() 
    }

    private fun checkAllCardsCoupled(): Boolean {
        if (!isBoardInitialized.value) {
            Log.w("GameVM_CheckAll", "Board not initialized. Returning false.")
            return false
        }
        val currentBoard = _tablePlay
        if (currentBoard == null) {
            Log.e("GameVM_CheckAll", "CRITICAL: _tablePlay is NULL. Returning false.")
            return false
        }

        for (x_idx in currentBoard.cardsArray.indices) {
            for (y_idx in currentBoard.cardsArray[x_idx].indices) {
                val cardState = currentBoard.cardsArray.getOrNull(x_idx)?.getOrNull(y_idx)
                if (cardState == null) {
                    Log.e("GameVM_CheckAll", "CRITICAL: cardState at [$x_idx][$y_idx] is null during iteration. Game state error.")
                    return false 
                }
                val cardValue = cardState.value
                if (cardValue == null) {
                    Log.e("GameVM_CheckAll", "CRITICAL: cardValue at [$x_idx][$y_idx] is null during iteration. Game state error.")
                    return false 
                }
                if (!cardValue.coupled) {
                    return false 
                }
            }
        }
        return true 
    }

    private fun calculateTimeEffectDeciPoints(timeDeltaInSeconds: Long, effectRateInteger: Int): Int {
        if (timeDeltaInSeconds <= 0) return 0
        val points = (100.0 / timeDeltaInSeconds.toDouble()) * effectRateInteger
        return points.roundToInt()
    }

    private fun calculateBoardDifficultyDeciBonusPoints(): Int {
        if (!isBoardInitialized.value) {
            Log.w("GameVM_Score", "calculateBoardDifficultyDeciBonusPoints - Board not initialized. Returning 0 bonus.")
            return 0
        }
        val currentBoard = _tablePlay
        if (currentBoard == null) {
            Log.e("GameVM_Score", "CRITICAL: calculateBoardDifficultyDeciBonusPoints - _tablePlay is NULL. Returning 0 bonus.")
            return 0
        }

        val minConfigurableBoardCells = PreferencesViewModel.MIN_BOARD_WIDTH * PreferencesViewModel.MIN_BOARD_HEIGHT
        val currentTotalCells = currentBoard.boardWidth * currentBoard.boardHeight

        if (currentTotalCells <= 0) return 0
        if (currentTotalCells <= minConfigurableBoardCells) return 0

        val bonusFloat = 2.0f * (1.0f - (minConfigurableBoardCells.toFloat() / currentTotalCells.toFloat()))
        val deciBonus = (bonusFloat * 10.0f).roundToInt()
        
        return max(0, deciBonus) 
    }

    private fun refreshPointsWrongCouple(timeDeltaInSeconds: Long) { 
        val timePenaltyDeciPoints = calculateTimeEffectDeciPoints(timeDeltaInSeconds, effectRateInteger = -2)
        _score.intValue = _score.intValue + timePenaltyDeciPoints 
        Log.d("GameVM_Score", "refreshPointsWrongCouple - Score: ${_score.intValue}, Time Penalty: $timePenaltyDeciPoints")
    }
    private fun refreshPointsRightCouple(timeDeltaInSeconds: Long) { 
        val timeBonusDeciPoints = calculateTimeEffectDeciPoints(timeDeltaInSeconds, effectRateInteger = 3) // MODIFIED
        val boardBonusDeciPoints = calculateBoardDifficultyDeciBonusPoints()
        _score.intValue = _score.intValue + timeBonusDeciPoints + boardBonusDeciPoints 
        Log.d("GameVM_Score", "refreshPointsRightCouple - Score: ${_score.intValue}, Time Bonus: $timeBonusDeciPoints, Board Bonus: $boardBonusDeciPoints")
    }
    private fun refreshPointsNoCoupleSelected(timeDeltaInSeconds: Long) { 
        val timePenaltyDeciPoints = calculateTimeEffectDeciPoints(timeDeltaInSeconds, effectRateInteger = -1)
        _score.intValue = _score.intValue + timePenaltyDeciPoints 
        Log.d("GameVM_Score", "refreshPointsNoCoupleSelected - Score: ${_score.intValue}, Time Penalty: $timePenaltyDeciPoints")
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            timerViewModel.stopAndAwaitTimerCompletion()
        }
    }
}
