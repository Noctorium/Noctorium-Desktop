package app.noctorium.ui

import androidx.compose.ui.awt.ComposeWindow
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import org.jetbrains.skiko.SkiaLayer
import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.HierarchyEvent

/**
 * The Glass theme's window: see-through, with the desktop behind it showing through the pages.
 *
 * Done without giving up the window Windows draws. An undecorated window would be the obvious way to a
 * transparent one, and it would cost dragging, snapping, resizing from every edge and the maximise behaviour,
 * redrawn by hand -- the trade [WindowChrome] already refused. Instead the frame keeps its title bar and edges,
 * and only the part Compose draws in is made clear: Compose is told to draw with transparency, the window
 * manager is told the frame reaches all the way across the window, and Windows 11 lays one of its own
 * backdrops -- Acrylic, the blurred desktop -- behind both. Windows 10 has no such backdrop to ask for, and gets
 * the older blur-behind accent the taskbar used, which is the same picture by a different call. [inPlace] says
 * when that is the way.
 *
 * Linux has no such call: AWT only draws a window clear when it has no frame at all. So there Glass is a window
 * without one -- Noctorium draws its own title bar, see FramelessTitleBar, and Compose its own edges to resize
 * by. KDE's window manager blurs what is behind such a window when asked; other desktops show it clear, and the
 * pages are drawn more solid to make up for it. [frameless] says when that is the way.
 *
 * Either way, whether Compose draws clear is settled when the window is first shown and cannot be changed after:
 * so crossing between Glass and any other theme builds the window again, in the same place and size, with the
 * player carrying on underneath (see Main). Every other theme gets exactly the window it always had.
 *
 * Anywhere else -- macOS, a desktop with no compositor, a machine too old -- the theme is its colours, a dark
 * slate, and nothing about the window changes.
 */
object SeeThroughWindow {
    private val windows = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

    /** The build of Windows, such as 22631, or 0 where it cannot be read or this is not Windows. */
    private val build: Int by lazy {
        if (!windows) 0 else runCatching { ntdllBuild() }.getOrDefault(0)
    }

    private val linux = System.getProperty("os.name").orEmpty().startsWith("Linux", ignoreCase = true)

    /**
     * Asks for the frameless way on any system, so that it can be tried on a Windows machine: -Dnoctorium.glass.frameless=true.
     * The window manager's blur is Linux's own and is not tried elsewhere.
     */
    private val forceFrameless = System.getProperty("noctorium.glass.frameless") == "true"

    /** Whether see-through is a matter of a few calls on the window as it is, with its own frame: Windows 10 and 11. */
    val inPlace: Boolean get() = windows && !forceFrameless && build >= WINDOWS_10_BLUR && dwm != null

    /** Whether see-through means a window without a frame, drawn clear by Compose: Linux, with a compositor. */
    val frameless: Boolean get() = !inPlace && (linux || forceFrameless) && translucentWindows

    /** Whether this machine can show the desktop through the window at all, one way or the other. */
    val supported: Boolean get() = inPlace || frameless

