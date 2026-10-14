package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import app.noctorium.connect.ConnectPeer
import app.noctorium.connect.ConnectState
import app.noctorium.connect.DeviceKind
import app.noctorium.connect.DeviceRef
import app.noctorium.connect.MusicPlace
import app.noctorium.connect.NowPlaying
import app.noctorium.connect.QueueSnapshot
import app.noctorium.connect.RemoteState
import app.noctorium.connect.toWire
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Connect page and the player as the remote control, without a window: what the strip says, what each device's
 * line says and offers, where the music is said to be, which state the player is drawn from, and where the buttons
 * lead. The pictures are [ConnectRenderCheck]'s.
 */
class ConnectTabTest {

    private val made = lazy { desktopAppState() }
    private val state: AppState by made

    @AfterTest
    fun close() {
        if (!made.isInitialized()) return
        fakeConnect(state, ConnectState())
        state.close()
    }

    // --- The strip and the button ---

    @Test
    fun `the strip says where the music is, or who is driving this computer, and nothing otherwise`() {
        assertEquals("Playing on Pocket", connectStripText(ConnectState(target = ConnectSamples.pocket)))
        assertEquals("Controlled from Laptop", connectStripText(ConnectState(controlledBy = "Laptop")))
        // Driving another wins: this computer's player is that device's remote then.
        assertEquals("Playing on Pocket", connectStripText(ConnectState(target = ConnectSamples.pocket, controlledBy = "Laptop")))
        assertNull(connectStripText(ConnectState(available = true, devices = ConnectSamples.three().devices)))
        assertNull(connectStripText(ConnectState()))
    }

    @Test
    fun `the Devices button is there once Connect runs or while something is going on, and names what is`() {
        assertFalse(devicesButtonShown(ConnectState()), "no account, nothing found, nothing going on")
        assertTrue(devicesButtonShown(ConnectState(available = true)))
        assertTrue(devicesButtonShown(ConnectState(target = ConnectSamples.pocket)))
        assertTrue(devicesButtonShown(ConnectState(controlledBy = "Laptop")))
        assertEquals("Noctorium Connect", devicesButtonLabel(ConnectState(available = true)))
        assertEquals("Playing on Pocket", devicesButtonLabel(ConnectState(target = ConnectSamples.pocket)))
    }

    @Test
    fun `a message the strip already says is not said again, and any other is`() {
        assertNull(connectNews(ConnectState(target = ConnectSamples.pocket, message = "Playing on Pocket.")))
        assertEquals(
            "Laptop did not answer. Its firewall may be keeping Noctorium out on this network.",
            connectNews(ConnectState(message = "Laptop did not answer. Its firewall may be keeping Noctorium out on this network.")),
        )
        assertNull(connectNews(ConnectState(message = " ")))
    }

    @Test
    fun `Play buttons and the song menu say where the music will start while driving`() {
        assertEquals("Play all", drivenLabel("Play all", null))
        assertEquals("Play all on Pocket", drivenLabel("Play all", "Pocket"))
        assertEquals("Play next on Pocket", drivenLabel("Play next", "Pocket"))
        assertEquals("Add to queue on Pocket", drivenLabel("Add to queue", "Pocket"))
    }

    // --- Where the music is ---

