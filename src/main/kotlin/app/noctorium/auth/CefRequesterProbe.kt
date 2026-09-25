package app.noctorium.auth

import app.noctorium.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

/**
 * Runs one request through [CefRequester] and prints what came back.
 *
 * For checking by hand that embedded Chromium starts without a window on this machine, that a page can
 * answer back through the message router, and -- the part worth knowing -- that a write reaches SoundCloud
 * at all. A write made by an ordinary client is answered 403 with a captcha whatever it carries. One made
 * here with a token SoundCloud does not accept should be answered 401, because 401 means the request got
 * far enough to be judged on its session rather than on what made it.
 *
 * It needs no account and changes nothing: the only write it makes is one that cannot succeed.
 */
object CefRequesterProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val installDir = SettingsRepository.defaultSettingsPath()?.parent?.resolve("chromium")
            ?: error("nowhere to put the browser on this system")
        val requester = CefRequester(installDir)
        val method = args.getOrNull(0) ?: "GET"
        val url = args.getOrNull(1) ?: PUBLIC_READ
        val token = args.getOrNull(2)

        runBlocking {
            val headers = token?.let { mapOf("Authorization" to "OAuth $it") }.orEmpty()
            val reply = requester.send(method, url, headers, null)
            println("PROBE $method status=${reply?.status} body=${reply?.body?.take(160)}")
        }
        requester.close()
        // Chromium keeps non-daemon threads of its own; nothing else here is waiting on them.
        Runtime.getRuntime().halt(0)
    }

    private const val PUBLIC_READ = "https://api-v2.soundcloud.com/resolve" +
        "?url=https%3A%2F%2Fsoundcloud.com%2Ftooore%2Fburial-forgive" +
        "&client_id=TtgWpck7e9mB8S4rdtWWX0pfVluhEPvy"
}
