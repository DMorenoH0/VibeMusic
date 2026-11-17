package com.vibemusic.app.data.youtube

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Headers required by Google when an API key is restricted to an Android app
 * (package name + signing certificate SHA-1). Sending them means a leaked key
 * is useless outside a build signed with the same certificate.
 */
object YouTubeApiHeaders {

    fun from(context: Context): Map<String, String> = buildMap {
        put("X-Android-Package", context.packageName)
        signingSha1(context)?.let { put("X-Android-Cert", it) }
    }

    private fun signingSha1(context: Context): String? = runCatching {
        val manager = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            ).signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            manager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            ).signatures
        }
        val signature = signatures?.firstOrNull() ?: return null
        MessageDigest.getInstance("SHA-1")
            .digest(signature.toByteArray())
            .joinToString("") { byte -> "%02X".format(byte.toInt() and 0xFF) }
    }.getOrNull()
}
