package com.roadrunner.app.security

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private val Context.integrityDataStore by preferencesDataStore(name = "integrity_cache")

private val KEY_PASSED = booleanPreferencesKey("integrity_passed")
private val KEY_CACHED_AT = longPreferencesKey("integrity_cached_at")
private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000

@Singleton
class IntegrityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = OkHttpClient()

    // Returns true if device passed integrity, false if it failed.
    // Throws on network/API error (caller decides whether to block or allow on error).
    suspend fun check(): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.integrityDataStore.data.first()
        val cachedAt = prefs[KEY_CACHED_AT] ?: 0L
        val now = System.currentTimeMillis()

        if (now - cachedAt < CACHE_TTL_MS) {
            return@withContext prefs[KEY_PASSED] ?: false
        }

        val passed = performCheck()
        context.integrityDataStore.edit { store ->
            store[KEY_PASSED] = passed
            store[KEY_CACHED_AT] = now
        }
        passed
    }

    private suspend fun performCheck(): Boolean {
        val deviceId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        val timestamp = System.currentTimeMillis().toString()
        val nonceInput = "$deviceId:$timestamp"
        val digest = MessageDigest.getInstance("SHA-256").digest(nonceInput.toByteArray())
        val nonce = Base64.getEncoder().encodeToString(digest)

        val integrityManager = IntegrityManagerFactory.createStandard(context)
        val tokenProvider = integrityManager.prepareIntegrityToken(
            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(
                    context.resources.getString(
                        context.resources.getIdentifier(
                            "play_integrity_cloud_project_number",
                            "string",
                            context.packageName
                        )
                    ).toLongOrNull() ?: 0L
                )
                .build()
        ).await()

        val tokenResponse = tokenProvider.request(
            StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                .setRequestHash(nonce)
                .build()
        ).await()

        val token = tokenResponse.token()

        val baseUrl = com.roadrunner.app.BuildConfig.BASE_URL
        val body = JSONObject().apply {
            put("token", token)
            put("nonce", nonce)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$baseUrl/api/v1/integrity/verify")
            .post(body)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return false

        val json = JSONObject(response.body?.string() ?: return false)
        return json.optBoolean("passed", false)
    }
}
