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
            // On, because the request browser is never drawn and cannot be created at all without it.
            // This only makes off-screen rendering available; the sign-in window is still a windowed
            // browser and is drawn the same way. CEF warns that switching it on can cost some rendering
            // performance on certain systems, which is the price of the only client SoundCloud will take
            // a write from.
            builder.cefSettings.windowless_rendering_enabled = true
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
