package app.noctorium.platform

import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Starting with the computer, and starting only once.
 *
 * Nothing here touches this machine's own startup list: the Windows entry is the registry, and writing to
 * the listener's registry is no business of a test. What is checked is everything around it -- the command
 * each choice writes, how one is read back, and the Linux entry, which is a file and can live in a folder
 * of the test's own.
 */
class StartupTest {
    private class MemoryEntry(var command: String? = null) : StartupEntry {
        override fun read(): String? = command
        override fun write(command: String) { this.command = command }
        override fun remove() { command = null }
    }

    @Test
    fun `each choice writes the command it reads back as`() {
        val entry = MemoryEntry()
        val startup = LaunchAtLogin(entry, launcher = """C:\Program Files\Noctorium\Noctorium.exe""")

        assertEquals(StartupMode.OFF, startup.mode())
        assertTrue(startup.set(StartupMode.TRAY))
        assertEquals("\"C:\\Program Files\\Noctorium\\Noctorium.exe\" --tray", entry.command)
        assertEquals(StartupMode.TRAY, startup.mode())

        assertTrue(startup.set(StartupMode.WINDOW))
        assertEquals("\"C:\\Program Files\\Noctorium\\Noctorium.exe\"", entry.command)
        assertEquals(StartupMode.WINDOW, startup.mode())

        assertTrue(startup.set(StartupMode.OFF))
        assertNull(entry.command)
        assertEquals(StartupMode.OFF, startup.mode())
    }

    @Test
    fun `a copy with no installed launcher cannot start with the computer`() {
        val entry = MemoryEntry()
        val fromSource = LaunchAtLogin(entry, launcher = null)
        assertFalse(fromSource.available)
        assertFalse(fromSource.set(StartupMode.TRAY))
        assertNull(entry.command, "nothing is written that could not start anything")
    }

    @Test
    fun `a refused write says so rather than claiming success`() {
        val locked = object : StartupEntry {
            override fun read(): String? = null
            override fun write(command: String) = throw SecurityException("managed by policy")
            override fun remove() = Unit
        }
        assertFalse(LaunchAtLogin(locked, launcher = "/opt/noctorium/bin/Noctorium").set(StartupMode.WINDOW))
    }

    @Test
    fun `the Linux entry is an autostart file, and one switched off by the desktop reads as off`() {
        val folder = Files.createTempDirectory("noctorium-autostart")
        try {
            val entry = XdgAutostartEntry(folder.resolve("autostart"))
            val startup = LaunchAtLogin(entry, launcher = "/opt/noctorium/bin/Noctorium")

            assertTrue(startup.set(StartupMode.TRAY))
            val file = folder.resolve("autostart").resolve(XdgAutostartEntry.FILE_NAME)
            val written = Files.readString(file)
            assertTrue("Exec=\"/opt/noctorium/bin/Noctorium\" --tray" in written, written)
            assertTrue("[Desktop Entry]" in written)
            assertEquals(StartupMode.TRAY, startup.mode())

            // GNOME's Startup Applications switches an entry off by editing it, not deleting it.
            Files.writeString(file, written.replace("X-GNOME-Autostart-enabled=true", "X-GNOME-Autostart-enabled=false"))
            assertEquals(StartupMode.OFF, startup.mode())

            assertTrue(startup.set(StartupMode.OFF))
            assertFalse(Files.exists(file))
        } finally {
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a second start wakes the first instead of running beside it`() {
        val folder = Files.createTempDirectory("noctorium-instance")
        val woken = CountDownLatch(1)
        val first = SingleInstance.claim(folder, wake = true) { woken.countDown() }
        try {
            assertNotNull(first, "the first start claims the folder")
            assertNull(SingleInstance.claim(folder, wake = true) {}, "the second one does not")
            assertTrue(woken.await(5, TimeUnit.SECONDS), "and the first was asked to show itself")
        } finally {
            first?.close()
        }

        // Once the first has gone, the next start is the first again.
        val again = SingleInstance.claim(folder, wake = true) {}
        try {
            assertNotNull(again)
        } finally {
            again?.close()
            folder.toFile().deleteRecursively()
        }
    }

    @Test
    fun `a start into the tray wakes nothing`() {
        val folder = Files.createTempDirectory("noctorium-instance")
        val woken = CountDownLatch(1)
        val first = SingleInstance.claim(folder, wake = true) { woken.countDown() }
        try {
            assertNull(SingleInstance.claim(folder, wake = false) {})
            assertFalse(woken.await(1, TimeUnit.SECONDS), "nobody asked to see a window")
        } finally {
            first?.close()
            folder.toFile().deleteRecursively()
        }
    }
}
