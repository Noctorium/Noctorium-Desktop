package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import app.noctorium.ui.icons.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.Destination
import app.noctorium.core.SearchBangs
import app.noctorium.core.SearchMode
import app.noctorium.domain.Track
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import app.noctorium.settings.themeSkin
import app.noctorium.settings.windows98Palette
import app.noctorium.ui.skins.LinearProgressIndicator
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.SkinWindow
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.WindowColours
import app.noctorium.ui.skins.listWell
import app.noctorium.ui.skins.skinShape
import app.noctorium.ui.skins.skinned

/*
 * The quick search bar: Ctrl+F (⌘F on a Mac) anywhere in Noctorium brings a search box up over whatever is on the
 * screen, and a global shortcut brings the same box up from any program, in a small window of its own.
 *
 * It is the application's own search -- the same providers, the same bangs -- so `!ytm daft punk` searches YouTube
 * Music alone, and what it found is still there on the Search page afterwards.
 */

/** What a key does in the quick search bar. */
internal enum class QuickKey { UP, DOWN, PLAY, QUEUE, OPEN_PAGE, CLOSE }

/**
 * What [key] means in the bar: the arrows choose, Enter plays, Shift+Enter adds to the queue, Ctrl+Enter (⌘ on a
 * Mac) opens the whole Search page, and Escape closes. Anything else is typing.
 */
internal fun quickSearchKey(key: Key, shift: Boolean = false, command: Boolean = false): QuickKey? = when (key) {
    Key.DirectionUp -> QuickKey.UP
    Key.DirectionDown -> QuickKey.DOWN
    Key.Enter, Key.NumPadEnter -> when {
        command -> QuickKey.OPEN_PAGE
        shift -> QuickKey.QUEUE
        else -> QuickKey.PLAY
    }
    Key.Escape -> QuickKey.CLOSE
    else -> null
}

/** The bang in what was typed, for the chip beside the box: what was typed, and the service it sends the search to. */
internal data class BangChip(val typed: String, val service: String, val mode: SearchMode)

/** The chip for [text]'s bang, at its start or its end, or null for a search of the usual services. */
internal fun bangChip(text: String): BangChip? = SearchBangs.parse(text).bang?.let { BangChip(it.typed, serviceName(it.mode), it.mode) }

/** A search's services as the bar names them: "every service" for !all, which the Search page calls Hybrid. */
internal fun serviceName(mode: SearchMode): String = if (mode == SearchMode.HYBRID) "Every service" else mode.displayName

/**
 * The songs the bar lists for [text]: the application's search results, once they are the results for exactly
 * what is in the box -- not the ones from a few letters ago, still on their way out.
 */
internal fun quickResults(text: String, ui: AppUiState, limit: Int = QUICK_RESULTS): List<Track> =
    if (text.isBlank() || ui.searchQuery != text || SearchBangs.parse(text).query.isBlank()) emptyList() else ui.searchResults.tracks.take(limit)

/** Enough to choose from without scrolling, which a bar meant to be quick should not ask for. */
internal const val QUICK_RESULTS = 7

/** The choice moved by an arrow, kept on the list: down from the last stays there, up from the first too. */
internal fun quickChoice(current: Int, key: QuickKey, count: Int): Int = when (key) {
    QuickKey.DOWN -> (current + 1).coerceAtMost((count - 1).coerceAtLeast(0))
    QuickKey.UP -> (current - 1).coerceAtLeast(0)
    else -> current
}

/** How the bar was left: closed, having started a song, or for the whole Search page. */
internal enum class QuickExit { CLOSED, PLAYED, SEARCH_PAGE }

/**
 * The bar itself, wherever it is shown. [leave] is told how it was left; [inWindow] when it is the small window
 * of its own, which draws its own frame under a Windows skin.
 */
