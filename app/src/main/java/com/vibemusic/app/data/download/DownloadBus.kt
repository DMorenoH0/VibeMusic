package com.vibemusic.app.data.download

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Process-wide progress channel used by the foreground service and the UI. */
object DownloadBus {

    private val _state = MutableStateFlow<DownloadProgress?>(null)
    val state: StateFlow<DownloadProgress?> = _state.asStateFlow()

    /** Last accepted request, kept so the UI can offer "Reintentar". */
    @Volatile
    var lastRequest: DownloadRequest? = null
        private set

    fun publish(progress: DownloadProgress) {
        _state.value = progress
    }

    fun remember(request: DownloadRequest) {
        lastRequest = request
    }

    fun forget() {
        lastRequest = null
    }

    fun clear() {
        _state.value = null
    }

    // ---------------- Recovery across process death ----------------

    fun savePending(context: Context, request: DownloadRequest) {
        val json = JSONObject()
            .put("url", request.url)
            .put("title", request.title)
            .put("artist", request.artist)
            .put("album", request.album)
            .put("artworkUrl", request.artworkUrl ?: "")
            .put("quality", request.quality.name)
            .put("wifiOnly", request.wifiOnly)
            .put("trackId", request.trackId ?: "")
            .put("localCopy", request.localCopy)
        prefs(context).edit().putString(KEY_PENDING, json.toString()).apply()
    }

    fun loadPending(context: Context): DownloadRequest? {
        val raw = prefs(context).getString(KEY_PENDING, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            DownloadRequest(
                url = json.getString("url"),
                title = json.optString("title"),
                artist = json.optString("artist"),
                album = json.optString("album"),
                artworkUrl = json.optString("artworkUrl").takeIf { it.isNotBlank() },
                quality = DownloadQuality.byName(json.optString("quality")),
                wifiOnly = json.optBoolean("wifiOnly", true),
                trackId = json.optString("trackId").takeIf { it.isNotBlank() },
                localCopy = json.optBoolean("localCopy", false)
            )
        }.getOrNull()
    }

    fun clearPending(context: Context) {
        prefs(context).edit().remove(KEY_PENDING).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("vibe_downloads", Context.MODE_PRIVATE)

    private const val KEY_PENDING = "pending_request"
}
