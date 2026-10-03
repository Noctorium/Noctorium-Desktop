package app.noctorium.platform

import app.noctorium.auth.CefRequester
import app.noctorium.auth.CefRuntime
import app.noctorium.playback.BackendLocator
import app.noctorium.settings.AppDirectories
import app.noctorium.settings.SecureCredentialStore
import app.noctorium.settings.SettingsRepository
import app.noctorium.update.DesktopUpdateInstaller
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * `Noctorium --self-test`: whether this installed copy can do the things that differ from one machine to the
 * next, said line by line, with an exit status a script can read.
 *
 * Written for the Macs the release is built on, where nobody can sit in front of the application and press
 * play -- but it asks the same questions anywhere. Each is one that an installer can get wrong without any
 * unit test noticing: the bundled mpv and yt-dlp have to be found and actually start (a bundle that lost its
 * executable bits, or code macOS refuses to run, fails here and nowhere else); the credential store has to
 * keep a secret and give it back; and embedded Chromium has to start and fetch a page, which on a Mac means
 * the whole of its helper machinery working inside the application bundle.
 *
 * It touches nothing of the listener's: the secret it stores is its own and is deleted again, and the page
 * Chromium fetches is a public one.
 */
object SelfTest {
    const val ARGUMENT = "--self-test"

    /**
     * `Noctorium --start-chromium`: an ordinary start, with embedded Chromium started in the background as a
     * sign-in would start it. For the release's check that quitting with Chromium running ends cleanly.
     */
    const val START_CHROMIUM = "--start-chromium"

    fun startChromium() {
        Thread({
            runCatching {
                val folder = chromiumFolder() ?: return@runCatching
                runBlocking { withTimeout(120_000) { CefRequester(folder).send("GET", PUBLIC_READ, emptyMap(), null) } }
            }
        }, "noctorium-start-chromium").apply { isDaemon = true }.start()
    }

    private fun chromiumFolder(): Path? = SettingsRepository.defaultSettingsPath()?.parent?.resolve("chromium")

    private var failed = false

    fun run(): Nothing {
        say(
            "Noctorium ${DesktopUpdateInstaller.readVersion() ?: "(no version)"} self-test on " +
                "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}, " +
                "Java ${System.getProperty("java.version")}",
        )
        check("data folder") { AppDirectories.base()?.toString() ?: error("there is nowhere to keep anything") }
        check("installed as") { DesktopUpdateInstaller().channel.name }
        check("mpv") { version(BackendLocator.mpv() ?: error("not found")) }
        check("yt-dlp") { version(BackendLocator.ytDlp() ?: error("not found")) }
        check("credential store") {
            val store = SecureCredentialStore()
            val key = "self-test"
            val value = "noctorium-${System.nanoTime()}"
            store.put(key, value)
            val back = store.get(key)
            store.remove(key)
            check(back == value) { "stored a secret and got back ${if (back == null) "nothing" else "something else"}" }
            if (store.persistent) "kept by the system" else "kept for this session only"
        }
        check("chromium") {
            val folder = chromiumFolder() ?: error("nowhere to unpack it")
            val requester = CefRequester(folder)
            val reply = runBlocking { withTimeout(120_000) { requester.send("GET", PUBLIC_READ, emptyMap(), null) } }
                ?: error("started, but the page never answered")
            // Any answer is the browser working: SoundCloud judging the request is past everything that can go
            // wrong on the machine.
            "answered HTTP ${reply.status}"
        }
        // Chromium is shut down, and waited for, before the process ends: ending it while Chromium was still
        // closing its browser crashed it on the way out on a Mac, after every check above had passed. The
        // browser goes with it.
        CefRuntime.shutdown(timeoutMillis = 15_000)
        say(if (failed) "SELFTEST FAILED" else "SELFTEST PASSED")
        System.out.flush()
        // Whatever else is still running -- the hidden window Chromium was given -- is nothing to wait for.
        Runtime.getRuntime().halt(if (failed) 1 else 0)
        error("unreachable")
    }

    private fun check(name: String, block: () -> String) {
        val answer = runCatching(block)
        answer.onSuccess { say("ok    $name: $it") }
        answer.onFailure {
            failed = true
            say("FAIL  $name: ${it.message ?: it::class.simpleName}")
        }
    }

    /** The first line a tool prints about itself, which is also the proof that it runs at all. */
    private fun version(program: Path): String {
        val process = ProcessBuilder(program.toString(), "--version").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("$program did not answer --version within a minute")
        }
        check(process.exitValue() == 0) { "$program exited with ${process.exitValue()}: ${output.take(200)}" }
        val where = if (BackendLocator.isBundled(program)) "built in" else program.toString()
        return "${output.lineSequence().firstOrNull().orEmpty().trim()} ($where)"
    }

    private fun say(line: String) = println(line)

    private const val PUBLIC_READ = "https://api-v2.soundcloud.com/resolve" +
        "?url=https%3A%2F%2Fsoundcloud.com%2Ftooore%2Fburial-forgive" +
        "&client_id=TtgWpck7e9mB8S4rdtWWX0pfVluhEPvy"
}
