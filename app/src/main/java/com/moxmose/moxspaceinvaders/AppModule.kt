package com.moxmose.moxspaceinvaders

import android.util.Log
import androidx.navigation.NavHostController
import com.moxmose.moxspaceinvaders.data.local.IAppSettingsDataStore
import com.moxmose.moxspaceinvaders.data.local.RealAppSettingsDataStore
import com.moxmose.moxspaceinvaders.data.local.dataStore
import com.moxmose.moxspaceinvaders.ui.BackgroundMusicManager
import com.moxmose.moxspaceinvaders.ui.GameViewModel
import com.moxmose.moxspaceinvaders.ui.NavigationManager
import com.moxmose.moxspaceinvaders.ui.OpeningMenuViewModel
import com.moxmose.moxspaceinvaders.ui.PreferencesViewModel
import com.moxmose.moxspaceinvaders.ui.SoundUtils
import com.moxmose.moxspaceinvaders.ui.TimerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val myAppModule = module {
    viewModel {
        TimerViewModel()
    }

    single<CoroutineScope>(named("ApplicationScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<IAppSettingsDataStore> { RealAppSettingsDataStore(androidContext().dataStore, get(named("ApplicationScope"))) }

    single {
        BackgroundMusicManager(
            context = androidContext(),
            appSettingsDataStore = get(),
            externalScope = get(named("ApplicationScope"))
        )
    }

    single { SoundUtils(androidContext(), get()) }

    viewModel { (navController: NavHostController) ->
        GameViewModel(
            navController = navController,
            timerViewModel = get(),
            appSettingsDataStore = get(),
            resourceNameToId = { resourceName ->
                try {
                    androidContext().resources.getIdentifier(
                        resourceName,
                        "drawable",
                        androidContext().packageName
                    )
                } catch (e: Exception) {
                    Log.e("KoinDI", "Resource ID not found for: $resourceName", e)
                    0
                }
            }
        )
    }

    viewModel { (navController: NavHostController) ->
        PreferencesViewModel(
            navController = navController,
            appSettingsDataStore = get(),
            backgroundMusicManager = get()
        )
    }

    viewModel { (navController: NavHostController) ->
        OpeningMenuViewModel(
            navController = navController,
            appSettingsDataStore = get()
        )
    }
}

val navigationModule = module {
    single {
        NavigationManager()
    }
}

val appModules = listOf(myAppModule, navigationModule)
