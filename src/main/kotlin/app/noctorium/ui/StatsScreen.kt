package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.account.AccountStats
import app.noctorium.account.StatsRange
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.domain.ProviderType
import app.noctorium.ui.skins.ScreenTitle

/** Which of the picture's two screens the Account destination shows: the account itself, or its Stats. */
internal object AccountView {
    var stats by mutableStateOf(false)
}

private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val PLAYED_AT = java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm")

private fun plays(count: Int) = if (count == 1) "1 play" else "$count plays"
private fun days(count: Int) = if (count == 1) "1 day" else "$count days"
private fun listened(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}

/**
 * Noctorium Stats inside Noctorium: the account's listens from every device, with the streaks and charts Noctorium
 * Stats has, over a week, a month, three months, a year or all of it. [load] stands in for the account in a render.
 */
@Composable
internal fun StatsScreen(state: AppState, load: (suspend (StatsRange) -> AccountStats?)? = null) {
    val signedIn = load != null || state.account.collectAsState().value.user != null
    var range by remember { mutableStateOf(StatsRange.MONTH) }
    var stats by remember { mutableStateOf<AccountStats?>(null) }
    var asked by remember { mutableStateOf(false) }
    LaunchedEffect(signedIn, range) {
        stats = null
        asked = false
        if (signedIn) stats = (load ?: state::accountStats)(range)
        asked = true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp).padding(top = 28.dp, bottom = 36.dp)) {
        ScreenTitle("Stats", 32.sp)
        Spacer(Modifier.height(14.dp))
        if (!signedIn) {
            Text(
                "Your stats come from your Noctorium account: every song played on any device signed in to it, with your streaks and charts.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            Button({ AccountView.stats = false; state.navigate(Destination.ACCOUNT) }) { Text("Sign in") }
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatsRange.entries.forEach { option ->
                FilterChip(selected = range == option, onClick = { range = option }, label = { Text(option.label) })
            }
        }
        Spacer(Modifier.height(14.dp))
        val full = stats
        when {
            full == null && !asked -> Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            full == null -> Text("Your stats could not be reached. Try again in a moment.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> Column(Modifier.widthIn(max = 1100.dp)) { Figures(full) }
        }
    }
}

@Composable
private fun Figures(full: AccountStats) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile("Listens", full.streams.toString(), Modifier.weight(1f))
        Tile("Listened", listened(full.msPlayed), Modifier.weight(1f))
        Tile("Songs", full.uniqueTracks.toString(), Modifier.weight(1f))
        Tile("Artists", full.artists.toString(), Modifier.weight(1f))
        Tile("Current streak", days(full.streak.current), Modifier.weight(1f))
        Tile("Longest streak", days(full.streak.longest), Modifier.weight(1f))
    }
    if (full.streams == 0) {
        Spacer(Modifier.height(16.dp))
        Text("Nothing played in this span yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Heading(if (full.timeline.bucket == "month") "Month by month" else "Day by day")
    Bars(full.timeline.points.map { it.minutes }, emptyList(), 140.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f)) {
            Heading("Hour of the day")
            Bars(full.hourly.map { it.minutes }, full.hourly.map { if (it.hour % 6 == 0) "${it.hour}" else "" })
        }
        Column(Modifier.weight(1f)) {
            Heading("Day of the week")
            Bars(full.weekdays.map { it.minutes }, full.weekdays.map { WEEKDAYS.getOrElse(it.day - 1) { "" } })
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(Modifier.weight(1f)) {
            Heading("Top songs")
            full.topTracks.forEachIndexed { i, song -> Ranked(i, song.title, song.artist, plays(song.streams)) }
        }
        Column(Modifier.weight(1f)) {
            Heading("Top artists")
            full.topArtists.forEachIndexed { i, artist ->
                Ranked(i, artist.artist, "${artist.tracks} ${if (artist.tracks == 1) "song" else "songs"} · ${artist.minutes} min", plays(artist.streams))
            }
        }
    }
    Heading("By service")
    full.byProvider.groupBy { AccountStats.providerName(it.provider) }.entries
        .map { (name, shares) -> Triple(name, shares.first().provider, shares.sumOf { it.streams }) }
        .sortedByDescending { it.third }
        .forEach { (name, code, count) ->
            val colour = runCatching { ProviderType.valueOf(code) }.getOrNull()?.badgeColour() ?: MaterialTheme.colorScheme.primary
            Share(name, "${count * 100 / full.streams}%", count.toFloat() / full.streams, colour)
        }
    if (full.recent.isNotEmpty()) Heading("Recently played")
    full.recent.forEach { play ->
        val at = runCatching { java.time.Instant.parse(play.playedAt).atZone(java.time.ZoneId.systemDefault()).format(PLAYED_AT) }.getOrDefault("")
        Ranked(null, play.title, "${play.artist} · ${AccountStats.providerName(play.provider)}", at)
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, Modifier.padding(top = 24.dp, bottom = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(14.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun Ranked(index: Int?, title: String, under: String, end: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (index != null) Text("${index + 1}", Modifier.width(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(under, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(end, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun Share(label: String, value: String, share: Float, colour: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row {
            Text(label, Modifier.weight(1f), fontSize = 14.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(share.coerceIn(0.02f, 1f)).fillMaxHeight().background(colour))
        }
    }
}

/** Bars as tall as their share of the tallest, side by side, with a label under each. */
@Composable
private fun Bars(values: List<Int>, labels: List<String>, height: Dp = 100.dp) {
    val most = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { value ->
            Box(
                Modifier.weight(1f).fillMaxHeight((value.toFloat() / most).coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(MaterialTheme.colorScheme.primary),
            )
        }
    }
    if (labels.any(String::isNotEmpty)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, softWrap = false) }
        }
    }
}
