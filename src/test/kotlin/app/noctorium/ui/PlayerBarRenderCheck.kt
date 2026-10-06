package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SurfaceStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws every player bar layout to a picture, off screen, for looking at.
 *
 * The real bars with the real theme, drawn by the same code the window draws with, but into an image:
 * no window opens, nothing takes the keyboard, and nothing plays. Off unless a folder is named:
 *
 *     ./gradlew :test --tests "*PlayerBarRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * Each layout is drawn at a wide window and at a narrow one, since both of the new ones change what they
 * show below 900 points.
 */
class PlayerBarRenderCheck {

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "PzYrr7K1dvU",
        title = "Archangel",
        artists = listOf(Artist("burial", "Burial", ProviderType.YOUTUBE_MUSIC)),
        durationMs = 240_000,
        artworkUrl = "https://i.ytimg.com/vi/PzYrr7K1dvU/hqdefault.jpg",
        sourceUrl = "https://music.youtube.com/watch?v=PzYrr7K1dvU",
    )

    @Test
    fun `every layout draws`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PAUSED, track = track, positionMs = 83_000, durationMs = 240_000)
        val state = desktopAppState()
        try {
            PlayerBarStyle.entries.forEach { style ->
                state.setPlayerBarStyle(style)
                listOf(1280, 860).forEach { width ->
                    val scene = ImageComposeScene(width, 160, Density(1f)) {
                        val preferences = state.settings.collectAsState().value.preferences
                        MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
                                PlayerBar(queue, playback, state)
                            }
                        }
                    }
                    try {
                        // Several frames, a moment apart, so the cover has arrived and faded in.
                        var image = scene.render(0)
                        repeat(30) { frame ->
                            Thread.sleep(120)
                            image = scene.render((frame + 1) * 120_000_000L)
                        }
                        val file = File(folder, "bar-${style.name.lowercase()}-$width.png")
                        file.writeBytes(image.encodeToData()!!.bytes)
                        assertTrue(file.length() > 1_000, "nothing was drawn for $style at $width")
                    } finally {
                        scene.close()
                    }
                }
            }
        } finally {
            state.close()
        }
    }

    /**
     * Floating, the Island and Display as the window arranges them, over a page of Home: at the top and at the
     * foot, on Night and on Day, across a wide window, a narrow one and the narrowest the window goes, and under
     * glass; with nothing playing; and the Island opened by the pointer.
     */
    @Test
    fun `the floating, island and display bars draw over a page`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = track, positionMs = 83_000, durationMs = 240_000)
        val state = desktopAppState()
        // The window's content column: the sidebar takes 196 of a 1280 window, and of the narrowest, 760.
        val widths = listOf(1084, 760, 564)
        try {
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            listOf(PlayerBarStyle.FLOATING, PlayerBarStyle.ISLAND, PlayerBarStyle.DISPLAY).forEach { style ->
                state.setPlayerBarStyle(style)
                val name = style.name.lowercase()
                listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY).forEach { theme ->
                    state.setTheme(theme)
                    PlayerBarPosition.entries.forEach { position ->
                        state.setPlayerBarPosition(position)
                        widths.forEach { width ->
                            window(folder, "window-$name-${theme.displayName.lowercase()}-${position.name.lowercase()}-$width.png", state, queue, playback, width)
                        }
                    }
                    state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
                    state.setSurfaceStyle(SurfaceStyle.GLASS)
                    window(folder, "window-$name-${theme.displayName.lowercase()}-glass-1084.png", state, queue, playback, 1084)
                    window(folder, "window-$name-${theme.displayName.lowercase()}-glass-564.png", state, queue, playback, 564)
                    state.setSurfaceStyle(SurfaceStyle.SOLID)
                }
                state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
                window(folder, "window-$name-nothing-playing.png", state, QueueState(), PlaybackState(), 1084)
            }
            // A seek bar of another style inside each, Display's compact one especially.
            state.setProgressBarStyle(ProgressBarStyle.LUNA)
            listOf(PlayerBarStyle.FLOATING, PlayerBarStyle.DISPLAY).forEach { style ->
                state.setPlayerBarStyle(style)
                window(folder, "window-${style.name.lowercase()}-luna.png", state, queue, playback, 1084)
            }
            state.setProgressBarStyle(ProgressBarStyle.BARS)
            state.setPlayerBarStyle(PlayerBarStyle.DISPLAY)
            window(folder, "window-display-bars.png", state, queue, playback, 1084)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)

            // The Island opened: the pointer on it, then moved up onto the part that has grown over the page,
            // where it should still count as on it, and then away, after which it shuts.
            state.setPlayerBarStyle(PlayerBarStyle.ISLAND)
            listOf(PlayerBarPosition.BOTTOM, PlayerBarPosition.TOP).forEach { position ->
                state.setPlayerBarPosition(position)
                listOf(SurfaceStyle.SOLID, SurfaceStyle.GLASS).forEach { surface ->
                    state.setSurfaceStyle(surface)
                    val atTop = position == PlayerBarPosition.TOP
                    val restY = if (atTop) 12f + 26f else 300f - 12f - 26f
                    val grownY = if (atTop) 12f + 52f + 18f else 300f - 12f - 52f - 18f
                    window(
                        folder,
                        "window-island-open-${position.name.lowercase()}-${surface.name.lowercase()}.png",
                        state,
                        queue,
                        playback,
                        1084,
                        pointer = listOf(Offset(542f, restY), Offset(560f, grownY)),
                    )
                }
            }
        } finally {
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
            state.setSurfaceStyle(SurfaceStyle.SOLID)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            Thread.sleep(300)
            state.close()
        }
    }

    /** The choice in Settings, every layout as a picture of itself, on a dark page and a pale one. */
    @Test
    fun `the layout picker draws`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val state = desktopAppState()
        try {
            listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY).forEach { theme ->
                state.setTheme(theme)
                state.setPlayerBarStyle(PlayerBarStyle.ISLAND)
                val scene = ImageComposeScene(720, 300, Density(1f)) {
                    val preferences = state.settings.collectAsState().value.preferences
                    MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Box(Modifier.padding(20.dp)) { PlayerBarStylePicker(preferences.playerBarStyle) {} }
                        }
                    }
                }
                try {
                    scene.render(0)
                    val file = File(folder, "bar-picker-${theme.displayName.lowercase()}.png")
                    file.writeBytes(scene.render(300_000_000L).encodeToData()!!.bytes)
                    assertTrue(file.length() > 1_000)
                } finally {
                    scene.close()
                }
            }
        } finally {
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            Thread.sleep(300)
            state.close()
        }
    }

    private val page = AppUiState(
        pinnedTracks = (1..6).map { song(it) },
        recentTracks = (7..12).map { song(it) },
        homeSections = listOf(HomeSection("quick", "Quick picks", ProviderType.YOUTUBE_MUSIC, "From YouTube Music", (13..20).map { song(it) })),
        homeLoading = false,
    )

    private fun song(n: Int) = Track(
        provider = if (n % 3 == 0) ProviderType.SOUNDCLOUD else ProviderType.YOUTUBE_MUSIC,
        id = "page-$n",
        title = "A song for the evening $n",
        artists = listOf(Artist("someone-$n", "Someone $n", ProviderType.YOUTUBE_MUSIC)),
        durationMs = 200_000,
        sourceUrl = "https://example.invalid/$n",
    )

    /**
     * The content column as the window lays it out -- glass, a bar that floats, or a strip above or below -- with
     * Home as the page, [pointer] visited in turn before the picture is taken.
     */
    private fun window(
        folder: File,
        name: String,
        state: AppState,
        queue: QueueState,
        playback: PlaybackState,
        width: Int,
        pointer: List<Offset> = emptyList(),
    ) {
        val scene = ImageComposeScene(width, 300, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val atTop = preferences.playerBarPosition == PlayerBarPosition.TOP
                    val screens: @Composable () -> Unit = { HomeScreen(page, state) }
                    Column(Modifier.fillMaxSize()) {
                        when {
                            preferences.surfaceStyle.isGlass -> Box(Modifier.weight(1f)) {
                                GlassContent(queue, playback, state, Color(preferences.themeColours().background), atTop, screens)
                            }
                            preferences.playerBarStyle.floats -> Box(Modifier.weight(1f)) {
                                FloatingContent(queue, playback, state, atTop, screens)
                            }
                            else -> {
                                if (atTop) PlayerBar(queue, playback, state)
                                Box(Modifier.weight(1f)) { screens() }
                                if (!atTop) PlayerBar(queue, playback, state)
                            }
                        }
                    }
                }
            }
        }
        try {
            var image = scene.render(0)
            var time = 0L
            repeat(20) {
                Thread.sleep(100)
                time += 100_000_000L
                image = scene.render(time)
            }
            pointer.forEachIndexed { index, at ->
                scene.sendPointerEvent(if (index == 0) PointerEventType.Enter else PointerEventType.Move, at)
                scene.sendPointerEvent(PointerEventType.Move, at)
                // Past the Island's wait before it shuts, so a pointer it has lost shows as a shut Island.
                repeat(8) {
                    Thread.sleep(80)
                    time += 80_000_000L
                    image = scene.render(time)
                }
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }
}
