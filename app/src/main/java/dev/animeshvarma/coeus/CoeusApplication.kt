package dev.animeshvarma.coeus

import android.app.Application
import timber.log.Timber

/**
 * Application class for the Coeus NFC application.
 */
class CoeusApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Plant Timber tree for logging
        timber.log.Timber.plant(timber.log.Timber.DebugTree())
    }
}