    @Test
    fun `the top of the page says where the music is and whether it plays there`() {
        val playing = PlaybackState(status = PlaybackStatus.PLAYING, track = Fixtures.tracks[2])
        assertEquals("Playing on this computer", musicPlaceLabel(MusicPlace.Here, playing))
        assertEquals("Paused on this computer", musicPlaceLabel(MusicPlace.Here, playing.copy(status = PlaybackStatus.PAUSED)))
        assertEquals("Playing on Pocket", musicPlaceLabel(MusicPlace.Driving(ConnectSamples.pocket), playing))
        assertEquals("Reaching Pocket…", musicPlaceLabel(MusicPlace.Driving(ConnectSamples.pocket), PlaybackState(status = PlaybackStatus.RESOLVING)))
        assertEquals("Paused on Studio", musicPlaceLabel(MusicPlace.Elsewhere(ConnectSamples.studio), PlaybackState()))
        assertEquals("Nothing playing", musicPlaceLabel(MusicPlace.Nowhere, PlaybackState()))

        val connect = ConnectSamples.driving()
        assertEquals("The player here is the remote control for Pocket.", musicPlaceRelation(MusicPlace.Driving(ConnectSamples.pocket), connect))
        assertEquals("Laptop is controlling this computer.", musicPlaceRelation(MusicPlace.Here, ConnectState(controlledBy = "Laptop")))
        assertEquals("Playing by itself.", musicPlaceRelation(MusicPlace.Elsewhere(ConnectSamples.studio), connect))
        assertNull(musicPlaceRelation(MusicPlace.Here, ConnectState()))
    }

    // --- Every device ---

    @Test
    fun `this computer comes first, then each device with what it plays, who drives whom, and one thing to do`() {
        val connect = ConnectSamples.driving()
        val lines = deviceLines(connect, MusicPlace.Driving(ConnectSamples.pocketDriven), here = null, nowMs = ConnectSamples.now)
        assertEquals(listOf("Desk", "Pocket", "Laptop", "Studio"), lines.map { it.name })

        val desk = lines[0]
        assertTrue(desk.thisDevice)
        assertEquals("This computer · controlling Pocket", desk.detail)
        assertNull(desk.song, "this computer's own player is idle while it drives another")
        assertEquals(DeviceAction.PlayHere, desk.action)
        assertEquals("Play here", desk.actionLabel)

        val pocket = lines[1]
        assertEquals("Phone · controlled from this computer", pocket.detail)
        assertTrue(pocket.hasMusic)
        assertNull(pocket.action, "nothing to press where the music already is")
        assertEquals("Paper Moonrise", pocket.song?.title)

        val laptop = lines[2]
        assertEquals("Computer", laptop.detail)
        assertEquals(DeviceAction.PlayOn(ConnectSamples.laptop), laptop.action)
        assertEquals("Play on Laptop", laptop.actionLabel)
        assertTrue(laptop.enabled)
        assertNull(laptop.song, "Idle")

        val studio = lines[3]
        assertEquals("Terminal", studio.detail)
        assertEquals("Play on Studio", studio.actionLabel)
        assertEquals("192.168.1.32:41236", studio.address)
    }

    @Test
    fun `a device's place in its song moves on while it plays and stands still while paused`() {
        val later = ConnectSamples.now + 5_000
        val lines = deviceLines(ConnectSamples.driving(), MusicPlace.Driving(ConnectSamples.pocketDriven), null, later)
        assertEquals(74_000L + 5_000L, lines.first { it.name == "Pocket" }.positionMs)
        assertEquals(40_000L, lines.first { it.name == "Studio" }.positionMs)
    }

    @Test
    fun `the device being driven shows what the player shows, so its line and the bar agree`() {
        val shown = NowPlaying("Paper Moonrise", "Paper Satellites", playing = false, positionMs = 90_000, durationMs = 254_000)
        val lines = deviceLines(ConnectSamples.driving(), MusicPlace.Driving(ConnectSamples.pocketDriven), null, ConnectSamples.now + 5_000, driven = shown)
        val pocket = lines.first { it.name == "Pocket" }
        assertEquals(90_000L, pocket.positionMs)
        assertEquals(false, pocket.song?.playing, "paused from here, before Pocket has said so")
        // The others are as they said.
        assertEquals(40_000L, lines.first { it.name == "Studio" }.positionMs)
    }

    @Test
    fun `a device that did not answer is followed by what to do about the firewall`() {
        assertEquals(FIREWALL_ADVICE, connectAdvice("Laptop did not answer. Its firewall may be keeping Noctorium out on this network."))
        assertNull(connectAdvice("Pocket did not answer."))
        assertTrue("tick Public too" in FIREWALL_ADVICE)
    }

