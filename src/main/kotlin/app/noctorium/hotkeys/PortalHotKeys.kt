package app.noctorium.hotkeys

import app.noctorium.settings.KeyChord
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Global shortcuts on Wayland, through the desktop portal's GlobalShortcuts interface.
 *
 * Wayland gives no program another's keys -- which is the point of it -- so a chord from anywhere is something
 * the desktop grants: Noctorium proposes its chords, the desktop asks the listener (KDE and GNOME show a dialog
 * of their own) and may bind them to other keys than the ones proposed. What the desktop says it bound is what
 * the page shows. Proposing again means a new session, so a change is a close and a new start.
 *
 * Not run under Wayland while it was written; see the release notes.
 */
internal class PortalHotKeys(private val onPressed: (Int) -> Unit) : HotKeyBackend {
    private val bus = DBusConnection.session()
    private val tokens = AtomicInteger(0)
    private val responses = ConcurrentHashMap<String, CompletableFuture<Pair<Long, Map<String, Variant>>>>()
    @Volatile private var session: String? = null
    private var bound: Map<Int, KeyChord> = emptyMap()
    private var lastResults: Map<Int, HotKeyResult> = emptyMap()

    init {
        bus.onSignal = ::signal
        // Asked for before anything is called, so a reply that comes back quickly is not missed.
        listOf(
            "type='signal',interface='org.freedesktop.portal.Request',member='Response'",
            "type='signal',interface='org.freedesktop.portal.GlobalShortcuts',member='Activated'",
        ).forEach { rule ->
            bus.call("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "AddMatch", "s", listOf(rule))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun signal(message: DBusMessage) {
        when (message.member) {
            "Response" -> responses.remove(message.path)?.complete((message.body[0] as Long) to (message.body[1] as Map<String, Variant>))
            "Activated" -> if (message.body.getOrNull(0) == session) {
                (message.body.getOrNull(1) as? String)?.removePrefix(ID_PREFIX)?.toIntOrNull()?.let { runCatching { onPressed(it) } }
            }
        }
    }

    /** Calls a portal method that answers through a Request, and waits for that answer. */
    private fun request(member: String, signature: String, args: (token: String) -> List<Any?>, wait: Long): Pair<Long, Map<String, Variant>> {
        val token = "noctorium${tokens.incrementAndGet()}"
        val sender = bus.uniqueName.removePrefix(":").replace('.', '_')
        val path = "/org/freedesktop/portal/desktop/request/$sender/$token"
        val answer = CompletableFuture<Pair<Long, Map<String, Variant>>>()
        responses[path] = answer
        try {
            bus.call(DESTINATION, OBJECT, INTERFACE, member, signature, args(token))
            return answer.get(wait, TimeUnit.SECONDS)
        } finally {
            responses.remove(path)
        }
    }

    @Synchronized
    override fun bind(wanted: Map<Int, KeyChord>, names: Map<Int, String>): Map<Int, HotKeyResult> {
        if (wanted == bound && session != null) return lastResults
        closeSession()
        val (created, made) = request(
            "CreateSession",
            "a{sv}",
            { token -> listOf(mapOf("handle_token" to Variant("s", token), "session_handle_token" to Variant("s", "noctorium"))) },
            wait = 30,
        )
        if (created != 0L) return wanted.mapValues { HotKeyResult.Failed("The desktop would not start a session") }
        val handle = (made["session_handle"]?.value as? String) ?: return wanted.mapValues { HotKeyResult.Failed("The desktop gave no session") }
        session = handle
        val shortcuts = wanted.map { (number, chord) ->
            listOf(
                "$ID_PREFIX$number",
                mapOf(
                    "description" to Variant("s", names[number] ?: "Noctorium"),
                    "preferred_trigger" to Variant("s", PortalKeys.trigger(chord)),
                ),
            )
        }
        // The listener is asked by the desktop, and may take a while to answer.
        val (response, results) = request(
            "BindShortcuts",
            "oa(sa{sv})sa{sv}",
            { token -> listOf(handle, shortcuts, "", mapOf("handle_token" to Variant("s", token))) },
            wait = 180,
        )
        @Suppress("UNCHECKED_CAST")
        val granted = ((results["shortcuts"]?.value as? List<List<Any?>>).orEmpty()).mapNotNull { (it[0] as? String)?.removePrefix(ID_PREFIX)?.toIntOrNull() }.toSet()
        bound = wanted
        lastResults = wanted.mapValues { (number, _) ->
            when {
                response == 1L -> HotKeyResult.Failed("Not allowed on this desktop")
                number in granted -> HotKeyResult.Registered
                else -> HotKeyResult.Taken
            }
        }
        return lastResults
    }

    private fun closeSession() {
        session?.let { path -> runCatching { bus.call(DESTINATION, path, "org.freedesktop.portal.Session", "Close") } }
        session = null
        bound = emptyMap()
    }

    @Synchronized
    override fun close() {
        closeSession()
        bus.close()
    }

    companion object {
        private const val DESTINATION = "org.freedesktop.portal.Desktop"
        private const val OBJECT = "/org/freedesktop/portal/desktop"
        private const val INTERFACE = "org.freedesktop.portal.GlobalShortcuts"
        private const val ID_PREFIX = "noctorium-"

        /** Whether this desktop's portal offers global shortcuts at all, by asking it for the interface's version. */
        fun available(): Boolean = runCatching {
            DBusConnection.session().use { bus ->
                val reply = bus.call(DESTINATION, OBJECT, "org.freedesktop.DBus.Properties", "Get", "ss", listOf(INTERFACE, "version"))
                ((reply.body.firstOrNull() as? Variant)?.value as? Number)?.toInt() ?: 0
            } >= 1
        }.getOrDefault(false)
    }
}

/** Chords as the portal proposes them: the XDG shortcuts format, modifiers in capitals and the key's xkb name. */
internal object PortalKeys {
    private val NAMED = mapOf(
        "Space" to "space", "Enter" to "Return", "Escape" to "Escape", "Tab" to "Tab", "Backspace" to "BackSpace",
        "Delete" to "Delete", "Insert" to "Insert", "Home" to "Home", "End" to "End", "PageUp" to "Page_Up",
        "PageDown" to "Page_Down", "Left" to "Left", "Up" to "Up", "Right" to "Right", "Down" to "Down",
        "Comma" to "comma", "Period" to "period", "Slash" to "slash", "Semicolon" to "semicolon", "Quote" to "apostrophe",
        "BracketLeft" to "bracketleft", "BracketRight" to "bracketright", "Backslash" to "backslash", "Minus" to "minus",
        "Equals" to "equal", "Backquote" to "grave",
        "MediaPlayPause" to "XF86AudioPlay", "MediaNext" to "XF86AudioNext", "MediaPrevious" to "XF86AudioPrev",
        "MediaStop" to "XF86AudioStop", "VolumeUp" to "XF86AudioRaiseVolume", "VolumeDown" to "XF86AudioLowerVolume",
        "VolumeMute" to "XF86AudioMute",
    )

    fun trigger(chord: KeyChord): String = buildList {
        if (chord.ctrl) add("CTRL")
        if (chord.alt) add("ALT")
        if (chord.shift) add("SHIFT")
        if (chord.meta) add("LOGO")
        add(NAMED[chord.key] ?: if (chord.key.length == 1) chord.key.lowercase() else chord.key)
    }.joinToString("+")
}
