package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.core.LinkState
import app.noctorium.core.LinkStatus
import app.noctorium.domain.LinkKind
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.playlists.LocalPlaylist
import app.noctorium.playlists.PlaylistImport
import app.noctorium.settings.SpotifyConnectionState
import app.noctorium.settings.SpotifyDataImportState
import app.noctorium.settings.ThemeSkin
import app.noctorium.spotify.SPOTIFY_DATA_EXPORT_HOW
import app.noctorium.spotify.SPOTIFY_DATA_EXPORT_LIMITS
import app.noctorium.spotify.SPOTIFY_CUT_NOTE
import app.noctorium.spotify.SPOTIFY_PUBLIC_NOTE
import app.noctorium.spotify.SPOTIFY_PUBLIC_PLAYLIST_LIMITS
import app.noctorium.spotify.SpotifyPublicPlaylistState
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.Classic
import app.noctorium.ui.skins.InfoBar
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.listWell
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.skinned

/*
 * Spotify with nobody signed in: following public playlists by their links, and importing Spotify's own data
 * download. The two halves of Spotify's page in Settings below the sign-in, the offer to keep a public playlist
 * opened from a pasted link, and the decisions they make.
 *
 * Both are for the listener whose sign-in does not work -- which is most of them while Spotify answers its Web API
 * only for an app whose owner has Premium. A public playlist is read from the page Spotify shows anybody, and only
 * as far as its first hundred songs; the download is everything the listener made, but as it was on the day
 * Spotify made the file. Each says so beside its controls, in the core's own words, which every player shows the
 * same -- and the playlists' limits as a warning, since following one and finding it cut short is the surprise.
 */

// --- Decisions ---

/** The heading of the part of Spotify's page that follows public playlists, and its place for the settings search. */
internal const val SPOTIFY_PUBLIC_PLAYLISTS = "Public playlists, without signing in"

/** The heading of the part that imports Spotify's data download, and its place for the settings search. */
internal const val SPOTIFY_DATA_IMPORT = "Import from Spotify's data download"

/** A followed public playlist's name: its own once it has been read, and its Spotify id until then. */
internal fun SpotifyPublicPlaylistState.shownTitle(): String = title.ifBlank { id }

/**
 * What is said under a followed public playlist's name: whose it is and how many songs it had when last read, or
 * that it has not been read yet. Blank when there is nothing to say but the problem, which is said under it.
 * That the page stopped at a hundred is said apart, on a tag of its own, where it is seen.
 */
internal fun spotifyPublicLine(playlist: SpotifyPublicPlaylistState): String = listOfNotNull(
    playlist.owner.takeIf(String::isNotBlank)?.let { "by $it" },
    playlist.songCount?.let { songs -> counted(songs, "song") ?: "no songs" } ?: "Not read yet".takeIf { playlist.lastError == null },
).joinToString(" · ")

/** The tag beside a followed playlist that Spotify's public page showed as much of as it ever shows. */
internal const val SPOTIFY_FIRST_HUNDRED_TAG = "First 100 songs only"

/**
 * What there is of Spotify with nobody signed in, for the line under it on the Settings list: "3 public
 * playlists · 12 imported playlists". Null for neither.
 */
internal fun spotifyWithoutSignInLine(followed: Int, imported: Int): String? =
    listOfNotNull(counted(followed, "public playlist"), counted(imported, "imported playlist"))
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

/** How many of the playlists made here an import of Spotify's data download made: Liked Songs among them. */
internal fun spotifyImportedCount(playlists: List<LocalPlaylist>): Int =
    playlists.count { it.importedFrom?.source == PlaylistImport.SPOTIFY_DATA }

/**
 * What the last import came to, to say under its button: why it did not happen, or what it made. Null while one
 * is running, which says how far it has got instead, and before there has been one.
 */
internal fun spotifyImportOutcome(import: SpotifyDataImportState): String? =
    if (import.importing) null else import.message ?: import.summary?.message

/** Whether the file chooser offers [name]: Spotify's .zip as it comes, or one of the .json files inside it. */
internal fun isSpotifyDataFileName(name: String): Boolean =
    name.endsWith(".zip", ignoreCase = true) || name.endsWith(".json", ignoreCase = true)

/** What the screen of a pasted link says about keeping a public Spotify playlist in the library. */
internal enum class KeepOffer {
    /** "Keep in library": a public playlist read from its page, not followed yet. */
    OFFERED,

    /** Being read again, to be kept. */
    KEEPING,

