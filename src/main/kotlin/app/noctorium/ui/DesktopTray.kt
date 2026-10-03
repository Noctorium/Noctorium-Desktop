package app.noctorium.ui

import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BaseMultiResolutionImage

/**
 * Noctorium's icon in the system tray, for while the window is closed and the music is not.
 *
 * Plain AWT rather than Compose's own `Tray`, for one thing it cannot do: open the window on a single click.
 * Compose's only answers a double click, which on Windows is the less usual gesture for a tray icon -- and
 * an icon that seems to do nothing when clicked, while the music plays on behind it, is exactly the moment
 * somebody decides the window has gone for good.
 *
 * Everything here runs on the AWT event thread, which is also the thread Compose for Desktop runs on, so it
 * is called straight from the interface and needs no hand-off.
 */
class DesktopTray(
    private val open: () -> Unit,
    private val playPause: () -> Unit,
    private val next: () -> Unit,
    private val previous: () -> Unit,
    private val quit: () -> Unit,
) : AutoCloseable {
    private val playItem = MenuItem("Play").apply { addActionListener { playPause() } }

    private val icon = TrayIcon(
        // Every size the tray might ask for, so a scaled display gets a sharp one rather than a blurred 16.
        BaseMultiResolutionImage(*listOf(16, 20, 24, 32, 40, 48, 64).map(AppIcon::image).toTypedArray()),
        "Noctorium",
        PopupMenu().apply {
            add(MenuItem("Open Noctorium").apply { addActionListener { open() } })
            addSeparator()
            add(playItem)
            add(MenuItem("Next").apply { addActionListener { next() } })
            add(MenuItem("Previous").apply { addActionListener { previous() } })
            addSeparator()
            add(MenuItem("Quit Noctorium").apply { addActionListener { quit() } })
        },
    ).apply {
        isImageAutoSize = true
        // A double click also arrives here, as two clicks; opening an open window is harmless.
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.button == MouseEvent.BUTTON1) open()
            }
        })
    }

    private var installed = false

    /** Puts the icon in the tray. False where the desktop has no tray to put it in. */
    fun install(): Boolean {
        if (installed) return true
        installed = runCatching { SystemTray.getSystemTray().add(icon) }.isSuccess
        return installed
    }

    /** What is playing, shown on hover, and the right word on the play item. */
    fun show(title: String?, artist: String?, playing: Boolean) {
        playItem.label = if (playing) "Pause" else "Play"
        // Windows cuts a tray tooltip off at 127 characters, so it is shortened here where the cut can
        // be made on purpose.
        icon.toolTip = when {
            title.isNullOrBlank() -> "Noctorium"
            artist.isNullOrBlank() -> "Noctorium\n$title"
            else -> "Noctorium\n$title · $artist"
        }.take(120)
    }

    /** A note from the tray, used once to say that closing the window did not stop the music. */
    fun note(title: String, message: String) {
        if (installed) icon.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    /**
     * The song that has just started, as a note from the tray: its title, and its artist beneath.
     *
     * Without the information symbol the note above carries, since a new song is not news about Noctorium.
     * Shortened the way the tooltip is, because the system cuts a long note off wherever it likes.
     */
    fun announce(title: String, artist: String?) {
        if (installed) icon.displayMessage(title.take(60), artist?.takeIf { it.isNotBlank() }?.take(120).orEmpty(), TrayIcon.MessageType.NONE)
    }

    override fun close() {
        if (installed) runCatching { SystemTray.getSystemTray().remove(icon) }
        installed = false
    }

    companion object {
        /**
         * Whether this desktop has a tray at all.
         *
         * Windows always does. On Linux it depends on the desktop -- GNOME shows none without an
         * extension -- and where there is none, closing into it would leave the music playing with nothing
         * on screen to bring the window back, so the close button keeps quitting.
         */
        val supported: Boolean by lazy { runCatching { SystemTray.isSupported() }.getOrDefault(false) }
    }
}
