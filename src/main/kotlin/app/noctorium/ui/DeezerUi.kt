package app.noctorium.ui

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.settings.DeezerConnectionState
import app.noctorium.settings.HomePart
import app.noctorium.settings.SettingsState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.icons.markVector
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.scrollingPage

/*
 * Deezer's page in Settings.
 *
 * Deezer, like Bandcamp, has nothing to sign in to: its catalogue is open to anybody who asks, and a listener's
 * profile -- its public playlists, loved tracks and albums -- is public under the number in its address. So its
 * page is that profile rather than a sign-in, and modelled on Bandcamp's. What it cannot do is play: Deezer's audio
 * is for Deezer's own apps, so every song is matched to the same recording on YouTube Music, as Spotify's are when
 * they are not played on Spotify, and the page says so before anything else.
 */

// --- Decisions ---

/** Deezer's own purple, as on its badge. */
private val DEEZER_PURPLE = Color(0xFFA238FF)

/**
 * Which profile was found, in words, or null when none is kept: its own name, as Deezer gave it, and its number,
 * which is what the box holds once a link pasted into it has been understood.
 */
internal fun deezerProfileLine(deezer: DeezerConnectionState): String? = when {
    deezer.profileId.isBlank() -> null
    deezer.name.isNotBlank() -> "Found on Deezer: ${deezer.name}, profile ${deezer.profileId}"
    else -> "Deezer profile ${deezer.profileId}"
}

/** The line under Deezer on the Settings list. */
internal fun deezerSummary(deezer: DeezerConnectionState): String = when {
    deezer.checking -> "Looking for the profile on Deezer…"
    deezer.found && deezer.name.isNotBlank() -> "Showing ${deezer.name}'s library"
    deezer.profileId.isNotBlank() -> "Showing profile ${deezer.profileId}"
    else -> "No sign-in needed — add your profile to see your library"
}

// --- The Settings page ---

/**
 * Deezer, set up: what it gives and how its songs play, then the profile whose library is shown.
 *
 * The profile is checked with Deezer before it is kept, so the button says when it is being checked, and under it
 * is whose profile was found, or why none was. A link pasted into the box -- the address bar's, or the share
 * button's -- becomes the number at the end of it once Deezer has answered, so it is seen to have been understood.
 */
@Composable
internal fun DeezerSettingsPanel(settings: SettingsState, state: AppState) {
    val deezer = settings.deezer
    val saved = deezer.profileId
    // Put back to the number Deezer answered to whenever a look succeeds; a failed one leaves what was typed, to be
    // corrected. As on Bandcamp's page.
    var profile by remember(saved) { mutableStateOf(saved) }
    LaunchedEffect(deezer.checking) { if (!deezer.checking && deezer.found) profile = saved }

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ProviderType.DEEZER.markVector(), null, Modifier.size(40.dp), tint = DEEZER_PURPLE)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (deezer.found) deezer.name.ifBlank { "Deezer" } else "Deezer",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Search, albums, artists and charts from Deezer, with nothing to sign in to",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Deezer can be searched on its own, and its albums, artists, playlists and links open here, with no account.")
            AccountFact("With your profile, its public playlists, loved tracks and albums are in your library, and Home gets Deezer's charts and editors' picks.")
            AccountFact(
                "Deezer's audio only plays in Deezer's own apps, so each song is matched to the same recording on " +
                    "YouTube Music and played from there. A song that cannot be found is reported rather than swapped for another.",
            )
            AccountFact("Nothing is ever written to Deezer, and Deezer songs can't be liked from here.")
        }

        SettingsPanelCard {
            SettingAnchor("Your Deezer profile") { Text("Your Deezer profile", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(6.dp))
            Text(
                "No sign-in is needed. Paste the link to your profile, deezer.com/profile/<number> or the link its " +
                    "Share button copies, or just the number. Its playlists and loved tracks are read only where " +
                    "they are public on Deezer. Leave it blank for none.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(
                profile,
                { profile = it.take(300) },
                label = { Text("Deezer profile") },
                placeholder = { Text("https://www.deezer.com/profile/1234567, or the number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tracksTyping(),
            )
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                // Blank and saved is the same as Remove, which is what "leave it blank for none" promises.
                Button({ state.setDeezerProfile(profile) }, enabled = !deezer.checking && profile.trim() != saved) {
                    if (deezer.checking) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (deezer.checking) "Checking…" else "Save")
                }
                if (saved.isNotBlank()) {
                    OutlinedButton({ profile = ""; state.setDeezerProfile("") }, enabled = !deezer.checking) { Text("Remove") }
                }
            }
            // Not while another profile is being looked for: the old one is still in force, but saying so under
            // "Checking…" would read as the answer.
            deezerProfileLine(deezer)?.takeIf { !deezer.checking }?.let { line ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(line, fontSize = 12.sp)
                }
            }
            deezer.message?.let { message ->
                Spacer(Modifier.height(11.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                    }
                }
            }
            // Given and then nowhere to be seen on Home is a puzzle; this says where its rows went.
            if (saved.isNotBlank() && HomePart.DEEZER in settings.preferences.hiddenHomeParts) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deezer's rows are put away under Look › Home and browsing, so Home shows them once they are back.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
