package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.WarningAmber
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.Button
import androidx.compose.material3.CircularProgressIndicator
import app.noctorium.ui.skins.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import app.noctorium.auth.EmbeddedBrowserSession
import app.noctorium.auth.VK_LOGIN
import app.noctorium.auth.VK_SESSION_URLS
import app.noctorium.auth.VK_SIGN_IN
import app.noctorium.auth.VK_SITE
import app.noctorium.auth.vkSessionCookies
import app.noctorium.core.AppState
import app.noctorium.settings.SettingsState
import app.noctorium.settings.VkConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/*
 * VK Music's page in Settings, and signing in to VK.
 *
 * VK offers its music to nobody else's apps, so there is no sign-in made for Noctorium to use. What there is
 * is the session of somebody signed in on vk.ru, which is what VK's own web player plays with -- so that is
 * what Noctorium asks for, by having the listener sign in on VK's real page in the embedded browser and
 * taking the two cookies that session is made of. The page says what that means before anybody signs in:
 * VK's terms do not allow it, VK may ask the listener to prove they are a person, and much of its music
 * does not play outside Russia.
 *
 * Nothing here ever shows or logs the cookies. They go from the browser straight to the core, which checks
 * them with VK and keeps them encrypted.
 */

// --- Decisions ---

/** VK's own blue, as on its badge. */
private val VK_BLUE = Color(0xFF0077FF)

/** The line under VK Music on the Settings list. */
internal fun vkSummary(vk: VkConnectionState): String = when {
    vk.checking -> "Checking the session with VK…"
    vk.connected && vk.accountName.isNotBlank() -> "Signed in as ${vk.accountName}"
    vk.connected -> "Signed in"
    else -> "Sign in with your vk.ru account"
}

// --- The Settings page ---

/**
 * VK Music, set up: what using it means, then signing in -- on VK's page, or by pasting the session -- or,
 * once signed in, whose music it is and signing out.
 *
 * Signing in is offered only while signed out. Signing in again over a session that works would leave it
 * unclear which of the two the library is reading, and VK looks harder at an account that signs in often.
 */
