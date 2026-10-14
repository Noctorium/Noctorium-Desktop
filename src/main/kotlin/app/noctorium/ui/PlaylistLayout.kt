package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.domain.PlaybackOrigin
import app.noctorium.domain.Track
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.QueueColumn
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.VisualiserStyle
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.Bevel
import app.noctorium.ui.skins.Classic
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.Luna
import app.noctorium.ui.skins.PanelTabs
import app.noctorium.ui.skins.PanelWindow
import app.noctorium.ui.skins.SkinCover
import app.noctorium.ui.skins.SongWindow
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.bevel
import app.noctorium.ui.skins.classicScrollbar
import app.noctorium.ui.skins.listWell
import app.noctorium.ui.skins.pixel
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.skinned
import app.noctorium.ui.skins.thinSunken
import app.noctorium.ui.skins.towards

/*
 * The Playlist layout: now playing as a desktop music player's window.
 *
 * The cover in a panel of its own on the left, while the window is wide enough for one; the queue as a table, with
 * the columns the listener chose and tabs along its top -- the queue, the playlist it came from while that is open,
 * and autoplay's songs once there are some; an info bar under it with the cover small, the title large, the artist
 * and the album, and the visualiser; and a status bar along the foot, saying what the stream is on the left and how
 * far into the whole queue the music has got on the right.
 *
 * Its transport is the player bar's: the layout is the window around it, as the player it is drawn after had its
 * toolbar above the playlist. Under a Windows skin each part stands in a window of its own on the desktop, the
 * table under property-sheet tabs and with that desktop's column headings, and the status bar is that desktop's.
 */

// --- Decisions ---

/** How much of a row a column takes: a fixed width, or a share of what the fixed ones leave. */
internal sealed interface ColumnWidth {
    data class Fixed(val dp: Int) : ColumnWidth

    /** A share of the room left, and the least it can be squeezed to before it is dropped instead. */
    data class Share(val weight: Float, val leastDp: Int) : ColumnWidth
}

internal fun QueueColumn.width(): ColumnWidth = when (this) {
    QueueColumn.NUMBER -> ColumnWidth.Fixed(40)
    QueueColumn.TITLE -> ColumnWidth.Share(2.2f, 140)
    QueueColumn.ARTIST -> ColumnWidth.Share(1.4f, 90)
    QueueColumn.ALBUM -> ColumnWidth.Share(1.4f, 90)
    QueueColumn.DURATION -> ColumnWidth.Fixed(64)
    QueueColumn.SERVICE -> ColumnWidth.Fixed(104)
}

/** What [columns] need at the least, in points: their fixed widths, and the narrowest each share may be. */
internal fun leastWidth(columns: List<QueueColumn>): Int = columns.sumOf { column ->
    when (val width = column.width()) {
        is ColumnWidth.Fixed -> width.dp
        is ColumnWidth.Share -> width.leastDp
    }
}

/** The columns given up first when a table is too narrow for all the chosen ones: the title is never given up. */
private val GIVEN_UP_FIRST = listOf(QueueColumn.SERVICE, QueueColumn.ALBUM, QueueColumn.NUMBER, QueueColumn.DURATION, QueueColumn.ARTIST)

/**
 * The columns a table [widthDp] wide draws, of the [chosen] ones in their order: each once, the title always (see
 * [QueueColumn.normalised]), and as many of the rest as fit, the least needed going first.
 */
internal fun queueColumnsThatFit(chosen: List<QueueColumn>, widthDp: Float): List<QueueColumn> {
    val columns = QueueColumn.normalised(chosen).toMutableList()
    for (column in GIVEN_UP_FIRST) {
        if (leastWidth(columns) <= widthDp) break
        columns.remove(column)
    }
    return columns
}

/**
 * The status bar's left half: what the stream is, as the player says, or why it cannot say -- which, while the song
 * plays on another device ([elsewhere], that device's name), is that it is there: this computer has no stream of it.
 */
