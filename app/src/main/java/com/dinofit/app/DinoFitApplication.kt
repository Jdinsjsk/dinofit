package com.dinofit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dinofit.app.work.StepSyncWorker
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration as OsmConfiguration
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class DinoFitApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        StepSyncWorker.schedule(this)
        // OpenStreetMap demande un identifiant d'application ; le cache des tuiles reste dans l'app.
        OsmConfiguration.getInstance().apply {
            load(this@DinoFitApplication, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = "DinoFit/1.0 ($packageName)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
