package app.noctorium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws every seek bar to pictures, off screen, for looking at: playing and paused, a song not yet loaded,
 * a long set, squeezed into a short bar, on a dark page and a pale one, and the Settings picker that
 * chooses between them. Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*SeekBarRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class SeekBarRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private val song = Track(
        provider = ProviderType.SOUNDCLOUD,
        id = "harbour-lights-421",
        title = "Harbour lights",
        artists = listOf(Artist("the-night-ferries", "The Night Ferries", ProviderType.SOUNDCLOUD)),
        durationMs = 245_000,
        sourceUrl = "https://example.invalid/harbour-lights",
    )

    private val playing = PlaybackState(status = PlaybackStatus.PLAYING, track = song, positionMs = 93_000, durationMs = 245_000)

    @Test
    fun `every seek bar draws`() {
        val folder = folder ?: return
        val state = desktopAppState()
        try {
            listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY, ThemePreset.WINDOWS_XP).forEach { theme ->
                state.setTheme(theme)
                val name = theme.name.lowercase()
                sheet(folder, "seek-bars-$name-playing.png", state, playing)
                sheet(folder, "seek-bars-$name-paused.png", state, playing.copy(status = PlaybackStatus.PAUSED))
            }
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            // At the very start, at the very end, and a song whose length is not known yet.
            sheet(folder, "seek-bars-start.png", state, playing.copy(positionMs = 0))
            sheet(folder, "seek-bars-end.png", state, playing.copy(positionMs = 245_000))
            sheet(folder, "seek-bars-unknown-length.png", state, playing.copy(durationMs = 0))
            sheet(folder, "seek-bars-nothing-loaded.png", state, PlaybackState())
            // A two-hour set, for the ruler's spacing, and the compact size the Display bar uses.
            sheet(folder, "seek-bars-long-set.png", state, playing.copy(positionMs = 2_900_000, durationMs = 7_200_000))
            sheet(folder, "seek-bars-compact.png", state, playing, compact = true, width = 380)
            sheet(folder, "seek-bars-narrow.png", state, playing, width = 300)
            // At twice the density, as on a screen scaled to 200%, where a pixel out of place shows.
            sheet(folder, "seek-bars-2x.png", state, playing, width = 520, density = 2f)
            hotLuna(folder, state)
            picker(folder, state)
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            Thread.sleep(300)
            state.close()
        }
    }

    /** Every style one under another, each named, at the same moment of the same song. */
    private fun sheet(
        folder: File,
        name: String,
        state: AppState,
        playback: PlaybackState,
        compact: Boolean = false,
        width: Int = 760,
        density: Float = 1f,
    ) {
        val scale = density.toInt().coerceAtLeast(1)
        val scene = scene(state, (width + 150) * scale, (ProgressBarStyle.entries.size * 46 + 40) * scale, density) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressBarStyle.entries.forEach { style ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(style.displayName, fontSize = 12.sp, modifier = Modifier.width(110.dp))
                        PlaybackProgressBar(playback, {}, Modifier.width(width.dp), style, compact = compact)
                    }
                }
            }
        }
        try {
            // Into the neon's breath, so a playing spark is caught partway rather than at rest.
            var image = scene.render(0)
            repeat(12) { frame -> image = scene.render((frame + 1) * 100_000_000L) }
            write(folder, name, image.encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

    /** Luna's thumb as the pointer rests on it, lit orange the way XP lit it. */
    private fun hotLuna(folder: File, state: AppState) {
        listOf(ThemePreset.WINDOWS_XP, ThemePreset.NOCTORIUM_NIGHT).forEach { theme ->
            state.setTheme(theme)
            val scene = scene(state, 560, 80) {
                Box(Modifier.padding(20.dp)) {
                    PlaybackProgressBar(playing, {}, Modifier.width(520.dp), ProgressBarStyle.LUNA)
                }
            }
            try {
                scene.render(0)
                // The bar starts after the 42 point time; the head is 93/245 of the way along what is left.
                val head = 20f + 42f + (520f - 84f) * 93f / 245f
                scene.sendPointerEvent(PointerEventType.Enter, Offset(head, 40f))
                scene.sendPointerEvent(PointerEventType.Move, Offset(head + 1f, 40f))
                scene.render(50_000_000L)
                write(folder, "seek-bar-luna-hot-${theme.name.lowercase()}.png", scene.render(100_000_000L).encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
    }

    /** The choice in Settings: every style as a card with a live preview. */
    private fun picker(folder: File, state: AppState) {
        listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.NOCTORIUM_DAY).forEach { theme ->
            state.setTheme(theme)
            val scene = scene(state, 760, ProgressBarStyle.entries.size * 157 + 40) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProgressBarStyle.entries.forEach { style ->
                        ProgressStyleOption(style, selected = style == ProgressBarStyle.BARS) {}
                    }
                }
            }
            try {
                var image = scene.render(0)
                repeat(6) { frame -> image = scene.render((frame + 1) * 100_000_000L) }
                write(folder, "seek-bar-picker-${theme.name.lowercase()}.png", image.encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
    }

    private fun write(folder: File, name: String, bytes: ByteArray) {
        val file = File(folder, name)
        file.writeBytes(bytes)
        assertTrue(file.length() > 1_000, "nothing was drawn for $name")
    }

    /** The application's own theme on its page colour, with things moving as they do in the window. */
    private fun scene(state: AppState, width: Int, height: Int, density: Float = 1f, content: @Composable () -> Unit) =
        ImageComposeScene(width, height, Density(density)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                CompositionLocalProvider(LocalMotion provides true) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.fillMaxWidth()) { content() }
                    }
                }
            }
        }
}
