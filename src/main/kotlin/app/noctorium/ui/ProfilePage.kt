package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.history.HistoryDay
import app.noctorium.history.HistoryEntry
import app.noctorium.history.historyByDay
import app.noctorium.history.matches
import app.noctorium.settings.HoverControls
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.AlertDialog
import app.noctorium.ui.skins.Button
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.HeadingRule
import app.noctorium.ui.skins.HorizontalDivider
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.Luna
import app.noctorium.ui.skins.OutlinedButton
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.classicScrollbar
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.skinned
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * The profile page: the name this computer's Noctorium goes by, and under it the history of what has been played
 * here -- kept on this computer and nowhere else.
 */

/**
 * The profile, then the history, as one list the page scrolls as a whole.
 *
 * A list rather than the column of cards the other pages are, because the history can be thousands of songs long
 * and a column lays every one of them out at once; a list lays out the screenful being looked at.
 */
@Composable
internal fun ProfileSettingsPanel(preferences: NoctoriumPreferences, state: AppState) {
    val history by state.history.collectAsState()
    var filter by remember { mutableStateOf("") }
    val shown = remember(history, filter) { if (filter.isBlank()) history else history.filter { it.matches(filter) } }
    val days = remember(shown) { historyByDay(shown) }
    val list = rememberLazyListState()
    LazyColumn(
        Modifier.fillMaxSize().classicScrollbar(list),
        state = list,
        contentPadding = PaddingValues(bottom = chromeBottom() + 12.dp),
    ) {
        item(key = "profile") {
            ProfileCard(preferences, state)
            Spacer(Modifier.height(12.dp))
        }
        item(key = "history") {
            HistoryCard(preferences, history.size, shown.size, filter, { filter = it }, state)
            Spacer(Modifier.height(6.dp))
        }
        if (history.isNotEmpty() && shown.isEmpty()) {
            item(key = "nothing") { HistoryNote("Nothing in your history matches “${filter.trim()}”.") }
        }
        days.forEach { day ->
            item(key = "day:${day.date}") { HistoryDayHeading(day) }
            items(day.entries, key = { it.key }) { entry -> HistoryRow(entry, state) }
        }
    }
}

/**
 * The name this computer's Noctorium goes by. Kept to a few lines, the name and its button side by side, so the
 * history under it starts on the first screen of the page.
 */
@Composable
private fun ProfileCard(preferences: NoctoriumPreferences, state: AppState) {
    var name by remember(preferences.profileName) { mutableStateOf(preferences.profileName) }
    SettingsPanelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Local profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("This name stays on this computer and identifies your Noctorium setup.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it.take(40) }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.weight(1f).tracksTyping())
            Spacer(Modifier.width(12.dp))
            Button({ state.setProfileName(name) }) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(7.dp)); Text("Save profile") }
        }
    }
}

/**
 * What the history is, the switch that stops it, how much is in it, the filter, and clearing it -- asked first,
 * since a cleared history cannot be had back.
 */
