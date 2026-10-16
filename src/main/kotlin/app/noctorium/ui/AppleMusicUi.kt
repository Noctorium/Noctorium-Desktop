package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import app.noctorium.auth.APPLE_MUSIC_SESSION_URLS
import app.noctorium.auth.APPLE_MUSIC_SIGN_IN
import app.noctorium.auth.APPLE_MUSIC_SITE
import app.noctorium.auth.EmbeddedBrowserSession
import app.noctorium.auth.appleMusicUserToken
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.settings.AppleMusicConnectionState
import app.noctorium.settings.SettingsState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.icons.markVector
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.scrollingPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Locale

/*
 * Apple Music's page in Settings, and signing in to Apple Music.
 *
 * Apple's catalogue is read the way its own web player reads it, so search, albums, artists and links work with
 * nobody signed in. Apple offers no sign-in made for an application like this one, so signing in is done as VK's
 * is: on Apple's real page -- the web player's own sign-in -- in the embedded browser, and Noctorium takes the one
 * thing the signed-in web player keeps, its media-user-token cookie. The core checks it with Apple before keeping
 * it, encrypted. Its songs are never played from Apple: each is matched to the same recording on YouTube Music.
 *
 * Nothing here ever shows or logs the token. It goes from the browser straight to the core.
 */

// --- Decisions ---

/** Apple Music's red, as on its badge. */
private val APPLE_MUSIC_RED = Color(0xFFFA243C)

/**
 * A storefront as somebody would name it: "us" is the United States. Apple's storefronts are named by country, in
 * lower case; one this computer's Java does not know is shown as Apple wrote it.
 */
internal fun storefrontName(storefront: String): String {
    val code = storefront.trim().uppercase(Locale.ROOT)
    val country = runCatching { Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH) }.getOrNull()
    return country?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) } ?: code
}

/** The line under Apple Music on the Settings list. */
internal fun appleMusicSummary(apple: AppleMusicConnectionState): String = when {
    apple.checking -> "Checking the sign-in with Apple…"
    apple.connected && apple.storefront.isNotBlank() -> "Signed in · ${storefrontName(apple.storefront)} storefront"
    apple.connected -> "Signed in"
    else -> "Search works now · sign in for your library"
}

// --- The Settings page ---

/**
 * Apple Music, set up: what it gives and how its songs play, then signing in -- on Apple's page, or by pasting the
 * token -- or, once signed in, which storefront the catalogue is read in and signing out; and, folded away, a
 * MusicKit developer token of the listener's own.
 *
 * Signing in is offered only while signed out, as on VK's page: signing in again over a sign-in that works would
 * leave it unclear which of the two the library is reading.
 */
@Composable
internal fun AppleMusicSettingsPanel(settings: SettingsState, state: AppState) {
    val apple = settings.appleMusic
    var signInOpen by remember { mutableStateOf(false) }
    var pasteOpen by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    var tokenOpen by remember { mutableStateOf(apple.ownDeveloperToken) }
    var developerToken by remember { mutableStateOf("") }
    if (signInOpen) AppleMusicSignInWindow(state) { signInOpen = false }

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ProviderType.APPLE_MUSIC.markVector(), null, Modifier.size(40.dp), tint = APPLE_MUSIC_RED)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (apple.connected) "Apple Music, signed in" else "Apple Music", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (apple.connected) {
                            "Your library and Apple Music's catalogue, with every song matched on YouTube Music"
                        } else {
                            "Apple Music's catalogue now, and your library once you sign in"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Apple Music can be searched on its own, and its albums, artists, playlists and links open here without signing in.")
            AccountFact(
                "Signed in, your playlists and songs are in your library, and Home gets what you played lately, what " +
                    "Apple recommends and what is most played where you are. That needs an Apple Music subscription.",
            )
            AccountFact(
                "Apple Music's audio only plays in Apple's own apps, so each song is matched to the same recording on " +
                    "YouTube Music and played from there. A song that cannot be found is reported rather than swapped for another.",
            )
            AccountFact("Nothing is ever written to Apple Music, and its songs can't be liked from here.")
        }

        SettingsPanelCard {
            if (apple.connected) {
                Text("Your Apple Music account", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(appleMusicSummary(apple.copy(checking = false)), fontSize = 12.sp)
                }
                Spacer(Modifier.height(13.dp))
                OutlinedButton(state::disconnectAppleMusic, enabled = !apple.checking) { Text("Sign out") }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Signing out forgets the sign-in on this computer. Nothing changes at Apple.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            } else {
                Text("Sign in", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "On Apple's own page, inside Noctorium, the way Apple Music's web player signs in. Your Apple " +
                        "Account password goes to Apple, never to Noctorium; only the web player's sign-in is kept, " +
                        "encrypted on this computer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                if (apple.storefront.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Until then the catalogue is read in the ${storefrontName(apple.storefront)} storefront, this computer's country.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button({ signInOpen = true }, enabled = !apple.checking) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Sign in to Apple Music")
                    }
                    TextButton({ pasteOpen = !pasteOpen }) {
                        Text("Paste the token instead")
                        Spacer(Modifier.width(4.dp))
                        Icon(if (pasteOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, Modifier.size(18.dp))
                    }
                }
                if (pasteOpen) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "For when the window cannot sign you in: from a browser signed in on music.apple.com, the " +
                            "media-user-token cookie, or a whole Cookie: line with it in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(9.dp))
                    // Hidden like a password, which is what it is: whoever reads it off the screen is signed in as you.
                    OutlinedTextField(
                        pasted,
                        { pasted = it },
                        label = { Text("media-user-token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().tracksTyping(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button({ state.completeAppleMusicSignIn(pasted); pasted = "" }, enabled = pasted.isNotBlank() && !apple.checking) {
                        Text("Sign in with this")
                    }
                }
            }
            if (apple.checking) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp))
                    Text("Checking the sign-in with Apple…", fontSize = 12.sp)
                }
            }
            apple.message?.let { message ->
                Spacer(Modifier.height(11.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                    }
                }
            }
        }

        SettingsPanelCard {
            SettingAnchor(APPLE_DEVELOPER_TOKEN) {
                Row(Modifier.fillMaxWidth().clickable { tokenOpen = !tokenOpen }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(APPLE_DEVELOPER_TOKEN, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (apple.ownDeveloperToken) "In use" else "Optional",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                    Icon(if (tokenOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (tokenOpen) "Fold away" else "Show")
                }
            }
            if (tokenOpen) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Apple Music is read with the developer token Apple's own web player is given. With an Apple " +
                        "Developer membership you can make one of your own from a MusicKit key, and Noctorium reads " +
                        "Apple Music with yours instead. Nothing else changes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                OutlinedTextField(
                    developerToken,
                    { developerToken = it.trim() },
                    label = { Text("Developer token") },
                    placeholder = { Text("The signed token, starting eyJ") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "Kept in this computer's credential store, never in the settings file.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button({ state.setAppleMusicDeveloperToken(developerToken); developerToken = "" }, enabled = developerToken.isNotBlank()) {
                        Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Use this token")
                    }
                    if (apple.ownDeveloperToken) {
                        OutlinedButton({ state.setAppleMusicDeveloperToken("") }) { Text("Go back to the web player's") }
                    }
                }
            }
        }
    }
}

