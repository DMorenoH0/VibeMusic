package com.vibemusic.app.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vibemusic.app.data.youtube.YouTubeVideo
import com.vibemusic.app.data.youtube.renderYouTubePlayerHtml
import com.vibemusic.app.data.youtube.youtubePlayerErrorMessage
import com.vibemusic.app.ui.components.VibePageBackground
import com.vibemusic.app.ui.formatVideoDuration
import com.vibemusic.app.ui.theme.VibeCream
import com.vibemusic.app.ui.theme.VibeMuted
import com.vibemusic.app.ui.theme.VibeRed
import com.vibemusic.app.ui.theme.VibeSurface
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Full-screen player for YouTube search results. The video is loaded through
 * the official IFrame Player API inside a WebView, so YouTube keeps its own
 * controls, branding, ads and playback restrictions. Nothing is extracted,
 * downloaded or proxied: VibeMusic only embeds the official player.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(
    video: YouTubeVideo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var loading by remember(video.id) { mutableStateOf(true) }
    var errorMessage by remember(video.id) { mutableStateOf<String?>(null) }

    val template = remember {
        runCatching {
            context.assets.open("youtube_player.html").bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }
    val html = remember(template, video.id) { renderYouTubePlayerHtml(template, video.id) }
    val invalidVideo = html.isEmpty()
    val displayedError = errorMessage
        ?: if (invalidVideo) youtubePlayerErrorMessage(2) else null

    val webView = remember(video.id) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // The user already tapped a result: allow the official player
                // to start without waiting for a second gesture.
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccess = false
                allowContentAccess = false
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        mainHandler.post {
                            loading = false
                            errorMessage =
                                "No se pudo cargar el reproductor de YouTube. Comprueba tu conexión."
                        }
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail
                ): Boolean {
                    mainHandler.post {
                        loading = false
                        errorMessage =
                            "El reproductor se cerró inesperadamente. Vuelve a intentarlo."
                    }
                    return true
                }

                // Keep every navigation inside this WebView. VibeMusic never
                // launches YouTube, Chrome or another external application.
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false
            }
        }
    }

    // The IFrame API does not report every failure (an unavailable video can
    // simply stay UNSTARTED), so poll the official player state natively.
    // A player that never leaves UNSTARTED means YouTube refused to play it.
    LaunchedEffect(webView, invalidVideo) {
        if (invalidVideo) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        var apiReady = false
        while (System.currentTimeMillis() - startedAt < 15_000 && errorMessage == null) {
            delay(700)
            val raw = webView.evaluateJavascriptValue(
                "(function(){try{return window.player ? String(player.getPlayerState()) : 'x';}" +
                    "catch(e){return 'x';}})()"
            ).trim().trim('"')
            if (raw == "x") continue
            val state = raw.toIntOrNull() ?: continue
            apiReady = true
            if (state >= 0) {
                loading = false
                return@LaunchedEffect
            }
        }
        loading = false
        if (errorMessage == null) {
            errorMessage = youtubePlayerErrorMessage(if (apiReady) -2 else -1)
        }
    }

    // If an error replaces the player, make sure no stray audio keeps playing.
    LaunchedEffect(displayedError) {
        if (displayedError != null) {
            webView.evaluateJavascript("vibePause();", null)
        }
    }

    DisposableEffect(webView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    webView.evaluateJavascript("vibePause();", null)
                    webView.onPause()
                    webView.pauseTimers()
                }

                Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START -> {
                    webView.onResume()
                    webView.resumeTimers()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.stopLoading()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }

    VibePageBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver a los resultados",
                        tint = VibeCream
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "YouTube",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VibeCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Reproductor integrado",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeMuted
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (displayedError == null && !invalidVideo) {
                    AndroidView(
                        factory = {
                            webView.apply {
                                loadDataWithBaseURL(
                                    "https://com.vibemusic.app",
                                    html,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (displayedError != null) {
                    PlayerErrorPanel(displayedError)
                }

                if (loading && displayedError == null) {
                    CircularProgressIndicator(
                        color = VibeRed,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = VibeCream,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    video.channel.ifBlank { "YouTube" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VibeMuted
                )

                val duration = formatVideoDuration(video.durationMs)
                val published = formatPublishedDate(video.publishedAt)
                if (duration.isNotEmpty() || published.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(
                            duration.takeIf { it.isNotEmpty() },
                            published.takeIf { it.isNotEmpty() }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = VibeMuted
                    )
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    color = VibeSurface,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = VibeRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Reproductor oficial de YouTube. Se mantienen sus controles, " +
                                "anuncios, marca y restricciones. VibeMusic no extrae audio, " +
                                "no descarga el vídeo y no bloquea anuncios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibeMuted
                        )
                    }
                }

                if (video.description.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Información",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibeCream,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = VibeMuted,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(22.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Volver a los resultados")
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Si el propietario ha desactivado la reproducción integrada, YouTube " +
                        "mostrará un error y el vídeo no se reproducirá aquí. La música local, " +
                        "la cola y las notificaciones de VibeMusic no dependen de YouTube.",
                    style = MaterialTheme.typography.labelSmall,
                    color = VibeMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerErrorPanel(message: String) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = VibeRed,
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = VibeCream,
            textAlign = TextAlign.Center
        )
    }
}

/** Shows yyyy-MM-dd from the API timestamp without requiring java.time on API 24. */
private fun formatPublishedDate(publishedAt: String): String {
    val date = publishedAt.trim().take(10)
    return if (date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) date else ""
}

/**
 * Evaluates a JavaScript expression in the player WebView and returns its raw
 * JSON-encoded result (for example "\"1\""). Runs on the main thread, which is
 * where WebView methods must be used.
 */
private suspend fun WebView.evaluateJavascriptValue(script: String): String =
    suspendCancellableCoroutine { cont ->
        evaluateJavascript(script) { value ->
            if (cont.isActive) cont.resume(value ?: "")
        }
    }