    /** In the library already, followed now or before. */
    KEPT,
}

/**
 * Whether the screen of [link] offers to keep it in the library, or says it is kept -- null for every link that is
 * not a public Spotify playlist. Offered only where the core says it can be ([LinkState.canKeepInLibrary]): a
 * playlist read from its public page, which an album never is, and read through a working sign-in never needs to be.
 */
internal fun keepOffer(link: LinkState, spotify: SpotifyConnectionState): KeepOffer? {
    val found = link.link ?: return null
    if (link.status != LinkStatus.DONE) return null
    return when {
        link.canKeepInLibrary -> if (spotify.addingPublicPlaylist) KeepOffer.KEEPING else KeepOffer.OFFERED
        found.provider == ProviderType.SPOTIFY && found.kind == LinkKind.PLAYLIST && spotify.publicPlaylists.any { it.id == found.id } -> KeepOffer.KEPT
        else -> null
    }
}

/** "1 song", "42 songs": [count] of [thing], or null for none. */
private fun counted(count: Int, thing: String): String? = when (count) {
    0 -> null
    1 -> "1 $thing"
    else -> "$count ${thing}s"
}

/** Spotify's own privacy settings, where "Download your data" is asked for. */
private const val SPOTIFY_PRIVACY_PAGE = "https://www.spotify.com/account/privacy/"

/**
 * Spotify's data download, chosen in the system's own Open dialog: the .zip, or a .json from inside it. Null when
 * the dialog was closed without one.
 */
private fun chooseSpotifyDataFile(): java.nio.file.Path? =
    chooseFile("Choose your Spotify data download", "*.zip;*.json", ::isSpotifyDataFileName)?.toPath()

// --- The Settings page ---

/**
 * Public Spotify playlists, followed by their links with nobody signed in: the warning of what that can and cannot
 * do, the box their links go in, and the ones followed so far.
 *
 * The warning goes first, above the box, so nobody pastes a link before knowing it will be cut at a hundred songs.
 * Each followed playlist says what it was when last read and, in the error's colour, why the last read did not
 * work when it did not; it opens in the library, and Remove stops following it, which changes nothing at Spotify.
 */
@Composable
internal fun SpotifyPublicPlaylistsCard(spotify: SpotifyConnectionState, state: AppState) {
    val library by state.library.collectAsState()
    val followed = spotify.publicPlaylists
    var link by remember { mutableStateOf("") }
    // Emptied once what it held has been followed, so the next one can be pasted straight in; a link that was turned
    // down stays, to be put right.
    var known by remember { mutableIntStateOf(followed.size) }
    LaunchedEffect(followed.size) {
        if (followed.size > known) link = ""
        known = followed.size
    }

    SettingsPanelCard {
        SettingAnchor(SPOTIFY_PUBLIC_PLAYLISTS) { Text(SPOTIFY_PUBLIC_PLAYLISTS, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste a playlist's link: in Spotify, choose Share, then Copy link to playlist. It joins your library with " +
                "Spotify's playlists, and is read again from Spotify every time the library is, so it stays in step.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        SpotifyLimitsNotice(
            Icons.Default.WarningAmber,
            "Public playlists only, and only their first 100 songs",
            SPOTIFY_PUBLIC_PLAYLIST_LIMITS,
        )
        Spacer(Modifier.height(14.dp))
        SpotifyLinkBox(link, { link = it.take(300) }, adding = spotify.addingPublicPlaylist) {
            state.addSpotifyPublicPlaylist(link.trim())
        }
        spotify.publicMessage?.takeIf { !spotify.addingPublicPlaylist }?.let { message ->
            Spacer(Modifier.height(11.dp))
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .6f), shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer(Modifier.weight(1f)) { Text(message, fontSize = 11.sp) }
                }
            }
        }
        if (followed.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "In your library",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            // A list under a Windows skin, sunk into the group box with its lines highlighted under the pointer; tiles
            // a little apart otherwise.
            Column(
                Modifier.fillMaxWidth().listWell(),
                verticalArrangement = Arrangement.spacedBy(if (skinned()) 0.dp else 7.dp),
            ) {
                followed.forEach { playlist ->
                    FollowedPlaylistRow(
                        playlist,
                        open = {
                            // The library's own entry where it has one, which says whose it is the way the library does.
                            val listed = library.playlists.firstOrNull { it.provider == ProviderType.SPOTIFY && it.id == playlist.id }
                            state.openPlaylist(listed ?: playlist.asLibraryPlaylist())
                            state.navigate(Destination.LIBRARY)
                        },
                        remove = { state.removeSpotifyPublicPlaylist(playlist.id) },
                    )
                }
            }
        }
    }
}

