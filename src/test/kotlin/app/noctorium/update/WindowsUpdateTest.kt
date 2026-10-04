package app.noctorium.update

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The script that installs a Windows update after Noctorium has gone, read as text.
 *
 * It only ever runs on a machine that is in the middle of updating, where a mistake in it shows up as a
 * Noctorium that closed and never came back. Checked here instead, on any machine, line by line.
 */
class WindowsUpdateTest {

    private val msi = """C:\Users\Cem Özoral\AppData\Local\Temp\noctorium-update\Noctorium-1.0.0-windows-x64.msi"""
    private val launcher = """C:\Program Files\Noctorium\Noctorium.exe"""
    private val script = WindowsUpdate.script(msi, launcher, outlive = listOf(4242, 17))

    @Test
    fun `msiexec is given the msi, nothing to click, and the folder Noctorium is already in`() {
        val expected = """/i "$msi" /passive /norestart MSIFASTINSTALL=7 INSTALLDIR="C:\Program Files\Noctorium""""
        assertTrue(WindowsUpdate.quoted(expected) in script, script)
    }

    @Test
    fun `Noctorium is waited for before anything is installed over it`() {
        val waiting = script.indexOf("foreach (\$id in @(4242, 17))")
        val installing = script.indexOf("msiexec.exe")
        assertTrue(waiting in 0 until installing, script)
    }

    @Test
    fun `whatever still runs out of the install folder is ended, and nothing else`() {
        assertTrue("\$folder = 'C:\\Program Files\\Noctorium'" in script, script)
        assertTrue("\$_.Path.StartsWith(\$folder + '\\', [StringComparison]::OrdinalIgnoreCase)" in script, script)
    }

    @Test
    fun `Noctorium is opened again from where it was, whether or not the install happened`() {
        val opening = script.lines().last()
        assertEquals(
            "if (Test-Path -LiteralPath \$launcher) { Start-Process -FilePath \$launcher -WorkingDirectory \$folder }",
            opening,
        )
        assertTrue("\$launcher = 'C:\\Program Files\\Noctorium\\Noctorium.exe'" in script, script)
    }

    @Test
    fun `the msi is cleared away only after an install that worked`() {
        assertTrue(
            "if (@(0, 3010) -contains \$installer.ExitCode) { Remove-Item -LiteralPath ${WindowsUpdate.quoted(msi)} -Force }" in script,
            script,
        )
    }

    @Test
    fun `a quote in a folder name cannot end the string it is in`() {
        assertEquals("'C:\\Users\\O''Brien'", WindowsUpdate.quoted("C:\\Users\\O'Brien"))
        // PowerShell ends a single-quoted string at a typographic quote too.
        assertEquals("'D:\\Rock \u2019\u2019n\u2019\u2019 Roll'", WindowsUpdate.quoted("D:\\Rock \u2019n\u2019 Roll"))
        // And nothing else is touched: a dollar sign means nothing between single quotes.
        assertEquals("'C:\\\$Recycle'", WindowsUpdate.quoted("C:\\\$Recycle"))
    }

    @Test
    fun `the encoded command is the script in UTF-16LE, accents and all`() {
        val decoded = String(Base64.getDecoder().decode(WindowsUpdate.encoded(script)), Charsets.UTF_16LE)
        assertEquals(script, decoded)
        assertTrue("Cem Özoral" in decoded)
    }
}
