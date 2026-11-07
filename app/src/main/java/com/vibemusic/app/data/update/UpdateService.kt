package com.vibemusic.app.data.update

import com.vibemusic.app.data.online.HttpJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReleaseInfo(
    val tag: String,
    val version: String,
    val name: String,
    val notes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val apkUrl: String?,
    val apkSize: Long,
    val sha256: String?
)

class UpdateException(message: String) : Exception(message)

/**
 * Reads the stable GitHub release published by the repository workflow.
 * `/releases/latest` deliberately skips drafts and pre-releases, which matches
 * the requirement that only published stable builds can trigger a notice.
 */
class GitHubReleaseService {

    suspend fun latestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val body = HttpJson.get(
            "https://api.github.com/repos/DMorenoH0/VibeMusic/releases/latest",
            timeoutMs = 15_000
        )
        try {
            parseLatestRelease(body)
        } catch (e: UpdateException) {
            throw e
        } catch (e: Exception) {
            throw UpdateException("La respuesta de GitHub no es válida.")
        }
    }
}

fun parseLatestRelease(json: String): ReleaseInfo {
    val root = JSONObject(json)
    if (root.optBoolean("draft", false)) throw UpdateException("La última versión aún es un borrador.")
    if (root.optBoolean("prerelease", false)) throw UpdateException("La última versión es una prueba.")
    val tag = root.optString("tag_name").ifBlank {
        throw UpdateException("La versión publicada no tiene etiqueta.")
    }
    val assets = root.optJSONArray("assets")
    var apkUrl: String? = null
    var apkSize = 0L
    var digest: String? = null
    if (assets != null) {
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            apkUrl = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
            apkSize = asset.optLong("size", 0L)
            digest = asset.optString("digest").takeIf { it.startsWith("sha256:") }
                ?.removePrefix("sha256:")
                ?.lowercase()
            if (name.equals("VibeMusic.apk", ignoreCase = true)) break
        }
    }
    val notes = root.optString("body").orEmpty()
    val fromBody = Regex("([a-fA-F0-9]{64})").find(notes)?.groupValues?.get(1)?.lowercase()
    return ReleaseInfo(
        tag = tag,
        version = normalizeVersion(tag),
        name = root.optString("name").ifBlank { "VibeMusic $tag" },
        notes = notes,
        htmlUrl = root.optString("html_url"),
        publishedAt = root.optString("published_at"),
        apkUrl = apkUrl,
        apkSize = apkSize,
        sha256 = digest ?: fromBody
    )
}

/** Removes a leading “v” and surrounding whitespace. */
fun normalizeVersion(raw: String): String =
    raw.trim().removePrefix("v").removePrefix("V").trim()

/**
 * Numeric semantic comparison that tolerates tags such as `v1.2`, `1.2.0` and
 * `1.2.0-beta1`. A stable build is newer than a pre-release with the same
 * numeric core. Returns a positive number when [latest] is newer.
 */
fun compareVersions(latest: String, installed: String): Int {
    val a = versionParts(latest)
    val b = versionParts(installed)
    val size = maxOf(a.numbers.size, b.numbers.size)
    for (i in 0 until size) {
        val left = a.numbers.getOrElse(i) { 0 }
        val right = b.numbers.getOrElse(i) { 0 }
        if (left != right) return left.compareTo(right)
    }
    return when {
        a.preRelease == b.preRelease -> 0
        a.preRelease && !b.preRelease -> -1
        !a.preRelease && b.preRelease -> 1
        else -> a.preReleaseLabel.compareTo(b.preReleaseLabel)
    }
}

fun isNewerVersion(latest: String, installed: String): Boolean =
    compareVersions(latest, installed) > 0

private data class VersionParts(val numbers: List<Int>, val preRelease: Boolean, val preReleaseLabel: String)

private fun versionParts(raw: String): VersionParts {
    val clean = normalizeVersion(raw)
    val core = clean.takeWhile { it.isDigit() || it == '.' }
    val numbers = core.split('.').mapNotNull { it.toIntOrNull() }
    val suffix = clean.drop(core.length).trim('-', '_', '+')
    return VersionParts(
        numbers = numbers.ifEmpty { listOf(0) },
        preRelease = suffix.isNotBlank() && !suffix.all { it.isDigit() },
        preReleaseLabel = suffix
    )
}
