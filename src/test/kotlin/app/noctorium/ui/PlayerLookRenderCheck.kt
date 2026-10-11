package app.noctorium.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.playback.AudioAnalysis
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.StreamFormat
import app.noctorium.settings.IconSet
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.QueueColumn
import app.noctorium.settings.SettingsRepository
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.VisualiserStyle
import app.noctorium.ui.icons.DESKTOP_ICON_SETS
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.test.Test

/**
 * Draws the desktop-music-player look to pictures, off screen, in the whole window with made-up music in it: the
 * Toolbar bar, the Slider, the Playlist layout with its table, visualiser and status bar, the volume booster, the
 * song's menu from the player bar, the queue's new buttons, the new settings, and every icon set -- in Night, a dark
 * theme and a pale one, and under 98, Noctorium 98 and XP. Off unless a folder is named, like the other render
 * checks; the pictures go to `looks/` under it:
 *
 *     ./gradlew :test --tests "*PlayerLookRenderCheck*" -Dnoctorium.renderBars=build/bars --no-configuration-cache
 *
 * The visualiser is handed a frame of sound to draw, since nothing plays in a picture.
 */
class PlayerLookRenderCheck {

    private val root: File? = System.getProperty("noctorium.renderBars")?.let(::File)

    /** A moment of a song as the player would measure it: the bass and the low middle strongest, falling off above. */
    private val sound = MutableStateFlow<AudioAnalysis?>(
        AudioAnalysis(
            bands = listOf(.58f, .74f, .69f, .62f, .57f, .6f, .51f, .44f, .36f, .22f),
            peaks = listOf(.86f, .81f),
            rms = listOf(.63f, .59f),
        ),
    )

    private val themes = listOf(
        ThemePreset.NOCTORIUM_NIGHT,
        ThemePreset.TOKYO_NIGHT,
        ThemePreset.CATPPUCCIN_LATTE,
        ThemePreset.WINDOWS_98,
        ThemePreset.WINDOWS_98_NOCTORIUM,
        ThemePreset.WINDOWS_XP,
    )

    private val playing = PlaybackState(
        status = PlaybackStatus.PLAYING,
        track = Fixtures.queue[2],
        positionMs = 58_000,
        durationMs = Fixtures.queue[2].durationMs ?: 0,
        format = StreamFormat("opus", 48_000, 2, 128_000),
    )

    private fun WindowTour.openWithSound(state: AppState, width: Int = 1280, height: Int = 800) =
        open(width, height) { CompositionLocalProvider(LocalAudioAnalysis provides sound) { NoctoriumApp(state) } }

    private fun WindowTour.fillPlaying() {
        fill()
        playback(playing)
    }

