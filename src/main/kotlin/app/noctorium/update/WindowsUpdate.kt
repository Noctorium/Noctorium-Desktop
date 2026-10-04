package app.noctorium.update

import java.util.Base64

/**
 * The few lines that carry a Windows update through after Noctorium has gone.
 *
 * Something has to outlive this process: Windows Installer cannot replace the files a running Noctorium is
 * holding open, and nothing would open the new one afterwards. So a hidden PowerShell is started with a
 * script that waits for Noctorium to be gone, ends whatever it left running out of the install folder (the
 * mpv it was playing through), runs msiexec on the msi, and opens Noctorium again -- the new one, or the old
 * one if the install did not happen, which is better than leaving nothing open.
 *
 * msiexec is run with a progress bar and nothing to click (`/passive`), since saying yes in Noctorium is the
 * decision and the UAC prompt is still there to refuse it at. `MSIFASTINSTALL=7` skips the System Restore
 * point and everything but file costing, which is most of an msi's own overhead. The install folder is passed
 * on rather than left to the msi, which does not remember one chosen at the first install and would
 * otherwise move Noctorium back to Program Files.
 *
 * Built here as text, apart from the process that runs it, so what it says can be tested anywhere -- which
 * is also why the paths are Windows paths held as strings: a Linux [java.nio.file.Path] cannot find the
 * folder of `C:\Program Files\Noctorium\Noctorium.exe`.
 */
internal object WindowsUpdate {

    /** Windows Installer's codes for done, and done but asking for a restart nobody has to take now. */
    private val SUCCESS = listOf(0, 3010)

    fun script(msi: String, launcher: String, outlive: List<Long>): String {
        val folder = launcher.substringBeforeLast('\\')
        val arguments = "/i \"$msi\" /passive /norestart MSIFASTINSTALL=7 INSTALLDIR=\"$folder\""
        return """
            |${'$'}ErrorActionPreference = 'SilentlyContinue'
            |foreach (${'$'}id in @(${outlive.joinToString(", ")})) {
            |    ${'$'}process = Get-Process -Id ${'$'}id
            |    if (${'$'}process) { ${'$'}null = ${'$'}process.WaitForExit(60000) }
            |}
            |${'$'}folder = ${quoted(folder)}
            |Get-Process | Where-Object { ${'$'}_.Path -and ${'$'}_.Path.StartsWith(${'$'}folder + '\', [StringComparison]::OrdinalIgnoreCase) } | Stop-Process -Force
            |${'$'}installer = Start-Process -FilePath "${'$'}env:SystemRoot\System32\msiexec.exe" -ArgumentList ${quoted(arguments)} -PassThru
            |${'$'}null = ${'$'}installer.Handle
            |${'$'}installer.WaitForExit()
            |if (@(${SUCCESS.joinToString(", ")}) -contains ${'$'}installer.ExitCode) { Remove-Item -LiteralPath ${quoted(msi)} -Force }
            |${'$'}launcher = ${quoted(launcher)}
            |if (Test-Path -LiteralPath ${'$'}launcher) { Start-Process -FilePath ${'$'}launcher -WorkingDirectory ${'$'}folder }
        """.trimMargin()
    }

    /**
     * The script as `-EncodedCommand` wants it: UTF-16LE, then Base64.
     *
     * Passed this way rather than as `-Command` text because nothing then stands between the script and
     * PowerShell that could re-quote it -- not Java's rules for building a Windows command line, and not
     * cmd's -- and a user folder with a space or an accent in its name arrives exactly as written.
     */
    fun encoded(script: String): String = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))

    /**
     * A PowerShell string that means exactly [text].
     *
     * Single-quoted, so nothing in it is expanded, which leaves the quote itself as the one character to
     * double. PowerShell also closes a single-quoted string at a typographic quote, and a folder can be named
     * with one of those, so they are doubled as well.
     */
    fun quoted(text: String): String =
        "'" + text.replace(Regex("['‘’‚‛]")) { it.value + it.value } + "'"
}
