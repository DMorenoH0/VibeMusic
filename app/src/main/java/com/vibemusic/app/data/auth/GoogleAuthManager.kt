package com.vibemusic.app.data.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vibemusic.app.BuildConfig
import com.vibemusic.app.data.GoogleProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AuthException(message: String) : Exception(message)

/**
 * Google Identity Services through AndroidX Credential Manager.
 *
 * Only the basic profile (stable subject id, name, e-mail) is persisted. The
 * ID token itself is never written to disk: it is validated/parsed in memory and
 * then discarded, which keeps the app free of long-lived bearer credentials.
 */
class GoogleAuthManager(private val context: Context) {

    private val clientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val configured: Boolean get() = clientId.isNotBlank()

    suspend fun signIn(): GoogleProfile = withContext(Dispatchers.IO) {
        if (!configured) {
            throw AuthException(
                "El inicio de sesión con Google necesita un ID de cliente OAuth configurado. " +
                    "Consulta la sección «Cuenta» para ver los pasos."
            )
        }
        val credentialManager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                profileFromIdToken(googleCredential.idToken)
            } else {
                throw AuthException("Google devolvió un tipo de credencial no compatible.")
            }
        } catch (e: GetCredentialCancellationException) {
            throw AuthException("Inicio de sesión cancelado.")
        } catch (e: NoCredentialException) {
            throw AuthException(
                "No hay ninguna cuenta de Google disponible en este dispositivo. " +
                    "Añade una cuenta en Ajustes del sistema e inténtalo de nuevo."
            )
        } catch (e: GetCredentialException) {
            throw AuthException("No se pudo iniciar sesión: ${e.message ?: "error de credenciales"}")
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

/** Extracts the public profile fields from the ID token payload without verifying it server-side. */
fun profileFromIdToken(token: String): GoogleProfile {
    val parts = token.split('.')
    if (parts.size < 2) throw AuthException("Google devolvió un token no válido.")
    val payload = runCatching {
        val decoded = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        String(decoded, Charsets.UTF_8)
    }.getOrElse { throw AuthException("No se pudo leer el perfil de Google.") }
    return profileFromPayload(payload)
}

/** Pure JSON profile parser, separated from Base64 so it can be unit tested on the JVM. */
fun profileFromPayload(payloadJson: String): GoogleProfile {
    val payload = runCatching { JSONObject(payloadJson) }
        .getOrElse { throw AuthException("No se pudo leer el perfil de Google.") }
    val id = payload.optString("sub")
    if (id.isBlank()) throw AuthException("El token de Google no contiene un identificador de cuenta.")
    val email = payload.optString("email")
    return GoogleProfile(
        id = id,
        name = payload.optString("name").ifBlank { email.substringBefore('@') },
        email = email
    )
}
