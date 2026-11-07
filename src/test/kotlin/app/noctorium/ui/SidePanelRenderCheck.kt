package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.ui.skins.Text
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the side panel beside a page -- the lyrics, and the queue -- with the player bar under them and the button
 * for the one open lit, to pictures, off screen, for looking at. The lyrics are asked for as the window asks for
 * them, so it needs the network. Off unless a folder is named:
 *
 *     ./gradlew :test --tests "*SidePanelRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class SidePanelRenderCheck {

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "5NV6Rdv1a3I",
        title = "Get Lucky",
        artists = listOf(Artist("daftpunk", "Daft Punk", ProviderType.YOUTUBE_MUSIC)),
        durationMs = 248_000,
        sourceUrl = "https://music.youtube.com/watch?v=5NV6Rdv1a3I",
    )

    private fun song(id: String, title: String, artist: String) = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = id,
        title = title,
        artists = listOf(Artist(artist.lowercase(), artist, ProviderType.YOUTUBE_MUSIC)),
        durationMs = 215_000,
        sourceUrl = "https://music.youtube.com/watch?v=$id",
    )

    @Test
    fun `the panel draws beside a page`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        folder.mkdirs()
        val state = desktopAppState()
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PAUSED, track = track, positionMs = 61_000, durationMs = 248_000)
        try {
            SidePanelState.shown = SidePanel.LYRICS
            draw(folder, "lyrics-panel.png", state, queue, playback)
            draw(folder, "lyrics-panel-nothing-playing.png", state, QueueState(), PlaybackState())
            // The queue: the song playing, and what follows it.
            val longer = QueueState(
                tracks = listOf(
                    track,
                    song("a1", "Instant Crush", "Daft Punk"),
                    song("a2", "Midnight City", "M83"),
                    song("a3", "Digital Love", "Daft Punk"),
                    song("a4", "Strobe", "deadmau5"),
                    song("a5", "Teardrop", "Massive Attack"),
                ),
                currentIndex = 0,
            )
            SidePanelState.shown = SidePanel.QUEUE
            draw(folder, "queue-panel.png", state, longer, playback)
            draw(folder, "queue-panel-empty.png", state, QueueState(), PlaybackState())
            SidePanelState.shown = null
            draw(folder, "side-panel-closed.png", state, queue, playback)
        } finally {
            SidePanelState.shown = null
            Thread.sleep(300)
            state.close()
        }
    }

    private fun draw(folder: File, name: String, state: app.noctorium.core.AppState, queue: QueueState, playback: PlaybackState) {
        val scene = ImageComposeScene(1280, 760, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
              // A Surface, as the window has round everything: it is what gives the writing its colour.
              Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        WithSidePanel(queue, state) {
                            Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.TopStart) {
                                Text("A page of the window", fontSize = 30.sp)
                            }
                        }
                    }
                    PlayerBar(queue, playback, state)
                }
              }
            }
        }
        try {
            // Time for the lyrics to come back from their sources.
            var image = scene.render(0)
            repeat(80) { frame ->
                Thread.sleep(100)
                image = scene.render((frame + 1) * 100_000_000L)
            }
            val file = File(folder, name)
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000)
        } finally {
            scene.close()
        }
    }
}