internal fun statusBarFormat(playback: PlaybackState, onSpotify: Boolean, elsewhere: String? = null): String = when {
    elsewhere != null -> "Playing on $elsewhere"
    onSpotify -> "Playing in Spotify's own app"
    playback.status == PlaybackStatus.RESOLVING -> "Finding the song…"
    playback.track == null -> "Stopped"
    else -> playback.format?.describe() ?: ""
}

/** The status bar's right half: how far into the whole queue, of how long the whole queue is. "12:40 / 1:03:41". */
internal fun statusBarTimes(queue: QueueState, positionMs: Long): String {
    val total = queue.durationMs
    if (total <= 0L) return "${formatPlaybackTime(positionMs)} / $UNKNOWN_PLAYBACK_TIME"
    return "${formatPlaybackTime(queue.elapsedMs(positionMs).coerceAtMost(total))} / ${formatPlaybackTime(total)}"
}

/** One of the table's tabs: the songs it lists, what its name says, and what choosing one of them does. */
private data class TableTab(val key: String, val label: String, val tracks: List<Track>, val playing: Int, val play: (Int) -> Unit)

/** The tab that holds the song's lyrics rather than a table of songs. */
private const val LYRICS_TAB = "lyrics"

// --- The layout ---

@Composable
internal fun PlaylistNowPlaying(queue: QueueState, track: Track, playback: PlaybackState, state: AppState, arrange: () -> Unit) {
    val preferences = state.settings.collectAsState().value.preferences
    val skinned = skinned()
    val gap = if (skinned) 6.dp else 14.dp
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(if (skinned) 6.dp else 18.dp)) {
            // The cover's panel only where the table keeps room enough beside it.
            val panel = maxWidth >= 900.dp
            val panelWidth = (maxWidth * .27f).coerceIn(230.dp, 360.dp)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                if (panel) CoverPanel(track, playback, Modifier.width(panelWidth).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    QueueTablePanel(queue, track, state, Modifier.weight(1f).fillMaxWidth())
                    InfoBar(track, playback, state, arrange, Modifier.fillMaxWidth())
                }
            }
        }
        if (preferences.statusBar) StatusBar(queue, playback, state)
    }
}

/**
 * The cover, as large as the panel is wide, in a panel of its own -- a window titled Album Art under a Windows skin --
 * and under it what is known about the song, as a desktop player's song information has it.
 */
@Composable
private fun CoverPanel(track: Track, playback: PlaybackState, modifier: Modifier) {
    if (skinned()) {
        PanelWindow(modifier, "Album Art") {
            Column(Modifier.fillMaxSize().padding(8.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { SkinCover(track, maxWidth - 8.dp) }
                Spacer(Modifier.height(10.dp))
                SongDetails(track, playback)
            }
        }
        return
    }
    LayoutPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            PanelHeading("Album art")
            Spacer(Modifier.height(10.dp))
            MotionContent(track, kind = MotionKind.TRACK, contentKey = { it.queueKey }) { shown ->
                RemoteArtwork(shown.artworkUrl, shown.provider, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            }
            Spacer(Modifier.height(18.dp))
            PanelHeading("Song info")
            Spacer(Modifier.height(8.dp))
            SongDetails(track, playback)
        }
    }
}