@Composable
internal fun VkSettingsPanel(settings: SettingsState, state: AppState) {
    val vk = settings.vk
    var signInOpen by remember { mutableStateOf(false) }
    var pasteOpen by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    if (signInOpen) VkSignInWindow(state) { signInOpen = false }

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones, null, Modifier.size(40.dp), tint = VK_BLUE)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (vk.connected) vk.accountName.ifBlank { "VK Music" } else "VK Music",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Your music and playlists from VK, through your vk.ru session",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("My music and your VK playlists appear in your library.")
            AccountFact("A heart on a VK song adds it to My music on VK.")
            AccountFact("VK can be searched, and Home gets VK's suggestions for you and what is popular there.")
        }

        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text(if (vk.connected) "What to keep in mind" else "Before you sign in", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            AccountFact(
                "VK doesn't offer its music to other apps, so Noctorium uses your vk.ru session, the way VK's own " +
                    "web player does.",
            )
            AccountFact(
                "VK's terms don't allow this. VK may ask you to confirm it's you, or freeze an account it thinks " +
                    "is automated.",
            )
            AccountFact("Many songs won't play outside Russia.")
            AccountFact("VK songs can't be downloaded.")
        }

        SettingsPanelCard {
            if (vk.connected) {
                Text("Your VK account", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(vkSummary(vk.copy(checking = false)), fontSize = 12.sp)
                }
                Spacer(Modifier.height(13.dp))
                OutlinedButton(state::disconnectVk, enabled = !vk.checking) { Text("Sign out") }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Signing out forgets the session on this computer. Nothing changes at VK.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            } else {
                Text("Sign in", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "On VK's own page, inside Noctorium, with VK ID and two-step confirmation as VK asks for them. " +
                        "Your password goes to VK, never to Noctorium; only the session is kept, encrypted on this " +
                        "computer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button({ signInOpen = true }, enabled = !vk.checking) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Sign in to VK")
                    }
                    TextButton({ pasteOpen = !pasteOpen }) {
                        Text("Paste cookies instead")
                        Spacer(Modifier.width(4.dp))
                        Icon(if (pasteOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp))
                    }
                }
                if (pasteOpen) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "From a browser signed in on vk.ru: the p cookie, which VK sets on login.vk.ru, and remixsid " +
                            "from vk.ru, as p=…; remixsid=…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(9.dp))
                    // Hidden like a password, which is what a session is: whoever reads it off the screen is
                    // signed in as you.
                    OutlinedTextField(
                        pasted,
                        { pasted = it },
                        label = { Text("Cookies") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().tracksTyping(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button({ state.completeVkSignIn(pasted); pasted = "" }, enabled = pasted.isNotBlank() && !vk.checking) {
                        Text("Sign in with these")
                    }
                }
            }
            if (vk.checking) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp))
                    Text("Checking the session with VK…", fontSize = 12.sp)
                }
            }
            vk.message?.let { message ->
                Spacer(Modifier.height(11.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

// --- Signing in ---

/**
 * VK's own sign-in page in embedded Chromium, watched until the session it produces is complete.
 *
 * The listener signs in there however VK asks -- a password, VK ID, a code -- and the cookie store is read
 * every couple of seconds for the two cookies a session is made of. Once both are there they go to the
 * core, which checks them with VK before keeping anything; this window closes when it has, and says what VK
 * said when it has not.
 *
 * The store is cleared first, as for SoundCloud and Google, so a session left over from an earlier attempt is
 * not harvested in place of a sign-in. And it is cleared again once Noctorium holds the session, so that one
 * holder renews it rather than two.
 */
@Composable
private fun VkSignInWindow(state: AppState, close: () -> Unit) {
    var status by remember { mutableStateOf("Starting the embedded browser…") }
    var component by remember { mutableStateOf<java.awt.Component?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var finishing by remember { mutableStateOf(false) }
    val session = remember { state.dataDirectory()?.let { EmbeddedBrowserSession(it.resolve("chromium")) } }

    DisposableEffect(session) { onDispose { session?.dispose() } }

    LaunchedEffect(session) {
        if (session == null) {
            status = "Noctorium has nowhere to store the browser on this system."
            return@LaunchedEffect
        }
        runCatching {
            session.start(
                url = VK_SIGN_IN,
                onProgress = { progress -> status = "Preparing Chromium — $progress" },
                onPageLoaded = { url -> currentUrl = url },
            )
        }.onSuccess { ui ->
            component = ui
            if (session.clearCookies(VK_SESSION_URLS)) session.navigate(VK_SIGN_IN)
            status = "Sign in to VK below. Noctorium picks the session up on its own."
        }.onFailure { error ->
            status = "Could not start the embedded browser: ${error.message?.take(180)}"
        }
    }

    // Read from a coroutine rather than inside a Chromium callback, as for SoundCloud: the browser's own
    // threads stay free, and nothing waits on Chromium from inside one of its own events.
    LaunchedEffect(component) {
        val live = session ?: return@LaunchedEffect
        if (component == null) return@LaunchedEffect
        // A session VK has already turned down, kept so the same one is not sent again every few seconds:
        // each try is a real request, and VK notices those. Held here only, and only for this window.
        var rejected: String? = null
        while (true) {
            delay(2_500)
            val site = runCatching { live.harvestCookies(VK_SITE) }.getOrDefault(emptyList())
            val login = runCatching { live.harvestCookies(VK_LOGIN) }.getOrDefault(emptyList())
            val cookies = vkSessionCookies(site, login) ?: continue
            if (cookies == rejected) continue

            finishing = true
            status = "Signed in on VK. Checking the session with VK…"
            state.completeVkSignIn(cookies)
            // The check is under way by the time that returns, so this waits for its answer.
            val answer = state.settings.first { !it.vk.checking }.vk
            if (answer.connected) {
                live.clearCookies(VK_SESSION_URLS)
                close()
                return@LaunchedEffect
            }
            rejected = cookies
            finishing = false
            status = (answer.message ?: "VK did not accept that session.") + " Sign in again below, or start over."
        }
    }

    DialogWindow(
        onCloseRequest = close,
        state = rememberDialogState(width = 980.dp, height = 760.dp),
        title = "Sign in to VK",
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (finishing || component == null) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(status, fontSize = 12.sp)
                    if (currentUrl.isNotBlank()) {
                        Text(currentUrl, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                OutlinedButton({
                    session?.clearCookies(VK_SESSION_URLS)
                    session?.navigate(VK_SIGN_IN)
                    status = "Starting over. Sign in to VK below."
                }) { Text("Start over") }
                Spacer(Modifier.width(8.dp))
                Button(close) { Text("Done") }
            }
            HorizontalDivider()
            component?.let { ui ->
                SwingPanel(background = Color.Black, factory = { ui }, modifier = Modifier.fillMaxSize())
            } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 520.dp)) {
                    Text("Getting the browser ready", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chromium ships with Noctorium, so nothing is being downloaded — it is unpacked once on this " +
                            "computer and every sign-in after this opens straight away.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
