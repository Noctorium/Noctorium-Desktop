package app.noctorium.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import app.noctorium.ui.icons.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.hotkeys.HotKeyResult
import app.noctorium.settings.KeyChord
import app.noctorium.settings.NoctoriumPreferences
import app.noctorium.settings.ShortcutPreferences
import app.noctorium.ui.skins.IconButton
import app.noctorium.ui.skins.PushButton
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.TextButton
import app.noctorium.ui.skins.skinned

/** What the Shortcuts tile says. */
internal fun shortcutsSubtitle(shortcuts: ShortcutPreferences): String {
    val search = InAppShortcuts.from(shortcuts, isMacPlatform).chordsOf(ShortcutAction.QUICK_SEARCH).firstOrNull { it != null }
    val quick = search?.let { "${chordLabel(it)} for quick search" } ?: "Every key can be changed"
    return if (shortcuts.globalEnabled) "$quick · global shortcuts on" else quick
}

/** A chord being recorded: which place it goes in, in Noctorium or [global]ly. */
internal data class Recording(val action: ShortcutAction, val slot: Int, val global: Boolean = false)

/** A chord pressed that something else already has, waiting for the listener to say which keeps it. */
private data class Clash(val recording: Recording, val chord: KeyChord, val others: List<ShortcutSlot>)

/**
 * Keyboard shortcuts: every one in Noctorium, each changeable -- click its keys, press the new ones -- and the
 * global ones, which work from any program once switched on, and the quick search that both can open.
 *
 * [recording] starts the page already listening for a chord, for a picture of it.
 */
