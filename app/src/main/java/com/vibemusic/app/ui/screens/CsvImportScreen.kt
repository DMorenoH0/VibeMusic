package com.vibemusic.app.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibemusic.app.data.csv.CsvColumnRole
import com.vibemusic.app.ui.components.ErrorBanner
import com.vibemusic.app.ui.components.SectionHeader
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import com.vibemusic.app.ui.theme.VibeSurfaceHigh
import com.vibemusic.app.viewmodel.VibeViewModel

@Composable
fun CsvImportScreen(
    vm: VibeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.csvImport.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.openCsvFile(uri, name)
        }
    }

    VibePageBackground {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 40.dp)
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
                    Column {
                        Text("Importar CSV", style = MaterialTheme.typography.headlineLarge, color = VibeCream)
                        Text(
                            "Listas desde TuneMyMusic u otros servicios",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = VibeSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Selecciona un archivo CSV. VibeMusic detecta el separador y las columnas de título, artista, álbum, duración e ISRC. Las canciones se emparejan con tu biblioteca para que la lista sea reproducible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            onClick = { picker.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv", "*/*")) },
                            shape = RoundedCornerShape(50),
                            color = VibeRed
                        ) {
                            Row(
                                Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (state.table == null) "Elegir archivo CSV" else "Elegir otro archivo",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        }
                    }
                }
            }

            state.error?.let { message ->
                item { ErrorBanner(message = message) }
            }

            val table = state.table
            if (table != null) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            state.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            color = VibeCream,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${table.rows.size} filas · separador ${delimiterName(table.delimiter)} · ${table.headers.size} columnas",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                }

                if (!state.finished) {
                    item { SectionHeader("Asignación de columnas") }
                    items(table.headers.size) { index ->
                        ColumnMappingRow(
                            header = table.headers[index],
                            role = state.mapping.getOrElse(index) { CsvColumnRole.IGNORE },
                            detected = state.mapping.getOrElse(index) { CsvColumnRole.IGNORE } != CsvColumnRole.IGNORE,
                            onRole = { role -> vm.updateCsvMapping(index, role) }
                        )
                    }

                    item { SectionHeader("Vista previa") }
                    item {
                        val matches = state.matches
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${matches?.matched?.size ?: 0} coincidencias en tu biblioteca · " +
                                    "${matches?.unmatched?.size ?: 0} sin reconocer · " +
                                    "${(matches?.duplicates ?: 0)} duplicadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                            Spacer(Modifier.height(8.dp))
                            (matches?.matched?.take(6) ?: emptyList()).forEach { match ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VibeRed, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            match.track.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VibeCream,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            match.track.artist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VibeMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            (matches?.unmatched?.take(4) ?: emptyList()).forEach { record ->
                                Text(
                                    "Sin coincidencia: ${record.title}${if (record.artist.isBlank()) "" else " · ${record.artist}"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    item {
                        PlaylistTargetPicker(
                            playlists = playlists.map { it.playlist.id to it.playlist.name },
                            selected = state.targetPlaylistId,
                            onSelect = vm::setCsvTargetPlaylist
                        )
                    }

                    item {
                        val matchedCount = state.matches?.matched?.size ?: 0
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            if (state.importing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = VibeRed,
                                    trackColor = VibeSurface
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Surface(
                                onClick = {
                                    if (!state.importing && matchedCount > 0) vm.importCsv()
                                },
                                shape = RoundedCornerShape(50),
                                color = if (matchedCount > 0 && !state.importing) VibeRed else VibeSurfaceHigh
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.UploadFile,
                                        contentDescription = null,
                                        tint = if (matchedCount > 0) androidx.compose.ui.graphics.Color.White else VibeMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (state.targetPlaylistId == null) {
                                            "Crear lista e importar $matchedCount"
                                        } else {
                                            "Importar $matchedCount canciones"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (matchedCount > 0) androidx.compose.ui.graphics.Color.White else VibeMuted
                                    )
                                }
                            }
                            if (matchedCount == 0) {
                                Text(
                                    "Ninguna canción del CSV coincide con tu biblioteca. Escanea o importa primero esos archivos para poder añadirlos.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VibeMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VibeRed, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Importación completada", style = MaterialTheme.typography.titleMedium, color = VibeCream)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.imported} canciones añadidas · ${state.skippedDuplicates} duplicadas omitidas · ${state.unmatched.size} sin reconocer",
                                style = MaterialTheme.typography.bodySmall,
                                color = VibeMuted
                            )
                            if (state.unmatched.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                state.unmatched.take(10).forEach { record ->
                                    Text(
                                        "• ${record.title}${if (record.artist.isBlank()) "" else " — ${record.artist}"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VibeMuted
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Estas cifras se basan en las coincidencias reales con tu biblioteca; no se inventan pistas que no puedas reproducir.",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibeMuted
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    onClick = { vm.dismissCsvImport(); picker.launch(arrayOf("text/*")) },
                                    shape = RoundedCornerShape(50),
                                    color = VibeSurfaceHigh
                                ) {
                                    Text(
                                        "Importar otro",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibeCream,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                                Surface(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(50),
                                    color = VibeRed
                                ) {
                                    Text(
                                        "Volver a Ajustes",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.importing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VibeRed)
        }
    }
}

@Composable
private fun ColumnMappingRow(
    header: String,
    role: CsvColumnRole,
    detected: Boolean,
    onRole: (CsvColumnRole) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(VibeSurface.copy(alpha = 0.55f))
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                header.ifBlank { "(sin nombre)" },
                style = MaterialTheme.typography.bodyMedium,
                color = VibeCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (detected) "Detectada automáticamente" else "Sin asignar",
                style = MaterialTheme.typography.labelSmall,
                color = VibeMuted
            )
        }
        Text(
            roleLabel(role),
            style = MaterialTheme.typography.labelLarge,
            color = if (role == CsvColumnRole.IGNORE) VibeMuted else VibeRed,
            fontWeight = FontWeight.SemiBold
        )
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(18.dp))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = VibeSurfaceHigh
        ) {
            CsvColumnRole.values().forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            roleLabel(option),
                            color = if (option == role) VibeRed else VibeCream
                        )
                    },
                    onClick = {
                        expanded = false
                        onRole(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaylistTargetPicker(
    playlists: List<Pair<Long, String>>,
    selected: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = playlists.firstOrNull { it.first == selected }?.second ?: "Crear una lista nueva"
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text("Añadir a", style = MaterialTheme.typography.labelMedium, color = VibeMuted)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(VibeSurface)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = VibeCream, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = VibeMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = VibeSurfaceHigh
        ) {
            DropdownMenuItem(
                text = { Text("Crear una lista nueva", color = if (selected == null) VibeRed else VibeCream) },
                onClick = {
                    expanded = false
                    onSelect(null)
                }
            )
            playlists.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = if (selected == id) VibeRed else VibeCream) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    }
                )
            }
        }
    }
}

private fun roleLabel(role: CsvColumnRole): String = when (role) {
    CsvColumnRole.TITLE -> "Título"
    CsvColumnRole.ARTIST -> "Artista"
    CsvColumnRole.ALBUM -> "Álbum"
    CsvColumnRole.DURATION -> "Duración"
    CsvColumnRole.ISRC -> "ISRC"
    CsvColumnRole.IGNORE -> "Ignorar"
}

private fun delimiterName(delimiter: Char): String = when (delimiter) {
    ';' -> "punto y coma"
    '\t' -> "tabulador"
    else -> "coma"
}
