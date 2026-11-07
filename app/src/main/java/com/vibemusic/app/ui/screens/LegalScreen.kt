package com.vibemusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibemusic.app.data.legal.LegalTexts
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeLine
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface

@Composable
fun LegalScreen(
    initial: String,
    onBack: () -> Unit
) {
    var key by rememberSaveable(initial) { mutableStateOf(initial) }
    val document = LegalTexts.document(key)

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
                    Text(
                        "Legal",
                        style = MaterialTheme.typography.headlineLarge,
                        color = VibeCream
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "privacy" to "Privacidad",
                        "terms" to "Términos",
                        "cookies" to "Cookies"
                    ).forEach { (docKey, label) ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (key == docKey) VibeRed else VibeSurface,
                            modifier = Modifier.clickable { key = docKey }
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (key == docKey) Color.White else VibeMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(
                        document.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VibeCream
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Actualizado: ${document.updated}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        document.intro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = VibeMuted
                    )
                }
            }

            items(document.sections.size) { index ->
                val (title, body) = document.sections[index]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = VibeCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VibeSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        "Este documento describe el comportamiento real de VibeMusic 1.1.0. Si una función opcional (como el inicio de sesión con Google) no está configurada, no se transmite ningún dato a ese servicio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