    /** Whether windows here can be drawn partly clear, which on Linux needs a compositing window manager. */
    private val translucentWindows: Boolean by lazy {
        !GraphicsEnvironment.isHeadless() && runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)
        }.getOrDefault(false)
    }

    /** How the window shows what is behind it, which decides how solid the pages over it have to be. */
    enum class Glazing {
        /** Not at all: the theme is its colours. */
        NONE,

        /** Clear, with the desktop behind it sharp: the pages are drawn more solid, or the writing would be lost. */
        CLEAR,

        /** Through the system's blur, which does half the work of keeping the writing readable. */
        BLURRED,
    }

    /** Windows 11 22H2 and later, where a backdrop is one documented attribute. */
    private val systemBackdrop: Boolean get() = build >= WINDOWS_11_22H2

    private const val WINDOWS_10_BLUR = 17134
    private const val WINDOWS_11_22H2 = 22621

    private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20
    private const val DWMWA_SYSTEMBACKDROP_TYPE = 38
    private const val DWMSBT_AUTO = 0
    private const val DWMSBT_TRANSIENTWINDOW = 3 // Acrylic

    private const val DWMWA_CAPTION_COLOR = 35
    private const val DWMWA_BORDER_COLOR = 34
    /** Lets the backdrop show through the title bar as well, rather than a colour painted over it. */
    private const val DWMWA_COLOR_DEFAULT = -0x1 // 0xFFFFFFFF

    private const val WCA_ACCENT_POLICY = 19
    private const val ACCENT_DISABLED = 0
    private const val ACCENT_ENABLE_BLURBEHIND = 3

    @Suppress("FunctionName")
    private interface Dwmapi : Library {
        fun DwmSetWindowAttribute(window: Pointer, attribute: Int, value: Pointer, size: Int): Int
        fun DwmExtendFrameIntoClientArea(window: Pointer, margins: Margins): Int
    }

    @Suppress("FunctionName")
    private interface User32 : Library {
        fun SetWindowCompositionAttribute(window: Pointer, data: CompositionData): Boolean
    }

    @Suppress("FunctionName")
    private interface Ntdll : Library {
        fun RtlGetVersion(info: OsVersionInfo): Int
    }

    @Structure.FieldOrder("left", "right", "top", "bottom")
    class Margins(@JvmField var left: Int = 0, @JvmField var right: Int = 0, @JvmField var top: Int = 0, @JvmField var bottom: Int = 0) : Structure()

    @Structure.FieldOrder("accentState", "accentFlags", "gradientColor", "animationId")
    class AccentPolicy : Structure() {
        @JvmField var accentState = 0
        @JvmField var accentFlags = 0
        @JvmField var gradientColor = 0
        @JvmField var animationId = 0
    }

    @Structure.FieldOrder("attribute", "data", "size")
    class CompositionData : Structure() {
        @JvmField var attribute = 0
        @JvmField var data: Pointer? = null
        @JvmField var size = 0
    }

    @Structure.FieldOrder("size", "major", "minor", "build", "platform", "servicePack")
    class OsVersionInfo : Structure() {
        @JvmField var size = 0
        @JvmField var major = 0
        @JvmField var minor = 0
        @JvmField var build = 0
        @JvmField var platform = 0
        @JvmField var servicePack = CharArray(128)
    }

    private val dwm: Dwmapi? by lazy { if (!windows) null else runCatching { Native.load("dwmapi", Dwmapi::class.java) }.getOrNull() }
    private val user32: User32? by lazy { if (!windows) null else runCatching { Native.load("user32", User32::class.java) }.getOrNull() }

    /** The real build: os.version and GetVersionEx both say whatever the program's manifest lets them. */
    private fun ntdllBuild(): Int {
        val ntdll = Native.load("ntdll", Ntdll::class.java)
        val info = OsVersionInfo()
        info.size = info.size()
        info.write()
        check(ntdll.RtlGetVersion(info) == 0)
        info.read()
        return info.build
    }

    /**
     * Readies the process for a see-through window, before anything of AWT has started: see Main.
     *
     * Swing paints the window's own background and the panels under Compose's surface, and on Windows it does
     * that through Direct3D by default, which writes every pixel solid -- so a clear page showed a black one. Its
     * plain GDI painting writes black as nothing at all once the frame covers the window, which is what lets the
     * desktop through. Nothing of Noctorium's is drawn by Swing beyond those backgrounds -- Compose draws through
     * a Direct3D of its own, which this leaves alone -- so it is asked for on every Windows machine, whichever
     * theme is chosen, and Glass can be picked at any time without starting again.
     *
     * It also starts watching for the Glass theme's window being shown: see [clearNextWindow].
     */
    fun prepareProcess() {
        if (!windows) return
        if (System.getProperty(JAVA2D_D3D) == null) System.setProperty(JAVA2D_D3D, "false")
        // Watches for a window becoming real, which happens to the window itself before anything inside it.
        runCatching { Toolkit.getDefaultToolkit().addAWTEventListener(::clearBeforeShown, AWTEvent.HIERARCHY_EVENT_MASK) }
    }

    private const val JAVA2D_D3D = "sun.java2d.d3d"

    /**
     * Whether the next window with a frame to be shown is the Glass theme's, set by Main as it builds one.
     *
     * Compose's renderer chooses a clear or a solid surface as it is attached to its window, once, and its own switch
     * for it refuses a window with a frame -- the very thing this keeps. The renderer underneath has no such rule; it
     * only has to be told before then. A window is given its native self before anything inside it is, and says so,
     * which is the moment [clearBeforeShown] uses.
     */
    @Volatile
    var clearNextWindow: Boolean = false

    private fun clearBeforeShown(event: AWTEvent) {
        if (event !is HierarchyEvent || event.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() == 0L) return
        val window = event.changed as? ComposeWindow ?: return
        if (!clearNextWindow || !window.isDisplayable || window.isUndecorated) return
        val layer = renderer(window) ?: return
        if (!layer.isDisplayable) layer.transparency = true
    }

    /**
     * Makes [window] see-through, or puts it back as it was. Where see-through is not [supported] it does nothing,
     * and the theme is only its colours.
     *
     * Returns how the window shows the desktop now, which is what the pages should be drawn for.
     *
     * A window is made clear or solid when it is built -- see [clearNextWindow] -- and one built solid stays NONE here.
     */
    fun apply(window: Window?, on: Boolean): Glazing {
        if (window !is ComposeWindow || !window.isDisplayable) return Glazing.NONE
        if (frameless) {
            if (!window.isTransparent) return Glazing.NONE
            return if (on && linux && KdeBlur.request(window)) Glazing.BLURRED else if (on) Glazing.CLEAR else Glazing.NONE
        }
        if (!inPlace) return Glazing.NONE
        if (on && renderer(window)?.transparency != true) return Glazing.NONE
        val dwm = dwm ?: return Glazing.NONE
        val handle = runCatching { Native.getWindowPointer(window) }.getOrNull() ?: return Glazing.NONE
        return runCatching {
            if (on) {
                // Under Compose's surface are Swing's panels and the window's own background, light grey, and they
                // would show through a clear page. Black is what the window manager takes for no paint at all once
                // the frame covers the window; null hands them their own grey again.
                underneath(window).forEach { it.background = java.awt.Color.BLACK }
            }
            // The whole window counts as frame, which is what a backdrop is laid behind; nothing of it, to undo.
            val reach = if (on) -1 else 0
            dwm.DwmExtendFrameIntoClientArea(handle, Margins(reach, reach, reach, reach))
            if (systemBackdrop) {
                // Dark, so the glass is smoked rather than milky and the title is written in white: Glass is a dark
                // theme. Off again, the title bar is coloured by WindowChrome as every other theme's is.
                dwm.set(handle, DWMWA_USE_IMMERSIVE_DARK_MODE, 1)
                dwm.set(handle, DWMWA_SYSTEMBACKDROP_TYPE, if (on) DWMSBT_TRANSIENTWINDOW else DWMSBT_AUTO)
                if (on) {
                    dwm.set(handle, DWMWA_CAPTION_COLOR, DWMWA_COLOR_DEFAULT)
                    dwm.set(handle, DWMWA_BORDER_COLOR, DWMWA_COLOR_DEFAULT)
                }
            } else {
                blurBehind(handle, on)
            }
            if (!on) underneath(window).forEach { it.background = null }
            window.repaint()
            if (on) Glazing.BLURRED else Glazing.NONE
        }.getOrDefault(Glazing.NONE)
    }

    /** Everything that paints a background beneath Compose's surface: the window and the panels it sits in. */
    private fun underneath(window: ComposeWindow): List<Component> {
        val layer = renderer(window) ?: return listOf(window)
        return generateSequence<Component>(layer.parent) { it.parent }.toList() + layer.components.toList()
    }

    /** Compose's drawing surface inside [window], which is a component like any other in it. */
    private fun renderer(window: ComposeWindow): SkiaLayer? {
        fun find(component: Component): SkiaLayer? {
            if (component is SkiaLayer) return component
            if (component is Container) component.components.forEach { child -> find(child)?.let { return it } }
            return null
        }
        return find(window)
    }

    /** Windows 10's way: the accent the Start menu and taskbar used for their blur. */
    private fun blurBehind(handle: Pointer, on: Boolean) {
        val user32 = user32 ?: return
        val policy = AccentPolicy().apply {
            accentState = if (on) ACCENT_ENABLE_BLURBEHIND else ACCENT_DISABLED
            write()
        }
        val data = CompositionData().apply {
            attribute = WCA_ACCENT_POLICY
            data = policy.pointer
            size = policy.size()
        }
        user32.SetWindowCompositionAttribute(handle, data)
    }

    private fun Dwmapi.set(handle: Pointer, attribute: Int, value: Int) {
        Memory(4).use { memory ->
            memory.setInt(0, value)
            DwmSetWindowAttribute(handle, attribute, memory, 4)
        }
    }

    /**
     * KDE's blur behind a window: a property on the X window that KWin reads, with no region in it meaning the
     * whole window. KWin is the only window manager that reads it -- GNOME's has nothing of the kind -- so it is
     * only set where KDE is the desktop, and its answer is whether it was set, not whether anybody blurred.
     */
    private object KdeBlur {
        private const val XA_CARDINAL = 6L
        private const val PROP_MODE_REPLACE = 0

        @Suppress("FunctionName")
        private interface X11 : Library {
            fun XOpenDisplay(name: String?): Pointer?
            fun XInternAtom(display: Pointer, name: String, onlyIfExists: Int): NativeLong
            fun XChangeProperty(display: Pointer, window: NativeLong, property: NativeLong, type: NativeLong, format: Int, mode: Int, data: Pointer, elements: Int): Int
            fun XFlush(display: Pointer): Int
            fun XCloseDisplay(display: Pointer): Int
        }

        private val x11: X11? by lazy { runCatching { Native.load("X11", X11::class.java) }.getOrNull() }

        private val kde: Boolean
            get() = System.getenv("XDG_CURRENT_DESKTOP").orEmpty().contains("KDE", ignoreCase = true) ||
                System.getenv("KDE_FULL_SESSION") == "true"

        fun request(window: Window): Boolean {
            if (!kde || System.getenv("DISPLAY").isNullOrEmpty()) return false
            val x11 = x11 ?: return false
            return runCatching {
                val id = Native.getWindowID(window)
                val display = x11.XOpenDisplay(null) ?: return false
                try {
                    val atom = x11.XInternAtom(display, "_KDE_NET_WM_BLUR_BEHIND_REGION", 0)
                    Memory(8).use { none ->
                        x11.XChangeProperty(display, NativeLong(id), atom, NativeLong(XA_CARDINAL), 32, PROP_MODE_REPLACE, none, 0)
                    }
                    x11.XFlush(display)
                } finally {
                    x11.XCloseDisplay(display)
                }
                true
            }.getOrDefault(false)
        }
    }
}
