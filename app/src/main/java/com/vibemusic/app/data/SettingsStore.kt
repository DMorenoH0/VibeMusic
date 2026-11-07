package com.vibemusic.app.data

import android.content.Context
import android.content.SharedPreferences
import com.vibemusic.app.ui.theme.VibeThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

data class GoogleProfile(
    val id: String,
    val name: String,
    val email: String
)

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibe_settings", Context.MODE_PRIVATE)

    private val _amoled = MutableStateFlow(prefs.getBoolean("amoled", true))
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _autoScan = MutableStateFlow(prefs.getBoolean("auto_scan", true))
    val autoScan: StateFlow<Boolean> = _autoScan.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.getBoolean("wifi_only", false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _theme = MutableStateFlow(
        runCatching {
            VibeThemeStyle.valueOf(
                prefs.getString("theme_style", null)
                    ?: if (prefs.getBoolean("amoled", true)) {
                        VibeThemeStyle.AMOLED.name
                    } else {
                        VibeThemeStyle.CRIMSON.name
                    }
            )
        }.getOrDefault(VibeThemeStyle.AMOLED)
    )
    val theme: StateFlow<VibeThemeStyle> = _theme.asStateFlow()

    private val _searchHistory = MutableStateFlow(readHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    private val _dismissedUpdateVersion = MutableStateFlow(prefs.getString("dismissed_update", null))
    val dismissedUpdateVersion: StateFlow<String?> = _dismissedUpdateVersion.asStateFlow()

    private val _googleProfile = MutableStateFlow(readProfile())
    val googleProfile: StateFlow<GoogleProfile?> = _googleProfile.asStateFlow()

    fun setAmoled(value: Boolean) {
        prefs.edit().putBoolean("amoled", value).apply()
        _amoled.value = value
    }

    fun setAutoScan(value: Boolean) {
        prefs.edit().putBoolean("auto_scan", value).apply()
        _autoScan.value = value
    }

    fun setWifiOnly(value: Boolean) {
        prefs.edit().putBoolean("wifi_only", value).apply()
        _wifiOnly.value = value
    }

    fun setTheme(style: VibeThemeStyle) {
        prefs.edit().putString("theme_style", style.name).apply()
        _theme.value = style
        if (style == VibeThemeStyle.AMOLED) setAmoled(true)
        if (style != VibeThemeStyle.AMOLED && style != VibeThemeStyle.LIGHT) setAmoled(false)
    }

    // ---------------- Search history ----------------
    private fun readHistory(): List<String> = runCatching {
        val raw = prefs.getString("search_history", null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) add(array.optString(i))
        }.filter { it.isNotBlank() }.take(12)
    }.getOrDefault(emptyList())

    fun addSearchHistory(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val next = (listOf(clean) + _searchHistory.value.filterNot { it.equals(clean, true) })
            .take(12)
        _searchHistory.value = next
        val array = JSONArray()
        next.forEach(array::put)
        prefs.edit().putString("search_history", array.toString()).apply()
    }

    fun removeSearchHistory(query: String) {
        val next = _searchHistory.value.filterNot { it.equals(query, true) }
        _searchHistory.value = next
        val array = JSONArray()
        next.forEach(array::put)
        prefs.edit().putString("search_history", array.toString()).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove("search_history").apply()
    }

    // ---------------- Updates ----------------
    fun dismissUpdate(version: String) {
        prefs.edit().putString("dismissed_update", version).apply()
        _dismissedUpdateVersion.value = version
    }

    fun clearDismissedUpdate() {
        prefs.edit().remove("dismissed_update").apply()
        _dismissedUpdateVersion.value = null
    }

    fun lastUpdateCheck(): Long = prefs.getLong("last_update_check", 0L)

    fun markUpdateChecked(at: Long = System.currentTimeMillis()) {
        prefs.edit().putLong("last_update_check", at).apply()
    }

    // ---------------- Google account (profile only, never the token) ----------------
    private fun readProfile(): GoogleProfile? {
        val id = prefs.getString("google_id", null) ?: return null
        return GoogleProfile(
            id = id,
            name = prefs.getString("google_name", "").orEmpty(),
            email = prefs.getString("google_email", "").orEmpty()
        )
    }

    fun saveGoogleProfile(profile: GoogleProfile) {
        prefs.edit()
            .putString("google_id", profile.id)
            .putString("google_name", profile.name)
            .putString("google_email", profile.email)
            .apply()
        _googleProfile.value = profile
    }

    fun clearGoogleProfile() {
        prefs.edit()
            .remove("google_id")
            .remove("google_name")
            .remove("google_email")
            .apply()
        _googleProfile.value = null
    }

    /** Clears every preference owned by VibeMusic (used by “Eliminar mis datos”). */
    fun clearAll() {
        prefs.edit().clear().apply()
        _amoled.value = true
        _autoScan.value = true
        _wifiOnly.value = false
        _theme.value = VibeThemeStyle.AMOLED
        _searchHistory.value = emptyList()
        _dismissedUpdateVersion.value = null
        _googleProfile.value = null
    }
}