/** What the song is, a line each: its title, artist, album, length, service and stream, those that are known. */
@Composable
private fun SongDetails(track: Track, playback: PlaybackState) {
    val details = songDetails(track, playback)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        details.forEach { (label, value) ->
            Row {
                Text(label, color = ink(.5f), fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(62.dp))
                Text(value, color = ink(.86f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** The song information's lines, as label and value, leaving out what is not known. */
internal fun songDetails(track: Track, playback: PlaybackState): List<Pair<String, String>> = listOfNotNull(
    "Title" to track.title,
    track.artistLine.takeIf { it.isNotBlank() }?.let { "Artist" to it },
    track.album?.title?.takeIf { it.isNotBlank() }?.let { "Album" to it },
    track.durationMs?.takeIf { it > 0 }?.let { "Length" to formatPlaybackTime(it) },
    "Service" to track.provider.displayName,
    playback.format?.takeIf { playback.track?.queueKey == track.queueKey }?.describe()?.let { "Stream" to it },
)

/** A panel of the layout, letting the backdrop show through it as the now playing screen's panels do. */
@Composable
private fun LayoutPanel(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = ink(.055f),
        border = BorderStroke(1.dp, ink(.08f)),
        content = content,
    )
}

/** The small name at the top of a panel, as a docked panel of a desktop player is captioned. */
@Composable
private fun PanelHeading(text: String) {
    Text(text, color = ink(.55f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

/**
 * The table under its tabs: the queue, the song's lyrics, the playlist the queue came from while it is open in the
 * library, and autoplay's songs once some are lined up. Above the queue's own table, what can be done to the queue:
 * stop after this song, clear all but this song, and clear.
 *
 * Lyrics come second, after the queue and before the two that come and go, so the tab is always in the same place.
 * It is the lyrics the now playing screen's other layouts have, sources and all, for [track] -- the song shown,
 * which keeps time with a song playing on another device too.
 */
@Composable
private fun QueueTablePanel(queue: QueueState, track: Track, state: AppState, modifier: Modifier) {
    val library by state.library.collectAsState()
    val current = queue.current
    val tabs = buildList {
        add(TableTab("queue", "Queue (${queue.tracks.size})", queue.tracks, queue.currentIndex) { state.jumpToQueueItem(it) })
        add(TableTab(LYRICS_TAB, "Lyrics", emptyList(), -1) {})
        // The playlist the song came from, while it is open: the library's own, or one on a service.
        val local = library.openLocalPlaylist?.takeIf { list -> current != null && list.tracks.any { it.queueKey == current.queueKey } }
        val remote = library.openPlaylist?.takeIf { list -> current != null && list.tracks.any { it.queueKey == current.queueKey } }
        when {
            local != null -> add(sourceTab(local.title, local.tracks, current, state))
            remote != null -> add(sourceTab(remote.title, remote.tracks, current, state))
        }
        if (queue.suggestions.isNotEmpty()) {
            add(TableTab("autoplay", "Autoplay (${queue.suggestions.size})", queue.suggestions, -1) { state.playSuggestion(it) })
        }
    }
    var chosen by remember { mutableStateOf("queue") }
    val tab = tabs.firstOrNull { it.key == chosen } ?: tabs.first()
    // Another device's queue, while this computer is its remote control: its songs play there when chosen, and the
    // actions over the table -- this computer's own queue's -- wait until the music is back.
    val remote = state.connect.collectAsState().value.target
    val page: @Composable () -> Unit = {
        Column(Modifier.fillMaxSize()) {
            if (tab.key == LYRICS_TAB) {
                Box(Modifier.weight(1f).fillMaxWidth()) { LyricsPanel(track, state) }
            } else {
                if (tab.key == "queue" && remote == null) QueueTableActions(queue, state)
                QueueTable(tab, state, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
    if (skinned()) {
        PanelWindow(modifier, "Playlist") {
            PanelTabs(tabs, tab, { it.label }, { chosen = it.key }, Modifier.fillMaxSize()) { page() }
        }
        return
    }
    LayoutPanel(modifier) {
        Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.Bottom) {
                tabs.forEach { each -> TableTabLabel(each.label, each == tab) { chosen = each.key } }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink(.09f)))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp)) { page() }
        }
    }
}

private fun sourceTab(title: String, tracks: List<Track>, current: Track?, state: AppState) = TableTab(
    "source",
    "$title (${tracks.size})",
    tracks,
    tracks.indexOfFirst { it.queueKey == current?.queueKey },
) { index -> tracks.getOrNull(index)?.let { state.play(it, PlaybackOrigin.PLAYLIST, tracks) } }

/** A tab's name, underlined in the accent while it is the one shown. */
@Composable
private fun TableTabLabel(label: String, chosen: Boolean, choose: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clickable(onClick = choose)
            .drawBehind {
                if (chosen) drawRect(accent, Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (chosen) MaterialTheme.colorScheme.onSurface else ink(.55f),
            fontSize = 12.sp,
            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** Stop after this song, clear all but this song, and clear: the queue's own actions, over its table. */
@Composable
private fun QueueTableActions(queue: QueueState, state: AppState) {
    val stopAfter by state.stopAfterCurrent.collectAsState()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Shorter names where the three in full would not fit beside one another.
        val short = maxWidth < 500.dp
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StopAfterThisSongButton(stopAfter, enabled = queue.current != null, short = short) { state.toggleStopAfterCurrent() }
            Spacer(Modifier.width(4.dp))
            ClearAllButThisSongButton(enabled = canClearAllButCurrent(queue), short = short, clear = state::clearAllButCurrent)
            Spacer(Modifier.width(4.dp))
            TextButton(state::clearQueue, enabled = queue.tracks.isNotEmpty()) {
                Icon(Icons.Default.ClearAll, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear", fontSize = 12.sp)
            }
        }
    }
}

/** The queue: the chosen columns' headings, then a row for each song, the one playing marked and lit. */
@Composable
private fun QueueTable(tab: TableTab, state: AppState, modifier: Modifier) {
    val preferences = state.settings.collectAsState().value.preferences
    val list = rememberLazyListState()
    // The song playing kept in sight as the queue moves on, unless it already is.
    LaunchedEffect(tab.key, tab.playing) {
        val visible = list.layoutInfo.visibleItemsInfo.map { it.index }
        if (tab.playing >= 0 && tab.playing !in visible) list.scrollToItem((tab.playing - 2).coerceAtLeast(0))
    }
    BoxWithConstraints(modifier) {
        val columns = queueColumnsThatFit(preferences.queueColumns, maxWidth.value - 16f)
        Column(Modifier.fillMaxSize()) {
            TableHeadings(columns)
            if (tab.tracks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f).listWell(), contentAlignment = Alignment.Center) {
                    Text("Nothing here yet. Play something, or add songs from their ⋮ menu.", color = ink(.5f), fontSize = 12.sp)
                }
                return@Column
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).listWell().classicScrollbar(list), state = list, contentPadding = PaddingValues(bottom = 6.dp)) {
                itemsIndexed(tab.tracks, key = { index, song -> "${tab.key}:${song.queueKey}:$index" }) { index, song ->
                    TableRow(song, index, playing = index == tab.playing, columns, state) { tab.play(index) }
                }
            }
        }
    }
}

/** The column headings: a desktop list's header, as 98 and XP drew theirs under their skins. */
@Composable
private fun TableHeadings(columns: List<QueueColumn>) {
    val skin = skin()
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (skin == ThemeSkin.STANDARD) 30.dp else 20.dp)
            .drawBehind {
                when (skin) {
                    ThemeSkin.STANDARD -> Unit
                    // A strip of raised buttons, one a column, as a 98 list's header was.
                    ThemeSkin.WINDOWS_98 -> drawRect(Classic.Face)
                    // XP's: pale, with a lighter rim along its foot.
                    ThemeSkin.WINDOWS_XP -> {
                        drawRect(Brush.verticalGradient(listOf(Luna.Window, Luna.Face.towards(Luna.Window, .55f))))
                        drawRect(Luna.Face.towards(Luna.ButtonFoot, .4f), Offset(0f, size.height - pixel * 3), Size(size.width, pixel * 3))
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEach { column ->
            Cell(column, Modifier.fillMaxHeight().headingFrame(skin)) {
                Text(
                    column.heading,
                    color = if (skin == ThemeSkin.STANDARD) ink(.55f) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = if (skin == ThemeSkin.STANDARD) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = column.alignment(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (skin == ThemeSkin.STANDARD) Box(Modifier.fillMaxWidth().height(1.dp).background(ink(.08f)))
}

/** Each heading framed as its desktop framed one: a raised button under 98, a faint divider at its right under XP. */
private fun Modifier.headingFrame(skin: ThemeSkin): Modifier = when (skin) {
    ThemeSkin.STANDARD -> this
    ThemeSkin.WINDOWS_98 -> drawBehind { bevel(Bevel.Raised) }
    ThemeSkin.WINDOWS_XP -> drawBehind {
        drawRect(Luna.GroupEdge, Offset(size.width - pixel, size.height * .2f), Size(pixel, size.height * .6f))
    }
}

/** A column's place in a row: its fixed width, or its share of what is left. */
@Composable
private fun RowScope.Cell(column: QueueColumn, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sized = when (val width = column.width()) {
        is ColumnWidth.Fixed -> modifier.width(width.dp.dp)
        is ColumnWidth.Share -> modifier.weight(width.weight)
    }
    Box(sized.padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) { content() }
}

private fun QueueColumn.alignment(): TextAlign = when (this) {
    QueueColumn.NUMBER, QueueColumn.DURATION -> TextAlign.End
    else -> TextAlign.Start
}

/** One song's row: its columns, the song playing marked ▶ and lit, its menu on a right click. */
@Composable
private fun TableRow(song: Track, index: Int, playing: Boolean, columns: List<QueueColumn>, state: AppState, play: () -> Unit) {
    val skinned = skinned()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val cells: @Composable RowScope.() -> Unit = {
        columns.forEach { column ->
            Cell(column) {
                if (column == QueueColumn.NUMBER && playing) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Icon(Icons.Default.PlayArrow, "Playing", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text(
                        cellText(song, index, column),
                        color = when {
                            column == QueueColumn.TITLE || playing -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = if (skinned) 11.sp else 12.sp,
                        fontWeight = if (playing && column == QueueColumn.TITLE && !skinned) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = column.alignment(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    SongMenuArea(song, state, Modifier.fillMaxWidth()) {
        if (skinned) {
            ListRow(play, Modifier.height(18.dp), shape = RectangleShape, color = Color.Transparent, selected = playing, interactionSource = interaction) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, content = cells)
            }
        } else {
            val accent = MaterialTheme.colorScheme.primary
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        when {
                            playing -> accent.copy(alpha = .2f)
                            hovered -> ink(.07f)
                            index % 2 == 1 -> ink(.025f)
                            else -> Color.Transparent
                        },
                    )
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, indication = null, onClick = play),
                verticalAlignment = Alignment.CenterVertically,
                content = cells,
            )
        }
    }
}

/** What a column says for a song: its place, its title, its artist, its album, its length, its service. */
internal fun cellText(song: Track, index: Int, column: QueueColumn): String = when (column) {
    QueueColumn.NUMBER -> (index + 1).toString()
    QueueColumn.TITLE -> song.title
    QueueColumn.ARTIST -> song.artistLine
    QueueColumn.ALBUM -> song.album?.title.orEmpty()
    QueueColumn.DURATION -> song.durationMs?.takeIf { it > 0 }?.let(::formatPlaybackTime).orEmpty()
    QueueColumn.SERVICE -> song.provider.displayName
}

/**
 * The info bar: the cover small, the title large with the artist and the album under it, the song's own actions --
 * the heart, its link, and the menu that rearranges this screen -- and the visualiser at the right.
 */
@Composable
private fun InfoBar(track: Track, playback: PlaybackState, state: AppState, arrange: () -> Unit, modifier: Modifier) {
    val preferences = state.settings.collectAsState().value.preferences
    // The visualiser measures the sound this computer makes, and makes none while the song plays on another device:
    // it gives its place to saying where the song is, rather than drawing a silence as though it were music.
    val elsewhere = state.connect.collectAsState().value.target?.name
    val visualiser = if (elsewhere != null) VisualiserStyle.OFF else visualiserShown(preferences.visualiser, state.audioAnalysisKind)
    val skinned = skinned()
    val inside: @Composable () -> Unit = {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // The visualiser gives way to the song's name and its buttons in a narrow window, and goes before they would.
            val cover = if (skinned) 64.dp else if (maxWidth < 560.dp) 64.dp else 84.dp
            val room = maxWidth - cover - (if (skinned) 16.dp else 28.dp) - 32.dp - 190.dp
            val meter = room.coerceAtMost(if (skinned) 180.dp else 220.dp)
            Row(Modifier.fillMaxWidth().padding(if (skinned) 8.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (skinned) {
                    SkinCover(track, cover)
                } else {
                    RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(cover).clip(RoundedCornerShape(10.dp)))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    MotionContent(track, kind = MotionKind.TRACK, contentKey = { it.queueKey }) { shown ->
                        Column {
                            Text(
                                shown.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = if (skinned) 18.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = if (skinned) 22.sp else 29.sp,
                            )
                            Text(shown.artistLine, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.75f), fontSize = if (skinned) 12.sp else 14.sp)
                            shown.album?.title?.takeIf { it.isNotBlank() && it != shown.title }?.let { album ->
                                Text(album, maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink(.5f), fontSize = if (skinned) 11.sp else 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(if (skinned) 2.dp else 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LikeButton(track, state, size = 30.dp)
                        IconButton({ state.copyTrackLink(track) }, Modifier.size(30.dp)) {
                            Icon(Icons.Default.Link, "Copy link", Modifier.size(17.dp), tint = ink(.6f))
                        }
                        IconButton(arrange, Modifier.size(30.dp)) {
                            Icon(Icons.Default.Tune, "Arrange this screen", Modifier.size(17.dp), tint = ink(.6f))
                        }
                        Spacer(Modifier.width(6.dp))
                        if (playsOnSpotify(track, state.settings.collectAsState().value.spotify)) {
                            OnSpotifyMark()
                            Spacer(Modifier.width(6.dp))
                        }
                        ProviderBadge(track.provider, compact = true)
                        DevicesButton(state, Modifier.size(30.dp), iconSize = 17.dp, quiet = ink(.6f))
                    }
                }
                if (visualiser != VisualiserStyle.OFF && meter >= 96.dp) {
                    Spacer(Modifier.width(16.dp))
                    AudioVisualiser(visualiser, state, Modifier.width(meter).height(if (skinned) 64.dp else 76.dp))
                }
                if (elsewhere != null && meter >= 96.dp) {
                    Spacer(Modifier.width(16.dp))
                    Row(
                        Modifier.width(meter).clickable { state.navigate(app.noctorium.core.Destination.CONNECT) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Icon(Icons.Default.Devices, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Playing on $elsewhere",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = if (skinned) 12.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
    if (skinned) {
        SongWindow(track, modifier) { inside() }
        return
    }
    LayoutPanel(modifier) { inside() }
}

/**
 * The status bar along the foot: what the stream is -- "Opus, stereo, 48 kHz, 128 kbit/s" -- on the left, as the
 * player reports it, and how far into the whole queue on the right. Under a Windows skin, that desktop's status bar:
 * the face, with each half in a pane set into it.
 */
@Composable
private fun StatusBar(queue: QueueState, playback: PlaybackState, state: AppState) {
    val onSpotify = playsOnSpotify(playback.track, state.settings.collectAsState().value.spotify)
    val left = statusBarFormat(playback, onSpotify, elsewhere = state.connect.collectAsState().value.target?.name)
    val right = statusBarTimes(queue, playback.positionMs)
    val skin = skin()
    when (skin) {
        ThemeSkin.STANDARD -> Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink(.08f)))
            Row(
                Modifier.fillMaxWidth().background(ink(.035f)).height(28.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(left, color = ink(.62f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(right, color = ink(.62f), fontSize = 11.sp, maxLines = 1)
            }
        }
        else -> Row(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .drawBehind { drawRect(if (skin == ThemeSkin.WINDOWS_XP) Luna.Face else Classic.Face) }
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            StatusPane(left, skin, Modifier.weight(1f))
            StatusPane(right, skin, Modifier.width(150.dp))
        }
    }
}

/** One of a Windows status bar's panes: sunk a pixel into the face under 98, set off by a line under XP. */
@Composable
private fun StatusPane(text: String, skin: ThemeSkin, modifier: Modifier) {
    Box(
        modifier
            .fillMaxHeight()
            .drawBehind {
                if (skin == ThemeSkin.WINDOWS_XP) {
                    drawRect(Luna.GroupEdge, Offset.Zero, Size(pixel, size.height))
                } else {
                    thinSunken()
                }
            }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
    }
}
