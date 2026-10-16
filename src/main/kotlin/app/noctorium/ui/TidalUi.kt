package app.noctorium.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.settings.SettingsState
import app.noctorium.settings.TidalConnectionState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.icons.markVector
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.scrollingPage

/*
 * Tidal's page in Settings.
 *
 * Tidal lets an application in only through an app registered with it, and Noctorium has none of its own yet, so
 * before anything else works the listener registers one -- free, at developer.tidal.com -- and gives its client id
 * here, and its secret where they want Tidal searched without signing in. The page says that plainly, first, since
 * nothing about a service called Tidal suggests a developer dashboard. Then it is Spotify's own-app sign-in: Tidal's
 * page in the browser, and the reply back to an address on this computer that has to be registered for the app.
 *
 * Nothing secret is ever shown here once it is saved. The client id and secret go straight to the core, which keeps
 * them in this computer's credential store; the boxes are empty again afterwards, for another app's.
 */

// --- Decisions ---

/** The line under Tidal on the Settings list. */
internal fun tidalSummary(tidal: TidalConnectionState): String = when {
    tidal.connecting -> "Waiting for Tidal…"
    tidal.connected && tidal.accountName.isNotBlank() -> "Connected as ${tidal.accountName}"
    tidal.connected -> "Your Tidal collection is connected"
    tidal.readsCatalogue -> "Tidal can be searched · connect to see your collection"
    tidal.configured -> "Your Tidal app is saved · connect to sign in"
    else -> "Needs a free Tidal developer app of your own"
}

/** What the card holding the Tidal app says it is, beside its name: needed, in use, or there to be chosen. */
internal fun tidalAppStatus(tidal: TidalConnectionState): String = when {
    !tidal.configured -> "Needed first"
    tidal.ownApp -> "In use"
    else -> "Optional"
}

// --- The Settings page ---

/**
 * Tidal, set up: what it gives and how its songs play; the Tidal app it goes through; then connecting.
 *
 * The app comes first, open, while there is none, because it is the whole of the setup until it is done; once there
 * is one it folds away, saying it is in use, and Connect is what the page is for. Connect is there either way, held
 * back with the reason until there is an app, so it is clear what the app is for.
 */
