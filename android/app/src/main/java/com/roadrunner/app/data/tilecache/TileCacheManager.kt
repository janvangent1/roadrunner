package com.roadrunner.app.data.tilecache

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns true if there are any tiles cached at zoom level 10.
     * This is a loose heuristic — per-route tracking is a v2 concern.
     */
    fun isTilesCached(routeId: String): Boolean {
        val zoom10Dir = File(context.cacheDir, "osmdroid/tiles/Mapnik/10")
        return zoom10Dir.listFiles()?.isNotEmpty() == true
    }

    /**
     * Enqueues a WorkManager job to download OSMDroid tiles for the given bounding box
     * at zoom levels 10–15. Uses ExistingWorkPolicy.KEEP to avoid duplicate downloads.
     */
    fun enqueueTileDownload(
        routeId: String,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
    ) {
        val data = Data.Builder()
            .putDouble("routeId_hash", routeId.hashCode().toDouble())
            .putDouble("minLat", minLat)
            .putDouble("maxLat", maxLat)
            .putDouble("minLon", minLon)
            .putDouble("maxLon", maxLon)
            .putString("routeId", routeId)
            .build()

        val request = OneTimeWorkRequest.Builder(TileCacheWorker::class.java)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("tiles_$routeId", ExistingWorkPolicy.KEEP, request)
    }
}