@Composable
private fun HistoryCard(
    preferences: NoctoriumPreferences,
    count: Int,
    matching: Int,
    filter: String,
    setFilter: (String) -> Unit,
    state: AppState,
) {
    var confirmClear by remember { mutableStateOf(false) }
    SettingsPanelCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardHeading(Icons.Default.History, "History")
            Spacer(Modifier.weight(1f))
            if (count > 0) {
                Text(
                    if (filter.isBlank()) songsPlayed(count) else "${"%,d".format(matching)} of ${songsPlayed(count)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Every song you listen to here, newest first. It stays on this computer: it is not sent to your Noctorium " +
                "account, your other devices or anywhere else.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        ToggleRow(
            "Keep a history of what I play",
            "A song goes in once you have heard half a minute of it. Off adds nothing more and keeps what is here.",
            preferences.keepHistory,
            change = state::setKeepHistory,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (count > 0) {
                OutlinedTextField(
                    filter,
                    setFilter,
                    placeholder = { Text("Filter by title, artist or album") },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    trailingIcon = if (filter.isEmpty()) null else {
                        { IconButton({ setFilter("") }, Modifier.size(30.dp)) { Icon(Icons.Default.Close, "Clear the filter", Modifier.size(16.dp)) } }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f).tracksTyping(),
                )
            } else {
                Text(
                    if (preferences.keepHistory) "Nothing played yet." else "Nothing here, and nothing goes in while it is off.",
                    Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(12.dp))
            SettingAnchor("Clear history") {
                OutlinedButton({ confirmClear = true }, enabled = count > 0) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Clear history…")
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear your history?") },
            text = { Text("All ${songsPlayed(count)} are forgotten, on this computer, for good. Nothing else is touched.") },
            confirmButton = { Button({ confirmClear = false; state.clearHistory() }) { Text("Clear") } },
            dismissButton = { OutlinedButton({ confirmClear = false }) { Text("Keep") } },
        )
    }
}

/** "1 song played", "1,204 songs played": a song played twice is two of them. */
private fun songsPlayed(count: Int): String = if (count == 1) "1 song played" else "${"%,d".format(count)} songs played"

/** A line said where the history's songs would be. */
@Composable
private fun HistoryNote(text: String) {
    Text(
        text,
        Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 8.dp, vertical = 18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
}

/**
 * A day's name over its songs: in small capitals in the accent, as Settings heads its sections, or bold over the
 * rule a folder's headings had under a Windows skin.
 */
@Composable
private fun HistoryDayHeading(day: HistoryDay) {
    val count = if (day.entries.size == 1) "1 song" else "${day.entries.size} songs"
    if (skinned()) {
        val xp = skin() == ThemeSkin.WINDOWS_XP
        Column(Modifier.widthIn(max = 720.dp).padding(top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(day.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (xp) Luna.GroupTitle else Color.Unspecified)
                Spacer(Modifier.width(8.dp))
                Text(count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(3.dp))
            if (xp) Box(Modifier.fillMaxWidth().height(1.dp).background(Luna.GroupEdge)) else HeadingRule()
        }
        return
    }
    Row(Modifier.widthIn(max = 720.dp).padding(start = 7.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            day.label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(count, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The time of day, as this computer writes it: 21:04, or 9:04 PM. */
private val timeOfDay: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * One listen: the cover, the song and who it is by, where it is from, and when it was played. A click plays it, as
 * a song on Home does; its menu is the song's menu everywhere, with Play first and Remove from history last. Quiet
 * until the pointer is on it, as the queue's lines are, since a history is a long list to read down.
 */
@Composable
private fun HistoryRow(entry: HistoryEntry, state: AppState) {
    val track = entry.track
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val always = state.settings.collectAsState().value.preferences.hoverControls == HoverControls.ALWAYS
    var menu by remember { mutableStateOf(false) }
    val time = remember(entry.playedAt) {
        // Newer locale data puts a narrow no-break space before AM and PM, which not every typeface has a letter for.
        Instant.ofEpochMilli(entry.playedAt).atZone(ZoneId.systemDefault()).toLocalTime().format(timeOfDay)
            .replace(' ', ' ').replace(' ', ' ')
    }
    ListRow(
        onClick = { state.playFromHome(track) },
        color = if (hovered || menu) ink(.06f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        selected = menu,
        interactionSource = interaction,
        modifier = Modifier.widthIn(max = 720.dp).hoverable(interaction),
    ) {
        Row(Modifier.fillMaxWidth().padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(42.dp).clip(skinShape(RoundedCornerShape(8.dp))))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ink(.92f))
                Text(
                    // An empty artist line means the service did not say, and is said as such.
                    track.artistLine.ifBlank { "Unknown artist" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = ink(.5f),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            ProviderBadge(track.provider)
            Text(time, Modifier.width(64.dp), color = ink(.45f), fontSize = 11.sp, textAlign = TextAlign.End, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Box {
                IconButton({ menu = true }, Modifier.size(30.dp).alpha(if (hovered || menu || always) 1f else 0f)) {
                    Icon(Icons.Default.MoreVert, "More for this song", Modifier.size(18.dp), tint = ink(.7f))
                }
                SongMenu(
                    track,
                    state,
                    menu,
                    dismiss = { menu = false },
                    first = { close ->
                        DropdownMenuItem(
                            text = { Text("Play") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                            onClick = { state.playFromHome(track); close() },
                        )
                        HorizontalDivider()
                    },
                    last = { close ->
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Remove from history") },
                            leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, null) },
                            onClick = { state.removeFromHistory(entry); close() },
                        )
                    },
                )
            }
        }
    }
}