@Composable
internal fun TidalSettingsPanel(settings: SettingsState, state: AppState) {
    val tidal = settings.tidal
    var appOpen by remember(tidal.configured) { mutableStateOf(!tidal.configured) }

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tidal's mark is black, which is the page itself on a dark theme: drawn in the writing's colour.
                Icon(ProviderType.TIDAL.markVector(), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (tidal.connected) tidal.accountName.ifBlank { "Tidal" } else "Tidal",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Your Tidal collection and mixes, with every song matched on YouTube Music",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Tidal can be searched on its own, and its albums, artists, playlists and links open here.")
            AccountFact("Connected, your collection and playlists are in your library, and Home gets the mixes Tidal makes for you.")
            AccountFact(
                "Tidal's audio only plays in Tidal's own apps, so each song is matched to the same recording on " +
                    "YouTube Music and played from there. A song that cannot be found is reported rather than swapped for another.",
            )
            AccountFact("Nothing is ever written to Tidal, and Tidal songs can't be liked from here.")
        }

        SettingsPanelCard {
            SettingAnchor("Your Tidal app") {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = tidal.configured) { appOpen = !appOpen },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Your Tidal app", fontWeight = FontWeight.SemiBold)
                        Text(
                            tidalAppStatus(tidal),
                            color = if (tidal.configured) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                        )
                    }
                    if (tidal.configured) {
                        Icon(if (appOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (appOpen) "Fold away" else "Show")
                    }
                }
            }
            if (tidal.ownApp) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tidal.readsCatalogue) "Your Tidal app is saved, and Tidal can be searched through it." else "Your Tidal app is saved.",
                        fontSize = 12.sp,
                    )
                }
            }
            if (appOpen) TidalAppSetup(tidal, state)
        }

        SettingsPanelCard {
            Text("Connect", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (tidal.configured) {
                    "On Tidal's own page, in your browser, through your Tidal app. Your password goes to Tidal, never " +
                        "to Noctorium; only the sign-in is kept, encrypted on this computer."
                } else {
                    "Connecting goes through your Tidal app, so it waits until one is saved above."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            if (tidal.connected) {
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(tidalSummary(tidal.copy(connecting = false)), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                // Connecting again over a sign-in that is kept is how one Tidal has stopped accepting is renewed,
                // so it stays offered, only quieter.
                val connect: @Composable () -> Unit = {
                    if (tidal.connecting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Link, null, Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        when {
                            tidal.connecting -> "Waiting for Tidal…"
                            tidal.connected -> "Connect again"
                            else -> "Connect Tidal"
                        },
                    )
                }
                val canConnect = tidal.configured && !tidal.connecting
                if (tidal.connected) {
                    OutlinedButton(state::connectTidal, enabled = canConnect) { connect() }
                    OutlinedButton(state::disconnectTidal, enabled = !tidal.connecting) { Text("Disconnect") }
                } else {
                    Button(state::connectTidal, enabled = canConnect) { connect() }
                }
            }
            tidal.message?.let { message ->
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

/**
 * Registering a Tidal app and giving it to Noctorium: why it is needed, where to go, the address to register, and
 * the boxes for its client id and secret.
 *
 * The secret is optional and hidden like a password, which it is. Neither box is filled in from what is kept, since
 * what is kept is in the credential store and not to be read back onto a screen; they are for a new app, and Remove
 * is how the one kept is let go of.
 */
@Composable
private fun TidalAppSetup(tidal: TidalConnectionState, state: AppState) {
    var clientId by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    Spacer(Modifier.height(10.dp))
    if (!tidal.configured) {
        Text(
            "Tidal needs a free developer app before Noctorium can search it or sign you in. Noctorium has no Tidal " +
                "app of its own yet, so register one of yours at developer.tidal.com: it takes a few minutes, costs " +
                "nothing and never expires.",
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 12.sp,
        )
    } else {
        Text(
            if (tidal.ownApp) "To go through another of your Tidal apps, give its client id below." else "For signing in through a Tidal app registered to you instead.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
    }
    Spacer(Modifier.height(11.dp))
    SetupStep(1, "Open Tidal's developer dashboard, sign in with your Tidal account and create an app. Any name will do.")
    SetupStep(2, "Add this exact address to the app's redirect URIs:")
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.padding(start = 26.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer { Text(tidal.redirectUri, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
            Spacer(Modifier.width(8.dp))
            IconButton(state::copyTidalRedirectUri, Modifier.size(26.dp)) {
                Icon(Icons.Default.ContentCopy, "Copy the redirect address", Modifier.size(15.dp))
            }
        }
    }
    SetupStep(3, "Give the app access to the user, collection, playlists, search and recommendations.")
    SetupStep(4, "Copy its client id below. Add its client secret too, to search Tidal without signing in.")
    Spacer(Modifier.height(11.dp))
    OutlinedButton(state::openTidalDeveloperPortal) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text("Open developer.tidal.com")
    }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        clientId,
        { clientId = it.trim().take(128) },
        label = { Text("Client ID") },
        placeholder = { Text("From your app's page on developer.tidal.com") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().tracksTyping(),
    )
    Spacer(Modifier.height(9.dp))
    OutlinedTextField(
        secret,
        { secret = it.trim().take(256) },
        label = { Text("Client secret (optional)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().tracksTyping(),
    )
    Spacer(Modifier.height(7.dp))
    Text(
        "Both are kept in this computer's credential store, never in the settings file.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
    Spacer(Modifier.height(13.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Button(
            {
                state.configureTidalApplication(clientId, secret)
                clientId = ""
                secret = ""
            },
            enabled = clientId.isNotBlank(),
        ) {
            Icon(Icons.Default.Save, null, Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Use this app")
        }
        if (tidal.ownApp) {
            OutlinedButton({ state.configureTidalApplication("", "") }) { Text("Remove") }
        }
    }
}