    @Test
    fun `nothing can be moved while the music is on its way, or with no music anywhere`() {
        val busy = deviceLines(ConnectSamples.driving().copy(busy = true), MusicPlace.Driving(ConnectSamples.pocketDriven), null, ConnectSamples.now)
        assertTrue(busy.none { it.enabled })
        val nowhere = deviceLines(ConnectState(available = true, thisDevice = "Desk", devices = listOf(ConnectSamples.laptop)), MusicPlace.Nowhere, null, 0)
        assertTrue(nowhere.none { it.enabled })
        // The music here: this computer offers nothing, and the others offer to take it.
        val here = deviceLines(ConnectSamples.three(), MusicPlace.Here, PlaybackState(status = PlaybackStatus.PLAYING, track = Fixtures.tracks[2]).asNowPlaying(), 0)
        assertTrue(here[0].hasMusic)
        assertNull(here[0].action)
        assertEquals("Harbour Lights", here[0].song?.title)
        assertTrue(here.drop(1).all { it.action is DeviceAction.PlayOn && it.enabled })
    }

    @Test
    fun `who is controlling whom is said once a pair, with this computer called that`() {
        assertEquals(listOf("This computer is controlling Pocket."), controlLines(ConnectSamples.driving()))
        val driven = ConnectState(
            thisDeviceId = "desk-id",
            controlledBy = "Laptop",
            controlledById = "laptop-id",
            devices = listOf(ConnectSamples.laptop.copy(driving = DeviceRef("desk-id", "Desk")), ConnectSamples.studio.copy(drivenBy = DeviceRef("pocket-id", "Pocket"))),
        )
        assertEquals(listOf("Laptop is controlling this computer.", "Pocket is controlling Studio."), controlLines(driven))
        assertTrue(controlLines(ConnectSamples.three()).isEmpty())
    }

    // --- Adding a device, and the reasons one is not found ---

    @Test
    fun `an address is taken with its port, and refused without one`() {
        assertEquals("192.168.1.30" to 41234, parseDeviceAddress(" 192.168.1.30:41234 "))
        assertEquals("fe80::1" to 41234, parseDeviceAddress("[fe80::1]:41234"))
        assertEquals("studio.local" to 5000, parseDeviceAddress("http://studio.local:5000/"))
        assertNull(parseDeviceAddress("192.168.1.30"))
        assertNull(parseDeviceAddress("192.168.1.30:0"))
        assertNull(parseDeviceAddress("192.168.1.30:99999"))
        assertNull(parseDeviceAddress("fe80::1:41234"), "an IPv6 address needs its brackets, or the port is part of it")
        assertNull(parseDeviceAddress(""))
    }

    @Test
    fun `the reasons a device is not found are the honest ones, the account first`() {
        val signedOut = connectChecks(ConnectState(), enabledHere = true)
        assertEquals(listOf(CheckKind.ACCOUNT, CheckKind.WIFI, CheckKind.FIREWALL, CheckKind.PHONE), signedOut.map { it.kind })
        assertEquals(false, signedOut[0].fine)
        assertTrue("tick Public too" in signedOut[2].detail)
        assertTrue("background" in signedOut[3].title)
        assertEquals(true, connectChecks(ConnectState(available = true), enabledHere = true)[0].fine)
        assertEquals(CheckKind.SWITCHED_OFF, connectChecks(ConnectState(), enabledHere = false)[0].kind)
    }

    @Test
    fun `the switch says what Connect does on and off, and that off ends a remote control under way`() {
        assertTrue(connectSwitchLine(enabled = false, ConnectState()).startsWith("Off:"))
        assertEquals("This computer finds your other devices, and they can find it.", connectSwitchLine(enabled = true, ConnectState()))
        assertTrue("ends the remote control" in connectSwitchLine(enabled = true, ConnectSamples.driving()))
    }

