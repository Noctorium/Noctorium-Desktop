package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The global shortcuts against a pretend system: nothing here registers a key with the real one, so the tests can
 * run on a machine somebody is using without taking Ctrl+Alt+Space from them.
 */
class GlobalHotKeysTest {

    /** A system's hot keys that only keeps a list: what was asked for, what it let go of, and a press on demand. */
    private class FakeSystem(private val taken: Set<KeyChord> = emptySet()) {
        val started = mutableListOf<FakeBackend>()
        val support = HotKeySupport.Available("pretend hot keys") { onPressed -> FakeBackend(taken, onPressed).also { started += it } }
    }

    private class FakeBackend(private val taken: Set<KeyChord>, val onPressed: (Int) -> Unit) : HotKeyBackend {
        var held: Map<Int, KeyChord> = emptyMap()
        val asked = mutableListOf<Map<Int, KeyChord>>()
        var names: Map<Int, String> = emptyMap()
        var closed = false

        override fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String>): Map<Int, HotKeyResult> {
            check(!closed) { "asked after closing" }
            asked += wanted
            this.names = names
            val results = wanted.mapValues { (_, chord) -> if (chord in taken) HotKeyResult.Taken else HotKeyResult.Registered }
            held = wanted.filterKeys { results[it] == HotKeyResult.Registered }
            return results
        }

        fun press(chord: KeyChord) = held.entries.first { it.value == chord }.key.let(onPressed)

