package com.vibemusic.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibe_settings", Context.MODE_PRIVATE)

    private val _amoled = MutableStateFlow(prefs.getBoolean("amoled", true))
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _autoScan = MutableStateFlow(prefs.getBoolean("auto_scan", true))
    val autoScan: StateFlow<Boolean> = _autoScan.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.getBoolean("wifi_only", false))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

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
}