@Composable
internal fun ShortcutsSettingsPanel(preferences: NoctoriumPreferences, state: AppState, recording: Recording? = null) {
    val shortcuts = preferences.desktop.shortcuts
    val inApp = InAppShortcuts.from(shortcuts, isMacPlatform)
    var listening by remember { mutableStateOf(recording) }
    var clash by remember { mutableStateOf<Clash?>(null) }
    var refusal by remember { mutableStateOf<Pair<ShortcutAction, String>?>(null) }

    // A chord pressed that is the one the place came with is kept as the default, not as a change of it.
    fun keep(into: Recording, chord: KeyChord) {
        val name = into.action.storedName(into.slot)
        if (chord == into.action.defaultChord(into.slot, isMacPlatform)) state.resetShortcut(name) else state.setShortcut(name, chord.toString())
    }

    fun record(into: Recording, chord: KeyChord?) {
        listening = null
        refusal = null
        if (chord == null) {
            // Taken away: blank in Noctorium, unbound globally.
            if (into.global) state.resetShortcut(into.action.id, global = true) else state.setShortcut(into.action.storedName(into.slot), "")
            return
        }
        if (into.global) {
            globalRefusal(chord)?.let { refusal = into.action to it; return }
            val others = globalBindings(shortcuts).filter { (action, bound) -> bound == chord && action != into.action }.keys
            if (others.isNotEmpty()) {
                clash = Clash(into, chord, others.map { ShortcutSlot(it, 0) })
                return
            }
            state.setShortcut(into.action.id, chord.toString(), global = true)
            return
        }
        val others = inApp.conflicts(chord, except = ShortcutSlot(into.action, into.slot))
        if (others.isNotEmpty()) {
            clash = Clash(into, chord, others)
            return
        }
        keep(into, chord)
    }

    fun settle(clash: Clash) {
        // The others give it up, and this one has it.
        clash.others.forEach { other ->
            if (clash.recording.global) state.resetShortcut(other.action.id, global = true) else state.setShortcut(other.storedName, "")
        }
        if (clash.recording.global) {
            state.setShortcut(clash.recording.action.id, clash.chord.toString(), global = true)
        } else {
            keep(clash.recording, clash.chord)
        }
    }

    SettingsPageColumn {
        SettingsPanelCard {
            CardHeading(Icons.Default.Keyboard, "In Noctorium")
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "While Noctorium is the window in front. Click a shortcut's keys and press the new ones: Escape leaves it as it " +
                        "was, Backspace takes it away. While you type in a box, only the ones with ${if (isMacPlatform) "⌘, ⌃ or ⌥" else "Ctrl or Alt"} still work.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                if (shortcuts.inApp.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    TextButton({ state.updateDesktop { copy(shortcuts = shortcuts.copy(inApp = emptyMap())) } }) {
                        Icon(Icons.Default.RestartAlt, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reset all", fontSize = 12.sp)
                    }
                }
            }
            ShortcutGroup.entries.forEach { group ->
                val actions = ShortcutAction.entries.filter { it.group == group }
                Spacer(Modifier.height(14.dp))
                GroupLabel(group.title)
                actions.forEach { action ->
                    val chords = inApp.chordsOf(action)
                    val changed = (0 until ShortcutAction.SLOTS).any { action.storedName(it) in shortcuts.inApp }
                    SettingAnchor(action.title) {
                        val pointer = remember { MutableInteractionSource() }
                        val pointed by pointer.collectIsHoveredAsState()
                        Column(Modifier.hoverable(pointer)) {
                            ShortcutRow(
                                title = action.title,
                                changed = changed,
                                reset = { (0 until ShortcutAction.SLOTS).forEach { state.resetShortcut(action.storedName(it)) } },
                            ) {
                                (0 until ShortcutAction.SLOTS).forEach { slot ->
                                    val here = Recording(action, slot)
                                    // A second place offered only while the row is pointed at, and once the first has a chord:
                                    // thirty "Add" buttons down the page would drown the keys themselves.
                                    if (slot == 0 || chords[slot] != null || listening == here || (pointed && chords[0] != null)) {
                                        ChordButton(chords[slot], listening == here, { listening = here; clash = null }, { record(here, it) }, { listening = null })
                                    }
                                }
                            }
                            clash?.takeIf { it.recording.action == action && !it.recording.global }?.let { found ->
                                ClashNote(found, { settle(found); clash = null }, { clash = null })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GroupLabel("Always")
            ShortcutRow("Close what is open, or leave the box you are typing in", changed = false, reset = {}) {
                KeyCaps(KeyChord("Escape"), dim = true)
            }
            ShortcutRow("The keyboard's own media keys", changed = false, reset = {}) {
                Text("▶❚❚  ⏭  ⏮  ⏹", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        GlobalShortcutsCard(shortcuts, state, listening, { listening = it; clash = null; refusal = null }, ::record, { listening = null }, clash, { settle(it); clash = null }, { clash = null }, refusal)

        SettingsPanelCard {
            CardHeading(Icons.Default.Search, "Quick search")
            Spacer(Modifier.height(6.dp))
            val quick = inApp.chordsOf(ShortcutAction.QUICK_SEARCH).firstOrNull { it != null }
            Text(
                (quick?.let { "${chordLabel(it)} brings a search bar up over everything in Noctorium. " } ?: "A search bar over everything in Noctorium. ") +
                    "Start or end what you type with a bang to search one service: !ytm YouTube Music, !sc SoundCloud, !sp Spotify, " +
                    "!bc Bandcamp, !vk VK, !all every one. The arrows choose, Enter plays, Shift+Enter adds to the queue, " +
                    "${if (isMacPlatform) "⌘" else "Ctrl"}+Enter opens the Search page and Escape closes it. As a global shortcut it opens " +
                    "in a small window of its own, even with Noctorium in the tray, and gives you back to where you were when it closes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * The global shortcuts: the switch, what this desktop uses for them -- or why it has none -- and each one that
 * makes sense from another program, with its chord, a suggestion where there is one, and what the system said.
 */
@Composable
private fun GlobalShortcutsCard(
    shortcuts: ShortcutPreferences,
    state: AppState,
    listening: Recording?,
    listen: (Recording) -> Unit,
    record: (Recording, KeyChord?) -> Unit,
    cancel: () -> Unit,
    clash: Clash?,
    settle: (Clash) -> Unit,
    dismiss: () -> Unit,
    refusal: Pair<ShortcutAction, String>?,
) {
    val live = LocalGlobalShortcuts.current?.collectAsState()?.value
    val supported = live?.supported ?: true
    val bound = shortcuts.global.mapNotNull { (id, chord) -> ShortcutAction.byId(id)?.let { action -> KeyChord.parse(chord)?.let { action to it } } }.toMap()
    SettingsPanelCard {
        CardHeading(Icons.Default.Language, "Global shortcuts")
        Spacer(Modifier.height(6.dp))
        Text(
            "Keys that work from any program, even with Noctorium in the tray. Each is asked of the system by name and " +
                "nothing else is seen: no other key you type ever reaches Noctorium. A chord another program already has " +
                "is left to it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        ToggleRow(
            "Use global shortcuts",
            when {
                !supported -> live?.unavailable ?: "Not supported on this desktop."
                shortcuts.globalEnabled -> "On" + (live?.how?.let { ", through $it." } ?: ".")
                else -> "Off: Noctorium takes no keys from other programs."
            },
            shortcuts.globalEnabled && supported,
            enabled = supported,
        ) { on -> state.setGlobalShortcuts(on) }
        val suggestions = ShortcutAction.entries.filter { it.global && it.globalSuggestion != null && it !in bound }
            .mapNotNull { action -> platformChord(action.globalSuggestion!!, isMacPlatform)?.let { action to it } }
            .filter { (_, chord) -> chord !in bound.values }
        if (supported && suggestions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Suggested: " + suggestions.joinToString(", ") { (action, chord) -> "${chordLabel(chord)} for ${action.title.lowercase()}" } + ".",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton({ suggestions.forEach { (action, chord) -> state.setShortcut(action.id, chord.toString(), global = true) } }) {
                    Text("Use these", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.alpha(if (shortcuts.globalEnabled && supported) 1f else .55f)) {
            ShortcutAction.entries.filter { it.global }.forEach { action ->
                val here = Recording(action, 0, global = true)
                Column {
                    ShortcutRow(action.title, changed = action in bound, reset = { state.resetShortcut(action.id, global = true) }, resetLabel = "Remove") {
                        if (shortcuts.globalEnabled && bound[action] != null) {
                            GlobalStatus(live?.results?.get(action.id))
                            Spacer(Modifier.width(10.dp))
                        }
                        ChordButton(bound[action], listening == here, { listen(here) }, { record(here, it) }, cancel, emptyLabel = "Not set")
                    }
                    if (refusal?.first == action) Note(refusal.second, error = true)
                    clash?.takeIf { it.recording.action == action && it.recording.global }?.let { found -> ClashNote(found, { settle(found) }, dismiss) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "They must hold two of ${if (isMacPlatform) "⌃, ⌥ and ⌘" else if (isLinuxPlatform) "Ctrl, Alt and Super" else "Ctrl, Alt and Win"}, " +
                "so they cannot take a key other programs use. ${if (isMacPlatform) "⌘F" else "Ctrl+F"} is Find everywhere and stays in Noctorium. " +
                "The media keys already work from anywhere.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** What the system said about one global chord, in a word or two. */
@Composable
private fun GlobalStatus(result: HotKeyResult?) {
    val (text, colour, icon) = when (result) {
        HotKeyResult.Registered -> Triple("Active", MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle)
        HotKeyResult.Pending, null -> Triple("Asking…", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Schedule)
        HotKeyResult.Taken -> Triple("Taken by another program", MaterialTheme.colorScheme.error, Icons.Default.ErrorOutline)
        is HotKeyResult.Unsupported -> Triple(result.reason, MaterialTheme.colorScheme.error, Icons.Default.ErrorOutline)
        is HotKeyResult.Failed -> Triple(result.reason, MaterialTheme.colorScheme.error, Icons.Default.ErrorOutline)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 240.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = colour)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = colour, maxLines = 1)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        if (skinned()) text else text.uppercase(),
        fontSize = if (skinned()) 12.sp else 10.sp,
        fontWeight = if (skinned()) FontWeight.Bold else FontWeight.SemiBold,
        color = if (skinned()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = if (skinned()) 0.sp else .8.sp,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** One shortcut: what it does, its keys, and a way back to how it came where it has been changed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutRow(title: String, changed: Boolean, reset: () -> Unit, resetLabel: String = "Reset", keys: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { keys() }
        }
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
            if (changed) {
                IconButton(reset, Modifier.size(28.dp)) { Icon(Icons.Default.RestartAlt, resetLabel, Modifier.size(16.dp)) }
            }
        }
    }
}

/**
 * A shortcut's keys as a button: clicked, it listens for the next chord and shows the keys held so far; Escape
 * stops listening, Backspace on its own takes the shortcut away, and any other key with what is held is the new
 * chord. Nothing it hears reaches the rest of the window.
 */
@Composable
private fun ChordButton(
    chord: KeyChord?,
    listening: Boolean,
    listen: () -> Unit,
    record: (KeyChord?) -> Unit,
    cancel: () -> Unit,
    emptyLabel: String = "Add",
) {
    val focus = remember { FocusRequester() }
    var held by remember { mutableStateOf("") }
    LaunchedEffect(listening) {
        held = ""
        if (listening) runCatching { focus.requestFocus() }
    }
    val classic = skinned()
    val recorder = Modifier
        .focusRequester(focus)
        .onPreviewKeyEvent { event ->
            if (!listening) return@onPreviewKeyEvent false
            val modifiers = KeyChord("X", event.isCtrlPressed, event.isAltPressed, event.isShiftPressed, event.isMetaPressed)
            if (event.type == KeyEventType.KeyUp) {
                held = if (modifiers.hasModifier) chordParts(modifiers).dropLast(1).joinToString(if (isMacPlatform) "" else "+") else ""
                return@onPreviewKeyEvent true
            }
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
            val plain = !modifiers.hasModifier
            when {
                event.key == Key.Escape && plain -> cancel()
                event.key == Key.Backspace && plain -> record(null)
                isModifierKey(event.key) -> held = chordParts(modifiers).dropLast(1).joinToString(if (isMacPlatform) "" else "+")
                else -> chordOf(event.key, event.isCtrlPressed, event.isAltPressed, event.isShiftPressed, event.isMetaPressed)?.let(record)
            }
            true
        }
        .focusable()
    if (listening) {
        val text = if (held.isEmpty()) "Press the keys…" else "$held+…"
        if (classic) {
            PushButton(cancel, recorder, latched = true, minWidth = 0.dp, minHeight = 0.dp, padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(text, fontSize = 12.sp)
            }
        } else {
            Surface(
                onClick = cancel,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .6f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = recorder,
            ) {
                Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
        return
    }
    if (chord == null) {
        if (classic) {
            PushButton(listen, minWidth = 0.dp, minHeight = 0.dp, padding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) { Text(emptyLabel, fontSize = 11.sp) }
        } else {
            Surface(onClick = listen, shape = RoundedCornerShape(8.dp), color = Color.Transparent, border = BorderStroke(1.dp, ink(.12f))) {
                Row(Modifier.padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(3.dp))
                    Text(emptyLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    if (classic) {
        PushButton(listen, minWidth = 0.dp, minHeight = 0.dp, padding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
            Text(chordLabel(chord), fontSize = 12.sp)
        }
        return
    }
    Surface(onClick = listen, shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
        KeyCaps(chord, Modifier.padding(2.dp))
    }
}

/** A chord as keys on a keyboard, one cap for each: Ctrl, Shift and the full stop are three keys. */
@Composable
internal fun KeyCaps(chord: KeyChord, modifier: Modifier = Modifier, dim: Boolean = false) {
    if (skinned()) {
        Text(chordLabel(chord), fontSize = 12.sp, modifier = modifier.alpha(if (dim) .7f else 1f))
        return
    }
    Row(modifier.alpha(if (dim) .7f else 1f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        chordParts(chord).forEach { part ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .9f),
                border = BorderStroke(1.dp, ink(.16f)),
            ) {
                Text(
                    part,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.widthIn(min = 14.dp).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** The chord pressed is somebody else's: says whose, and lets the listener move it here or leave it. */
@Composable
private fun ClashNote(clash: Clash, move: () -> Unit, leave: () -> Unit) {
    val names = clash.others.joinToString(" and ") { "“${it.action.title}”" }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (skinned()) 1f else .5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text("${chordLabel(clash.chord)} is already $names.", fontSize = 12.sp, modifier = Modifier.weight(1f))
            TextButton(move) { Text("Use it here instead", fontSize = 12.sp) }
            IconButton(leave, Modifier.size(28.dp)) { Icon(Icons.Default.Close, "Keep it there", Modifier.size(15.dp)) }
        }
    }
}

@Composable
private fun Note(text: String, error: Boolean) {
    Text(
        text,
        fontSize = 11.sp,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