    @Test
    fun `the new look draws in every theme`() {
        val root = root ?: return
        keepingFiles {
            val state = desktopAppState()
            try {
                prepare(state)
                themes.forEach { theme ->
                    state.setTheme(theme)
                    val skinned = theme.skin != app.noctorium.settings.ThemeSkin.STANDARD
                    val tour = WindowTour(state, File(root, "looks/${theme.name.lowercase()}"))
                    try {
                        tour.openWithSound(state)
                        tour.fillPlaying()
                        tour.go(Destination.HOME)
                        tour.capture("home-toolbar")
                        tour.capture("toolbar", crop = Rect.makeXYWH(0f, 800f - 90f, 1280f, 90f))
                        // The song's menu, from a right click on the song in the bar.
                        val song = when (theme.skin) {
                            app.noctorium.settings.ThemeSkin.WINDOWS_98 -> Offset(350f, 781f)
                            app.noctorium.settings.ThemeSkin.WINDOWS_XP -> Offset(470f, 781f)
                            app.noctorium.settings.ThemeSkin.STANDARD -> Offset(520f, 774f)
                        }
                        tour.rightClick(song)
                        tour.capture("toolbar-song-menu", crop = Rect.makeXYWH(300f, 0f, 700f, 800f))
                        tour.press(Key.Escape, '\u001b')
                        tour.settle()
                        // The speaker, opened, with the booster at 400%.
                        tour.click(Offset(if (skinned) 1254f else 1247f, if (skinned) 781f else 774f))
                        tour.capture("volume-booster", crop = Rect.makeXYWH(880f, 380f, 400f, 420f))
                        tour.press(Key.Escape, '\u001b')
                        tour.settle()
                        state.setPlayerBarPosition(PlayerBarPosition.TOP)
                        tour.capture("toolbar-top", crop = Rect.makeXYWH(0f, 0f, 1280f, 90f))
                        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
                        tour.go(Destination.NOW_PLAYING)
                        tour.capture("now-playing-playlist", frames = 16)
                        state.setVisualiser(VisualiserStyle.LEVEL_METER)
                        state.setQueueColumns(QueueColumn.entries.toList())
                        tour.capture("now-playing-playlist-meter-every-column", frames = 16)
                        state.setVisualiser(VisualiserStyle.BARS)
                        state.setQueueColumns(QueueColumn.DEFAULT)
                        // Stop after this song set, and the menu of a row of the table.
                        state.setStopAfterCurrent(true)
                        tour.rightClick(Offset(760f, if (skinned) 200f else 210f))
                        tour.capture("now-playing-row-menu", frames = 10)
                        tour.press(Key.Escape, '\u001b')
                        tour.settle()
                        tour.go(Destination.QUEUE)
                        tour.capture("queue")
                        state.setStopAfterCurrent(false)
                        // The other bars, and the Slider in them.
                        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.STACKED, PlayerBarStyle.DISPLAY, PlayerBarStyle.TASKBAR).forEach { style ->
                            state.setPlayerBarStyle(style)
                            tour.go(Destination.HOME)
                            tour.capture("bar-${style.name.lowercase()}-slider", crop = Rect.makeXYWH(0f, 800f - 110f, 1280f, 110f))
                        }
                        state.setPlayerBarStyle(PlayerBarStyle.TOOLBAR)
                        // Narrower windows: the layout without its cover's panel, and the toolbar giving things up.
                        listOf(860, 640).forEach { width ->
                            tour.openWithSound(state, width, 640)
                            tour.fillPlaying()
                            tour.go(Destination.NOW_PLAYING)
                            tour.capture("now-playing-playlist-$width", frames = 16)
                            tour.go(Destination.HOME)
                            tour.capture("toolbar-$width", crop = Rect.makeXYWH(0f, 640f - 90f, width.toFloat(), 90f))
                        }
                        // The settings with the new choices in, whole.
                        tour.openWithSound(state, 1280, 8400)
                        tour.fillPlaying()
                        tour.settingsPage("CUSTOMIZATION")
                        tour.capture("settings-customization-whole")
                        tour.openWithSound(state, 1280, 1500)
                        tour.fillPlaying()
                        tour.settingsPage("PLAYBACK_QUEUE")
                        tour.capture("settings-playback")
                    } finally {
                        tour.close()
                    }
                }
            } finally {
                restore(state)
                state.close()
            }
        }
    }

    @Test
    fun `every icon set draws`() {
        val root = root ?: return
        keepingFiles {
            val state = desktopAppState()
            try {
                prepare(state)
                state.setPlayerBarStyle(PlayerBarStyle.INLINE)
                state.updateNowPlaying { NowPlayingPreferences() }
                listOf(ThemePreset.NOCTORIUM_NIGHT, ThemePreset.CATPPUCCIN_LATTE, ThemePreset.WINDOWS_98).forEach { theme ->
                    state.setTheme(theme)
                    val tour = WindowTour(state, File(root, "looks/icons/${theme.name.lowercase()}"))
                    try {
                        DESKTOP_ICON_SETS.forEach { set ->
                            state.setIconSet(set)
                            tour.openWithSound(state)
                            tour.fillPlaying()
                            tour.go(Destination.HOME)
                            tour.capture("home-${set.name.lowercase()}")
                            tour.go(Destination.NOW_PLAYING)
                            tour.capture("now-playing-${set.name.lowercase()}", frames = 14)
                            tour.go(Destination.QUEUE)
                            tour.capture("queue-${set.name.lowercase()}")
                        }
                        // The picker, as Settings shows it.
                        state.setIconSet(IconSet.LINE)
                        tour.openWithSound(state, 1280, 8400)
                        tour.fillPlaying()
                        tour.settingsPage("CUSTOMIZATION")
                        tour.capture("settings-customization-line")
                    } finally {
                        tour.close()
                    }
                }
            } finally {
                state.setIconSet(IconSet.STANDARD)
                restore(state)
                state.close()
            }
        }
    }

    /**
     * The song in every bar, right-clicked: its menu, with Add to playlist in it, opens where the pointer was. Each
     * layout has its song somewhere else, so each is clicked where its own is; under 98 and XP as well as Night.
     */
    @Test
    fun `the song in every bar opens its menu on a right click`() {
        val root = root ?: return
        keepingFiles {
            val state = desktopAppState()
            // Where the song is in each layout, in a window 1280 by 800: the standard skin's, and where 98's narrower rail and XP's
            // task pane, and the skins' smaller bars, put it instead.
            val standard = mapOf(
                PlayerBarStyle.INLINE to Offset(600f, 763f),
                PlayerBarStyle.STACKED to Offset(330f, 768f),
                PlayerBarStyle.CENTERED to Offset(320f, 756f),
                PlayerBarStyle.SLIM to Offset(320f, 776f),
                PlayerBarStyle.SLIM_LEFT to Offset(480f, 776f),
                PlayerBarStyle.SPOTLIGHT to Offset(400f, 712f),
                PlayerBarStyle.FLOATING to Offset(620f, 756f),
                PlayerBarStyle.ISLAND to Offset(620f, 762f),
                PlayerBarStyle.DISPLAY to Offset(700f, 752f),
                PlayerBarStyle.TASKBAR to Offset(560f, 774f),
                PlayerBarStyle.TOOLBAR to Offset(520f, 774f),
            )
            val songs = mapOf(
                ThemePreset.NOCTORIUM_NIGHT to standard,
                ThemePreset.WINDOWS_98 to standard + mapOf(
                    PlayerBarStyle.CENTERED to Offset(200f, 750f),
                    PlayerBarStyle.SLIM to Offset(200f, 762f),
                    PlayerBarStyle.SLIM_LEFT to Offset(360f, 762f),
                    PlayerBarStyle.SPOTLIGHT to Offset(270f, 714f),
                    PlayerBarStyle.FLOATING to Offset(510f, 748f),
                    PlayerBarStyle.TASKBAR to Offset(360f, 786f),
                    PlayerBarStyle.TOOLBAR to Offset(350f, 781f),
                ),
                ThemePreset.WINDOWS_XP to standard + mapOf(
                    PlayerBarStyle.SLIM to Offset(330f, 762f),
                    PlayerBarStyle.SLIM_LEFT to Offset(490f, 762f),
                    PlayerBarStyle.TOOLBAR to Offset(470f, 781f),
                ),
            )
            try {
                prepare(state)
                songs.forEach { (theme, places) ->
                    state.setTheme(theme)
                    val tour = WindowTour(state, File(root, "looks/song-menu/${theme.name.lowercase()}"))
                    try {
                        tour.openWithSound(state)
                        tour.fillPlaying()
                        tour.go(Destination.HOME)
                        places.forEach { (style, at) ->
                            state.setPlayerBarStyle(style)
                            tour.settle()
                            tour.rightClick(at)
                            tour.capture("menu-${style.name.lowercase()}", frames = 10)
                            tour.press(Key.Escape, '\u001b')
                            tour.hover(Offset(640f, 300f))
                            tour.settle()
                        }
                    } finally {
                        tour.close()
                    }
                }
            } finally {
                restore(state)
                state.close()
            }
        }
    }

    /** The new look, chosen, with nothing that would make two drawings of the same screen differ. */
    private fun prepare(state: AppState) {
        state.setAnimations(false)
        state.setIconSet(IconSet.STANDARD)
        state.setConnectEnabled(false)
        state.setUpdateCheckOnLaunch(false)
        state.setPlayerBarStyle(PlayerBarStyle.TOOLBAR)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.setProgressBarStyle(ProgressBarStyle.SLIDER)
        state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.PLAYLIST) }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
        state.setVisualiser(VisualiserStyle.BARS)
        state.setQueueColumns(QueueColumn.DEFAULT)
        state.setStatusBar(true)
        state.setVolumeBoostPercent(400)
    }

    private fun restore(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAnimations(true)
        state.setConnectEnabled(true)
        state.setUpdateCheckOnLaunch(true)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
        state.setVolumeBoostPercent(100)
        state.setIconSet(IconSet.STANDARD)
        state.setVisualiser(VisualiserStyle.BARS)
        state.setQueueColumns(QueueColumn.DEFAULT)
        state.setStopAfterCurrent(false)
    }

    /**
     * Runs [check] and puts the files the application keeps -- its settings, the kept queue, what was played lately --
     * back as they were before it. The render checks share one folder of those, and this one's choices, a boost at
     * 400% or the Toolbar, left there would show in every check after it. Putting the state's own settings back is
     * not enough: a tour closes the window, which closes the state, and a state that is closed writes nothing more.
     */
    private fun keepingFiles(check: () -> Unit) {
        val home = SettingsRepository.defaultSettingsPath()?.parent
        val before = home?.toFile()?.listFiles { file -> file.isFile }.orEmpty().associate { it.name to it.readBytes() }
        try {
            check()
        } finally {
            // A last write from the state closing may still be on its way.
            Thread.sleep(800)
            home?.toFile()?.listFiles { file -> file.isFile }.orEmpty().forEach { file ->
                val was = before[file.name]
                if (was == null) file.delete() else file.writeBytes(was)
            }
            before.forEach { (name, bytes) -> home?.resolve(name)?.toFile()?.takeUnless { it.exists() }?.writeBytes(bytes) }
        }
    }
}
