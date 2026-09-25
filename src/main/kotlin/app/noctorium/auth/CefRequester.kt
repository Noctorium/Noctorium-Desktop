package app.noctorium.auth

import app.noctorium.net.BrowserReply
import app.noctorium.net.BrowserRequester
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import java.nio.file.Path
import javax.swing.JWindow
import javax.swing.SwingUtilities

/**
 * Makes a request from inside the Chromium this application already ships, on SoundCloud's origin.
 *
 * The same refusal the phone hit: SoundCloud's bot protection answers a write 403 with a captcha unless a
 * browser makes it, and no arrangement of user agent, client hints or clearance cookie gets an ordinary
 * client past it. Reads are waved through. On the desktop the browser to use is the one the sign-in window
 * runs in -- the same Chromium, the same cookie store, sharing [CefRuntime] so there is only ever one.
 *
 * The page is loaded off-screen and never drawn. That is the only difference from the sign-in window, and
 * it is why [CefRuntime] enables windowless rendering: a browser with nowhere to draw cannot otherwise be
 * created at all.
 */
class CefRequester(private val installDir: Path) : BrowserRequester {

    private val json = Json { ignoreUnknownKeys = true }
    private val pending = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<BrowserReply>>()
    private var nextId = java.util.concurrent.atomic.AtomicLong(1)

    /** One request at a time: each waits on the one page, and a reload underneath another would lose it. */
    private val turn = Mutex()
    private val starting = Mutex()

    private var client: CefClient? = null
    private var browser: CefBrowser? = null

    /** The window the browser lives in, off where no screen reaches. */
    private var holder: JWindow? = null

    @Volatile
    private var loaded: CompletableDeferred<Unit>? = null

    @Volatile
    private var cleared = false

    override suspend fun send(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): BrowserReply? = turn.withLock {
        withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { request(method, url, headers, body) }
    }

