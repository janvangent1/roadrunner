package com.roadrunner.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStorage @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val KEYSET_PREFS = "roadrunner_token_keyset"
        private const val KEYSET_NAME = "__roadrunner_token_keyset__"
        private const val TOKEN_PREFS = "roadrunner_tokens"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://roadrunner_token_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(TOKEN_PREFS, Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        val encryptedAccess = Base64.encodeToString(
            aead.encrypt(accessToken.toByteArray(), null), Base64.NO_WRAP)
        val encryptedRefresh = Base64.encodeToString(
            aead.encrypt(refreshToken.toByteArray(), null), Base64.NO_WRAP)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encryptedAccess)
            .putString(KEY_REFRESH_TOKEN, encryptedRefresh)
            .apply()
    }

    fun getAccessToken(): String? = decrypt(prefs.getString(KEY_ACCESS_TOKEN, null))
    fun getRefreshToken(): String? = decrypt(prefs.getString(KEY_REFRESH_TOKEN, null))

    fun clearTokens() = prefs.edit().clear().apply()

    private fun decrypt(value: String?): String? {
        if (value == null) return null
        return try {
            String(aead.decrypt(Base64.decode(value, Base64.NO_WRAP), null))
        } catch (e: Exception) { null }
    }
}
