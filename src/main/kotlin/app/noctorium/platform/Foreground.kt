package app.noctorium.platform

import app.noctorium.hotkeys.MacApplication
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef

/**
 * The window that was in front before Noctorium's quick search window came up over it, and handing the front back
 * to it when that closes -- so a song found from inside a text editor leaves the caret in the text editor.
 *
 * On Windows the window itself is remembered and asked to come forward again, which Windows allows because the
 * program asking is the one in front at that moment. A Mac has no window to name across programs, so Noctorium
 * hides itself instead, as ⌘H does, and the Mac brings forward whatever was before it -- but only while its own
 * window is not showing, since hiding would take that away too. Elsewhere the window manager does what it does
 * when a window closes, which is usually the same thing.
 */
object Foreground {
    private val os = System.getProperty("os.name").orEmpty()
    private val windows = os.startsWith("Windows", ignoreCase = true)
    private val mac = os.startsWith("Mac", ignoreCase = true)

    /** What is in front now, to give the front back to later; null where there is nothing to remember. */
    fun capture(): Any? = if (windows) runCatching { User32.INSTANCE.GetForegroundWindow() }.getOrNull() else null

    /** Gives the front back to [previous]; [noctoriumShowing] is whether Noctorium's own window is open. */
    fun restore(previous: Any?, noctoriumShowing: Boolean) {
        when {
            windows -> (previous as? WinDef.HWND)?.let { runCatching { User32.INSTANCE.SetForegroundWindow(it) } }
            mac && !noctoriumShowing -> MacApplication.hide()
        }
    }
}
