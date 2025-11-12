package com.moxmose.moxspaceinvaders.ui

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import com.moxmose.moxspaceinvaders.model.ScoreEntry
import kotlinx.coroutines.flow.StateFlow

class OpeningMenuViewModel(
    private val navController: NavHostController,
    private val appSettingsDataStore: IAppSettingsDataStore
) : ViewModel() {

    val topRanking: StateFlow<List<ScoreEntry>> = appSettingsDataStore.topRanking
    val lastPlayedEntry: StateFlow<ScoreEntry?> = appSettingsDataStore.lastPlayedEntry
    val selectedBackgrounds: StateFlow<Set<String>> = appSettingsDataStore.selectedBackgrounds

    fun onStartGameClicked() {
        navController.navigate(Screen.GameScreen.route)
    }

    fun onSettingsClicked() {
        navController.navigate(Screen.PreferencesScreen.route)
    }
}