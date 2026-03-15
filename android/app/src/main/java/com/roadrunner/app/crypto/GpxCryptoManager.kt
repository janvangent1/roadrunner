package com.roadrunner.app.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.roadrunner.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxCryptoManager @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val KEYSET_PREFS = "roadrunner_gpx_prefs"
        private const val KEYSET_NAME = "roadrunner_gpx_keyset"
        private const val MASTER_KEY_URI = "android-keystore://roadrunner_gpx_master"
    }

    private val streamingAead: StreamingAead by lazy {
        StreamingAeadConfig.register()

        val prefs = context.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE)

        // Seed the keyset from BuildConfig on first install.
        // AndroidKeysetManager.Builder checks KEYSET_NAME in KEYSET_PREFS; if absent it would
        // generate a fresh key (wrong). So we pre-populate the SharedPrefs entry with the server
        // keyset serialized as JSON before AndroidKeysetManager loads it for the first time.
        if (!prefs.contains(KEYSET_NAME)) {
            val keysetBytes = Base64.decode(BuildConfig.TINK_KEYSET_B64, Base64.DEFAULT)
            val reader = BinaryKeysetReader.withBytes(keysetBytes)
            val serverKeysetHandle = CleartextKeysetHandle.read(reader)

            // Serialize to JSON so AndroidKeysetManager can read it from SharedPrefs
            val baos = ByteArrayOutputStream()
            CleartextKeysetHandle.write(serverKeysetHandle, JsonKeysetWriter.withOutputStream(baos))
            prefs.edit().putString(KEYSET_NAME, baos.toString("UTF-8")).apply()
        }

        // AndroidKeysetManager loads the keyset from SharedPrefs and wraps it with the
        // Android Keystore master key for hardware-backed protection at rest.
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM_HKDF_4KB"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(StreamingAead::class.java)
    }

    /**
     * Decrypts the given encrypted GPX file in-memory and returns the plaintext bytes.
     *
     * The [routeId] must match the AAD used by the server during encryption
     * (server uses Buffer.from(routeId) as AAD — same bytes as routeId.toByteArray() in UTF-8).
     *
     * @param routeId The route ID used as AAD during server-side encryption.
     * @param encFile The encrypted .enc file on disk.
     * @return Plaintext GPX bytes.
     * @throws Exception if decryption fails (wrong key, corrupted file, or wrong AAD).
     */
    fun decryptToByteArray(routeId: String, encFile: File): ByteArray {
        val aad = routeId.toByteArray()
        return encFile.inputStream().use { fileStream ->
            streamingAead.newDecryptingStream(fileStream, aad).use { it.readBytes() }
        }
    }

    /**
     * Returns whether an encrypted GPX file exists for the given route.
     *
     * @param routeId The route ID whose .enc file to check.
     * @param filesDir The app's files directory (Context.filesDir).
     * @return true if the file `filesDir/gpx/{routeId}.enc` exists, false otherwise.
     */
    fun encryptedFileExists(routeId: String, filesDir: File): Boolean {
        return File(filesDir, "gpx/$routeId.enc").exists()
    }
}