    @Test
    fun `the switch at the top of the page turns Connect off and on`() {
        // From on, whatever an earlier test in the shared settings left it at.
        state.setConnectEnabled(true)
        assertTrue(state.settings.value.preferences.connect.enabled)
        state.setConnectEnabled(false)
        assertFalse(state.settings.value.preferences.connect.enabled)
        state.setConnectEnabled(true)
        assertTrue(state.settings.value.preferences.connect.enabled)
    }

    // --- The player is the remote ---

    @Test
    fun `while driving, the player is drawn from the driven device and this computer plays nothing`() {
        fakeConnect(state, ConnectSamples.driving())
        waitFor { state.shownPlayback.value.track?.title == "Paper Moonrise" }
        assertEquals(PlaybackStatus.PLAYING, state.shownPlayback.value.status)
        assertEquals(.7f, state.shownPlayback.value.volume)
        assertFalse(state.shownPlayback.value.volumeBoostAvailable, "the booster is this computer's")
        waitFor { state.shownQueue.value.tracks.size == 6 }
        assertEquals("Paper Moonrise", state.shownQueue.value.current?.title)
        assertFalse(state.playback.value.isPlaying, "nothing plays here while the music is on Pocket")
        waitFor { state.musicPlace.value is MusicPlace.Driving }

        // Let go of, the player is this computer's own again.
        fakeConnect(state, ConnectState(available = true))
        waitFor { state.shownPlayback.value.track == null }
        assertEquals(state.playback.value, state.shownPlayback.value)
    }

    @Test
    fun `the strip and the Devices button open the Connect page`() {
        state.navigate(Destination.HOME)
        // The made-up state is put back on each try: on a slow machine Connect's own start can land after it and
        // say there is nothing to show, and a strip that is not drawn cannot be clicked.
        waitFor(15_000) {
            fakeConnect(state, ConnectSamples.driving())
            click(state, 600, 26) { ConnectStrip(state, pill = false) }
            state.ui.value.destination == Destination.CONNECT
        }

        state.navigate(Destination.HOME)
        waitFor { state.ui.value.destination == Destination.HOME }
        waitFor(15_000) {
            fakeConnect(state, ConnectSamples.driving())
            click(state, 48, 48) { DevicesButton(state) }
            state.ui.value.destination == Destination.CONNECT
        }
    }

    @Test
    fun `the strip is drawn only while there is something to say`() {
        fakeConnect(state, ConnectState(available = true))
        // Nothing drawn: the scene stays the colour it was cleared to.
        val empty = drawn(state, 600, 26) { ConnectStrip(state, pill = false) }
        fakeConnect(state, ConnectSamples.driving())
        val strip = drawn(state, 600, 26) { ConnectStrip(state, pill = false) }
        assertTrue(empty < 0.01f, "a strip with nothing to say: $empty")
        assertTrue(strip > 0.5f, "the strip while driving covers its band: $strip")
    }

    // --- Helpers ---

    private fun waitFor(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "timed out" }
            Thread.sleep(25)
        }
    }

    private fun scene(state: AppState, width: Int, height: Int, content: @androidx.compose.runtime.Composable () -> Unit) =
        ImageComposeScene(width, height, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null)))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
            }
        }

    private fun click(state: AppState, width: Int, height: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
        val scene = scene(state, width, height, content)
        try {
            repeat(4) { scene.render(it * 50_000_000L); Thread.sleep(40) }
            val middle = Offset(width / 2f, height / 2f)
            scene.sendPointerEvent(PointerEventType.Move, middle)
            scene.sendPointerEvent(PointerEventType.Press, middle)
            scene.sendPointerEvent(PointerEventType.Release, middle)
            scene.render(400_000_000L)
        } finally {
            scene.close()
        }
    }

    /** How much of the scene is drawn on, from 0 to 1: pixels that are not fully clear. */
    private fun drawn(state: AppState, width: Int, height: Int, content: @androidx.compose.runtime.Composable () -> Unit): Float {
        val scene = scene(state, width, height, content)
        try {
            var image = scene.render(0)
            repeat(4) { Thread.sleep(40); image = scene.render((it + 1) * 50_000_000L) }
            val bitmap = org.jetbrains.skia.Bitmap.makeFromImage(image)
            var painted = 0
            for (y in 0 until height) for (x in 0 until width) if (bitmap.getColor(x, y) ushr 24 != 0) painted++
            return painted.toFloat() / (width * height)
        } finally {
            scene.close()
        }
    }
}

