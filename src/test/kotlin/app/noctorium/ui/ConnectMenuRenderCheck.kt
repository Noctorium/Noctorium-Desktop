package app.noctorium.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.connect.ConnectPeer
import app.noctorium.connect.ConnectState
import app.noctorium.connect.DeviceKind
import app.noctorium.connect.RemoteState
import app.noctorium.connect.WireArtist
import app.noctorium.connect.WireTrack
import app.noctorium.desktopAppState
import app.noctorium.domain.ProviderType
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Connect menu, drawn off screen for looking at, and the words it chooses.
 *
 * The drawing is off unless a folder is named, like the other render checks:
 *
 *     ./gradlew :test --tests "*ConnectMenuRenderCheck*" -Dnoctorium.renderConnect=build/connect
 */
class ConnectMenuRenderCheck {

    private val phone = ConnectPeer("phone", "Pocket", DeviceKind.PHONE, "192.168.1.30", 41234, 0)
    private val laptop = ConnectPeer("laptop", "Laptop", DeviceKind.DESKTOP, "192.168.1.31", 41235, 0)

    private val remote = RemoteState(
        deviceId = "phone",
        deviceName = "Pocket",
        kind = DeviceKind.PHONE,
        playing = true,
        track = WireTrack(
            provider = ProviderType.SOUNDCLOUD,
            id = "1",
            title = "Harbour Lights",
            artists = listOf(WireArtist("a", "Night Tram", ProviderType.SOUNDCLOUD)),
            durationMs = 225_000,
            sourceUrl = "https://soundcloud.com/1",
        ),
        positionMs = 62_000,
        durationMs = 225_000,
        volume = 0.6f,
        queueSize = 9,
        queueIndex = 1,
    )

    @Test
    fun `the menu draws, choosing, driving and refused`() {
        val folder = System.getProperty("noctorium.renderConnect")?.let(::File) ?: return
        folder.mkdirs()
        val state = desktopAppState()
        try {
            mapOf(
                "choosing" to ConnectState(available = true, listening = true, thisDevice = "Desk", devices = listOf(phone, laptop)),
                "driving" to ConnectState(available = true, thisDevice = "Desk", devices = listOf(phone, laptop), target = phone, remote = remote, message = "Playing on Pocket."),
                "refused" to ConnectState(
                    available = true,
                    thisDevice = "Desk",
                    devices = listOf(phone, laptop),
                    message = "Laptop did not answer. Its firewall may be keeping Noctorium out on this network.",
                ),
            ).forEach { (name, connect) ->
                val scene = ImageComposeScene(340, 520, Density(1f)) {
                    val preferences = state.settings.collectAsState().value.preferences
                    MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                        // A surface, as the menu is, so what is drawn takes the colour the menu gives it.
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column(Modifier.width(320.dp)) {
                                ConnectMenu(state, connect, playingHere = false, close = {})
                            }
                        }
                    }
                }
                try {
                    val file = File(folder, "connect-$name.png")
                    // Several frames, a moment apart, so the fonts and icons have arrived.
                    var image = scene.render(0)
                    repeat(10) { frame ->
                        Thread.sleep(100)
                        image = scene.render((frame + 1) * 100_000_000L)
                    }
                    file.writeBytes(image.encodeToData()!!.bytes)
                    assertTrue(file.length() > 1_000, "nothing was drawn for $name")
                } finally {
                    scene.close()
                }
            }
        } finally {
            state.close()
        }
    }

    @Test
    fun `the remote control says where the other device has got to`() {
        assertEquals("Night Tram — 1:02 of 3:45 · 2 of 9", remoteLine(remote))
        assertEquals("Night Tram — 1:02 of 3:45 · 2 of 9 · paused", remoteLine(remote.copy(playing = false)))
        assertEquals("", remoteLine(null))
    }

    @Test
    fun `a message is shown unless the line above already says it`() {
        val driving = ConnectState(target = phone, message = "Playing on Pocket.")
        assertEquals("Playing on Pocket", connectSubtitle(driving))
        assertNull(connectNews(driving, connectSubtitle(driving)))

        val refused = ConnectState(devices = listOf(phone), message = "Pocket did not answer.")
        assertEquals("Pocket did not answer.", connectNews(refused, connectSubtitle(refused)))
        assertEquals("Sending the music…", connectSubtitle(ConnectState(busy = true)))
    }
}