/** The fold for a developer token of the listener's own, by the name the settings search finds it by. */
internal const val APPLE_DEVELOPER_TOKEN = "Use my own MusicKit developer token"

// --- Signing in ---

/**
 * Apple Music's web player at its sign-in, in embedded Chromium, watched until it holds the listener's token.
 *
 * The listener signs in there however Apple asks -- a password, a code sent to their devices -- in the window
 * Apple's page opens for it, which Chromium is left to open as it would anywhere. The cookie store is read every
 * couple of seconds for the media-user-token the web player keeps once signed in; when it is there it goes to the
 * core, which checks it with Apple before keeping anything. This window closes when Apple has said yes, and says
 * what Apple said when it has not.
 *
 * The store is cleared first, as for VK, so a sign-in left over from an earlier attempt is not taken in place of a
 * new one; and cleared again once Noctorium holds the token, so the browser does not go on holding a second copy.
 */
@Composable
private fun AppleMusicSignInWindow(state: AppState, close: () -> Unit) {
    var status by remember { mutableStateOf("Starting the embedded browser…") }
    var component by remember { mutableStateOf<java.awt.Component?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var finishing by remember { mutableStateOf(false) }
    val session = remember { state.dataDirectory()?.let { EmbeddedBrowserSession(it.resolve("chromium")) } }

    DisposableEffect(session) { onDispose { session?.dispose() } }

    LaunchedEffect(session) {
        if (session == null) {
            status = "Noctorium has nowhere to store the browser on this system. Paste the token instead."
            return@LaunchedEffect
        }
        runCatching {
            session.start(
                url = APPLE_MUSIC_SIGN_IN,
                onProgress = { progress -> status = "Preparing Chromium — $progress" },
                onPageLoaded = { url -> currentUrl = url },
            )
        }.onSuccess { ui ->
            component = ui
            if (session.clearCookies(APPLE_MUSIC_SESSION_URLS)) session.navigate(APPLE_MUSIC_SIGN_IN)
            status = "Sign in to Apple Music below, and in the window Apple opens for it. Noctorium picks the sign-in up on its own."
        }.onFailure { error ->
            status = "Could not start the embedded browser: ${error.message?.take(180)}. Paste the token instead."
        }
    }

    // Read from a coroutine rather than inside a Chromium callback, as for VK: the browser's own threads stay free.
    LaunchedEffect(component) {
        val live = session ?: return@LaunchedEffect
        if (component == null) return@LaunchedEffect
        // A token Apple has already turned down, kept so the same one is not sent again every few seconds. Held
        // here only, and only for this window.
        var rejected: String? = null
        while (true) {
            delay(2_500)
            val cookies = runCatching { live.harvestCookies(APPLE_MUSIC_SITE) }.getOrDefault(emptyList())
            val token = appleMusicUserToken(cookies) ?: continue
            if (token == rejected) continue

            finishing = true
            status = "Signed in on Apple's page. Checking the sign-in with Apple…"
            state.completeAppleMusicSignIn(token)
            // The check is under way by the time that returns, so this waits for its answer.
            val answer = state.settings.first { !it.appleMusic.checking }.appleMusic
            if (answer.connected) {
                live.clearCookies(APPLE_MUSIC_SESSION_URLS)
                close()
                return@LaunchedEffect
            }
            rejected = token
            finishing = false
            status = (answer.message ?: "Apple did not accept that sign-in.") + " Sign in again below, or start over."
        }
    }

    DialogWindow(
        onCloseRequest = close,
        state = rememberDialogState(width = 980.dp, height = 760.dp),
        title = "Sign in to Apple Music",
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
                    session?.clearCookies(APPLE_MUSIC_SESSION_URLS)
                    session?.navigate(APPLE_MUSIC_SIGN_IN)
                    status = "Starting over. Sign in to Apple Music below."
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
