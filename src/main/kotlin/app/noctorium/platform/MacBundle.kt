package app.noctorium.platform

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * The application bundle a Mac is running Noctorium from, and the housekeeping it needs.
 *
 * Noctorium is not signed with an Apple developer identity, so a copy downloaded in a browser arrives marked
 * as quarantined, and macOS asks once whether to open it. That answer covers the application the listener
 * opened; it says nothing about the mpv inside it, which Noctorium starts by itself and which would otherwise
 * be refused in silence the first time something is played. Once the listener has let Noctorium in, the mark
 * has done its job, so the first thing a Mac copy does is take it off its own bundle.
 *
 * An update leaves the previous bundle beside the new one under a hidden name, because the running copy is
 * still using it when it is replaced; the next start clears it away.
 */
object MacBundle {
    val isMac: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)

    /** `/Applications/Noctorium.app`, from the launcher jpackage put at `Contents/MacOS/Noctorium` inside it. */
    fun current(launcher: String? = System.getProperty("jpackage.app-path")): Path? =
        launcher?.takeIf(String::isNotBlank)?.let(::bundleOf)

    /** The `.app` folder holding [path], or null when it is not inside one. */
    fun bundleOf(path: String): Path? {
        val marker = path.indexOf(".app/")
        if (marker < 0) return if (path.endsWith(".app")) Path.of(path) else null
        return Path.of(path.substring(0, marker + ".app".length))
    }

    /** Where an update puts the copy it replaced, until the next start. */
    fun previous(bundle: Path): Path = bundle.resolveSibling(".${bundle.fileName}.old")

    /** Run once at startup, in the background: neither is worth delaying the window for. */
    fun tidy() {
        if (!isMac) return
        val bundle = current() ?: return
        Thread({
            runCatching { deleteRecursively(previous(bundle)) }
            runCatching {
                val process = ProcessBuilder("/usr/bin/xattr", "-dr", "com.apple.quarantine", bundle.toString())
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.readAllBytes()
                if (!process.waitFor(30, TimeUnit.SECONDS)) process.destroyForcibly()
            }
        }, "mac-bundle-tidy").apply { isDaemon = true }.start()
    }

    fun deleteRecursively(path: Path) {
        if (!Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return
        Files.walk(path).use { walk -> walk.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
    }
}