/**
 * The box a public playlist's link is pasted into, and Add under it -- or Enter in the box. Spinning, and saying it
 * is reading the playlist, while Spotify's page is read: nothing is kept until it has been.
 */
@Composable
internal fun SpotifyLinkBox(link: String, onLink: (String) -> Unit, adding: Boolean, add: () -> Unit) {
    val canAdd = link.isNotBlank() && !adding
    OutlinedTextField(
        link,
        onLink,
        label = { Text("Playlist link") },
        placeholder = { Text("https://open.spotify.com/playlist/…") },
        leadingIcon = { Icon(Icons.Default.Link, null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { if (canAdd) add() }),
        modifier = Modifier.fillMaxWidth().tracksTyping(),
    )
    Spacer(Modifier.height(11.dp))
    Button(add, enabled = canAdd) {
        if (adding) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Add, null, Modifier.size(17.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(if (adding) "Reading the playlist…" else "Add")
    }
}

/** One followed public playlist: its cover, its name, what it was when last read, and Remove. */
@Composable
private fun FollowedPlaylistRow(playlist: SpotifyPublicPlaylistState, open: () -> Unit, remove: () -> Unit) {
    val line = spotifyPublicLine(playlist)
    ListRow(open, shape = RoundedCornerShape(12.dp), color = ink(.04f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = if (skinned()) 4.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteArtwork(playlist.artworkUrl, ProviderType.SPOTIFY, Modifier.size(40.dp).clip(skinShape(RoundedCornerShape(8.dp))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        playlist.shownTitle(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (playlist.mayBeLonger) {
                        Spacer(Modifier.width(8.dp))
                        FirstHundredTag()
                    }
                }
                if (line.isNotBlank()) {
                    Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                playlist.lastError?.let { problem ->
                    Spacer(Modifier.height(2.dp))
                    Text(problem, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            TextButton(remove) { Text("Remove", fontSize = 12.sp) }
        }
    }
}

/**
 * "First 100 songs only", beside a playlist Spotify's page showed as much of as it shows: in the warning's colour,
 * since it is that warning come true for this one.
 */
@Composable
private fun FirstHundredTag() {
    val tint = warningTint()
    Text(
        SPOTIFY_FIRST_HUNDRED_TAG,
        color = tint,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(skinShape(RoundedCornerShape(6.dp)))
            .background(tint.copy(alpha = .14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * The playlist as the library would list it, for opening one the library has not listed yet: as the core lists a
 * followed public playlist, owner, note and all.
 */
private fun SpotifyPublicPlaylistState.asLibraryPlaylist(): Playlist = Playlist(
    id = id,
    title = shownTitle(),
    provider = ProviderType.SPOTIFY,
    ownerName = listOfNotNull(
        owner.takeIf(String::isNotBlank),
        SPOTIFY_PUBLIC_NOTE,
        SPOTIFY_CUT_NOTE.takeIf { mayBeLonger },
    ).joinToString(" · "),
    artworkUrl = artworkUrl,
    sourceUrl = url,
    trackCount = songCount,
    isPublic = true,
    ownedByListener = false,
)

/**
 * Spotify's data download, imported: how to get it, what the import leaves out, the button that asks for the file,
 * and what came of it -- how far it has got while it runs, then what it made or why it could not.
 */
@Composable
internal fun SpotifyImportCard(spotify: SpotifyConnectionState, state: AppState) {
    val import = spotify.dataImport
    SettingsPanelCard {
        SettingAnchor(SPOTIFY_DATA_IMPORT) { Text(SPOTIFY_DATA_IMPORT, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(6.dp))
        Text(
            "Every playlist you made and your Liked Songs, as playlists of Noctorium's own, with no sign-in at all.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(SPOTIFY_DATA_EXPORT_HOW, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        SpotifyLimitsNotice(Icons.Default.Info, "A copy, as it was when Spotify made the file", SPOTIFY_DATA_EXPORT_LIMITS)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            // Resting while a file is imported, which says how far it has got under it: one spinner, not two.
            Button({ chooseSpotifyDataFile()?.let { state.importSpotifyData(it) } }, enabled = !import.importing) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Choose file…")
            }
            OutlinedButton({ state.openExternalUrl(SPOTIFY_PRIVACY_PAGE) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Open Spotify's privacy settings")
            }
        }
        if (import.importing) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text(import.progress ?: "Importing…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        spotifyImportOutcome(import)?.let { outcome ->
            Spacer(Modifier.height(12.dp))
            ImportOutcome(outcome, import.failed)
        }
    }
}

/** What the last import came to: what it made, with a tick, or why it did not happen, in the error's colour. */
@Composable
private fun ImportOutcome(message: String, failed: Boolean) {
    val tint = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = if (failed) MaterialTheme.colorScheme.errorContainer.copy(alpha = .3f) else MaterialTheme.colorScheme.surface.copy(alpha = .6f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (failed) Icons.Default.ErrorOutline else Icons.Default.CheckCircle, null, Modifier.size(17.dp), tint = tint)
            Spacer(Modifier.width(9.dp))
            SelectionContainer(Modifier.weight(1f)) {
                Text(message, fontSize = 12.sp, color = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified)
            }
        }
    }
}

/**
 * What one of the two ways in without a sign-in can and cannot do, said beside its controls: [heading] in bold and
 * the core's own words under it, with [icon].
 *
 * In the warning's colour, as the beta warning above the page is, and told apart from it by where it is -- inside the
 * part of the page it is about, not across the top -- and by its heading. Under a Windows skin the beta warning is
 * the information bar; this is how a dialog of that desktop warned instead, its sign large beside its words.
 */
@Composable
internal fun SpotifyLimitsNotice(icon: ImageVector, heading: String, text: String) {
    val tint = warningTint()
    if (skinned()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Icon(icon, null, Modifier.size(32.dp), tint = tint)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(heading, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                SelectionContainer { Text(text, fontSize = 12.sp) }
            }
        }
        return
    }
    Surface(
        color = tint.copy(alpha = .09f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = .4f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp)) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(heading, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = tint)
                Spacer(Modifier.height(3.dp))
                SelectionContainer { Text(text, fontSize = 12.sp) }
            }
        }
    }
}

/**
 * The warning's colour: Material's tertiary, as the beta warning has it, and under a Windows skin the dark amber of
 * its sign there -- lifted on Noctorium 98's dark face, where that amber would all but disappear.
 */
@Composable
private fun warningTint(): Color = when {
    !skinned() -> MaterialTheme.colorScheme.tertiary
    skin() == ThemeSkin.WINDOWS_98 && Classic.palette.dark -> Color(0xFFF0B44A)
    else -> CLASSIC_WARNING_SIGN
}

// --- A pasted link ---

/**
 * What the screen of a pasted public Spotify playlist says about keeping it: Keep in library, while it is offered,
 * and once it is kept, that it is -- with what went wrong, should the page not read a second time.
 */
@Composable
internal fun KeepInLibraryOffer(link: LinkState, spotify: SpotifyConnectionState, state: AppState) {
    val offer = keepOffer(link, spotify) ?: return
    // Whether Keep was pressed here, so what the core said back is shown here only then: the same words are
    // Settings' too, about whatever was added or removed there last.
    var asked by remember(link.link) { mutableStateOf(false) }
    if (offer == KeepOffer.KEPT) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("In your library with Spotify's playlists, and kept in step with Spotify.", fontSize = 13.sp)
        }
        return
    }
    val keep: () -> Unit = {
        asked = true
        link.link?.url?.let(state::addSpotifyPublicPlaylist)
    }
    val text = "A public Spotify playlist. Keep it in your library, and it stays in step with Spotify, read again " +
        "every time the library is."
    val button: @Composable () -> Unit = {
        Button(keep, enabled = offer == KeepOffer.OFFERED) {
            if (offer == KeepOffer.KEEPING) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Add, null, Modifier.size(17.dp))
            }
            Spacer(Modifier.width(7.dp))
            Text(if (offer == KeepOffer.KEEPING) "Keeping…" else "Keep in library")
        }
    }
    Column {
        // The information bar under a Windows skin, with its button at the end, as the clipboard's offer is.
        if (skinned()) {
            InfoBar(Icons.AutoMirrored.Filled.PlaylistAdd, text, MaterialTheme.colorScheme.primary, action = button)
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(text, Modifier.weight(1f), fontSize = 13.sp)
                    Spacer(Modifier.width(12.dp))
                    button()
                }
            }
        }
        spotify.publicMessage?.takeIf { asked && offer == KeepOffer.OFFERED }?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
    }
}
