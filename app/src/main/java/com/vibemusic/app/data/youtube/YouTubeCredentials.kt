package com.vibemusic.app.data.youtube

/**
 * Resolution rules for the YouTube Data API credential.
 *
 * The key can come from two places:
 *  1. A key typed by the user and stored encrypted on the device (takes
 *     precedence, so a bad build-time key can be corrected without a rebuild).
 *  2. The build-time key injected from `VIBE_YOUTUBE_API_KEY` (local.properties
 *     or GitHub Actions secret) for development and release builds.
 *
 * This object contains no Android dependencies so the rules stay unit-tested.
 */
object YouTubeCredentials {

    /** Longest plausible Google API key; avoids pasting whole documents by mistake. */
    private const val MAX_KEY_LENGTH = 200

    fun resolve(buildKey: String, storedKey: String?): String {
        val stored = storedKey?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        return buildKey.trim()
    }

    /**
     * Basic sanity check. It does not prove the key is valid: only a request to
     * Google can do that. It rejects empty values, whitespace and absurd sizes.
     */
    fun isWellFormed(key: String): Boolean {
        val clean = key.trim()
        if (clean.length < 20 || clean.length > MAX_KEY_LENGTH) return false
        return clean.none { it.isWhitespace() }
    }

    /** Shows only the last four characters of a stored key in the UI. */
    fun mask(key: String): String {
        val clean = key.trim()
        if (clean.length <= 4) return "••••"
        return "••••" + clean.takeLast(4)
    }
}
