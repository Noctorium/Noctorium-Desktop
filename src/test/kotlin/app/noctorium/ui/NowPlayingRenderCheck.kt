package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.CoverSize
import app.noctorium.settings.CoverStyle
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPanelWidth
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.nowPlayingBackdrop
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the now playing screen in every layout, and in a few of its looks, to pictures, off screen; and the
 * Settings card and the screen's own menu that choose them. Off unless a folder is named, like
 * [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*NowPlayingRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class NowPlayingRenderCheck {

    private val artist = Artist("judy-collins", "Judy Collins", ProviderType.YOUTUBE_MUSIC)

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "dQw4w9WgXcQ",
        title = "Amazing Grace",
        artists = listOf(artist),
        album = Album("whales-and-nightingales", "Whales & Nightingales", listOf(artist), ProviderType.YOUTUBE_MUSIC),
        durationMs = 245_000,
        artworkUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
        sourceUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
    )

    private val others = (1..8).map { n ->
        Track(
            provider = if (n % 2 == 0) ProviderType.SOUNDCLOUD else ProviderType.YOUTUBE_MUSIC,
            id = "other-$n",
            title = "Another song $n",
            artists = listOf(Artist("someone-$n", "Someone $n", ProviderType.YOUTUBE_MUSIC)),
            durationMs = 180_000L + n * 7_000L,
            sourceUrl = "https://example.invalid/$n",
        )
    }

    @Test
    fun `every now playing layout draws`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(others[0], others[1], track) + others.drop(2), currentIndex = 2)
        val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = track, positionMs = 83_000, durationMs = 245_000)
        val state = desktopAppState()
        try {
            state.setTheme(ThemePreset.CRIMSON)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            NowPlayingLayout.entries.forEach { layout ->
                state.updateNowPlaying { NowPlayingPreferences(layout = layout) }
                screen(folder, "now-playing-${layout.name.lowercase()}.png", state, queue, playback)
            }
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.STAGE, cover = CoverStyle.RECORD) }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.COVER)
            screen(folder, "now-playing-stage-record-blurred.png", state, queue, playback)
            state.updateNowPlaying { copy(layout = NowPlayingLayout.FOCUS, cover = CoverStyle.CIRCLE) }
            screen(folder, "now-playing-focus-circle-blurred.png", state, queue, playback)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SIDE_BY_SIDE, panelHidden = true) }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            screen(folder, "now-playing-panel-hidden.png", state, queue, playback)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SING_ALONG) }
            screen(folder, "now-playing-sing_along-narrow.png", state, queue, playback, width = 800)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.STAGE) }
            screen(folder, "now-playing-stage-narrow.png", state, queue, playback, width = 800)
            // The menu, opened the way a listener opens it: a click on the sliders at the foot of the hero.
            state.updateNowPlaying { NowPlayingPreferences() }
            screen(folder, "now-playing-menu-open.png", state, queue, playback, click = Offset(765f, 705f))
            state.setTheme(ThemePreset.WINDOWS_XP)
            state.setProgressBarStyle(ProgressBarStyle.CLASSIC)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.BANNER, cover = CoverStyle.SQUARE) }
            screen(folder, "now-playing-banner-xp.png", state, queue, playback)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.updateNowPlaying { NowPlayingPreferences() }
            panel(folder, "now-playing-settings.png", state, 760, 1180) { NowPlayingSettingsCard(it, state) }
            panel(folder, "now-playing-menu.png", state, 420, 560) {
                Surface(Modifier.width(384.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    NowPlayingArranger(it.desktop.nowPlaying, it.nowPlayingBackdrop, state) {}
                }
            }
        } finally {
            state.updateNowPlaying { NowPlayingPreferences() }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.close()
        }
    }

    /**
     * Full cover, Split, Cover flow, Turntable and Big type: at 1280 by 800 and 1920 by 1080, on Night and on Day;
     * at the smallest the window goes and at a 4K screen's full size; and the states each has to get right -- the
     * panel tucked away, the arm at rest, the cover cut round, a title too long for the poster.
     */
    @Test
    fun `the new layouts draw at every size`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val covers = File(folder, "covers").apply { mkdirs() }
        val queue = QueueState(
            tracks = flowQueue(covers).take(4) + track + flowQueue(covers).drop(4),
            currentIndex = 4,
        )
        val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = track, positionMs = 83_000, durationMs = 245_000)
        val fresh = listOf(
            NowPlayingLayout.IMMERSIVE,
            NowPlayingLayout.SPLIT,
            NowPlayingLayout.COVER_FLOW,
            NowPlayingLayout.TURNTABLE,
            NowPlayingLayout.POSTER,
        )
        val state = desktopAppState()
        try {
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY).forEach { theme ->
                state.setTheme(theme)
                fresh.forEach { layout ->
                    state.updateNowPlaying { NowPlayingPreferences(layout = layout) }
                    val name = "layout-${layout.name.lowercase()}-${theme.displayName.lowercase()}"
                    screen(folder, "$name-1280.png", state, queue, playback, width = 1280, height = 800)
                    screen(folder, "$name-1920.png", state, queue, playback, width = 1920, height = 1080)
                }
            }
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            fresh.forEach { layout ->
                state.updateNowPlaying { NowPlayingPreferences(layout = layout) }
                // The window at its smallest, less the sidebar and the bar; and a 4K screen at 100%.
                screen(folder, "layout-${layout.name.lowercase()}-smallest.png", state, queue, playback, width = 564, height = 446)
                screen(folder, "layout-${layout.name.lowercase()}-4k.png", state, queue, playback, width = 3644, height = 2050, frames = 40)
            }

            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SPLIT, panelHidden = true) }
            screen(folder, "layout-split-panel-hidden.png", state, queue, playback, width = 1280, height = 800)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SPLIT) }
            screen(folder, "layout-split-narrow.png", state, queue, playback, width = 800, height = 760)

            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.TURNTABLE, panelHidden = true) }
            screen(folder, "layout-turntable-panel-hidden.png", state, queue, playback, width = 1280, height = 800)
            screen(folder, "layout-turntable-start.png", state, queue, playback.copy(positionMs = 0), width = 1280, height = 800)
            screen(folder, "layout-turntable-end.png", state, queue, playback.copy(positionMs = 245_000), width = 1280, height = 800)
            screen(folder, "layout-turntable-resting.png", state, queue, playback.copy(status = PlaybackStatus.RESOLVING, durationMs = 0), width = 1280, height = 800)
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.TURNTABLE, panelWidth = NowPlayingPanelWidth.WIDE) }
            screen(folder, "layout-turntable-narrow.png", state, queue, playback, width = 800, height = 760)

            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.COVER_FLOW, cover = CoverStyle.CIRCLE, coverSize = CoverSize.LARGER) }
            screen(folder, "layout-cover_flow-circle.png", state, queue, playback, width = 1280, height = 800)
            // At the head of the queue, with nothing to its left.
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.COVER_FLOW) }
            screen(folder, "layout-cover_flow-first.png", state, queue.copy(currentIndex = 0), playback.copy(track = queue.tracks[0]), width = 1280, height = 800)

            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.POSTER) }
            val long = track.copy(
                id = "long-title",
                title = "The Longest Night of the Year (Live at the Harbour Hall, Second Set)",
            )
            screen(folder, "layout-poster-long-title.png", state, queue.copy(tracks = listOf(long), currentIndex = 0), playback.copy(track = long), width = 1280, height = 800)
            state.setTheme(ThemePreset.GRUVBOX)
            screen(folder, "layout-poster-gruvbox.png", state, queue, playback, width = 1280, height = 800)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)

            // The settings card and the screen's own menu, with all eleven to choose from.
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.TURNTABLE) }
            panel(folder, "layout-settings.png", state, 760, 1300) { NowPlayingSettingsCard(it, state) }
            panel(folder, "layout-menu.png", state, 420, 700) {
                Surface(Modifier.width(384.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    NowPlayingArranger(it.desktop.nowPlaying, it.nowPlayingBackdrop, state) {}
                }
            }
            state.setTheme(ThemePreset.NOCTORIUM_DAY)
            panel(folder, "layout-settings-day.png", state, 760, 1300) { NowPlayingSettingsCard(it, state) }
        } finally {
            state.updateNowPlaying { NowPlayingPreferences() }
            state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            Thread.sleep(300)
            state.close()
        }
    }

    /**
     * Eight songs for the cover flow with covers of their own, drawn here and read from the disk, so the row has
     * colour in it without asking the network for eight more pictures.
     */
    private fun flowQueue(folder: File): List<Track> {
        val names = listOf(
            "Harbour lights", "Paper moons", "Low tide", "The long way round",
            "Northern line", "Glasshouse", "Slow river", "Static and stars",
        )
        val colours = listOf(
            0xFFE76F51 to 0xFF264653, 0xFF2A9D8F to 0xFFE9C46A, 0xFF6D597A to 0xFFEAAC8B, 0xFF023E8A to 0xFF48CAE4,
            0xFFD62828 to 0xFF003049, 0xFF606C38 to 0xFFFEFAE0, 0xFF3D405B to 0xFFF2CC8F, 0xFF7209B7 to 0xFF4CC9F0,
        )
        return names.mapIndexed { n, title ->
            val file = File(folder, "flow-$n.png")
            if (!file.isFile) file.writeBytes(drawnCover(colours[n].first, colours[n].second, n))
            Track(
                provider = if (n % 2 == 0) ProviderType.SOUNDCLOUD else ProviderType.BANDCAMP,
                id = "flow-$n",
                title = title,
                artists = listOf(Artist("the-night-ferries", "The Night Ferries", ProviderType.SOUNDCLOUD)),
                durationMs = 200_000L + n * 9_000L,
                artworkUrl = file.toURI().toString(),
                sourceUrl = "https://example.invalid/flow-$n",
            )
        }
    }

    /** A square cover: a gradient of two colours with a ring and a bar across it, different for each [seed]. */
    private fun drawnCover(from: Long, to: Long, seed: Int): ByteArray {
        val side = 600
        val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(side, side)
        val canvas = surface.canvas
        val gradient = org.jetbrains.skia.Paint().apply {
            shader = org.jetbrains.skia.Shader.makeLinearGradient(0f, 0f, side.toFloat(), side.toFloat(), intArrayOf(from.toInt(), to.toInt()))
        }
        canvas.drawRect(org.jetbrains.skia.Rect.makeWH(side.toFloat(), side.toFloat()), gradient)
        val mark = org.jetbrains.skia.Paint().apply {
            color = 0x66FFFFFF
            mode = org.jetbrains.skia.PaintMode.STROKE
            strokeWidth = 26f
        }
        canvas.drawCircle(side * (.3f + .07f * (seed % 4)), side * (.35f + .05f * (seed % 3)), side * (.16f + .02f * seed), mark)
        canvas.drawRect(org.jetbrains.skia.Rect.makeXYWH(0f, side * (.7f + .02f * seed), side.toFloat(), side * .06f), org.jetbrains.skia.Paint().apply { color = 0x33000000 })
        return surface.makeImageSnapshot().encodeToData()!!.bytes
    }

    private fun screen(
        folder: File,
        name: String,
        state: AppState,
        queue: QueueState,
        playback: PlaybackState,
        width: Int = 1280,
        click: Offset? = null,
        height: Int = 760,
        frames: Int = 60,
    ) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            themed(state) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NowPlayingScreen(queue, playback, state)
                }
            }
        }
        try {
            // Long enough for the cover to arrive over the network the first time, and for lyrics.
            var image = scene.render(0)
            repeat(frames) { frame ->
                Thread.sleep(100)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            if (click != null) {
                scene.sendPointerEvent(PointerEventType.Move, click)
                scene.sendPointerEvent(PointerEventType.Press, click)
                scene.sendPointerEvent(PointerEventType.Release, click)
                repeat(10) { frame ->
                    Thread.sleep(50)
                    image = scene.render((61 + frame) * 100_000_000L)
                }
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000)
        } finally {
            scene.close()
        }
    }

    private fun panel(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        content: @Composable (app.noctorium.settings.NoctoriumPreferences) -> Unit,
    ) {
        val scene = ImageComposeScene(width, height, Density(1f)) {
            themed(state) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.padding(18.dp)) { content(state.settings.collectAsState().value.preferences) }
                }
            }
        }
        try {
            scene.render(0)
            File(folder, name).writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

    @Composable
    private fun themed(state: AppState, content: @Composable () -> Unit) {
        val preferences = state.settings.collectAsState().value.preferences
        MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
            // Still, so each picture is the arrangement rather than a moment of the change into it.
            CompositionLocalProvider(LocalMotion provides false, content = content)
        }
    }
}
