package app.noctorium.update

import app.noctorium.auth.CefRuntime
import app.noctorium.platform.MacBundle
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * What a desktop Noctorium can do about a new version of itself.
 *
 * Nothing here replaces the running application. Windows hands the msi to Windows Installer and quits so its
 * own files stop being locked; Linux hands the package to the package manager through a
 * graphical privilege prompt, because /opt belongs to the package manager and a program that writes there
 * behind its back leaves a machine whose package database is a lie.
 *
 * The important case is neither: a copy running from an unzipped folder, or straight out of Gradle, was
 * never installed by anything. Running an installer over it would leave the old folder exactly where it is
 * and put a second copy somewhere else, so those are told and sent to the release page instead.
 */
class DesktopUpdateInstaller(
    override val currentVersion: Version? = readVersion(),
) : UpdateInstaller {

    override val channel: UpdateChannel by lazy { detectChannel() }

    override fun downloadDirectory(): Path =
        Path.of(System.getProperty("java.io.tmpdir"), "noctorium-update")

    override suspend fun install(file: Path): String? = when (channel) {
        UpdateChannel.WINDOWS_INSTALLER -> runWindowsInstaller(file)
        UpdateChannel.DEBIAN_PACKAGE -> runLinuxPackage(file, listOf("dpkg", "--install", file.toString()))
        UpdateChannel.FEDORA_PACKAGE -> runLinuxPackage(file, listOf("rpm", "--upgrade", "--force", file.toString()))
        UpdateChannel.ARCH_PACKAGE -> runLinuxPackage(file, listOf("pacman", "--upgrade", "--noconfirm", file.toString()))
        UpdateChannel.APPIMAGE -> replaceAppImage(file)
        UpdateChannel.MAC_DMG -> replaceMacApp(file)
        else -> "This copy of Noctorium was not installed by an installer, so it cannot update itself."
    }

    /**
     * Installs the msi with nothing to click, and opens the new Noctorium when it is done.
     *
     * The installer cannot replace files this process is holding open, so the work is handed to a hidden
     * PowerShell that outlives it (see [WindowsUpdate]) and Noctorium quits. What is left on screen is the
     * UAC prompt, which is the last chance to say no, and then a progress bar -- not the whole wizard again,
     * after somebody has already said yes in Noctorium.
     *
     * Should PowerShell not start, the msi is opened the way it always was, wizard and all, so an update is
     * never stuck behind the faster way of doing it.
     */
    private fun runWindowsInstaller(file: Path): String? {
        val launcher = System.getProperty("jpackage.app-path")?.takeIf(String::isNotBlank)
            ?: return "Could not tell where Noctorium is installed. The update is at $file."
        val windows = System.getenv("SystemRoot")?.takeIf(String::isNotBlank) ?: "C:\\Windows"
        val script = WindowsUpdate.script(file.toString(), launcher, outlive = processesToOutlive(launcher))
        val unattended = runCatching {
            ProcessBuilder(
                "$windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
                "-EncodedCommand", WindowsUpdate.encoded(script),
            ).start()
        }
        if (unattended.isSuccess) leave()
        return runCatching {
            ProcessBuilder("$windows\\System32\\msiexec.exe", "/i", file.toString()).start()
            // Long enough for the installer to be on screen before this window disappears, so it does not
            // look as though Noctorium closed for no reason.
            Thread.sleep(1_200)
            leave()
            @Suppress("UNREACHABLE_CODE") null
        }.getOrElse { "Could not start the installer: ${it.message}" }
    }

    /**
     * This process, and the launcher above it when there is one.
     *
     * jpackage's launcher sometimes starts the JVM as a second process and waits for it, and that launcher
     * is Noctorium.exe in the install folder -- a file the installer has to replace -- so it is waited for as
     * well as the JVM itself.
     */
    private fun processesToOutlive(launcher: String): List<Long> {
        val self = ProcessHandle.current()
        val parent = self.parent().orElse(null)
            ?.takeIf { it.info().command().orElse("").equals(launcher, ignoreCase = true) }
        return listOfNotNull(self.pid(), parent?.pid())
    }

    /**
     * Asks the package manager, through whatever this desktop uses to ask for a password.
     *
     * pkexec is the graphical front for a privileged command on every desktop that ships polkit, which is
     * all of the ones these packages target. Without it there is no way to install a package that does not
     * involve a terminal, so the file is left where it is and the command is handed over instead -- which
     * is more use than a failure nobody can act on.
     */
    private fun runLinuxPackage(file: Path, command: List<String>): String? {
        if (which("pkexec") == null) {
            return "Downloaded to $file. Install it with: sudo ${command.joinToString(" ")}"
        }
        return runCatching {
            val process = ProcessBuilder(listOf("pkexec") + command).start()
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES)) {
                process.destroyForcibly()
                return "The installer did not finish. The package is at $file."
            }
            when (process.exitValue()) {
                0 -> {
                    leave()
                    @Suppress("UNREACHABLE_CODE") null
                }
                // polkit answers 126 when the prompt is dismissed, which is a decision rather than a fault.
                126, 127 -> "Update cancelled. The package is at $file."
                else -> "The package manager refused it. The package is at $file."
            }
        }.getOrElse { "Could not run the package manager: ${it.message}. The package is at $file." }
    }

    /**
     * How this copy got here.
     *
     * `jpackage.app-path` is set by the launcher jpackage builds, so its absence means this is not a
     * packaged build at all -- a Gradle run, or a jar. Its presence is not enough on its own, because the
     * same launcher goes into the app image that gets zipped up; what that means per platform is decided
     * below.
     */
    /**
     * An AppImage is one file, and updating it is putting the new one where the old one was.
     *
     * Written beside it and moved over it in one step, so a failure halfway leaves the old one working;
     * then the new one is started and this one leaves, which is the restart an update needs anyway.
     */
    private fun replaceAppImage(file: Path): String? {
        val current = System.getenv("APPIMAGE")?.takeIf(String::isNotBlank)?.let(Path::of)
            ?: return "Could not tell where this AppImage is. The new one is at $file."
        return runCatching {
            val incoming = current.resolveSibling(".${current.fileName}.new")
            Files.copy(file, incoming, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            incoming.toFile().setExecutable(true, false)
            Files.move(incoming, current, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            ProcessBuilder(current.toString()).start()
            Thread.sleep(800)
            leave()
            @Suppress("UNREACHABLE_CODE") null
        }.getOrElse { "Could not replace ${current.fileName}: ${it.message}. The new one is at $file." }
    }

    /**
     * On a Mac, the application is one folder, and updating it is putting the new folder where the old one is.
     *
     * The disk image is opened out of sight, its Noctorium.app copied beside the running one with ditto (which
     * keeps everything a bundle relies on), and the two swapped by renaming -- which macOS allows while the old
     * one is running -- so that at every moment there is a whole application in place. The one replaced is
     * left under a hidden name until the next start clears it, because this process is still running out of
     * it. Then the new copy is started, a moment after this one has gone so it does not find this one still
     * holding the single-instance claim and hand itself back to it.
     *
     * Where the folder it sits in cannot be written -- an account that is not an administrator, with
     * Noctorium in /Applications -- the disk image is opened in the Finder instead, for dragging across.
     */
    private fun replaceMacApp(file: Path): String? {
        val bundle = MacBundle.current() ?: return "Could not tell where this copy of Noctorium is. The update is at $file."
        val folder = bundle.parent ?: return "Could not tell where this copy of Noctorium is. The update is at $file."
        if (!Files.isWritable(folder)) {
            runCatching { ProcessBuilder("/usr/bin/open", file.toString()).start() }
            return "Drag Noctorium from the window that opened into $folder to finish updating."
        }
        val mount = runCatching { Files.createTempDirectory("noctorium-update") }.getOrNull()
            ?: return "Could not open the update. It is at $file."
        if (!quietly("/usr/bin/hdiutil", "attach", "-nobrowse", "-readonly", "-noautoopen", "-mountpoint", mount.toString(), file.toString())) {
            return "Could not open the update. It is at $file."
        }
        return try {
            val incoming = mount.resolve("Noctorium.app")
            if (!Files.isDirectory(incoming)) return "The update holds no Noctorium.app. It is at $file."
            val staged = folder.resolve(".Noctorium.app.new")
            MacBundle.deleteRecursively(staged)
            if (!quietly("/usr/bin/ditto", incoming.toString(), staged.toString())) {
                MacBundle.deleteRecursively(staged)
                return "Could not copy the update into $folder. It is at $file."
            }
            quietly("/usr/bin/xattr", "-dr", "com.apple.quarantine", staged.toString())
            val previous = MacBundle.previous(bundle)
            MacBundle.deleteRecursively(previous)
            Files.move(bundle, previous)
            try {
                Files.move(staged, bundle)
            } catch (failure: Exception) {
                Files.move(previous, bundle)
                throw failure
            }
            ProcessBuilder("/bin/sh", "-c", "sleep 2; /usr/bin/open -n \"$0\"", bundle.toString()).start()
            leave()
            @Suppress("UNREACHABLE_CODE") null
        } catch (failure: Exception) {
            "Could not replace Noctorium: ${failure.message}. The update is at $file."
        } finally {
            if (!quietly("/usr/bin/hdiutil", "detach", mount.toString())) quietly("/usr/bin/hdiutil", "detach", "-force", mount.toString())
            runCatching { Files.deleteIfExists(mount) }
        }
    }

    /**
     * Ends this copy so the new one can take its place -- with Chromium shut down first, if a sign-in started
     * it, since ending the process while it was still running could crash it on the way out.
     */
    private fun leave(): Nothing {
        CefRuntime.shutdown(timeoutMillis = 5_000)
        exitProcess(0)
    }

    /** Runs a system tool to the end and says whether it worked, keeping its output out of the way. */
    private fun quietly(vararg command: String): Boolean = runCatching {
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        process.inputStream.readAllBytes()
        if (!process.waitFor(5, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            return false
        }
        process.exitValue() == 0
    }.getOrDefault(false)

    private fun detectChannel(): UpdateChannel {
        // The two that say so themselves, before anything else is asked: inside either, the launcher's
        // path is the sandbox's or a temporary mount's and no package manager knows it.
        if (!System.getenv("FLATPAK_ID").isNullOrBlank()) return UpdateChannel.FLATPAK
        if (!System.getenv("APPIMAGE").isNullOrBlank()) return UpdateChannel.APPIMAGE
        val launcher = System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }
            ?: return UpdateChannel.UNMANAGED

        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.startsWith("windows")) return windowsChannel(launcher)
        if (os.startsWith("mac")) return macChannel(launcher)
        if (!os.startsWith("linux")) return UpdateChannel.UNMANAGED

        // Which package manager put it there, asked of the package manager rather than guessed from the
        // distribution's name: plenty of machines have both, and only one of them owns this file.
        if (owns(listOf("dpkg", "--search", launcher))) return UpdateChannel.DEBIAN_PACKAGE
        if (owns(listOf("rpm", "--query", "--file", launcher))) return UpdateChannel.FEDORA_PACKAGE
        if (owns(listOf("pacman", "--query", "--owns", launcher))) return UpdateChannel.ARCH_PACKAGE
        return UpdateChannel.UNMANAGED
    }

    /**
     * On Windows, where it is sitting decides whether it was installed.
     *
     * jpackage sets jpackage.app-path for its own launcher, and it builds the same launcher for an
     * installed application and for the app image that gets zipped up -- so the property says the
     * application was built by jpackage, not that anything installed it. Somebody running the unzipped
     * folder would otherwise be handed an installer, which would put a second copy in Program Files and
     * leave the folder they are actually running exactly as it was.
     *
     * So the location is what is checked, and an unfamiliar one is treated as unmanaged. That is the safe
     * way round: the worst case is being told to update by hand, where the other way is two copies.
     */
    private fun windowsChannel(launcher: String): UpdateChannel {
        val installedUnder = listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs" },
        )
        val where = launcher.lowercase()
        return if (installedUnder.any { where.startsWith(it.lowercase()) }) {
            UpdateChannel.WINDOWS_INSTALLER
        } else {
            UpdateChannel.UNMANAGED
        }
    }

    /**
     * On a Mac, any bundle can be replaced except one that is not where it will stay: still inside the disk
     * image it came in, or run from the read-only copy macOS makes of a quarantined application it has not
     * been allowed to settle -- App Translocation, under /private/var/folders. Replacing either would update
     * something that disappears, so those are told to move Noctorium into Applications first.
     */
    private fun macChannel(launcher: String): UpdateChannel {
        val bundle = MacBundle.bundleOf(launcher)?.toString() ?: return UpdateChannel.UNMANAGED
        if (bundle.startsWith("/Volumes/") || "/AppTranslocation/" in bundle) return UpdateChannel.UNMANAGED
        return UpdateChannel.MAC_DMG
    }

    private fun owns(command: List<String>): Boolean = runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return false
        }
        process.exitValue() == 0
    }.getOrDefault(false)

    private fun which(tool: String): Path? =
        System.getenv("PATH")?.split(java.io.File.pathSeparator)
            ?.map { Path.of(it, tool) }
            ?.firstOrNull { Files.isExecutable(it) }

    companion object {
        /**
         * The version written in at build time.
         *
         * A jar manifest would do for a jar, and jpackage does not build one -- it builds a runtime image,
         * and the manifest is not what ends up next to the launcher. So the build writes a properties file
         * into the resources instead, from the same value the installers are named after.
         */
        fun readVersion(): Version? = runCatching {
            DesktopUpdateInstaller::class.java.getResourceAsStream("/noctorium-version.properties")?.use { stream ->
                val properties = java.util.Properties().apply { load(stream) }
                Version.parse(properties.getProperty("version"))
            }
        }.getOrNull()
    }
}
