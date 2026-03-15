package com.roadrunner.app

import android.app.Application
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class RoadrunnerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // CRITICAL: must be set before any OSMDroid map is created — Android 13+ scoped storage
        Configuration.getInstance().apply {
            load(this@RoadrunnerApp, PreferenceManager.getDefaultSharedPreferences(this@RoadrunnerApp))
            osmdroidTileCache = File(filesDir, "osmdroid")
            userAgentValue = BuildConfig.APPLICATION_ID
        }
    }
}
