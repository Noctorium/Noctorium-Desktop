package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.AutoplaySource
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.FontChoice
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SettingsRepository
import app.noctorium.settings.SpotifyPlayback
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws what autoplay, the speed and the queue's own controls added to the desktop to pictures, off screen,
 * for looking at: the Playback and queue page, Hybrid search's services, Up next with YouTube Music's radio and
 * with SoundCloud's related tracks under the queue, Spotify carrying on by itself, autoplay switched off, the
 * queue's menu and the dialog that saves it, and the speed and the sleep timer's fade where the player keeps
 * them.
 *
 * Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*PlaybackQueueRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * Every name here is made up. The settings it changes are the test run's own, under build/test-home, and are
 * put back; the covers are painted here, so nothing goes to the network for them.
 */
class PlaybackQueueRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private fun song(id: String, title: String, artist: String, provider: ProviderType = ProviderType.YOUTUBE_MUSIC, minutes: Int = 3) = Track(
        provider = provider,
        id = id,
        title = title,
        artists = listOf(Artist(artist.lowercase(), artist, provider)),
        durationMs = minutes * 60_000L + 24_000L,
        artworkUrl = COVER + COVERS[id.hashCode().mod(COVERS.size)],
        sourceUrl = "https://example.invalid/$id",
    )

    private val queued = listOf(
        song("q1", "Harbour Lights", "Night Tram"),
        song("q2", "Slow Rivers", "Paper Lanterns", minutes = 4),
        song("q3", "Glass Weather", "Night Tram"),
        song("q4", "Low Tide", "Sample Choir", minutes = 5),
    )

    private val radio = listOf(
        song("y1", "Station Lights", "The Late Rooms"),
        song("y2", "Cold Coffee", "Paper Lanterns", minutes = 4),
        song("y3", "Overground", "Sample Choir"),
        song("y4", "Half Past Two", "Night Tram", minutes = 2),
        song("y5", "Window Seat", "The Late Rooms"),
    )

    private val related = listOf(
        song("s1", "late night edit", "sample_producer", ProviderType.SOUNDCLOUD),
        song("s2", "rain loop 04", "bedroom.tapes", ProviderType.SOUNDCLOUD, minutes = 2),
        song("s3", "tram stop (demo)", "night_tram", ProviderType.SOUNDCLOUD),
    )

    /** The queue playing its second-to-last song, which is when autoplay lines its songs up. */
    private val nearEnd = QueueState(tracks = queued, currentIndex = queued.lastIndex - 1)

    @Test
    fun `the Playback and queue page and Hybrid search's services draw`() {
        val folder = folder ?: return
        withState { state ->
            panel(folder, "playback-settings.png", state)
            state.setPlaybackSpeed(1.25f)
            state.setAutoplayFrom(AutoplaySource.YOUTUBE_MUSIC)
            state.setSleepFade(30)
            panel(folder, "playback-settings-changed.png", state)
            state.setAutoplay(false)
            panel(folder, "playback-settings-autoplay-off.png", state)

            state.setHybridSearchService(ProviderType.SOUNDCLOUD, false)
            state.setHybridSearchService(ProviderType.BANDCAMP, false)
            draw(folder, "settings-browsing-hybrid.png", state, 760, 1080) {
                BrowsingSettingsCard(state.settings.collectAsState().value.preferences, state)
            }
            // The list of settings, with the new tile under Sound.
            Thread.sleep(300)
            state.clearSettingsMessage()
            draw(folder, "settings-home-playback.png", state, 1000, 620, padded = false) { SettingsScreen(state) }
        }
    }

    @Test
    fun `Up next draws autoplay's songs, and says why when there are none`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                val youTube = nearEnd.copy(suggestions = radio, suggestionsFrom = "YouTube Music radio")
                draw(folder, "queue-youtube-radio.png", state, 1100, 980, padded = false) { QueueScreen(youTube, state) }
                val soundCloud = QueueState(
                    tracks = queued.take(2) + song("q9", "tram stop", "night_tram", ProviderType.SOUNDCLOUD),
                    currentIndex = 1,
                    suggestions = related,
                    suggestionsFrom = "Related on SoundCloud",
                )
                draw(folder, "queue-soundcloud-related.png", state, 1100, 760, padded = false) { QueueScreen(soundCloud, state) }
                // Early in a long queue: nothing lined up yet, and a word on when there will be.
                draw(folder, "queue-waiting.png", state, 1100, 640, padded = false) { QueueScreen(nearEnd.copy(currentIndex = 0), state) }
                // The now playing panel's narrower Up next, with the radio under the queue.
                val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = youTube.current, positionMs = 84_000, durationMs = 264_000)
                draw(folder, "now-playing-up-next-radio.png", state, 1280, 760, padded = false, frames = 12) {
                    NowPlayingScreen(youTube, playback, state)
                }

                // The queue's menu, and then the dialog its last item opens.
                drawAfterClicks(folder, "queue-actions-menu.png", state, 1100, 760, listOf(Offset(MENU_X, MENU_Y))) {
                    QueueScreen(youTube, state)
                }
                drawAfterClicks(folder, "queue-save-dialog.png", state, 1100, 760, listOf(Offset(MENU_X, MENU_Y), Offset(SAVE_X, SAVE_Y))) {
                    QueueScreen(youTube, state)
                }

                state.setAutoplay(false)
                draw(folder, "queue-autoplay-off.png", state, 1100, 640, padded = false) { QueueScreen(youTube, state) }
            }
        }
    }

    @Test
    fun `a queue ending on a song Spotify plays is carried on by Spotify`() {
        val folder = folder ?: return
        // Spotify songs set to play on Spotify, in the test run's own settings, read by the state as it starts.
        val repository = SettingsRepository()
        val before = repository.load()
        repository.save(before.copy(spotifyCanPlay = true, spotifyPlayback = SpotifyPlayback.ON_SPOTIFY))
        try {
            withCovers {
                withState { state ->
                    val deadline = System.currentTimeMillis() + 10_000
                    while (!state.settings.value.spotify.playsOnSpotify && System.currentTimeMillis() < deadline) Thread.sleep(50)
                    assertTrue(state.settings.value.spotify.playsOnSpotify, "the state never said Spotify songs play on Spotify")

                    val spotifySong = song("0spotifysample000000000", "Northern Line", "Sample Choir", ProviderType.SPOTIFY)
                    val endsOnSpotify = QueueState(tracks = queued.take(3) + spotifySong, currentIndex = 3)
                    draw(folder, "queue-spotify-chooses.png", state, 1100, 640, padded = false) { QueueScreen(endsOnSpotify, state) }
                    // The speed, resting, while Spotify's app plays the song.
                    val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = spotifySong, positionMs = 40_000, durationMs = 204_000)
                    drawAfterClicks(folder, "bar-volume-speed-on-spotify.png", state, 1280, 520, listOf(Offset(VOLUME_X, 520 - BAR_FROM_FOOT)), padded = false) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(endsOnSpotify, playback, state) }
                    }
                }
            }
        } finally {
            repository.save(repository.load().copy(spotifyCanPlay = before.spotifyCanPlay, spotifyPlayback = before.spotifyPlayback))
        }
    }

    @Test
    fun `the speed and the sleep timer's fade draw where the player keeps them`() {
        val folder = folder ?: return
        withCovers {
            withState { state ->
                state.setPlaybackSpeed(1.25f)
                state.setSleepFade(30)
                val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = nearEnd.current, positionMs = 84_000, durationMs = 264_000)
                drawAfterClicks(folder, "bar-volume.png", state, 1280, 520, listOf(Offset(VOLUME_X, 520 - BAR_FROM_FOOT)), padded = false) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(nearEnd, playback, state) }
                }
                drawAfterClicks(folder, "bar-speed.png", state, 1280, 520, listOf(Offset(SPEED_X, 520 - BAR_FROM_FOOT)), padded = false) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(nearEnd, playback, state) }
                }
                drawAfterClicks(folder, "bar-sleep-timer-fade.png", state, 1280, 760, listOf(Offset(SLEEP_X, 760 - BAR_FROM_FOOT)), padded = false) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { PlayerBar(nearEnd, playback, state) }
                }
            }
        }
    }

    // --- Drawing ---

    private fun panel(folder: File, name: String, state: AppState) =
        draw(folder, name, state, 760, 1400) { PlaybackQueueSettingsPanel(state.settings.collectAsState().value.preferences, state) }

    private fun draw(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        padded: Boolean = true,
        frames: Int = 1,
        content: @Composable () -> Unit,
    ) {
        val scene = scene(state, width, height, padded, content)
        try {
            var image = scene.render(0)
            repeat(frames) { frame ->
                if (frames > 1) Thread.sleep(80)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }

    /**
     * Draws [content] after clicking each of [at] in turn, the way a listener opens a menu and then picks from
     * it: the pointer arrives, presses and lets go, and a few frames pass for what opens to grow into place.
     */
    private fun drawAfterClicks(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        at: List<Offset>,
        padded: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        val scene = scene(state, width, height, padded, content)
        try {
            var time = 0L
            fun frame(): org.jetbrains.skia.Image {
                time += 60_000_000L
                return scene.render(time)
            }
            frame()
            frame()
            var image = frame()
            at.forEach { point ->
                scene.sendPointerEvent(PointerEventType.Move, point)
                frame()
                scene.sendPointerEvent(PointerEventType.Press, point)
                frame()
                scene.sendPointerEvent(PointerEventType.Release, point)
                repeat(10) {
                    Thread.sleep(60)
                    image = frame()
                }
            }
            File(folder, name).writeBytes(image.encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

    /** The application's own theme and typeface on the page colour, still, as [PersonalisationRenderCheck] has it. */
    private fun scene(state: AppState, width: Int, height: Int, padded: Boolean = true, content: @Composable () -> Unit) =
        ImageComposeScene(width, height, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(
                colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null))),
                typography = noctoriumTypography(preferences.font),
            ) {
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (padded) Box(Modifier.padding(20.dp)) { content() } else content()
                    }
                }
            }
        }

    /**
     * A state for one check, with the look and everything these checks change put back before and after, so
     * the test run's settings are left as they were found. See [BandcampRenderCheck].
     */
    private fun withState(check: (AppState) -> Unit) {
        val state = desktopAppState()
        try {
            reset(state)
            check(state)
        } finally {
            reset(state)
            Thread.sleep(400)
            state.close()
        }
    }

    private fun reset(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAccent(AccentPreset.THEME)
        state.setFont(FontChoice.DEFAULT)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setCornerStyle(CornerStyle.SOFT)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlaybackSpeed(1f)
        state.setAutoplay(true)
        state.setAutoplayFrom(AutoplaySource.SAME_SERVICE)
        state.setSleepFade(0)
        ProviderType.entries.filter { it in app.noctorium.settings.DEFAULT_HYBRID_SEARCH }
            .forEach { state.setHybridSearchService(it, true) }
    }

    /** Painted covers, one per name in [COVERS], in each size the loader might ask the store for. */
    private fun withCovers(draw: () -> Unit) {
        COVERS.forEachIndexed { index, name ->
            val cover = cover(index * 53f + 10f)
            listOf(64, 128, 256, 512, 1024).forEach { size -> ArtworkStore.put(COVER + name + ArtworkStore.SIZE_SEPARATOR + size, cover) }
        }
        try {
            draw()
        } finally {
            ArtworkStore.clear()
        }
    }

    private fun cover(hue: Float): ImageBitmap {
        val size = 256f
        val image = ImageBitmap(size.toInt(), size.toInt())
        val canvas = Canvas(image)
        canvas.drawRect(
            Rect(0f, 0f, size, size),
            Paint().apply {
                shader = LinearGradientShader(
                    Offset.Zero,
                    Offset(size, size),
                    listOf(Color.hsv(hue % 360f, .55f, .8f), Color.hsv((hue + 70f) % 360f, .7f, .35f)),
                )
            },
        )
        canvas.drawCircle(Offset(size * .5f, size * .5f), size * .18f, Paint().apply { color = Color.White.copy(alpha = .2f) })
        return image
    }

    private companion object {
        /** An address no loader can fetch, so a painted cover is all there is for it. */
        const val COVER = "test://cover/"
        val COVERS = listOf("a", "b", "c", "d", "e", "f", "g")

        /** The queue's menu button, and the menu's last item once it is open, in a 1100-wide Queue page. */
        const val MENU_X = 1050f
        const val MENU_Y = 52f
        const val SAVE_X = 960f
        const val SAVE_Y = 210f

        /** The volume, speed and sleep timer buttons on the Inline bar, and how far above the picture's foot they sit. */
        const val VOLUME_X = 1248f
        const val SPEED_X = 1213f
        const val SLEEP_X = 1110f
        const val BAR_FROM_FOOT = 37f
    }
}
