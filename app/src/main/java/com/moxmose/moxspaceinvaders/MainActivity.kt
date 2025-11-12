package com.moxmose.moxspaceinvaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.moxmose.moxspaceinvaders.ui.BackgroundMusicManager
import com.moxmose.moxspaceinvaders.ui.NavGraph
import com.moxmose.moxspaceinvaders.ui.SoundUtils
import com.moxmose.moxspaceinvaders.ui.theme.MoxSpaceInvadersTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Inject the managers. Koin will create and manage their lifecycles.
    private val backgroundMusicManager: BackgroundMusicManager by inject()
    private val soundUtils: SoundUtils by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // By referencing the managers here, we ensure Koin initializes them.
        backgroundMusicManager.toString()
        soundUtils.toString()

        val screenWidthDp = resources.configuration.screenWidthDp
        val isConsideredPhone = screenWidthDp < 600
        if (isConsideredPhone) {
            enableEdgeToEdge()
        }

        setContent {
            MoxSpaceInvadersTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    NavGraph(innerPadding = it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundMusicManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        backgroundMusicManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release all sound resources when the app is destroyed.
        soundUtils.release()
    }
}