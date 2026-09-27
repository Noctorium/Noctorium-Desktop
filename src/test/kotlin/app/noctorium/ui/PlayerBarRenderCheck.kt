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
import app.noctorium.settings.PlayerBarStyle
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
}
