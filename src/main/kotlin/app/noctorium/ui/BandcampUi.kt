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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import app.noctorium.ui.skins.scrollingPage
import app.noctorium.ui.skins.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import androidx.compose.material3.Surface
import app.noctorium.ui.skins.Text
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
import app.noctorium.bandcamp.BandcampGenre
import app.noctorium.core.AppState
import app.noctorium.settings.BandcampConnectionState
import app.noctorium.settings.HomePart
import app.noctorium.settings.SettingsState

/*
 * Bandcamp's page in Settings, and the few decisions it makes.
 *
 * Bandcamp is the one service in Noctorium with nothing to sign in to: search, Home and playing are served to
 * whoever asks, and a fan's collection is public under the name in their address. So its page is a name and
 * a choice of genres rather than a sign-in, and it says so first, before anybody goes looking for a password
 * box. The settings themselves are the core's, shared with the phone; the decisions are plain functions at
 * the top, so they can be tested without a window.
 */

// --- Decisions ---

/**
 * Whose collection the library shows, in words, or null when it shows none.
 *
 * The fan's own name once Bandcamp has confirmed it, and the address before then -- which is every launch,
 * since only the name in the address is kept.
 */
internal fun bandcampCollectionLine(username: String, fanName: String): String? = when {
    username.isBlank() -> null
    fanName.isNotBlank() -> "Showing the collection of $fanName"
    else -> "Showing the collection at bandcamp.com/$username"
}

/** The line under Bandcamp on the Settings list. */
internal fun bandcampSummary(username: String, bandcamp: BandcampConnectionState): String =
    if (bandcamp.checking) {
        "Checking the name with Bandcamp…"
    } else {
        bandcampCollectionLine(username, bandcamp.fanName) ?: "No sign-in needed — add your name to see your collection"
    }

/**
 * The genres after one is switched on or off.
 *
 * Home draws their rows in this order, so the order is the listener's and is kept: the others stay where they
 * were, and one switched on goes after them.
 */
internal fun List<BandcampGenre>.withGenre(genre: BandcampGenre, on: Boolean): List<BandcampGenre> = when {
    !on -> this - genre
    genre in this -> this
    else -> this + genre
}

/** Said under the genres: how the numbers on them work, or what Home keeps when none is picked. */
internal fun genreRowsLine(genres: List<BandcampGenre>): String =
    if (genres.isEmpty()) {
        "None picked. Bandcamp's best-selling songs and its new releases are still on Home."
    } else {
        "Numbered in the order Home shows them. One switched off and on again goes to the end."
    }

/** Bandcamp's own blue-green, as Spotify's page wears Spotify's green: the badge's colour, lit for a dark page. */
private val BANDCAMP_TEAL = Color(0xFF629AA9)

// --- Drawing ---

/**
 * Bandcamp, set up: the name whose collection the library shows, and the genres Home has rows for.
 *
 * Modelled on the Spotify page and as plain about what this is. The name is checked with Bandcamp before it
 * is kept, so the button says when it is being checked, and under it is whose collection was found, or why
 * none was.
 */
@Composable
internal fun BandcampSettingsPanel(settings: SettingsState, state: AppState) {
    val bandcamp = settings.bandcamp
    val saved = settings.preferences.bandcampUsername
    val genres = settings.preferences.bandcampGenres
    // Put back to the name Bandcamp answered to whenever a check succeeds -- the same name again included -- so
    // a whole address pasted into the box is seen to become the name at the end of it. A failed check leaves
    // what was typed, to be corrected.
    var name by remember(saved) { mutableStateOf(saved) }
    LaunchedEffect(bandcamp.checking) { if (!bandcamp.checking && bandcamp.message == null) name = saved }

    Column(
        Modifier.fillMaxSize().scrollingPage().padding(bottom = chromeBottom()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsPanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Album, null, Modifier.size(40.dp), tint = BANDCAMP_TEAL)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (saved.isBlank()) "Bandcamp" else bandcamp.fanName.ifBlank { saved },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Albums, artists and songs from Bandcamp, with nothing to sign in to",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            AccountFact("Search, Home and playing need no account. Bandcamp serves them to everybody.")
            AccountFact("Songs play as the stream Bandcamp's own pages carry. The few an artist keeps for buyers do not play.")
            AccountFact("Bandcamp songs are bought, not downloaded. A song's menu opens its Bandcamp page, where it can be bought.")
            AccountFact("Nothing is ever written to Bandcamp.")
        }

        SettingsPanelCard {
            Text("Your collection", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "No sign-in is needed. Give your Bandcamp name, the end of bandcamp.com/<name>, to see your " +
                    "collection and wishlist in the library. Leave it blank for none.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(
                name,
                { name = it.take(200) },
                label = { Text("Bandcamp name") },
                placeholder = { Text("your-name, or your whole Bandcamp address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tracksTyping(),
            )
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                // Blank and saved is the same as Remove, which is what "leave it blank for none" promises.
                Button({ state.setBandcampUsername(name) }, enabled = !bandcamp.checking && name.trim() != saved) {
                    if (bandcamp.checking) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (bandcamp.checking) "Checking…" else "Save")
                }
                if (saved.isNotBlank()) {
                    OutlinedButton({ state.setBandcampUsername("") }, enabled = !bandcamp.checking) { Text("Remove") }
                }
            }
            // Not while a new name is being asked about: the old one is still in force, but saying so under
            // "Checking…" would read as the answer.
            bandcampCollectionLine(saved, bandcamp.fanName)?.takeIf { !bandcamp.checking }?.let { line ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(line, fontSize = 12.sp)
                }
            }
            bandcamp.message?.let { message ->
                Spacer(Modifier.height(11.dp))
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                    }
                }
            }
        }

        SettingsPanelCard {
            Text("Genres on Home", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Each genre picked adds a row of its best-sellers to Home.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(13.dp))
            ToggleChips(
                label = null,
                options = BandcampGenre.entries,
                on = { it in genres },
                name = { it.displayName },
                mark = { genre -> (genres.indexOf(genre) + 1).takeIf { it > 0 }?.toString() },
            ) { genre, on -> state.setBandcampGenres(genres.withGenre(genre, on)) }
            Spacer(Modifier.height(8.dp))
            Text(genreRowsLine(genres), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            // Chosen here and then nowhere to be seen is a puzzle; this says where they went.
            if (HomePart.BANDCAMP in settings.preferences.hiddenHomeParts) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bandcamp's rows are put away under Customization, so these show once they are back.",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
