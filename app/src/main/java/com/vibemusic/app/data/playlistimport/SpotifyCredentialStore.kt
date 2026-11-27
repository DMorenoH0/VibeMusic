package com.vibemusic.app.data.playlistimport

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
 * Stores the user's own Spotify application credentials encrypted with an
 * Android Keystore AES-256/GCM key. Nothing is embedded in the APK and the
 * secret never leaves the device. The Client ID is not a secret by itself, but
 * it is encrypted too so a stolen preferences file reveals nothing.
 */
class SpotifyCredentialStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasCredentials(): Boolean =
        prefs.contains(KEY_CLIENT_ID) && prefs.contains(KEY_CLIENT_SECRET)

    @Synchronized
    fun load(): SpotifyCredentials? {
        val id = decrypt(prefs.getString(KEY_CLIENT_ID, null)) ?: return null
        val secret = decrypt(prefs.getString(KEY_CLIENT_SECRET, null)) ?: return null
        if (id.isBlank() || secret.isBlank()) return null
        return SpotifyCredentials(id, secret)
    }

    @Synchronized
    fun save(credentials: SpotifyCredentials) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, encrypt(credentials.clientId.trim()))
            .putString(KEY_CLIENT_SECRET, encrypt(credentials.clientSecret.trim()))
            .apply()
    }

    @Synchronized
    fun clear() {
        prefs.edit().remove(KEY_CLIENT_ID).remove(KEY_CLIENT_SECRET).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val body = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(payload: String?): String? {
        payload ?: return null
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
        const val KEY_CLIENT_ID = "spotify_client_id_v1"
        const val KEY_CLIENT_SECRET = "spotify_client_secret_v1"
        const val ALIAS = "vibemusic_spotify_credentials"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
