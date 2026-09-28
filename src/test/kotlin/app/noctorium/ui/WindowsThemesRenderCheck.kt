package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.CornerStyle
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the Windows 98 and XP themes to pictures, off screen: the player bar with the Classic seek bar,
 * and the whole window. Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*WindowsThemesRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class WindowsThemesRenderCheck {

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "PzYrr7K1dvU",
        title = "Archangel",
        artists = listOf(Artist("burial", "Burial", ProviderType.YOUTUBE_MUSIC)),
        durationMs = 240_000,
        sourceUrl = "https://music.youtube.com/watch?v=PzYrr7K1dvU",
    )

    @Test
    fun `the Windows themes draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PAUSED, track = track, positionMs = 83_000, durationMs = 240_000)
        val state = desktopAppState()
        try {
            state.setProgressBarStyle(ProgressBarStyle.CLASSIC)
            state.setCornerStyle(CornerStyle.SHARP)
            listOf(ThemePreset.WINDOWS_98, ThemePreset.WINDOWS_XP).forEach { theme ->
                state.setTheme(theme)
                listOf(PlayerBarStyle.CENTERED, PlayerBarStyle.INLINE).forEach { layout ->
                    state.setPlayerBarStyle(layout)
                    val scene = ImageComposeScene(1180, 150, Density(1f)) {
                        val preferences = state.settings.collectAsState().value.preferences
                        MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
                                PlayerBar(queue, playback, state)
                            }
                        }
                    }
                    try {
                        scene.render(0)
                        val file = File(folder, "windows-${theme.displayName.lowercase()}-${layout.name.lowercase()}.png")
                        file.writeBytes(scene.render(500_000_000L).encodeToData()!!.bytes)
                        assertTrue(file.length() > 1_000)
                    } finally {
                        scene.close()
                    }
                }
                state.setPlayerBarStyle(PlayerBarStyle.INLINE)
                val window = ImageComposeScene(1280, 800, Density(1f)) { NoctoriumApp(state) }
                try {
                    var image = window.render(0)
                    repeat(10) { frame ->
                        Thread.sleep(100)
                        image = window.render((frame + 1) * 100_000_000L)
                    }
                    File(folder, "windows-${theme.displayName.lowercase()}-window.png").writeBytes(image.encodeToData()!!.bytes)
                } finally {
                    window.close()
                }
            }
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
            state.setCornerStyle(CornerStyle.SOFT)
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            state.close()
        }
    }
}
