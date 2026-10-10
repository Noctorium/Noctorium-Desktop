package app.noctorium.ui

import app.noctorium.core.AppState
import app.noctorium.playback.VolumeBoost
import app.noctorium.settings.KeyChord
import app.noctorium.settings.ShortcutPreferences

/**
 * Does what [shortcut] asks of the player and the queue, wherever it was pressed -- in the window, or anywhere on
 * the machine as a global shortcut. True when it was one of those; false for the ones only the window can answer
 * (the search box, the help sheet, the quick search bar, showing or hiding the window), which the caller handles.
 */
fun AppState.perform(shortcut: Shortcut): Boolean {
    val playback = playback.value
    when (shortcut) {
        Shortcut.PlayPause -> togglePlayback()
        Shortcut.Next -> next()
        Shortcut.Previous -> previous()
        Shortcut.Stop -> stop()
        Shortcut.StopAfterCurrent -> toggleStopAfterCurrent()
        is Shortcut.Seek -> {
            val duration = playback.durationMs
            if (duration > 0) seekTo((playback.positionMs + shortcut.deltaMs).coerceIn(0, duration))
        }
        is Shortcut.Volume -> setVolume((playback.volume + shortcut.delta).coerceIn(0f, 1f))
        Shortcut.Mute -> toggleMute()
        is Shortcut.Boost -> {
            val level = settings.value.preferences.volumeBoostPercent
            setVolumeBoostPercent(VolumeBoost.clamp(level + shortcut.deltaPercent))
        }
        Shortcut.Shuffle -> toggleShuffle()
        Shortcut.Repeat -> cycleRepeat()
        Shortcut.Like -> queue.state.value.current?.let(::toggleLike)
        Shortcut.ClearAllButCurrent -> clearAllButCurrent()
        is Shortcut.Go -> navigate(shortcut.destination)
        Shortcut.Search, Shortcut.QuickSearch, Shortcut.PasteLink, Shortcut.Help, Shortcut.ToggleWindow, Shortcut.Dismiss -> return false
    }
    return true
}

/**
 * The global shortcuts the listener bound, by action -- whether or not they are switched on, which the caller
 * decides -- leaving out a chord that is not a chord any more and any [globalRefusal] would turn away.
 */
fun globalBindings(preferences: ShortcutPreferences, mac: Boolean = isMacPlatform, linux: Boolean = isLinuxPlatform): Map<ShortcutAction, KeyChord> =
    ShortcutAction.entries.filter { it.global }.mapNotNull { action ->
        val chord = preferences.global[action.id]?.let(KeyChord::parse) ?: return@mapNotNull null
        if (globalRefusal(chord, mac, linux) != null) null else action to chord
    }.toMap()

/**
 * What the application around the window hands it: what the global shortcuts are doing, the quick search bar
 * asked for by one of them while Noctorium was already in front (a count, so asking twice opens it twice), and a
 * way to hide the window -- into the tray where there is one.
 */
data class DesktopHooks(
    val globalShortcuts: kotlinx.coroutines.flow.StateFlow<app.noctorium.hotkeys.GlobalHotKeysState>? = null,
    val quickSearchAsked: Int = 0,
    val toggleWindow: (() -> Unit)? = null,
)

/**
 * What the global shortcuts are doing, for the Shortcuts page: handed down from where they are registered, which
 * is the application, not the window. Null where nothing registers them -- a picture drawn off screen.
 */
internal val LocalGlobalShortcuts =
    androidx.compose.runtime.staticCompositionLocalOf<kotlinx.coroutines.flow.StateFlow<app.noctorium.hotkeys.GlobalHotKeysState>?> { null }
