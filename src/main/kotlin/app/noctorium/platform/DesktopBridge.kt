package app.noctorium.platform

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * What a desktop can do that a phone cannot, and the other way round.
 *
 * All four answers here are awt or a Windows command, and none of them exists on Android — which is the
 * entire reason [SystemBridge] is an interface rather than a utility object.
 */
class DesktopBridge : SystemBridge {

    /**
     * Opens [url] in the browser, and throws when nothing did, so that whoever asked can say so and offer the link.
     *
     * On Linux the desktop's own openers are asked first, `xdg-open` before the rest (see [LinuxLinks]): Java's way
     * goes through GNOME's libraries, and under KDE, Wayland or the Flatpak it so often opened nothing -- signing in
     * to Last.fm opened no browser at all. Java's is still tried after them, and is the way on Windows and the Mac.
     */
    override fun openUrl(url: String) {
        require(url.startsWith("https://")) { "Only secure links can be opened" }
        browse(url)
    }

    /**
     * Opens a page Noctorium serves from this computer itself -- a stream widget -- in the browser, and throws when
     * nothing did.
     *
     * [openUrl] takes secure links only, and rightly, since everything it opens is somewhere on the web. A page served
     * from this computer is never https, and is not on the web either, so it comes here instead -- and only an address
     * on 127.0.0.1 does, so that this is no way round that rule for anywhere else.
     */
    fun openOwnPage(url: String) {
        require(url.startsWith("http://127.0.0.1:")) { "Only Noctorium's own pages on this computer open this way" }
        browse(url)
    }

    private fun browse(url: String) {
        val os = System.getProperty("os.name").orEmpty()
        val linux = !os.startsWith("Windows", ignoreCase = true) && !os.startsWith("Mac", ignoreCase = true)
        if (linux && LinuxLinks.open(url)) return
        val desktop = Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        check(desktop) { "No browser opened on this computer. Copy the link and open it yourself." }
        Desktop.getDesktop().browse(URI(url))
    }

    override fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    /**
     * Selects the file in Explorer rather than merely opening the folder it sits in.
     *
     * A folder with four hundred things in it, opened at the top, has not shown anybody the file they just
     * saved. Explorer, or the Finder on a Mac, is asked directly because awt has no way to express "and highlight
     * this one"; where that is not available — Linux, or Explorer missing — opening the folder is the fallback.
     */
    override fun revealFile(path: Path) {
        val os = System.getProperty("os.name").orEmpty()
        val opened = runCatching {
            when {
                os.startsWith("Windows", ignoreCase = true) ->
                    ProcessBuilder("explorer.exe", "/select,${path.toAbsolutePath()}").start()
                // The Finder does the same with open -R.
                os.startsWith("Mac", ignoreCase = true) ->
                    ProcessBuilder("/usr/bin/open", "-R", path.toAbsolutePath().toString()).start()
                else -> null
            } != null
        }.getOrDefault(false)
        if (opened) return
        runCatching {
            val folder = path.parent ?: return
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(folder.toFile())
        }
    }

    /**
     * The desktop, which is where somebody looks for a file they have just saved.
     *
     * Falls back to the home directory on a system that has no Desktop folder, and to null when even that
     * cannot be found — the caller treats null as "ask them to choose one".
     */
    override fun defaultExportFolder(): Path? {
        val home = System.getProperty("user.home")?.takeIf(String::isNotBlank)?.let(Path::of) ?: return null
        return home.resolve("Desktop").takeIf(Files::isDirectory) ?: home.takeIf(Files::isDirectory)
    }
}
