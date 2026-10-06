package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TabletAndroid
import androidx.compose.material.icons.filled.Tv
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.Button
import androidx.compose.material3.Icon
import app.noctorium.ui.skins.IconButton
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.RadioButton
import androidx.compose.material3.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.SettingsState
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.spotify.SpotifyDevice

/*
 * Spotify on the desktop: its page in Settings, and the tag that says a song is playing in Spotify's own app.
 *
 * There are two sign-ins, and they are one sign-in asking for more or less. The ordinary one reads the
 * library, keeps hearts in step with Liked Songs, searches, and gives Home two rows; its songs are matched on
 * YouTube Music to be played. The Premium one also asks to tell the account's Spotify app what to play --
 * the only way Spotify lets its music be played by anything but itself -- and then Spotify songs play there,
 * on whichever device the listener picks. Noctorium never has Spotify's audio either way, and the page says
 * so plainly, since somebody would reasonably expect otherwise.
 */

// --- Decisions ---

/** Spotify's green, for what on screen is Spotify's own: its page's mark and the On Spotify tag. */
internal val SPOTIFY_GREEN = Color(0xFF1DB954)

/**
 * Whether [track] is played by the account's own Spotify app rather than by this computer: a Spotify song,
 * with Spotify songs set to play on Spotify.
 *
 * A Spotify song played the other way is replaced in the queue by the recording it was matched to, so it is
 * no longer a Spotify song by the time it plays, and this rightly says no.
 */
internal fun playsOnSpotify(track: Track?, spotify: SpotifyConnectionState): Boolean =
    track?.provider == ProviderType.SPOTIFY && spotify.playsOnSpotify

/** The line under Spotify on the Settings list. */
internal fun spotifySummary(spotify: SpotifyConnectionState): String {
    val name = spotify.accountName.takeIf(String::isNotBlank)
    return when {
        spotify.connecting -> "Waiting for Spotify…"
        !spotify.connected -> "Connect any account, or Premium to play on Spotify"
        spotify.playsOnSpotify -> listOfNotNull(name, "Premium · songs play on Spotify").joinToString(" · ")
        spotify.canPlay -> listOfNotNull(name, "Premium · songs matched on YouTube Music").joinToString(" · ")
        name != null -> "Reading $name's library"
        else -> "Your Spotify library is connected"
    }
}

/** What is said under a device's name: what sort of thing it is, and anything Spotify said about it. */
internal fun spotifyDeviceLine(device: SpotifyDevice): String = listOfNotNull(
    device.type.takeIf(String::isNotBlank),
    "playing now".takeIf { device.isActive },
    "Spotify won't take commands for it".takeIf { device.isRestricted },
).joinToString(" · ")

/**
 * Whether the device chosen before is missing from the list Spotify gave last -- closed, or asleep -- in which
 * case songs go to whichever one Spotify is using instead, as the player decides.
 */
internal fun chosenDeviceAway(devices: List<SpotifyDevice>, chosen: String): Boolean =
    chosen.isNotBlank() && devices.none { it.id == chosen }

/** A picture for each of Spotify's words for a device, and a general one for any it adds later. */
private fun deviceIcon(type: String): ImageVector = when (type.lowercase()) {
    "computer" -> Icons.Default.Computer
    "smartphone" -> Icons.Default.PhoneAndroid
    "tablet" -> Icons.Default.TabletAndroid
    "speaker" -> Icons.Default.Speaker
    "tv" -> Icons.Default.Tv
    "castvideo", "castaudio" -> Icons.Default.Cast
    "automobile" -> Icons.Default.DirectionsCar
    "gameconsole" -> Icons.Default.SportsEsports
    else -> Icons.Default.Devices
}

// --- The On Spotify tag ---

/**
 * Whether the song playing now is played by the account's Spotify app. See [playsOnSpotify].
 *
 * Asked of the player rather than the queue: it is what is making the sound that the equaliser and the boost
 * would have to reach.
 */
@Composable
internal fun playingOnSpotify(state: AppState): Boolean {
    val playback by state.playback.collectAsState()
    val spotify = state.settings.collectAsState().value.spotify
    return playsOnSpotify(playback.track, spotify)
}

/**
 * A small tag saying the song is playing in Spotify's own app, beside the song wherever the player names it.
 *
 * Wanted because nothing else looks any different: the seek bar, pause and next all work as they always do,
 * while the music comes out of a phone across the room -- or out of nothing, if Spotify is closed there.
 */
