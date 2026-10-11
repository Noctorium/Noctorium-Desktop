package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.hotkeys.GlobalHotKeysState
import app.noctorium.hotkeys.HotKeyResult
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Draws the new Settings to pictures, off screen, in the standard theme, a light one, 98, Noctorium 98 and XP: the
 * home with its sections, a page of Look with its way back, the search with results and one chosen, a setting the
 * search found lit up on its page, the Shortcuts page listening for a chord and settling a clash, and the quick
 * search bar with a bang's chip and songs under it. Off unless a folder is named, like [PlayerBarRenderCheck]; the
 * pictures go to `settings-search/` under it:
 *
 *     ./gradlew :test --tests "*SettingsSearchRenderCheck*" -Dnoctorium.renderBars=build/bars --no-configuration-cache
 */
class SettingsSearchRenderCheck {

    private val themes = listOf(
        ThemePreset.NOCTORIUM_NIGHT,
        ThemePreset.NOCTORIUM_DAY,
        ThemePreset.WINDOWS_98,
        ThemePreset.WINDOWS_98_NOCTORIUM,
        ThemePreset.WINDOWS_XP,
    )

    @Test
    fun `the new settings, their search and the quick search draw`() {
        val root = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        val state = desktopAppState()
        try {
            prepare(state)
            themes.forEach { theme ->
                state.setTheme(theme)
                val folder = File(root, "settings-search/${theme.name.lowercase()}")
                tour(WindowTour(state, folder), state)
                shortcuts(folder, state)
            }
        } finally {
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setAnimations(true)
            state.updateDesktop { copy(shortcuts = app.noctorium.settings.ShortcutPreferences()) }
            state.close()
        }
    }

    private fun prepare(state: AppState) {
        state.setAnimations(false)
        state.setConnectEnabled(false)
        state.setUpdateCheckOnLaunch(false)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
    }

    private fun tour(tour: WindowTour, state: AppState) {
        try {
            tour.open()
            tour.fill()
            tour.go(Destination.HOME)
            tour.settle()
            tour.go(Destination.SETTINGS)
            tour.capture("home")
            // The whole of the home, every section, in a window tall enough for it.
            tour.open(width = 1280, height = 1500)
            tour.fill()
            tour.go(Destination.SETTINGS)
            tour.capture("home-whole")
            tour.open()
            tour.fill()
            // A page of Look, with the way back and the section it is in.
            tour.settingsPage("PLAYER_BAR")
            tour.capture("page-player-bar")
            // The search: what people call a setting, and the arrow moving the choice down one.
            openSearch(tour, state, "dark mode")
            tour.capture("search-dark-mode")
            openSearch(tour, state, "boost")
            tour.press(Key.DirectionDown, '\u0000')
            tour.capture("search-boost-chosen")
            openSearch(tour, state, "hotkeys")
            tour.capture("search-hotkeys")
            openSearch(tour, state, "equalizer")
            tour.capture("search-equalizer")
            openSearch(tour, state, "zzzz")
            tour.capture("search-nothing")
            // A setting found, lit up on its page after scrolling to it: well down its page, so the scroll shows.
            tour.settingsPage("NOW_PLAYING", anchor = "Dim the lines not being sung")
            tour.capture("found-dim-lines", frames = 6)
            tour.settingsPage("THEME", anchor = "Animations")
            tour.capture("found-animations", frames = 6)
            tour.settingsPage("SHORTCUTS", anchor = "Stop after this song")
            tour.capture("found-shortcut", frames = 6)
            // The quick search bar over the window: Ctrl+F, a bang, and songs under it, the second one chosen.
            tour.go(Destination.HOME)
            tour.settle()
            tour.press(Key.F, 'f', ctrl = true)
            tour.settle(3)
            tour.capture("quick-search-empty")
            tour.type("!ytm night")
            tour.searched("!ytm night", Fixtures.searchTracks.filter { it.provider == app.noctorium.domain.ProviderType.YOUTUBE_MUSIC })
            tour.settle(2)
            tour.press(Key.DirectionDown, '\u0000')
            tour.capture("quick-search-bang")
            tour.press(Key.Escape, '\u001b')
            tour.settle()
            tour.press(Key.F, 'f', ctrl = true)
            tour.settle(3)
            tour.type("harbour !sc")
            tour.searched("harbour !sc", Fixtures.tracks.filter { it.provider == app.noctorium.domain.ProviderType.SOUNDCLOUD })
            tour.capture("quick-search-bang-at-end")
            tour.press(Key.Escape, '\u001b')
            tour.settle()
        } finally {
            tour.close()
        }
    }

    /** Settings, with [query] typed in its box, as the search box shows it. */
    private fun openSearch(tour: WindowTour, state: AppState, query: String) {
        tour.go(Destination.HOME)
        tour.settle(2)
        openSettingsSearch(state, query)
        tour.settle(2)
    }

    /**
     * The Shortcuts page on its own, under the theme in force: listening for Stop's new chord with Ctrl held, then
     * the clash when Ctrl+F -- the quick search's -- is pressed; and the global shortcuts switched on, with one
     * registered, one another program holds and one being asked about.
     */
    private fun shortcuts(folder: File, state: AppState) {
        val live = MutableStateFlow(
            GlobalHotKeysState(
                how = "Windows' own hot keys",
                enabled = true,
                results = mapOf("quickSearch" to HotKeyResult.Registered, "playPause" to HotKeyResult.Taken, "next" to HotKeyResult.Pending),
            ),
        )
        state.updateDesktop {
            copy(
                shortcuts = app.noctorium.settings.ShortcutPreferences(
                    inApp = mapOf("clearAllButCurrent" to "Ctrl+Shift+Delete"),
                    global = mapOf("quickSearch" to "Ctrl+Alt+Space", "playPause" to "Ctrl+Alt+Home", "next" to "Ctrl+Alt+PageDown"),
                    globalEnabled = true,
                ),
            )
        }
        val scene = ImageComposeScene(1000, 2600, Density(1f)) {
            NoctoriumLook(state) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGlobalShortcuts provides live) {
                    // A Surface, for the writing colour the window's own gives everything inside it.
                    androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(Modifier.padding(24.dp)) {
                            val preferences = state.settings.collectAsState().value.preferences
                            ShortcutsSettingsPanel(preferences, state, Recording(ShortcutAction.STOP, 0))
                        }
                    }
                }
            }
        }
        try {
            var time = 0L
            fun frame(): org.jetbrains.skia.Image {
                var image = scene.render(time)
                repeat(6) {
                    Thread.sleep(40)
                    time += 100_000_000L
                    image = scene.render(time)
                }
                return image
            }
            fun save(name: String, image: org.jetbrains.skia.Image) =
                File(folder, "$name.png").writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
            frame()
            @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
            scene.sendKeyEvent(KeyEvent(Key.CtrlLeft, KeyEventType.KeyDown, isCtrlPressed = true))
            save("shortcuts-recording", frame())
            @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
            scene.sendKeyEvent(KeyEvent(Key.F, KeyEventType.KeyDown, codePoint = 'f'.code, isCtrlPressed = true))
            save("shortcuts-conflict", frame())
        } finally {
            scene.close()
            state.updateDesktop { copy(shortcuts = app.noctorium.settings.ShortcutPreferences()) }
        }
    }
}
