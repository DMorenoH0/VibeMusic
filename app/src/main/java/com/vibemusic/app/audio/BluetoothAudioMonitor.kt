package com.vibemusic.app.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Detects the current audio output with official Android APIs.
 *
 * Android does not expose a public "headphone model" database: a Bluetooth
 * name is a user-editable label, not proof of the model. This monitor therefore
 * reports only what the platform actually provides and marks manufacturer
 * guesses as inferred from the name. The user can always type the real model.
 */
class BluetoothAudioMonitor(private val context: Context) {

    data class AudioOutput(
        val key: String,
        val name: String,
        val kind: String,
        val manufacturer: String?,
        val manufacturerInferred: Boolean,
        val model: String?,
        val codec: String?,
        val bluetooth: Boolean,
        val wired: Boolean
    )

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val _output = MutableStateFlow<AudioOutput?>(null)
    val output: StateFlow<AudioOutput?> = _output.asStateFlow()

    private var started = false

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
    }

    fun start() {
        if (started) return
        started = true
        audioManager?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { audioManager?.unregisterAudioDeviceCallback(callback) }
    }

    fun refresh() {
        val manager = audioManager ?: return
        val devices = runCatching {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }.getOrDefault(emptyList())
        val relevant = devices.filter { it.isRelevantOutput() }
        // Prefer a Bluetooth/wired output over the built-in speaker.
        val selected = relevant.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: relevant.firstOrNull { it.isBluetoothType() }
            ?: relevant.firstOrNull()
        _output.value = selected?.toOutput()
    }

    private fun AudioDeviceInfo.toOutput(): AudioOutput {
        val rawName = productName?.toString()?.trim().orEmpty()
            .ifBlank { "Dispositivo de audio" }
        val type = type
        val bluetooth = isBluetoothType()
        val wired = type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE
        )
        val brand = if (bluetooth) guessBrand(rawName) else null
        return AudioOutput(
            key = rawName,
            name = rawName,
            kind = when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Auriculares/altavoz Bluetooth (A2DP)"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Auriculares Bluetooth (llamadas)"
                AudioDeviceInfo.TYPE_BLE_HEADSET -> "Auriculares Bluetooth LE"
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Altavoz Bluetooth LE"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Auriculares con cable"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Auriculares con micrófono"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "Auriculares USB"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "Dispositivo de audio USB"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Altavoz del teléfono"
                else -> "Salida de audio"
            },
            manufacturer = brand,
            manufacturerInferred = brand != null,
            // A Bluetooth name is not a model id; do not pretend otherwise.
            model = null,
            // The negotiated codec (SBC/AAC/aptX/LDAC) is only exposed to
            // privileged system apps through @SystemApi, so no public API is
            // used here and no codec is invented.
            codec = null,
            bluetooth = bluetooth,
            wired = wired
        )
    }

    private fun AudioDeviceInfo.isRelevantOutput(): Boolean =
        isBluetoothType() || type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE
        )

    private fun AudioDeviceInfo.isBluetoothType(): Boolean = type in setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER
    )

    companion object {

        fun hasBluetoothPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun bluetoothPermission(): String = Manifest.permission.BLUETOOTH_CONNECT

        /**
         * Only a hint for the UI. It is never used to apply acoustic
         * corrections automatically and the label says "según el nombre".
         */
        fun guessBrand(name: String): String? {
            val value = name.lowercase()
            val brands = listOf(
                "sony" to "Sony",
                "jbl" to "JBL",
                "bose" to "Bose",
                "sennheiser" to "Sennheiser",
                "samsung" to "Samsung",
                "xiaomi" to "Xiaomi",
                "redmi" to "Xiaomi",
                "apple" to "Apple",
                "airpods" to "Apple",
                "marshall" to "Marshall",
                "steelseries" to "SteelSeries",
                "beats" to "Beats",
                "soundcore" to "Anker",
                "anker" to "Anker",
                "huawei" to "Huawei",
                "oneplus" to "OnePlus",
                "realme" to "realme",
                "oppo" to "OPPO",
                "nothing" to "Nothing",
                "google" to "Google",
                "pixel" to "Google"
            )
            return brands.firstOrNull { value.contains(it.first) }?.second
        }
    }
}
