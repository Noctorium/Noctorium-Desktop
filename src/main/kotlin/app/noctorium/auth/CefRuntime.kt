package app.noctorium.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import java.io.File
import java.nio.file.Path

/**
 * The one embedded Chromium this application runs, built once and shared.
 *
 * `CefApp` is process-wide whether anybody wants it to be or not: its settings are read at start-up and a
 * second one cannot be built with different ones. Two callers need it now -- the sign-in window, and the
 * browser that makes SoundCloud's writes -- and they need the same one, because what makes the second work
 * at all is the cookie store the first filled in.
 */
internal object CefRuntime {
    private val building = Mutex()

    @Volatile
    private var app: CefApp? = null

    /**
     * Chromium, unpacked if this is the first use.
     *
     * [onProgress] reports the unpacking, which is not instant the first time and is silent afterwards.
     */
    suspend fun app(installDir: Path, onProgress: (String) -> Unit = {}): CefApp = building.withLock {
        app?.let { return@withLock it }
        withContext(Dispatchers.IO) {
            val builder = CefAppBuilder()
            builder.setInstallDir(File(installDir.toString()))
            // Off, and it has to stay off. Switching it on is the obvious way to give the request browser
            // somewhere to render, and CEF's own note that it "may reduce rendering performance on some
            // systems" turned out to mean the sign-in page -- the one somebody actually looks at and types
            // into -- going sluggish. The request browser is a windowed one in a window nobody sees
            // instead; see [CefRequester].
            builder.cefSettings.windowless_rendering_enabled = false
            builder.cefSettings.cache_path = installDir.resolve("cache").toString()
            // Chromium's own sandbox is built on the kind of namespace a Flatpak does not allow inside its
            // own, and without this the sign-in page never opens there. The Flatpak is the sandbox.
            if (!System.getenv("FLATPAK_ID").isNullOrBlank()) builder.addJcefArgs("--no-sandbox", "--no-zygote")
            builder.setProgressHandler { state, percent ->
                // States read like INSTALL / EXTRACTING / INITIALIZING; DOWNLOADING should never appear now
                // that the natives are bundled, and if it does the platform artifact is missing from the build.
                val stage = state.name.lowercase().replace('_', ' ')
                onProgress(if (percent >= 0f) "$stage ${percent.toInt()}%" else stage)
            }
            builder.build().also { app = it }
        }
    }

    /**
     * Shuts Chromium down and waits until it says it has, up to [timeoutMillis].
     *
     * A process that simply ends while Chromium is still closing a browser can be taken down by Chromium on
     * the way out -- on a Mac it was, inside its font code, after everything it had been asked to do had
     * succeeded. Not for the interface thread: on a Mac, Chromium finishes shutting down on that thread, so
     * waiting there would wait for itself.
     */
    fun shutdown(timeoutMillis: Long) {
        val running = app ?: return
        app = null
        runCatching { running.dispose() }
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (CefApp.getState() != CefApp.CefAppState.TERMINATED && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
    }
}