/** Made-up devices on a made-up network, for the tests and the pictures: nobody's real ones. */
internal object ConnectSamples {
    val now: Long = System.currentTimeMillis()

    private val moonrise = Fixtures.tracks[7]

    val pocket = ConnectPeer("pocket-id", "Pocket", DeviceKind.PHONE, "192.168.1.30", 41234, now)
    val laptop = ConnectPeer("laptop-id", "Laptop", DeviceKind.DESKTOP, "192.168.1.31", 41235, now)
    val studio = ConnectPeer(
        "studio-id", "Studio", DeviceKind.TERMINAL, "192.168.1.32", 41236, now,
        nowPlaying = song(Fixtures.tracks[10], playing = false, positionMs = 40_000),
    )

    /** Pocket as it says itself while this computer drives it: playing Paper Moonrise, driven by Desk. */
    val pocketDriven = pocket.copy(nowPlaying = song(moonrise, playing = true, positionMs = 74_000), drivenBy = DeviceRef("desk-id", "Desk"))

    fun song(track: app.noctorium.domain.Track, playing: Boolean, positionMs: Long) = NowPlaying(
        title = track.title,
        artist = track.artistLine,
        provider = track.provider.name,
        artworkUrl = track.artworkUrl,
        playing = playing,
        positionMs = positionMs,
        durationMs = track.durationMs ?: 0,
    )

    /** Three other devices, nobody driving anybody. */
    fun three() = ConnectState(
        available = true,
        listening = true,
        thisDevice = "Desk",
        thisDeviceId = "desk-id",
        devices = listOf(pocket, laptop, studio),
    )

    /** This computer as Pocket's remote control, Pocket playing Paper Moonrise from a queue of six. */
    fun driving(atMs: Long = now) = three().copy(
        devices = listOf(pocketDriven, laptop, studio),
        target = pocketDriven,
        remote = RemoteState(
            deviceId = "pocket-id",
            deviceName = "Pocket",
            kind = DeviceKind.PHONE,
            playing = true,
            track = moonrise.toWire(),
            positionMs = 74_000,
            durationMs = moonrise.durationMs ?: 0,
            volume = .7f,
            queueSize = 6,
            queueIndex = 2,
            repeat = "ALL",
            controlledBy = "Desk",
            controlledById = "desk-id",
        ),
        remoteAtMs = atMs,
        remoteQueue = QueueSnapshot(Fixtures.tracks.drop(5).take(6), 2),
    )

    /** Laptop driving this computer, which is playing. */
    fun driven() = three().copy(
        devices = listOf(pocket, laptop.copy(driving = DeviceRef("desk-id", "Desk")), studio),
        controlledBy = "Laptop",
        controlledById = "laptop-id",
    )

    /** The music on Laptop, by itself; this computer idle. */
    fun elsewhere() = three().copy(
        devices = listOf(pocket, laptop.copy(nowPlaying = song(Fixtures.tracks[9], playing = true, positionMs = 101_000)), studio),
    )
}

/**
 * Puts [connect] where AppState reads Connect from, as WindowTour puts a library or a search: Connect's own state is
 * private to it, and there is no other way to show a page of devices without devices.
 */
internal fun fakeConnect(state: AppState, connect: ConnectState) {
    val manager = AppState::class.java.getDeclaredField("connectManager").apply { isAccessible = true }.get(state)
    @Suppress("UNCHECKED_CAST")
    val flow = manager.javaClass.getDeclaredField("mutableState").apply { isAccessible = true }.get(manager) as MutableStateFlow<ConnectState>
    flow.value = connect
}