@Composable
internal fun OnSpotifyMark() {
    // Spotify's own green is too pale to read as writing on a light page, so a deeper one there.
    val green = if (MaterialTheme.colorScheme.background.luminance() > .5f) Color(0xFF117A37) else SPOTIFY_GREEN
    HoverHint("Spotify plays this song in your own Spotify app") {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SPOTIFY_GREEN.copy(alpha = .16f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Speaker, null, Modifier.size(11.dp), tint = green)
            Spacer(Modifier.width(3.dp))
            Text(
                "On Spotify",
                color = green,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

// --- The Settings page ---

/**
 * Spotify, set up: the two sign-ins, where Spotify songs play, and a Spotify app of the listener's own.
 *
 * Says plainly what each sign-in is, because "connect Spotify" would otherwise reasonably be read as "play
 * from Spotify" -- which only the Premium one does, and even then by Spotify's own app, never by Noctorium.
 * A Spotify app of the listener's own is still offered, folded away at the bottom, for somebody who would
 * rather sign in through that.
 */
@Composable
internal fun SpotifySettingsPanel(settings: SettingsState, state: AppState) {
    val spotify = settings.spotify
    var clientId by remember(settings.preferences.spotifyClientId) {
        mutableStateOf(settings.preferences.spotifyClientId)
    }
    var ownAppOpen by remember { mutableStateOf(settings.preferences.spotifyClientId.isNotBlank()) }
    val redirect = remember { state.spotifyRedirectUri() }
    // A client id typed in but not yet put to use would otherwise be signed in through the old one.
    val canConnect = !spotify.connecting && clientId == settings.preferences.spotifyClientId
    // Where Spotify is open is asked once, when the page opens with a Premium sign-in and nothing known yet:
    // every question counts against the little Spotify allows an app in development, so never on a timer.
    LaunchedEffect(spotify.canPlay) {
        if (spotify.canPlay && spotify.devices.isEmpty()) state.refreshSpotifyDevices()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LibraryMusic, null, Modifier.size(40.dp), tint = SPOTIFY_GREEN)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            spotify.connected && spotify.accountName.isNotBlank() -> spotify.accountName
                            spotify.connected -> "Spotify connected"
                            else -> "Spotify not connected"
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when {
                            spotify.playsOnSpotify -> "Your library and likes, with songs played in your Spotify app"
                            spotify.connected -> "Your library and likes, with songs matched on YouTube Music"
                            else -> "Your playlists, Liked Songs and search, and with Premium, playing on Spotify"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                if (spotify.connected && spotify.canPlay) PremiumTag()
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Your Spotify playlists and Liked Songs appear in your library, in Spotify's order.")
            AccountFact("A heart on a Spotify song saves it to your Liked Songs, and taking the heart off removes it.")
            AccountFact("Spotify can be searched on its own, and Home gets your top songs and what you played lately.")
            AccountFact(
                "Without Premium, each song is matched on YouTube Music and played from there. A song that cannot " +
                    "be found is reported rather than swapped for another.",
            )
            AccountFact("With Premium, Spotify plays the song in your own Spotify app. Noctorium never decodes Spotify's audio.")
        }

        SettingsPanelCard {
            Text("Connect", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (spotify.ownApp) {
                    "Both sign in through your own Spotify app, in your browser. Connecting with the other one " +
                        "switches to it."
                } else {
                    "Both sign in on Spotify's own page, in your browser, with nothing to set up first. Connecting " +
                        "with the other one switches to it."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            SignInOption(
                title = "Spotify",
                text = "Any account. Your playlists and Liked Songs in the library, hearts that save to Liked Songs, " +
                    "Spotify in search and two rows on Home. Songs play matched on YouTube Music.",
                inUse = spotify.connected && !spotify.canPlay,
                button = "Connect Spotify",
                again = "Reconnect Spotify",
                enabled = canConnect,
                // Quiet once Premium is in use: from there it is a step down, and should not be the button the
                // page offers loudest.
                prominent = !spotify.connected,
                connect = { state.connectSpotify() },
            )
            Spacer(Modifier.height(9.dp))
            SignInOption(
                title = "Spotify Premium",
                text = "All of that, and Spotify songs play on Spotify itself: in your own Spotify app, on this " +
                    "computer, your phone or a speaker, so keep Spotify open somewhere. Noctorium tells it what to " +
                    "play, and never decodes Spotify's audio.",
                note = "While a Spotify app is in development mode, Spotify plays for it only if the account that " +
                    "owns the app has Premium too.",
                inUse = spotify.connected && spotify.canPlay,
                button = "Connect Spotify Premium",
                again = "Reconnect Spotify Premium",
                enabled = canConnect,
                connect = state::connectSpotifyPremium,
            )
            if (spotify.connected) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(state::disconnectSpotify) { Text("Disconnect") }
            }
            spotify.message?.let { message ->
                Spacer(Modifier.height(11.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .6f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                    }
                }
            }
        }

        if (spotify.connected) SpotifyPlaybackCard(spotify, state)

        SettingsPanelCard {
            Row(
                Modifier.fillMaxWidth().clickable { ownAppOpen = !ownAppOpen },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Use your own Spotify app", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (spotify.ownApp) "In use" else "Optional",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Icon(if (ownAppOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (ownAppOpen) "Fold away" else "Show")
            }
            if (ownAppOpen) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "For signing in through an app registered to you instead of Noctorium's. It takes a minute " +
                        "and never expires.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(13.dp))
                SetupStep(1, "Open Spotify's developer dashboard and create an app. Any name will do.")
                SetupStep(2, "Add this exact address to the app's Redirect URIs, and tick the Web API:")
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .7f),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.padding(start = 26.dp, top = 4.dp, bottom = 8.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SelectionContainer { Text(redirect, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                        Spacer(Modifier.width(8.dp))
                        IconButton(state::copySpotifyRedirectUri, Modifier.size(26.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy the redirect address", Modifier.size(15.dp))
                        }
                    }
                }
                SetupStep(3, "Copy the app's Client ID from its settings, paste it below, and connect again.")
                Spacer(Modifier.height(11.dp))
                OutlinedButton(state::openSpotifyDashboard) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Open Spotify's dashboard")
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    clientId,
                    { clientId = it.trim().take(64) },
                    label = { Text("Client ID") },
                    placeholder = { Text("32 characters from the dashboard") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tracksTyping(),
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "An identifier and not a password, so it is kept in your settings file. The sign-in itself is " +
                        "stored encrypted for your account on this computer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        { state.setSpotifyClientId(clientId) },
                        enabled = clientId.isNotBlank() && clientId != settings.preferences.spotifyClientId,
                    ) {
                        Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Use this app")
                    }
                    if (spotify.ownApp) {
                        OutlinedButton({ clientId = ""; state.setSpotifyClientId("") }) { Text("Go back to Noctorium's") }
                    }
                }
            }
        }
    }
}

/**
 * One of the two ways in: what it gives, and the button that signs in that way.
 *
 * The one in use is lit and says so, because the two buttons otherwise look like two separate accounts
 * rather than one account signed in with more or less.
 */
@Composable
private fun SignInOption(
    title: String,
    text: String,
    note: String? = null,
    inUse: Boolean,
    button: String,
    again: String,
    enabled: Boolean,
    prominent: Boolean = true,
    connect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (inUse) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f) else ink(.03f),
        border = BorderStroke(1.dp, if (inUse) MaterialTheme.colorScheme.primary.copy(alpha = .45f) else ink(.08f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (inUse) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(5.dp))
                    Text("In use", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            note?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.tertiary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(11.dp))
            // Signing in again the way already in use is how a lapsed sign-in is renewed, so it stays offered,
            // just quieter than the way not taken.
            if (inUse) {
                OutlinedButton(connect, enabled = enabled) { Text(again) }
            } else if (!prominent) {
                OutlinedButton(connect, enabled = enabled) {
                    Icon(Icons.Default.Link, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(button)
                }
            } else {
                Button(connect, enabled = enabled) {
                    Icon(Icons.Default.Link, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(button)
                }
            }
        }
    }
}

/**
 * Where Spotify songs play: on Spotify or matched elsewhere, and on which of the account's devices.
 *
 * The switch is there for every account so the choice can be seen, and only works with the Premium sign-in.
 * The devices are those Spotify reported when last asked; the list is not kept fresh on its own, since every
 * question counts against what Spotify allows, and there is a button to ask again.
 */
@Composable
private fun SpotifyPlaybackCard(spotify: SpotifyConnectionState, state: AppState) {
    SettingsPanelCard {
        Text("Where Spotify songs play", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            "Play Spotify songs on Spotify",
            when {
                !spotify.canPlay -> "Needs the Premium sign-in above. Until then they are matched on YouTube Music."
                spotify.playsOnSpotify -> "In your Spotify app, on the device below."
                else -> "Off: they are matched on YouTube Music and play here."
            },
            spotify.playsOnSpotify,
            enabled = spotify.canPlay,
        ) { on -> state.setSpotifyPlayback(on) }
        if (spotify.canPlay) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Play on", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Spotify has to be open there: a phone, a computer, a speaker or a browser tab signed in to the " +
                            "same account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                TextButton(state::refreshSpotifyDevices) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            DeviceChoice(
                Icons.Default.Devices,
                "Any active device",
                "Whichever one Spotify is playing on",
                selected = spotify.device.isBlank(),
                enabled = true,
            ) { state.chooseSpotifyDevice("") }
            spotify.devices.forEach { device ->
                DeviceChoice(
                    deviceIcon(device.type),
                    device.name,
                    spotifyDeviceLine(device),
                    selected = device.id == spotify.device,
                    enabled = !device.isRestricted,
                ) { state.chooseSpotifyDevice(device.id) }
            }
            if (spotify.devices.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Spotify has not said it is open anywhere. Open it on a phone or a computer, then refresh.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            if (chosenDeviceAway(spotify.devices, spotify.device)) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "The device chosen before is not open just now, so songs go to whichever one Spotify is using.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

/** One place Spotify can play, chosen by clicking anywhere on its row. */
@Composable
private fun DeviceChoice(
    icon: ImageVector,
    name: String,
    line: String,
    selected: Boolean,
    enabled: Boolean,
    choose: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = choose)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, choose, enabled = enabled)
        Icon(
            icon,
            null,
            Modifier.size(19.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
            )
            Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Premium", beside the account's name, once the sign-in is the one that plays. */
@Composable
private fun PremiumTag() {
    // Black on the green, as Spotify writes on its own: white on it is too faint to read at this size.
    Text(
        "Premium",
        color = Color.Black,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SPOTIFY_GREEN.copy(alpha = .9f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** A numbered instruction, for the one part of Noctorium that asks somebody to go and do something else. */
@Composable
private fun SetupStep(number: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(
            "$number.",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}
