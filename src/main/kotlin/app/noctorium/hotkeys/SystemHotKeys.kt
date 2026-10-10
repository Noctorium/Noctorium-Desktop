package app.noctorium.hotkeys

/**
 * Which hot-key service this desktop has: Windows' RegisterHotKey, the Mac's Carbon hot keys, X11's key grabs, or
 * the GlobalShortcuts portal on Wayland -- and, where there is none, why not, in words for the Shortcuts page.
 *
 * Decided from the environment alone, without asking the system for anything: [support] is looked at by the page
 * long before anything is registered, and in a test nothing is registered at all.
 */
object SystemHotKeys {
    fun support(
        os: String = System.getProperty("os.name").orEmpty(),
        environment: Map<String, String> = System.getenv(),
        portal: () -> Boolean = PortalHotKeys::available,
    ): HotKeySupport {
        val name = os.lowercase()
        return when {
            name.startsWith("windows") -> HotKeySupport.Available("Windows' own hot keys") { WindowsHotKeys(it) }
            name.startsWith("mac") -> HotKeySupport.Available("macOS's own hot keys") { MacHotKeys(it) }
            name.startsWith("linux") || name.contains("bsd") -> linux(environment, portal)
            else -> HotKeySupport.Unavailable("Global shortcuts are not supported on this system.")
        }
    }

    private fun linux(environment: Map<String, String>, portal: () -> Boolean): HotKeySupport {
        val wayland = environment["XDG_SESSION_TYPE"].equals("wayland", ignoreCase = true) || !environment["WAYLAND_DISPLAY"].isNullOrBlank()
        if (wayland) {
            // Under Wayland an X grab only sees keys while one of XWayland's own windows has focus, which is no
            // use for a shortcut meant to work from anywhere. The portal is the way, where the desktop offers it.
            return if (portal()) {
                HotKeySupport.Available("your desktop's global shortcuts, which ask you first") { PortalHotKeys(it) }
            } else {
                HotKeySupport.Unavailable(
                    "Not supported on this desktop: it is a Wayland session whose desktop does not offer global " +
                        "shortcuts to programs. KDE Plasma and GNOME 48 or newer do.",
                )
            }
        }
        if (environment["DISPLAY"].isNullOrBlank()) {
            return HotKeySupport.Unavailable("Not supported on this desktop: there is no X display to take keys from.")
        }
        return HotKeySupport.Available("X11 key grabs") { X11HotKeys(it) }
    }
}