        override fun close() {
            closed = true
            held = emptyMap()
        }
    }

    private val now = Executor { it.run() }
    private val search = KeyChord("Space", ctrl = true, alt = true)
    private val play = KeyChord("Home", ctrl = true, alt = true)

    @Test
    fun `nothing is asked of the system while they are off, or while none is bound`() {
        val system = FakeSystem()
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = false, bindings = mapOf("quickSearch" to search))
        keys.apply(enabled = true, bindings = emptyMap())
        assertTrue(system.started.isEmpty(), "a backend was started with nothing to hold")
        assertTrue(keys.state.value.results.isEmpty())
        assertEquals("pretend hot keys", keys.state.value.how)
    }

    @Test
    fun `switched on, each chord is asked for and a press arrives by the action's name`() {
        val system = FakeSystem()
        val pressed = mutableListOf<String>()
        val keys = GlobalHotKeys(system.support, onPressed = { pressed += it }, title = { "Title of $it" }, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search, "playPause" to play))
        val backend = system.started.single()
        assertEquals(setOf(search, play), backend.held.values.toSet())
        assertEquals(mapOf("quickSearch" to HotKeyResult.Registered, "playPause" to HotKeyResult.Registered), keys.state.value.results)
        assertEquals(setOf("Title of quickSearch", "Title of playPause"), backend.names.values.toSet(), "a portal lists them by name")
        backend.press(search)
        backend.press(play)
        assertEquals(listOf("quickSearch", "playPause"), pressed)
    }

    @Test
    fun `a chord another program holds is reported as taken, and the rest still work`() {
        val system = FakeSystem(taken = setOf(play))
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search, "playPause" to play))
        assertEquals(HotKeyResult.Taken, keys.state.value.results["playPause"])
        assertEquals(HotKeyResult.Registered, keys.state.value.results["quickSearch"])
        assertEquals(setOf(search), system.started.single().held.values.toSet())
    }

    @Test
    fun `switching them off gives every chord back, down to the backend, and on again starts afresh`() {
        val system = FakeSystem()
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        keys.apply(enabled = false, bindings = mapOf("quickSearch" to search))
        assertTrue(system.started.single().closed)
        assertTrue(keys.state.value.results.isEmpty())
        assertFalse(keys.state.value.enabled)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        assertEquals(2, system.started.size)
        assertFalse(system.started.last().closed)
    }

    @Test
    fun `a changed chord is asked for in place of the old one, by the same number`() {
        val system = FakeSystem()
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search, "playPause" to play))
        val other = KeyChord("F13")
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to other, "playPause" to play))
        val backend = system.started.single()
        val (first, second) = backend.asked
        val number = first.entries.first { it.value == search }.key
        assertEquals(other, second[number], "the action keeps its number, so a press is still its press")
        assertEquals(play, second.values.single { it != other })
    }

    @Test
    fun `two actions on one chord cannot both have it`() {
        val system = FakeSystem()
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = linkedMapOf("quickSearch" to search, "like" to search))
        assertEquals(listOf(search), system.started.single().held.values.toList())
        assertEquals(setOf("quickSearch"), keys.state.value.results.keys)
    }

    @Test
    fun `closing gives everything back, and nothing is asked afterwards`() {
        val system = FakeSystem()
        val keys = GlobalHotKeys(system.support, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        keys.close()
        assertTrue(system.started.single().closed)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        assertEquals(1, system.started.size)
    }

    @Test
    fun `where the system has none, it says why and asks nothing`() {
        val keys = GlobalHotKeys(HotKeySupport.Unavailable("Not supported on this desktop."), onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        assertFalse(keys.state.value.supported)
        assertEquals("Not supported on this desktop.", keys.state.value.unavailable)
        assertTrue(keys.state.value.results.isEmpty())
    }

    @Test
    fun `a backend that cannot start says so against each chord`() {
        val keys = GlobalHotKeys(HotKeySupport.Available("broken") { error("No display") }, onPressed = {}, worker = now)
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        val result = assertIs<HotKeyResult.Failed>(keys.state.value.results["quickSearch"])
        assertEquals("No display", result.reason)
    }

    @Test
    fun `while the system is asked, each chord is shown as being asked for`() {
        val queued = mutableListOf<Runnable>()
        val keys = GlobalHotKeys(FakeSystem().support, onPressed = {}, worker = { queued += it })
        keys.apply(enabled = true, bindings = mapOf("quickSearch" to search))
        assertEquals(HotKeyResult.Pending, keys.state.value.results["quickSearch"])
        queued.forEach(Runnable::run)
        assertEquals(HotKeyResult.Registered, keys.state.value.results["quickSearch"])
    }

    // --- Which system, and its names for keys ---

    @Test
    fun `each desktop gets its own hot keys, and Wayland without the portal is told it has none`() {
        assertIs<HotKeySupport.Available>(SystemHotKeys.support("Windows 11", emptyMap()) { false })
        assertIs<HotKeySupport.Available>(SystemHotKeys.support("Mac OS X", emptyMap()) { false })
        val x11 = assertIs<HotKeySupport.Available>(SystemHotKeys.support("Linux", mapOf("DISPLAY" to ":0", "XDG_SESSION_TYPE" to "x11")) { false })
        assertEquals("X11 key grabs", x11.how)
        val wayland = assertIs<HotKeySupport.Unavailable>(SystemHotKeys.support("Linux", mapOf("WAYLAND_DISPLAY" to "wayland-0", "DISPLAY" to ":0")) { false })
        assertTrue(wayland.reason.startsWith("Not supported on this desktop"), wayland.reason)
        assertIs<HotKeySupport.Available>(SystemHotKeys.support("Linux", mapOf("XDG_SESSION_TYPE" to "wayland")) { true })
        assertIs<HotKeySupport.Unavailable>(SystemHotKeys.support("Linux", emptyMap()) { false })
        assertIs<HotKeySupport.Unavailable>(SystemHotKeys.support("Plan 9", emptyMap()) { false })
    }

    /** Every key a chord can be written with, as [KeyChord] knows them. */
    private val everyKey: List<String> =
        ('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString) + (1..24).map { "F$it" } + KeyChord.NAMED_KEYS

    @Test
    fun `Windows and X have a name for every key a chord can have`() {
        everyKey.forEach { key ->
            assertNotNull(WindowsKeys.virtualKey(key), "Windows: $key")
            assertNotNull(X11Keys.keysym(key), "X: $key")
            assertTrue(PortalKeys.trigger(KeyChord(key, ctrl = true)).startsWith("CTRL+"), "portal: $key")
        }
        assertEquals(0x41, WindowsKeys.virtualKey("A"))
        assertEquals(0x87, WindowsKeys.virtualKey("F24"))
        assertEquals(0x20, WindowsKeys.virtualKey("Space"))
        assertEquals(0x2 or 0x1, WindowsKeys.modifiers(KeyChord("Space", ctrl = true, alt = true)))
        assertEquals(0x61L, X11Keys.keysym("A"), "X's letters are lower case")
        assertEquals(0xFFBEL, X11Keys.keysym("F1"))
        assertEquals(4 or 8, X11Keys.modifiers(KeyChord("Space", ctrl = true, alt = true)))
        assertEquals("CTRL+ALT+space", PortalKeys.trigger(KeyChord("Space", ctrl = true, alt = true)))
        assertEquals("LOGO+SHIFT+a".split('+').toSet(), PortalKeys.trigger(KeyChord("A", shift = true, meta = true)).split('+').toSet())
    }

    @Test
    fun `a Mac names every key on its keyboard, and not the media keys, which its hot keys cannot take`() {
        (('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString) + (1..20).map { "F$it" }).forEach { key ->
            assertNotNull(MacKeys.keyCode(key), "Mac: $key")
        }
        assertEquals(0x31, MacKeys.keyCode("Space"))
        assertEquals(0x03, MacKeys.keyCode("F"))
        assertNull(MacKeys.keyCode("MediaPlayPause"))
        assertEquals((1 shl 12) or (1 shl 11), MacKeys.modifiers(KeyChord("Space", ctrl = true, alt = true)))
        assertEquals(1 shl 8, MacKeys.modifiers(KeyChord("F", meta = true)))
    }

    // --- Just enough D-Bus ---

    @Test
    fun `D-Bus values written are read back as they were, aligned as the format asks`() {
        val signature = "oa(sa{sv})sa{sv}"
        val values = listOf(
            "/org/freedesktop/portal/desktop/session/1_23/noctorium",
            listOf(
                listOf("noctorium-1", mapOf("description" to Variant("s", "Quick search"), "preferred_trigger" to Variant("s", "CTRL+ALT+space"))),
                listOf("noctorium-2", mapOf("description" to Variant("s", "Play or pause"))),
            ),
            "",
            mapOf("handle_token" to Variant("s", "noctorium7")),
        )
        val bytes = DBusWriter().apply { write(signature, values) }.bytes()
        val read = DBusReader(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)).read(signature)
        assertEquals(values, read)
    }

    @Test
    fun `a D-Bus string is its length, its bytes and a nul, and a uint64 waits for eight-byte alignment`() {
        val bytes = DBusWriter().apply { write("yst", listOf(7, "ab", 1L)) }.bytes()
        // y, three bytes of padding to the uint32 length, "ab\0", then padding up to the eighth byte for the t.
        assertEquals(listOf(7, 0, 0, 0, 2, 0, 0, 0, 'a'.code, 'b'.code, 0), bytes.take(11).map { it.toInt() and 0xFF })
        assertEquals(16 + 8, bytes.size)
        assertEquals(1, bytes[16].toInt())
        assertEquals(listOf("s", "a{sv}", "(yv)", "t"), DBusSignature.split("sa{sv}(yv)t"))
    }

    @Test
    fun `the session bus is found where the environment says, and an abstract socket is not one Java can open`() {
        assertEquals("/run/user/1000/bus", DBusConnection.sessionSocket(mapOf("DBUS_SESSION_BUS_ADDRESS" to "unix:path=/run/user/1000/bus"))?.toString()?.replace('\\', '/'))
        assertEquals(
            "/tmp/dbus-socket",
            DBusConnection.sessionSocket(mapOf("DBUS_SESSION_BUS_ADDRESS" to "unix:abstract=/tmp/dbus-x;unix:path=/tmp/dbus-socket,guid=abc"))?.toString()?.replace('\\', '/'),
        )
        assertNull(DBusConnection.sessionSocket(mapOf("DBUS_SESSION_BUS_ADDRESS" to "unix:abstract=/tmp/dbus-x")))
    }
}