@Composable
internal fun QuickSearchBar(state: AppState, leave: (QuickExit) -> Unit, modifier: Modifier = Modifier, inWindow: Boolean = false, initial: String = "") {
    val ui by state.ui.collectAsState()
    var text by remember { mutableStateOf(initial) }
    val results = quickResults(text, ui)
    var chosen by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(results.map { it.queueKey }) { chosen = 0 }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val chip = bangChip(text)
    val command = if (isMacPlatform) "⌘" else "Ctrl"

    fun act(key: QuickKey) {
        when (key) {
            QuickKey.UP, QuickKey.DOWN -> chosen = quickChoice(chosen, key, results.size)
            QuickKey.PLAY -> results.getOrNull(chosen)?.let { track ->
                state.playFromSearch(track, results)
                leave(QuickExit.PLAYED)
            }
            QuickKey.QUEUE -> results.getOrNull(chosen)?.let { track ->
                state.addToQueue(track)
                note = "Added “${track.title}” to the queue"
            }
            QuickKey.OPEN_PAGE -> {
                state.navigate(Destination.SEARCH)
                leave(QuickExit.SEARCH_PAGE)
            }
            QuickKey.CLOSE -> leave(QuickExit.CLOSED)
        }
    }

    val keys = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val key = quickSearchKey(event.key, event.isShiftPressed, if (isMacPlatform) event.isMetaPressed else event.isCtrlPressed) ?: return@onPreviewKeyEvent false
        act(key)
        true
    }
    val type: (String) -> Unit = { typed ->
        text = typed
        note = null
        state.search(typed)
    }

    val searching = text.isNotBlank() && ui.searchQuery == text && ui.searchLoading
    val where = chip?.service ?: serviceName(ui.searchMode)
    val footer = when {
        note != null -> note!!
        text.isBlank() -> "Start or end with !ytm, !sc, !sp, !bc, !vk, !am, !dz, !td or !all to search one service."
        else -> "↑↓ choose  ·  Enter play  ·  Shift+Enter add to queue  ·  $command+Enter all results  ·  Esc close"
    }

    if (skinned()) {
        val window: @Composable () -> Unit = {
            SkinWindow(title = { Text("Quick search") }, modifier = modifier, onClose = { leave(QuickExit.CLOSED) }) {
                Column(Modifier.padding(10.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = type,
                        label = { Text("Search for:") },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(14.dp)) },
                        prefix = chip?.let { { SkinBangChip(it) } },
                        placeholder = { Text("A song, an artist, an album") },
                        singleLine = true,
                        visualTransformation = BangHighlight(chip, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).tracksTyping().then(keys),
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.fillMaxWidth().heightIn(min = 60.dp).listWell().padding(2.dp)) {
                        QuickResultsList(results, chosen, searching, text, where, ui, { chosen = it; act(QuickKey.PLAY) })
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(footer, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (inWindow) window() else WindowColours(window)
        return
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, ink(.1f)),
        modifier = modifier.shadow(28.dp, RoundedCornerShape(20.dp)),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                if (chip != null) {
                    BangChipView(chip)
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (text.isEmpty()) {
                        Text("Search, or try “!ytm daft punk”", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = type,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = BangHighlight(chip, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).tracksTyping().then(keys),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("Esc", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(ink(.06f)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            if (searching) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp)) else Box(Modifier.fillMaxWidth().height(2.dp).background(ink(.06f)))
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                QuickResultsList(results, chosen, searching, text, where, ui, { chosen = it; act(QuickKey.PLAY) })
            }
            Text(
                footer,
                fontSize = 11.sp,
                color = if (note != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().background(ink(.03f)).padding(horizontal = 18.dp, vertical = 9.dp),
            )
        }
    }
}

/** The songs found, the one chosen lit; or what is going on instead -- searching, nothing found, nothing typed. */
@Composable
private fun QuickResultsList(results: List<Track>, chosen: Int, searching: Boolean, text: String, where: String, ui: AppUiState, play: (Int) -> Unit) {
    val classic = skinned()
    when {
        results.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(if (classic) 0.dp else 2.dp)) {
            results.forEachIndexed { index, track -> QuickResultRow(track, index == chosen) { play(index) } }
        }
        text.isBlank() || SearchBangs.parse(text).query.isBlank() -> QuickLine(
            if (text.isBlank()) "Songs from every service you search, as you type." else "Searching $where. Now type what to look for.",
        )
        searching || ui.searchQuery != text -> QuickLine("Searching $where…")
        ui.errorMessage != null -> QuickLine(ui.errorMessage.orEmpty(), error = true)
        else -> QuickLine("Nothing on $where for “${SearchBangs.parse(text).query}”.")
    }
}

@Composable
private fun QuickLine(text: String, error: Boolean = false) {
    Text(
        text,
        fontSize = 12.sp,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
    )
}

@Composable
private fun QuickResultRow(track: Track, chosen: Boolean, play: () -> Unit) {
    if (skinned()) {
        ListRow(play, shape = RectangleShape, color = Color.Transparent, selected = chosen) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artistLine, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ProviderBadge(track.provider, compact = true)
            }
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f) else Color.Transparent)
            .clickable(remember { MutableInteractionSource() }, null, onClick = play)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(40.dp).clip(skinShape(RoundedCornerShape(8.dp))))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artistLine, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ProviderBadge(track.provider, compact = true)
        if (chosen) {
            Spacer(Modifier.width(10.dp))
            Text("Enter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The chip saying where a bang sends the search: the service, in the accent, beside the box. */
@Composable
internal fun BangChipView(chip: BangChip, compact: Boolean = false) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .16f))
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(if (compact) 12.dp else 14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(chip.service, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

/** The same chip under a Windows skin: the service in a small sunken box, as a field's unit was written. */
@Composable
private fun SkinBangChip(chip: BangChip) {
    Text(
        "${chip.service}: ",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** Writes the bang in the box in the accent, so it reads as an instruction rather than a word to look for. */
private class BangHighlight(private val chip: BangChip?, private val accent: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val bang = chip?.typed ?: return TransformedText(text, OffsetMapping.Identity)
        val trimmed = text.text.trimStart()
        val at = if (trimmed.startsWith(bang)) text.text.indexOf(bang) else text.text.lastIndexOf(bang)
        if (at < 0) return TransformedText(text, OffsetMapping.Identity)
        val styled = AnnotatedString.Builder(text).apply {
            addStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold), at, at + bang.length)
        }.toAnnotatedString()
        return TransformedText(styled, OffsetMapping.Identity)
    }

    override fun equals(other: Any?): Boolean = other is BangHighlight && other.chip == chip && other.accent == accent
    override fun hashCode(): Int = chip.hashCode() * 31 + accent.hashCode()
}

/**
 * The bar over the window, for Ctrl+F: the page dimmed behind it under the standard skin, and clicking the dimmed
 * page closes it. Under a Windows skin it is a window standing in front, as a Find dialog did, with nothing dimmed.
 */
@Composable
internal fun BoxScope.QuickSearchOverlay(state: AppState, close: () -> Unit) {
    val classic = skinned()
    Box(
        Modifier
            .matchParentSize()
            .then(if (classic) Modifier else Modifier.background(Color.Black.copy(alpha = .38f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = close),
    )
    QuickSearchBar(
        state,
        leave = { close() },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = if (classic) 90.dp else 76.dp, start = 24.dp, end = 24.dp)
            .widthIn(max = if (classic) 520.dp else 660.dp)
            .fillMaxWidth(),
    )
}

/** The quick search bar alone, filling a small window of its own: the global shortcut's. */
@Composable
internal fun QuickSearchWindowContent(state: AppState, leave: (QuickExit) -> Unit) {
    Box(Modifier.fillMaxSize().padding(if (skinned()) 0.dp else 18.dp), contentAlignment = Alignment.TopCenter) {
        QuickSearchBar(state, leave, Modifier.fillMaxWidth(), inWindow = true)
    }
}

/** The bar over the whole window, holding the keyboard while it is open. */
@Composable
internal fun QuickSearchPopup(state: AppState, close: () -> Unit) {
    androidx.compose.ui.window.Popup(onDismissRequest = close, properties = androidx.compose.ui.window.PopupProperties(focusable = true)) {
        Box(Modifier.fillMaxSize()) { QuickSearchOverlay(state, close) }
    }
}

/**
 * The window's look -- theme, skin, accent, typeface, text size -- for something drawn outside the window: the quick
 * search's own small window. The accent is the theme's or the one chosen; one that follows the cover takes the
 * theme's here, since this window has no cover in it to follow.
 */
@Composable
internal fun NoctoriumLook(state: AppState, content: @Composable () -> Unit) {
    val preferences = state.settings.collectAsState().value.preferences
    val theme = preferences.themeColours()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val typing = remember { mutableStateOf(false) }
    app.noctorium.ui.skins.SkinnedMaterialTheme(
        preferences.themeSkin,
        theme,
        Color(preferences.resolvedAccent(null)),
        preferences.font,
        preferences.cornerStyle,
        preferences.windows98Palette,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalTyping provides typing,
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, density.fontScale * preferences.textSize.scale),
            LocalMotion provides preferences.animations,
            content = content,
        )
    }
}
