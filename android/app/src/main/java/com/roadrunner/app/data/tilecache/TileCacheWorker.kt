package com.roadrunner.app.data.tilecache

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.tan

class TileCacheWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val TAG = "TileCacheWorker"

    private fun lon2tile(lon: Double, zoom: Int): Int =
        ((lon + 180) / 360 * (1 shl zoom)).toInt()

    private fun lat2tile(lat: Double, zoom: Int): Int {
        val rad = Math.toRadians(lat)
        return ((1 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2 * (1 shl zoom)).toInt()
    }

    override suspend fun doWork(): Result {
        val minLat = inputData.getDouble("minLat", 0.0)
        val maxLat = inputData.getDouble("maxLat", 0.0)
        val minLon = inputData.getDouble("minLon", 0.0)
        val maxLon = inputData.getDouble("maxLon", 0.0)
        val routeId = inputData.getString("routeId") ?: "unknown"

        Log.d(TAG, "Starting tile download for route $routeId: lat=$minLat-$maxLat lon=$minLon-$maxLon")

        val client = OkHttpClient()

        for (z in 10..15) {
            val xMin = lon2tile(minLon, z)
            val xMax = lon2tile(maxLon, z)
            // Y axis is inverted in tile coordinates: maxLat gives minY, minLat gives maxY
            val yMin = lat2tile(maxLat, z)
            val yMax = lat2tile(minLat, z)

            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    val file = File(applicationContext.cacheDir, "osmdroid/tiles/Mapnik/$z/$x/$y.png")
                    if (file.exists()) continue

                    file.parentFile?.mkdirs()

                    try {
                        val request = Request.Builder()
                            .url("https://tile.openstreetmap.org/$z/$x/$y.png")
                            .header("User-Agent", "RoadrunnerApp/1.0")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                response.body?.bytes()?.let { bytes ->
                                    file.writeBytes(bytes)
                                }
                            } else {
                                Log.w(TAG, "Non-200 response for tile $z/$x/$y: ${response.code}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to download tile $z/$x/$y: ${e.message}")
                    }
                }
            }
        }

        Log.d(TAG, "Tile download complete for route $routeId")
        return Result.success()
    }
}
