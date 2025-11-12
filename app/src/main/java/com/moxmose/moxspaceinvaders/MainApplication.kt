package com.moxmose.moxspaceinvaders

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level

/**
 * [MainApplication] is the main Application class for this Android application.
 *
 * It extends the [android.app.Application] class and is responsible for initializing
 * the Koin dependency injection framework during application startup.
 *
 * This class is referenced in the AndroidManifest.xml file to ensure that it is
 * instantiated when the application starts.
 *
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        GlobalContext.startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApplication)
            modules(appModules)
        }
    }
}