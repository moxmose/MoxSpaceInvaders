
package com.moxmose.moxspaceinvaders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import com.moxmose.moxspaceinvaders.model.BackgroundMusic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val navController: NavHostController,
    val appSettingsDataStore: IAppSettingsDataStore,
    private val backgroundMusicManager: BackgroundMusicManager
) : ViewModel() {

    val playerName: StateFlow<String> = appSettingsDataStore.playerName
    val availableBackgrounds: List<String> = List(8) { i -> "background_%02d".format(i) }

    private val _selectedBackgrounds = MutableStateFlow<Set<String>>(emptySet())
    val selectedBackgrounds: StateFlow<Set<String>> = _selectedBackgrounds.asStateFlow()

    private var backgroundSelectionFallback: String? = null

    val availablePlayerShips: List<String> = (1..4).map { "astro_pl_$it" }
    val availableEnemyShips: List<String> = (1..4).map { "astro_al_$it" }
    val availableMotherShips: List<String> = (1..3).map { "astro_mo_$it" }

    val playerShip: StateFlow<String> = appSettingsDataStore.playerShip
    val enemyShip: StateFlow<String> = appSettingsDataStore.enemyShip
    val motherShip: StateFlow<String> = appSettingsDataStore.motherShip

    private val _tempPlayerShip = MutableStateFlow("")
    val tempPlayerShip: StateFlow<String> = _tempPlayerShip.asStateFlow()

    private val _tempEnemyShip = MutableStateFlow("")
    val tempEnemyShip: StateFlow<String> = _tempEnemyShip.asStateFlow()

    private val _tempMotherShip = MutableStateFlow("")
    val tempMotherShip: StateFlow<String> = _tempMotherShip.asStateFlow()

    val selectedBoardWidth: StateFlow<Int> = appSettingsDataStore.selectedBoardWidth
    val selectedBoardHeight: StateFlow<Int> = appSettingsDataStore.selectedBoardHeight

    private val _selectionError = MutableStateFlow<String?>(null)
    val selectionError: StateFlow<String?> = _selectionError.asStateFlow()

    private val _boardDimensionError = MutableStateFlow<String?>(null)
    val boardDimensionError: StateFlow<String?> = _boardDimensionError.asStateFlow()

    // Music Preferences
    val isMusicEnabled: StateFlow<Boolean> = appSettingsDataStore.isMusicEnabled
    val musicVolume: StateFlow<Float> = appSettingsDataStore.musicVolume
    val selectedMusicTrackNames: StateFlow<Set<String>> = appSettingsDataStore.selectedMusicTrackNames

    // Sound Effects Preferences
    val areSoundEffectsEnabled: StateFlow<Boolean> = appSettingsDataStore.areSoundEffectsEnabled
    val soundEffectsVolume: StateFlow<Float> = appSettingsDataStore.soundEffectsVolume

    private var lastSavePlayerShipJob: Job? = null
    private var lastSaveEnemyShipJob: Job? = null
    private var lastSaveMotherShipJob: Job? = null
    private var lastSaveBackgroundsJob: Job? = null
    private var lastSaveDimensionsJob: Job? = null
    private var lastSaveMusicJob: Job? = null
    private var lastSaveSfxJob: Job? = null

    init {
        viewModelScope.launch {
            appSettingsDataStore.isDataLoaded.filter { it }.first()

            _selectedBackgrounds.value = appSettingsDataStore.selectedBackgrounds.first()
            _tempPlayerShip.value = appSettingsDataStore.playerShip.first()
            _tempEnemyShip.value = appSettingsDataStore.enemyShip.first()
            _tempMotherShip.value = appSettingsDataStore.motherShip.first()
        }
    }

    fun updatePlayerName(newName: String) {
        if (newName.length <= PLAYERNAME_MAX_LENGTH) {
            viewModelScope.launch {
                appSettingsDataStore.savePlayerName(newName)
            }
        }
    }

    fun prepareForBackgroundSelection() {
        val currentSelection = _selectedBackgrounds.value
        backgroundSelectionFallback = availableBackgrounds.firstOrNull { it in currentSelection } ?: availableBackgrounds.first()
    }

    fun updateBackgroundSelection(backgroundName: String, isSelected: Boolean) {
        val currentSelection = _selectedBackgrounds.value.toMutableSet()
        if (isSelected) {
            currentSelection.add(backgroundName)
        } else {
            if (currentSelection.size > 1) { // Prevents deselection of the last item
                currentSelection.remove(backgroundName)
            }
        }
        _selectedBackgrounds.value = currentSelection
    }

    fun toggleSelectAllBackgrounds(selectAll: Boolean) {
        if (selectAll) {
            _selectedBackgrounds.value = availableBackgrounds.toSet()
        } else {
            backgroundSelectionFallback?.let {
                _selectedBackgrounds.value = setOf(it)
            }
        }
    }

    fun confirmBackgroundSelections() {
        lastSaveBackgroundsJob = viewModelScope.launch {
            appSettingsDataStore.saveSelectedBackgrounds(_selectedBackgrounds.value)
        }
    }

    fun prepareForPlayerShipSelection() {
        _tempPlayerShip.value = playerShip.value
    }

    fun updatePlayerShipSelection(shipName: String) {
        _tempPlayerShip.value = shipName
    }

    fun confirmPlayerShipSelection() {
        if (_tempPlayerShip.value.isNotBlank()) {
            _selectionError.value = null
            lastSavePlayerShipJob = viewModelScope.launch {
                appSettingsDataStore.savePlayerShip(_tempPlayerShip.value)
            }
        } else {
            _selectionError.value = "A player ship must be selected."
        }
    }

    fun prepareForEnemyShipSelection() {
        _tempEnemyShip.value = enemyShip.value
    }

    fun updateEnemyShipSelection(shipName: String) {
        _tempEnemyShip.value = shipName
    }

    fun confirmEnemyShipSelection() {
        if (_tempEnemyShip.value.isNotBlank()) {
            _selectionError.value = null
            lastSaveEnemyShipJob = viewModelScope.launch {
                appSettingsDataStore.saveEnemyShip(_tempEnemyShip.value)
            }
        } else {
            _selectionError.value = "An enemy ship must be selected."
        }
    }

    fun prepareForMotherShipSelection() {
        _tempMotherShip.value = motherShip.value
    }

    fun updateMotherShipSelection(shipName: String) {
        _tempMotherShip.value = shipName
    }

    fun confirmMotherShipSelection() {
        if (_tempMotherShip.value.isNotBlank()) {
            _selectionError.value = null
            lastSaveMotherShipJob = viewModelScope.launch {
                appSettingsDataStore.saveMotherShip(_tempMotherShip.value)
            }
        } else {
            _selectionError.value = "A mother ship must be selected."
        }
    }

    fun clearSelectionError() {
        _selectionError.value = null
    }

    fun updateBoardDimensions(newWidth: Int, newHeight: Int) {
        if (newWidth < MIN_BOARD_WIDTH || newWidth > MAX_BOARD_WIDTH || newHeight < MIN_BOARD_HEIGHT || newHeight > MAX_BOARD_HEIGHT) {
            _boardDimensionError.value = "Invalid dimensions."
            return
        }

        _boardDimensionError.value = null
        lastSaveDimensionsJob = viewModelScope.launch {
            appSettingsDataStore.saveBoardDimensions(newWidth, newHeight)
        }
    }

    fun clearBoardDimensionError() {
        _boardDimensionError.value = null
    }

    fun onBackToMainMenuClicked() {
        viewModelScope.launch {
            lastSaveBackgroundsJob?.join()
            lastSavePlayerShipJob?.join()
            lastSaveEnemyShipJob?.join()
            lastSaveMotherShipJob?.join()
            lastSaveDimensionsJob?.join()
            lastSaveMusicJob?.join()
            lastSaveSfxJob?.join()
            backgroundMusicManager.stopPreview() // Ensure preview is stopped
            navController.popBackStack()
        }
    }

    fun saveIsMusicEnabled(isEnabled: Boolean) {
        lastSaveMusicJob = viewModelScope.launch {
            appSettingsDataStore.saveIsMusicEnabled(isEnabled)
        }
    }

    fun saveMusicVolume(volume: Float) {
        lastSaveMusicJob = viewModelScope.launch {
            appSettingsDataStore.saveMusicVolume(volume)
        }
    }

    fun saveSelectedMusicTracks(trackNames: Set<String>) {
        lastSaveMusicJob = viewModelScope.launch {
            appSettingsDataStore.saveSelectedMusicTracks(trackNames)
        }
    }

    fun saveAreSoundEffectsEnabled(isEnabled: Boolean) {
        lastSaveSfxJob = viewModelScope.launch {
            appSettingsDataStore.saveAreSoundEffectsEnabled(isEnabled)
        }
    }

    fun saveSoundEffectsVolume(volume: Float) {
        lastSaveSfxJob = viewModelScope.launch {
            appSettingsDataStore.saveSoundEffectsVolume(volume)
        }
    }

    fun playMusicPreview(track: BackgroundMusic) {
        backgroundMusicManager.playPreview(track)
    }

    fun stopMusicPreview() {
        backgroundMusicManager.stopPreview()
    }

    override fun onCleared() {
        super.onCleared()
        backgroundMusicManager.stopPreview()
    }

    companion object {
        const val PLAYERNAME_MAX_LENGTH = 20
        const val MIN_BOARD_WIDTH = 3
        const val MAX_BOARD_WIDTH = 5
        const val MIN_BOARD_HEIGHT = 4
        const val MAX_BOARD_HEIGHT = 6
    }
}
