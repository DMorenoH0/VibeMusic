package com.vibemusic.app.audio

import android.content.Context
import org.json.JSONObject

/**
 * Persists one equalisation profile per audio device (Bluetooth speaker,
 * headphones, wired set...). Keys are the user-visible device names because
 * Android does not expose a stable public hardware id for audio outputs.
 */
class AudioProfileStore(context: Context) {

    data class DeviceProfile(
        val deviceKey: String,
        val presetName: String,
        val gainsDb: List<Float>,
        val autoApply: Boolean,
        val model: String? = null
    )

    private val prefs = context.applicationContext
        .getSharedPreferences("vibe_audio_profiles", Context.MODE_PRIVATE)

    var autoApply: Boolean
        get() = prefs.getBoolean("auto_apply", false)
        set(value) = prefs.edit().putBoolean("auto_apply", value).apply()

    var customGains: List<Float>
        get() = readGains("custom_gains") ?: List(EqPresets.anchorsHz.size) { 0f }
        set(value) {
            prefs.edit().putString("custom_gains", encode(value)).apply()
        }

    fun profileFor(deviceKey: String): DeviceProfile? {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val item = root.optJSONObject(normalize(deviceKey)) ?: return null
            DeviceProfile(
                deviceKey = deviceKey,
                presetName = item.optString("preset", "Balanced"),
                gainsDb = readGains(item.optString("gains")) ?: EqPresets.byName(item.optString("preset")).gainsDb,
                autoApply = item.optBoolean("auto", autoApply),
                model = item.optString("model").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    fun saveProfile(
        deviceKey: String,
        presetName: String,
        gainsDb: List<Float>,
        auto: Boolean,
        model: String? = null
    ) {
        val root = runCatching {
            JSONObject(prefs.getString(KEY_PROFILES, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        val existingModel = root.optJSONObject(normalize(deviceKey))
            ?.optString("model")
            ?.takeIf { it.isNotBlank() }
        root.put(
            normalize(deviceKey),
            JSONObject()
                .put("preset", presetName)
                .put("gains", encode(gainsDb))
                .put("auto", auto)
                .put("model", model ?: existingModel.orEmpty())
        )
        prefs.edit().putString(KEY_PROFILES, root.toString()).apply()
    }

    /** Stores a user-supplied model name; a Bluetooth name is not a model. */
    fun setDeviceModel(deviceKey: String, model: String) {
        val existing = profileFor(deviceKey)
        saveProfile(
            deviceKey = deviceKey,
            presetName = existing?.presetName ?: "Balanced",
            gainsDb = existing?.gainsDb ?: List(EqPresets.anchorsHz.size) { 0f },
            auto = existing?.autoApply ?: autoApply,
            model = model.trim().take(60).takeIf { it.isNotBlank() }
        )
    }

    fun removeProfile(deviceKey: String) {
        val root = runCatching {
            JSONObject(prefs.getString(KEY_PROFILES, "{}") ?: "{}")
        }.getOrDefault(JSONObject())
        root.remove(normalize(deviceKey))
        prefs.edit().putString(KEY_PROFILES, root.toString()).apply()
    }

    fun profiles(): Map<String, DeviceProfile> {
        val raw = prefs.getString(KEY_PROFILES, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { key ->
                    val item = root.optJSONObject(key) ?: return@forEach
                    put(
                        key,
                        DeviceProfile(
                            deviceKey = key,
                            presetName = item.optString("preset", "Balanced"),
                            gainsDb = readGains(item.optString("gains"))
                                ?: EqPresets.byName(item.optString("preset")).gainsDb,
                            autoApply = item.optBoolean("auto", autoApply),
                            model = item.optString("model").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun normalize(key: String): String =
        key.trim().lowercase().replace(Regex("\\s+"), " ")

    private fun encode(gains: List<Float>): String =
        gains.joinToString(",") { it.toString() }

    private fun readGains(raw: String?): List<Float>? {
        if (raw.isNullOrBlank()) return null
        val values = raw.split(',').mapNotNull { it.trim().toFloatOrNull() }
        return if (values.size == EqPresets.anchorsHz.size) values else null
    }

    private companion object {
        const val KEY_PROFILES = "profiles"
    }
}
