package com.vibemusic.app.data.download

import com.vibemusic.app.data.model.Track
import com.vibemusic.app.data.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongDownloadResolverTest {

    private fun track(
        id: String,
        source: TrackSource,
        title: String = "Afterglow",
        artist: String = "Luna Vale",
        uri: String = "file:///data/user/0/com.vibemusic.app/files/demo/afterglow.wav",
        downloadUrl: String? = null,
        downloadBlockedReason: String? = null
    ) = Track(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = "Afterglow",
        durationMs = 40_000L,
        source = source,
        downloadUrl = downloadUrl,
        downloadBlockedReason = downloadBlockedReason
    )

    @Test
    fun `demo song becomes a one tap local request`() {
        val demo = track("demo:afterglow", TrackSource.DEMO)
        val availability = SongDownloadResolver.resolve(demo)

        assertTrue(availability is SongDownloadAvailability.Available)
        val request = (availability as SongDownloadAvailability.Available).request
        assertEquals("demo:afterglow", request.trackId)
        assertTrue(request.localCopy)
        assertEquals(false, request.wifiOnly)
        assertEquals(DownloadQuality.HIGH, request.quality)
        assertEquals(demo.uri, request.url)
        assertEquals("Afterglow", request.title)
        assertEquals("Luna Vale", request.artist)
    }

    @Test
    fun `downloaded demo is not offered twice`() {
        val demo = track("demo:afterglow", TrackSource.DEMO)
        val availability = SongDownloadResolver.resolve(
            demo,
            downloadedKeys = setOf("demo:afterglow")
        )
        assertTrue(availability is SongDownloadAvailability.Downloaded)
    }

    @Test
    fun `identity match also prevents duplicate copies`() {
        val demo = track("demo:afterglow", TrackSource.DEMO)
        val availability = SongDownloadResolver.resolve(
            demo,
            downloadedIdentities = setOf(SongDownloadResolver.identity(demo))
        )
        assertTrue(availability is SongDownloadAvailability.Downloaded)
    }

    @Test
    fun `deezer preview cannot be downloaded`() {
        val online = track(
            id = "online:1",
            source = TrackSource.ONLINE,
            uri = "https://cdns-preview.deezer.com/stream/abc.mp3"
        )
        val availability = SongDownloadResolver.resolve(online)

        assertTrue(availability is SongDownloadAvailability.Unavailable)
        assertTrue(
            (availability as SongDownloadAvailability.Unavailable).reason.contains("vistas previas")
        )
    }

    @Test
    fun `youtube reference explains why download is unavailable`() {
        val youtube = track("youtube:abc", TrackSource.YOUTUBE, uri = "https://www.youtube.com/watch?v=abc")
        val availability = SongDownloadResolver.resolve(youtube)

        assertTrue(availability is SongDownloadAvailability.Unavailable)
        assertTrue(
            (availability as SongDownloadAvailability.Unavailable).reason.contains("YouTube")
        )
    }

    @Test
    fun `local file already plays offline and needs no copy`() {
        val local = track("local:1", TrackSource.LOCAL, uri = "content://media/external/audio/media/1")
        val availability = SongDownloadResolver.resolve(local)

        assertTrue(availability is SongDownloadAvailability.Unavailable)
        assertTrue(
            (availability as SongDownloadAvailability.Unavailable).reason.contains("sin conexión")
        )
    }

    @Test
    fun `authorised direct url preserves metadata and quality`() {
        val direct = track(
            id = "provider:9",
            source = TrackSource.IMPORTED,
            title = "Original",
            artist = "Artist",
            downloadUrl = "https://provider.example/audio/original.mp3"
        )
        val availability = SongDownloadResolver.resolve(
            direct,
            quality = DownloadQuality.MEDIUM,
            wifiOnly = false
        )

        assertTrue(availability is SongDownloadAvailability.Available)
        val request = (availability as SongDownloadAvailability.Available).request
        assertEquals("https://provider.example/audio/original.mp3", request.url)
        assertEquals("Original", request.title)
        assertEquals("Artist", request.artist)
        assertEquals("Afterglow", request.album)
        assertEquals(DownloadQuality.MEDIUM, request.quality)
        assertEquals(false, request.wifiOnly)
        assertEquals("https://provider.example/audio/original.mp3", request.trackId)
        assertEquals(false, request.localCopy)
    }

    @Test
    fun `key is stable for downloads and demo songs`() {
        val demo = track("demo:crimson_drive", TrackSource.DEMO)
        val direct = track(
            "provider:1",
            TrackSource.LOCAL,
            downloadUrl = "https://provider.example/a.mp3 "
        )
        val preview = track("online:1", TrackSource.ONLINE)

        assertEquals("demo:crimson_drive", SongDownloadResolver.keyFor(demo))
        assertEquals("https://provider.example/a.mp3", SongDownloadResolver.keyFor(direct))
        assertEquals(null, SongDownloadResolver.keyFor(preview))
    }

    @Test
    fun `audius authorised download keeps identity metadata and artwork`() {
        val audius = track(
            id = "audius:track:492515762",
            source = TrackSource.AUDIUS,
            title = "Dance With The Afterglow",
            artist = "Luna Vale",
            uri = "https://api.audius.co/v1/tracks/492515762/stream?app_name=VibeMusic",
            downloadUrl = "https://api.audius.co/v1/tracks/492515762/download?app_name=VibeMusic"
        ).copy(artworkUri = "https://creatornode.audius.co/artwork/cover.jpg")

        val availability = SongDownloadResolver.resolve(audius)

        assertTrue(availability is SongDownloadAvailability.Available)
        val request = (availability as SongDownloadAvailability.Available).request
        assertEquals(
            "https://api.audius.co/v1/tracks/492515762/download?app_name=VibeMusic",
            request.url
        )
        assertEquals("audius:track:492515762", request.trackId)
        assertEquals("https://creatornode.audius.co/artwork/cover.jpg", request.artworkUrl)
        assertEquals("Dance With The Afterglow", request.title)
    }

    @Test
    fun `audius gated download shows the provider reason and no request`() {
        val reason = "El artista pide que lo sigas en Audius antes de descargar."
        val audius = track(
            id = "audius:track:7",
            source = TrackSource.AUDIUS,
            uri = "https://api.audius.co/v1/tracks/7/stream?app_name=VibeMusic",
            downloadBlockedReason = reason
        )

        val availability = SongDownloadResolver.resolve(audius)

        assertTrue(availability is SongDownloadAvailability.Unavailable)
        val unavailable = availability as SongDownloadAvailability.Unavailable
        assertEquals(reason, unavailable.reason)
        assertEquals("Descarga no disponible", unavailable.label)
    }

    @Test
    fun `audius without an authorised file is rejected`() {
        val audius = track(
            id = "audius:track:8",
            source = TrackSource.AUDIUS,
            uri = "https://api.audius.co/v1/tracks/8/stream?app_name=VibeMusic"
        )
        val availability = SongDownloadResolver.resolve(audius)

        assertTrue(availability is SongDownloadAvailability.Unavailable)
        assertTrue(
            (availability as SongDownloadAvailability.Unavailable).reason.contains("autorizado")
        )
    }

    @Test
    fun `audius id is the stable download key`() {
        val audius = track(
            id = "audius:track:9",
            source = TrackSource.AUDIUS,
            downloadUrl = "https://api.audius.co/v1/tracks/9/download?app_name=VibeMusic"
        )
        assertEquals("audius:track:9", SongDownloadResolver.keyFor(audius))
    }

    @Test
    fun `identity ignores case and spacing`() {
        val first = track("a", TrackSource.DEMO, title = "  Neon Skyline ", artist = "MIDNIGHT Pulse")
        val second = track("b", TrackSource.DEMO, title = "neon skyline", artist = "midnight pulse")

        assertEquals(SongDownloadResolver.identity(first), SongDownloadResolver.identity(second))
    }
}