    private suspend fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): BrowserReply? {
        // Settled on something harmless first, when this is a request that must not be made twice.
        if (!cleared && method.uppercase() !in REPEATABLE) settle(headers)

        val first = attempt(method, url, headers, body) ?: return null
        if (first.status != REFUSED) return first.also { cleared = true }
        // The first request of a run is often refused while the protection's own script is still deciding
        // about this browser, and a reload cures it. Only where asking twice cannot do any harm: creating
        // a playlist twice makes two, which is exactly what happened on the phone before this guard.
        if (method.uppercase() !in REPEATABLE) return first
        reload()
        return attempt(method, url, headers, body)?.also { cleared = it.status != REFUSED } ?: first
    }

    /** Earns the clearance on a plain read, so the request that follows need not be repeatable. */
    private suspend fun settle(headers: Map<String, String>) {
        val session = headers.filterKeys { it.equals("Authorization", ignoreCase = true) }
        repeat(2) { round ->
            if (round > 0) reload()
            val reply = attempt("GET", SETTLING_URL, session, null) ?: return
            if (reply.status != REFUSED) {
                cleared = true
                return
            }
        }
    }

    private suspend fun attempt(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): BrowserReply? {
        val page = runCatching { browser() }.getOrNull() ?: return null
        val id = nextId.getAndIncrement().toString()
        val answer = CompletableDeferred<BrowserReply>()
        pending[id] = answer
        return try {
            page.executeJavaScript(script(id, method, url, headers, body), page.url, 0)
            answer.await()
        } finally {
            pending.remove(id)
        }
    }

    private suspend fun browser(): CefBrowser = starting.withLock {
        browser?.let { return@withLock it }
        val app = CefRuntime.app(installDir)
        val cefClient = app.createClient()
        // How the page answers. Chromium runs the fetch in its own process, so the reply comes back
        // through this rather than as the result of the call that started it.
        val router = CefMessageRouter.create()
        router.addHandler(
            object : CefMessageRouterHandlerAdapter() {
                override fun onQuery(
                    target: CefBrowser?,
                    frame: CefFrame?,
                    queryId: Long,
                    request: String?,
                    persistent: Boolean,
                    callback: CefQueryCallback?,
                ): Boolean {
                    deliver(request)
                    callback?.success("")
                    return true
                }
            },
            true,
        )
        cefClient.addMessageRouter(router)
        cefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(
                target: CefBrowser?,
                isLoading: Boolean,
                canGoBack: Boolean,
                canGoForward: Boolean,
            ) {
                if (!isLoading) loaded?.takeIf { !it.isCompleted }?.complete(Unit)
            }
        })
        client = cefClient
        val finished = CompletableDeferred<Unit>()
        loaded = finished
        val page = onSwingThread {
            val created = cefClient.createBrowser(ORIGIN_PAGE, false, false)
            // An ordinary browser in a window placed far off the desktop, rather than an off-screen one.
            // Off-screen rendering would be the tidier answer and costs too much: switching it on is a
            // process-wide setting, and it made the sign-in page somebody actually types into sluggish.
            // A browser does need a real window to live in -- without one it is never created and the
            // request never runs -- so it gets one, a pixel wide, where no screen reaches and nothing in
            // the task bar shows it.
            holder = JWindow().apply {
                setBounds(OFF_SCREEN, OFF_SCREEN, 1, 1)
                contentPane.add(created.uiComponent)
                isVisible = true
            }
            created
        }
        browser = page
        withTimeoutOrNull(LOAD_TIMEOUT_MILLIS) { finished.await() }
        page
    }

    /** Swing objects are built on Swing's own thread; everything calling this is on a coroutine's. */
    private fun <T> onSwingThread(make: () -> T): T {
        if (SwingUtilities.isEventDispatchThread()) return make()
        var result: T? = null
        var failure: Throwable? = null
        SwingUtilities.invokeAndWait {
            runCatching(make).onSuccess { result = it }.onFailure { failure = it }
        }
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private suspend fun reload() {
        val page = browser ?: return
        val finished = CompletableDeferred<Unit>()
        loaded = finished
        page.reload()
        withTimeoutOrNull(LOAD_TIMEOUT_MILLIS) { finished.await() }
    }

    private fun deliver(request: String?) {
        val message = runCatching { json.parseToJsonElement(request.orEmpty()).let { it as? JsonObject } }
            .getOrNull() ?: return
        val id = (message["id"] as? JsonPrimitive)?.contentOrNull ?: return
        val status = (message["status"] as? JsonPrimitive)?.intOrNull ?: 0
        val body = (message["body"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        pending.remove(id)?.complete(BrowserReply(status, body))
    }

    /**
     * The fetch, and the one line that carries its answer back.
     *
     * `window.cefQuery` is Chromium's own way for a page to speak to the application embedding it, which is
     * what the message router above is listening on.
     */
    private fun script(
        id: String,
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?,
    ): String {
        val headerJson = JsonObject(headers.mapValues { (_, value) -> JsonPrimitive(value) }).toString()
        // Absent rather than null: fetch refuses a body on a GET outright.
        val bodyLine = body?.let { "body: ${quote(it)}," }.orEmpty()
        return """
            (function () {
              var deliver = function (status, text) {
                window.cefQuery({
                  request: JSON.stringify({ id: ${quote(id)}, status: status, body: text }),
                  onSuccess: function () {},
                  onFailure: function () {}
                });
              };
              try {
                fetch(${quote(url)}, {
                  method: ${quote(method)},
                  headers: $headerJson,
                  $bodyLine
                  credentials: 'include'
                }).then(function (reply) {
                  return reply.text().then(function (text) { deliver(reply.status, text); });
                }).catch(function (error) { deliver(0, String(error)); });
              } catch (error) { deliver(0, String(error)); }
            })();
        """.trimIndent()
    }

    private fun quote(value: String) = JsonPrimitive(value).toString()

    /** Lets go of the browser. Chromium itself stays up; it is process-wide and shared with sign-in. */
    fun close() {
        val closing = browser
        val disposing = client
        browser = null
        client = null
        // Off the caller's thread, for the same reason the sign-in window closes that way: shutting a CEF
        // browser pumps Chromium's message loop, and doing it on the interface thread freezes the window.
        val window = holder
        holder = null
        Thread({
            runCatching { closing?.close(true) }
            runCatching { disposing?.dispose() }
            runCatching { SwingUtilities.invokeLater { window?.dispose() } }
        }, "noctorium-cef-requester-dispose").apply { isDaemon = true }.start()
    }

    private companion object {
        /** The site itself: the origin the website makes these calls from, with its own scripts running. */
        const val ORIGIN_PAGE = "https://soundcloud.com/"

        /** What the bot protection answers a browser it has not cleared. */
        const val REFUSED = 403

        /** The methods a refusal may be retried on, because sending one twice changes nothing. */
        val REPEATABLE = setOf("GET", "HEAD", "PUT", "DELETE")

        /** A read on the protected host, used to earn the clearance where a retry would not be safe. */
        const val SETTLING_URL = "https://api-v2.soundcloud.com/me"

        /** Far enough off the desktop that no arrangement of monitors puts it on one. */
        const val OFF_SCREEN = -32_000

        const val LOAD_TIMEOUT_MILLIS = 20_000L
        const val REQUEST_TIMEOUT_MILLIS = 45_000L
    }
}
