package com.vibemusic.app.data.youtube

private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{5,20}")

/**
 * Embeds a validated YouTube identifier in the local HTML template. The
 * template itself is an asset, so it can be audited and changed without
 * touching Kotlin code. Invalid identifiers return an empty string instead of
 * being injected into the page.
 */
fun renderYouTubePlayerHtml(template: String, videoId: String): String {
    if (!videoId.matches(YOUTUBE_VIDEO_ID)) return ""
    return template.replace("__VIDEO_ID__", videoId)
}

/**
 * Messages shown when the official embedded player reports an error. Codes
 * follow the YouTube IFrame Player API:
 * 2 invalid parameter, 5 HTML5 error, 100 not found/private,
 * 101 and 150 embedding disabled by the owner, 153 missing referrer.
 * -1 is our own timeout when the player never becomes ready.
 */
fun youtubePlayerErrorMessage(code: Int): String = when (code) {
    2 -> "El identificador del vídeo de YouTube no es válido."
    5 -> "El reproductor HTML5 no pudo iniciar este vídeo."
    100 -> "El vídeo no existe, es privado o ha sido eliminado."
    101, 150 -> "El propietario no permite reproducir este vídeo de forma integrada. Prueba con otro resultado."
    152 -> "YouTube no permitió cargar el reproductor integrado para este vídeo. Prueba con otro resultado."
    153 -> "YouTube no permite reproducir este vídeo integrado en la aplicación."
    -1 -> "El reproductor de YouTube no respondió. Comprueba tu conexión e inténtalo de nuevo."
    -2 -> "YouTube no pudo reproducir este vídeo. Puede no existir, ser privado o no permitir la reproducción integrada."
    else -> "No se pudo reproducir este vídeo (código $code)."
}
