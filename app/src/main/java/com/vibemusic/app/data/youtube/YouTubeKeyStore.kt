package com.vibemusic.app.data.youtube

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user-supplied YouTube API key encrypted with a hardware-backed
 * Android Keystore key (AES-256/GCM). The key never leaves the device and is
 * never written to logs or to the app's backup (backups are disabled).
 */
class YouTubeKeyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasStoredKey(): Boolean = prefs.contains(KEY_CIPHERTEXT)

    @Synchronized
    fun load(): String? {
        val payload = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            if (bytes.size <= IV_SIZE) return@runCatching null
            val iv = bytes.copyOfRange(0, IV_SIZE)
            val body = bytes.copyOfRange(IV_SIZE, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            cipher.doFinal(body).toString(Charsets.UTF_8).trim().ifBlank { null }
        }.getOrNull()
    }

    @Synchronized
    fun save(key: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val body = cipher.doFinal(key.trim().toByteArray(Charsets.UTF_8))
        val payload = Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
        prefs.edit().putString(KEY_CIPHERTEXT, payload).apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_CIPHERTEXT).apply()
    }

    /**
     * Returns the Keystore entry, creating it on first use. If the Keystore was
     * invalidated (for example after a system restore) [load] returns null and
     * the user can save the key again.
     */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "vibemusic_secure"
        const val KEY_CIPHERTEXT = "youtube_api_key_v1"
        const val ALIAS = "vibemusic_youtube_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
