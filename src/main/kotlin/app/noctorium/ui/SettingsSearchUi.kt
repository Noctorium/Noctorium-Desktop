package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import app.noctorium.ui.icons.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.settings.SettingMatch
import app.noctorium.settings.SettingMatchKind
import app.noctorium.settings.SettingsSearch
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.skinned

/** A setting the search found: where it is, and how it was found. */
internal data class FoundSetting(val setting: IndexedSetting, val match: SettingMatch)

/**
 * The box at the top of Settings. Typing in it turns the home into the settings that match, best first; the
 * arrows choose among them and Enter opens the one chosen, on its page, lit up. Escape empties the box, and from
 * an empty box gives the keyboard back.
 */
@Composable
internal fun SettingsSearchBox(
    query: String,
    search: (String) -> Unit,
    matches: List<FoundSetting>,
    selected: Int,
    select: (Int) -> Unit,
    open: (SettingsPage, String?) -> Unit,
    /** Where a setting found on a page outside Settings -- the Connect page -- is opened. */
    go: (app.noctorium.core.Destination) -> Unit = {},
    typeHere: Int = 0,
) {
    val classic = skinned()
    val focus = remember { FocusRequester() }
    LaunchedEffect(typeHere) { if (typeHere > 0) runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value = query,
        onValueChange = search,
        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(if (classic) 14.dp else 22.dp)) },
        trailingIcon = if (query.isEmpty()) null else {
            { IconButton({ search("") }, Modifier.size(if (classic) 16.dp else 32.dp)) { Icon(Icons.Default.Close, "Clear the search", Modifier.size(if (classic) 12.dp else 18.dp)) } }
        },
        label = if (classic) { { Text("Find a setting:") } } else null,
        placeholder = { Text("Search settings: try “dark mode”, “louder” or “hotkeys”") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 860.dp)
            .focusRequester(focus)
            .tracksTyping()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> { if (matches.isNotEmpty()) select((selected + 1).coerceAtMost(matches.lastIndex)); matches.isNotEmpty() }
                    Key.DirectionUp -> { if (matches.isNotEmpty()) select((selected - 1).coerceAtLeast(0)); matches.isNotEmpty() }
                    Key.Enter, Key.NumPadEnter -> matches.getOrNull(selected)?.let { found ->
                        found.setting.openWith(open, go)
                        true
                    } ?: false
                    // An empty box lets Escape through, to give the keyboard back to the shortcuts.
                    Key.Escape -> if (query.isNotEmpty()) { search(""); true } else false
                    else -> false
                }
            },
    )
}

/** The settings that match [query], as rows of the home's list, or a word saying there are none. */
internal fun LazyListScope.settingsResults(
    query: String,
    matches: List<FoundSetting>,
    selected: Int,
    open: (SettingsPage, String?) -> Unit,
    go: (app.noctorium.core.Destination) -> Unit = {},
) {
    if (matches.isEmpty()) {
        item(key = "no-settings") { NothingFound(query) }
        return
    }
    item(key = "settings-found") {
        Text(
            if (matches.size == 1) "One setting" else "${matches.size} settings",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp),
        )
    }
    itemsIndexed(matches, key = { _, found -> found.setting.entry.id }) { index, found ->
        FoundSettingRow(found, query, index == selected) { found.setting.openWith(open, go) }
    }
}

@Composable
private fun NothingFound(query: String) {
    Row(Modifier.padding(vertical = 26.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.SearchOff, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("No setting matches “${query.trim()}”", fontWeight = FontWeight.SemiBold)
            Text(
                "Try another word for it, or fewer words.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One setting found: its name with the words typed picked out, where it lives, and what it does -- or, for one
 * found by another word for it, which word.
 */
@Composable
private fun FoundSettingRow(found: FoundSetting, query: String, chosen: Boolean, open: () -> Unit) {
    val entry = found.setting.entry
    val accent = MaterialTheme.colorScheme.primary
    val classic = skinned()
    val title = highlighted(entry.title, query, if (classic) null else accent)
    val why = listOfNotNull(
        when (found.match.kind) {
            SettingMatchKind.KEYWORD -> found.match.keyword?.let { "Also called “$it”" }
            SettingMatchKind.SECTION -> "In ${found.setting.where}"
            else -> null
        },
        found.setting.onlyWhen?.let { "There $it" },
    ).joinToString(" · ").ifEmpty { null }
    if (classic) {
        ListRow(open, shape = RectangleShape, color = androidx.compose.ui.graphics.Color.Transparent, selected = chosen) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(found.setting.icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(found.setting.where, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        listOfNotNull(why, entry.description.ifBlank { null }).joinToString(" · "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        return
    }
    Surface(
        onClick = open,
        shape = RoundedCornerShape(14.dp),
        color = if (chosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
        border = if (chosen) BorderStroke(1.dp, accent.copy(alpha = .6f)) else null,
        modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(found.setting.icon, null, Modifier.size(22.dp), tint = accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(10.dp))
                    Text(found.setting.where, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (entry.description.isNotBlank()) {
                    Text(entry.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (why != null) {
                    Text(why, fontSize = 11.sp, color = accent.copy(alpha = .85f))
                }
            }
            if (chosen) {
                Spacer(Modifier.width(10.dp))
                Text("Enter", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * [text] with the words of it that [query]'s words begin picked out, in bold and in [colour] where one is given
 * -- the way a search shows why it found what it found. Matched as the search matches: case and accents aside.
 */
internal fun highlighted(text: String, query: String, colour: androidx.compose.ui.graphics.Color?): AnnotatedString {
    val typed = SettingsSearch.words(query)
    return buildAnnotatedString {
        Regex("\\S+|\\s+").findAll(text).forEach { piece ->
            val word = piece.value
            val normal = SettingsSearch.normalise(word)
            val hit = word.isNotBlank() && typed.any { it.isNotEmpty() && normal.split(' ').any { part -> part.startsWith(it) } }
            if (hit) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colour ?: androidx.compose.ui.graphics.Color.Unspecified)) { append(word) }
            } else {
                append(word)
            }
        }
    }
}
