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
            builder.setProgressHandler { state, percent ->
                // States read like INSTALL / EXTRACTING / INITIALIZING; DOWNLOADING should never appear now
                // that the natives are bundled, and if it does the platform artifact is missing from the build.
                val stage = state.name.lowercase().replace('_', ' ')
                onProgress(if (percent >= 0f) "$stage ${percent.toInt()}%" else stage)
            }
            builder.build().also { app = it }
        }
    }
}
