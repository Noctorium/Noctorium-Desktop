package app.noctorium.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.nio.file.Files
import java.nio.file.Path

/** Whether Noctorium starts when the listener signs in to the computer, and how it comes up if it does. */
enum class StartupMode(val displayName: String, val description: String) {
    OFF("Off", "Noctorium starts when you open it."),
    WINDOW("Open the window", "Noctorium opens with the computer, as though you had started it yourself."),
    TRAY("In the tray", "Noctorium starts with the computer and waits in the tray, out of the way until you want it."),
}

/**
 * The operating system's list of programs to start at sign-in, reduced to the one entry that is Noctorium's.
 *
 * Read from there every time rather than remembered in the settings, because the listener can switch it
 * off from there too -- Task Manager's Startup tab, GNOME's Startup Applications -- and a copy in the
 * settings file would go on saying "on" about an entry that no longer starts anything.
 */
interface StartupEntry {
    /** The command the entry runs, or null when there is none or it has been switched off. */
    fun read(): String?

    fun write(command: String)

    fun remove()
}

/**
 * Starting Noctorium with the computer.
 *
 * Only an installed copy can: the entry has to name a program that will still be there next time, and a
 * copy run from its source through Gradle has no such program. [launcher] is the installed one --
 * jpackage says where it is in `jpackage.app-path` -- and null otherwise, which makes this unavailable.
 */
class LaunchAtLogin(
    private val entry: StartupEntry?,
    private val launcher: String?,
) {
    val available: Boolean get() = entry != null && launcher != null

    fun mode(): StartupMode {
        if (!available) return StartupMode.OFF
        val command = runCatching { entry?.read() }.getOrNull() ?: return StartupMode.OFF
        return if (TRAY_ARGUMENT in command.split(' ')) StartupMode.TRAY else StartupMode.WINDOW
    }

    /** Makes it so. False when it could not be, which the settings page says rather than pretending. */
    fun set(mode: StartupMode): Boolean {
        val target = entry ?: return false
        val program = launcher ?: return false
        return runCatching {
            when (mode) {
                StartupMode.OFF -> target.remove()
                StartupMode.WINDOW -> target.write(command(program, tray = false))
                StartupMode.TRAY -> target.write(command(program, tray = true))
            }
        }.isSuccess && mode() == mode
    }

    companion object {
        /** What the entry passes to start Noctorium in the tray rather than in a window. */
        const val TRAY_ARGUMENT = "--tray"

        /** The command line an entry runs, quoted so a program under "Program Files" is one argument. */
        fun command(program: String, tray: Boolean): String =
            "\"$program\"" + if (tray) " $TRAY_ARGUMENT" else ""

        /** This machine's, worked out once. */
        val system: LaunchAtLogin by lazy {
            // An AppImage runs from a mount that is gone after it closes; the file itself is what to start.
            val launcher = System.getenv("APPIMAGE")?.takeIf(String::isNotBlank)
                ?: System.getProperty("jpackage.app-path")?.takeIf(String::isNotBlank)
            val os = System.getProperty("os.name").orEmpty()
            val entry = when {
                // A Flatpak's ~/.config is its own, and an autostart entry written there starts nothing.
                !System.getenv("FLATPAK_ID").isNullOrBlank() -> null
                os.startsWith("Windows", ignoreCase = true) -> WindowsRunEntry()
                os.startsWith("Linux", ignoreCase = true) -> XdgAutostartEntry.forThisUser()
                else -> null
            }
            LaunchAtLogin(entry, launcher)
        }
    }
}

/**
 * The `Run` key in the listener's own part of the registry, which is where Windows looks at sign-in.
 *
 * Per user, so it needs no administrator and never starts Noctorium for somebody else on the machine.
 * Written through the registry API rather than `reg.exe`, which would need a path with spaces in it
 * quoted inside a quoted argument -- the one thing command lines on Windows do least predictably.
 *
 * Task Manager does not delete an entry it disables: it marks it off under `StartupApproved`, and the
 * entry stays. That mark is read, so an entry switched off there reads as off here, and cleared when the
 * listener switches it on again here, since that is them saying they want it after all.
 */
internal class WindowsRunEntry(
    private val key: String = RUN_KEY,
    private val approvedKey: String = APPROVED_KEY,
    private val name: String = VALUE_NAME,
) : StartupEntry {
    override fun read(): String? {
        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, key, name)) return null
        if (switchedOff()) return null
        return Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, key, name)
    }

    override fun write(command: String) {
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, key, name, command)
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, approvedKey, name)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, approvedKey, name)
        }
    }

    override fun remove() {
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, key, name)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, key, name)
        }
    }

    /** The first byte of Task Manager's mark is even for on and odd for off. */
    private fun switchedOff(): Boolean = runCatching {
        if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, approvedKey)) return false
        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, approvedKey, name)) return false
        val mark = Advapi32Util.registryGetBinaryValue(WinReg.HKEY_CURRENT_USER, approvedKey, name)
        mark.isNotEmpty() && (mark[0].toInt() and 1) == 1
    }.getOrDefault(false)

    companion object {
        const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        const val APPROVED_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\StartupApproved\\Run"
        const val VALUE_NAME = "Noctorium"
    }
}

/**
 * A `.desktop` file in the XDG autostart folder, which GNOME, KDE and the rest all read at sign-in.
 *
 * A desktop's own Startup Applications switches an entry off by writing `Hidden=true` or GNOME's
 * `X-GNOME-Autostart-enabled=false` into it rather than deleting it, so either reads as off.
 */
class XdgAutostartEntry(private val folder: Path) : StartupEntry {
    private val file: Path get() = folder.resolve(FILE_NAME)

    override fun read(): String? {
        if (!Files.isRegularFile(file)) return null
        val lines = Files.readAllLines(file)
        val switchedOff = lines.any {
            it.trim().equals("Hidden=true", ignoreCase = true) ||
                it.trim().equals("X-GNOME-Autostart-enabled=false", ignoreCase = true)
        }
        if (switchedOff) return null
        return lines.firstOrNull { it.startsWith("Exec=") }?.removePrefix("Exec=")?.trim()
    }

    override fun write(command: String) {
        Files.createDirectories(folder)
        Files.writeString(
            file,
            """
                [Desktop Entry]
                Type=Application
                Name=Noctorium
                Comment=One music player for YouTube Music and SoundCloud
                Exec=$command
                Terminal=false
                X-GNOME-Autostart-enabled=true
            """.trimIndent() + "\n",
        )
    }

    override fun remove() {
        Files.deleteIfExists(file)
    }

    companion object {
        const val FILE_NAME = "noctorium.desktop"

        fun forThisUser(): XdgAutostartEntry? {
            val config = System.getenv("XDG_CONFIG_HOME")?.takeIf(String::isNotBlank)?.let(Path::of)
                ?: System.getProperty("user.home")?.takeIf(String::isNotBlank)?.let { Path.of(it, ".config") }
                ?: return null
            return XdgAutostartEntry(config.resolve("autostart"))
        }
    }
}
