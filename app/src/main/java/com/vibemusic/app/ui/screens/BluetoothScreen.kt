package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.audio.AudioProfileStore
import com.vibemusic.app.audio.EqPresets
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel
import kotlin.math.roundToInt

/**
 * Bluetooth output detection plus the equaliser. Android exposes the device
 * name and type, but neither the hardware model nor the negotiated codec
 * (privileged API), so both are handled honestly. Manufacturer is shown as a
 * hint inferred from the name and the user can type the real model, because a
 * Bluetooth name is editable and not an identifier.
 */
@Composable
fun BluetoothScreen(
    vm: VibeViewModel,
    onBack: () -> Unit,
    onRequestBluetoothPermission: () -> Unit
) {
    val eq by vm.eqState.collectAsStateWithLifecycle()
    val output by vm.bluetoothOutput.collectAsStateWithLifecycle()
    // Permissions change outside Compose (system dialog or Settings), so the
    // value is refreshed whenever the screen resumes.
    var permission by remember { mutableStateOf(vm.hasBluetoothPermission()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permission = vm.hasBluetoothPermission()
                vm.refreshBluetooth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var gains by remember(eq.presetName) { mutableStateOf(eq.gainsDb) }
    var model by rememberSaveable(eq.activeDeviceKey, eq.profiles) {
        mutableStateOf(eq.profiles.firstOrNull { it.deviceKey.equals(eq.activeDeviceKey, true) }?.model.orEmpty())
    }
    LaunchedEffect(eq.presetName, eq.gainsDb) { gains = eq.gainsDb }

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = VibeCream
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Bluetooth y ecualizador",
                            style = MaterialTheme.typography.headlineSmall,
                            color = VibeCream
                        )
                        Text(
                            "Perfiles para tus auriculares y altavoces",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                    IconButton(onClick = vm::refreshBluetooth) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Actualizar", tint = VibeCream)
                    }
                }
            }

            item { DeviceCard(
                name = output?.name ?: "Sin dispositivo externo",
                kind = output?.kind ?: "Se está usando el altavoz del teléfono",
                manufacturer = output?.manufacturer,
                inferred = output?.manufacturerInferred == true,
                codec = output?.codec,
                permission = permission,
                onRequestPermission = onRequestBluetoothPermission
            ) }

            if (!permission) {
                item {
                    NoticeCard(
                        "Para leer el nombre y el códec del dispositivo Bluetooth, Android 12 o superior " +
                            "requiere el permiso «Dispositivos cercanos». VibeMusic solo lo usa para mostrar " +
                            "la salida de audio actual."
                    )
                }
            }

            if (eq.activeDeviceKey != null) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        Text(
                            "Modelo del dispositivo",
                            style = MaterialTheme.typography.titleSmall,
                            color = VibeCream
                        )
                        Text(
                            "El nombre Bluetooth no identifica el modelo. Si lo conoces, escríbelo " +
                                "para asociarlo a este perfil.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it.take(60) },
                                singleLine = true,
                                label = { Text("Modelo (opcional)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VibeRed,
                                    unfocusedBorderColor = VibeLine,
                                    focusedTextColor = VibeCream,
                                    unfocusedTextColor = VibeCream,
                                    cursorColor = VibeRed
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { vm.setDeviceModel(model) }) {
                                Icon(Icons.Rounded.Save, contentDescription = "Guardar modelo", tint = VibeRed)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Ecualizador") }

            if (!eq.available) {
                item {
                    NoticeCard(
                        eq.message ?: "Este dispositivo no permite aplicar un ecualizador global. " +
                            "Puedes guardar perfiles, pero el efecto no se aplicará."
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Ecualización activa", style = MaterialTheme.typography.titleSmall, color = VibeCream)
                        Text(
                            "Aplica el perfil al audio de VibeMusic",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                    Switch(
                        checked = eq.enabled,
                        onCheckedChange = vm::setEqEnabled,
                        colors = SwitchDefaults.colors(checkedTrackColor = VibeRed)
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Aplicar al conectar", style = MaterialTheme.typography.titleSmall, color = VibeCream)
                        Text(
                            "Recuerda el perfil de cada dispositivo y aplícalo automáticamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                    Switch(
                        checked = eq.autoApply,
                        onCheckedChange = vm::setEqAutoApply,
                        colors = SwitchDefaults.colors(checkedTrackColor = VibeRed)
                    )
                }
            }

            item {
                LazyRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(EqPresets.all) { preset ->
                        FilterChip(
                            selected = eq.presetName == preset.name,
                            onClick = { vm.applyEqPreset(preset.name) },
                            label = { Text(preset.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VibeRed,
                                selectedLabelColor = VibeCream,
                                labelColor = VibeCream
                            )
                        )
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        EqPresets.byName(eq.presetName).description,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    EqPresets.anchorLabels.forEachIndexed { index, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = VibeMuted,
                                modifier = Modifier.width(96.dp)
                            )
                            Slider(
                                value = gains.getOrElse(index) { 0f },
                                onValueChange = { value ->
                                    gains = gains.toMutableList().also { it[index] = value }
                                },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(
                                    thumbColor = VibeRed,
                                    activeTrackColor = VibeRed,
                                    inactiveTrackColor = VibeSurface
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${gains.getOrElse(index) { 0f }.roundToInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibeMuted,
                                modifier = Modifier.width(46.dp)
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                // Keep the preset name when the sliders were
                                // not touched; only manual changes become "Custom".
                                val label = if (gains == eq.gainsDb) eq.presetName else "Custom"
                                vm.applyEqGains(gains, label)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibeRed),
                            modifier = Modifier.weight(1f)
                        ) { Text("Aplicar ajuste") }
                        TextButton(onClick = vm::resetEq) { Text("Restablecer", color = VibeMuted) }
                    }
                    if (eq.activeDeviceKey != null) {
                        TextButton(onClick = vm::saveEqForActiveDevice) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = VibeRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar perfil de este dispositivo", color = VibeRed)
                        }
                    }
                    if (eq.bands.isNotEmpty()) {
                        Text(
                            "El dispositivo expone ${eq.bands.size} bandas reales. VibeMusic asigna " +
                                "los cinco controles a la banda más cercana de cada rango.",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibeMuted
                        )
                    }
                }
            }

            if (eq.profiles.isNotEmpty()) {
                item { SectionHeader("Perfiles guardados") }
                items(eq.profiles, key = { it.deviceKey }) { profile ->
                    ProfileRow(
                        profile = profile,
                        onApply = { vm.applyEqGains(profile.gainsDb, profile.presetName) },
                        onDelete = { vm.removeEqProfile(profile.deviceKey) }
                    )
                }
            }

            item {
                NoticeCard(
                    "El ecualizador usa la API pública de Android sobre la sesión de audio global. " +
                        "Si tu fabricante no expone efectos, VibeMusic lo indica en vez de simular que funciona. " +
                        "No se inventan mediciones acústicas por modelo: los presets son ajustes de partida.\n\n" +
                        "El códec negociado (SBC, AAC, aptX, LDAC…) solo está disponible para apps del " +
                        "sistema con permisos privilegiados, así que la app no puede mostrarlo sin inventarlo."
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(
    name: String,
    kind: String,
    manufacturer: String?,
    inferred: Boolean,
    codec: String?,
    permission: Boolean,
    onRequestPermission: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = VibeSurfaceHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(VibeRed.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (kind.contains("Altavoz", true)) Icons.Rounded.Speaker else Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = VibeRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = VibeCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(kind, style = MaterialTheme.typography.bodySmall, color = VibeMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (manufacturer != null) {
                    InfoTag(
                        if (inferred) "$manufacturer (según el nombre)" else manufacturer
                    )
                }
                if (codec != null) InfoTag("Códec $codec")
                if (!permission) InfoTag("Permiso pendiente")
            }
            if (!permission) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = VibeRed)
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Permitir dispositivos cercanos")
                }
            }
        }
    }
}

@Composable
private fun InfoTag(text: String) {
    Surface(shape = RoundedCornerShape(50), color = VibeSurface) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = VibeMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun NoticeCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = VibeSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = VibeMuted,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
private fun ProfileRow(
    profile: AudioProfileStore.DeviceProfile,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.BluetoothAudio,
            contentDescription = null,
            tint = VibeRed,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.model ?: profile.deviceKey,
                style = MaterialTheme.typography.titleSmall,
                color = VibeCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${profile.presetName} · ${profile.gainsDb.joinToString(" / ") { "${it.roundToInt()}" }} dB",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar perfil", tint = VibeMuted)
        }
    }
}
