package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.lyrics.LyricLine
import app.noctorium.lyrics.LyricsProviderId
import app.noctorium.lyrics.LyricsProviderOutcome
import app.noctorium.lyrics.LyricsProviderStatus
import app.noctorium.lyrics.LyricsResult
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.QueueState
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.EqualizerPreset
import app.noctorium.settings.EqualizerSettings
import app.noctorium.settings.FontChoice
import app.noctorium.settings.HomePart
import app.noctorium.settings.LyricsAlignment
import app.noctorium.settings.LyricsLook
import app.noctorium.settings.LyricsSize
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.resolvedAccent
import app.noctorium.settings.themeColours
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Draws everything added for making Noctorium the listener's own to pictures, off screen, for looking at:
 * the Sound page, the accent picker, the new settings, the player bars with buttons put away, the lyrics set
 * large, to the left and undimmed, and Home with parts put away. And drags the equaliser and the colour
 * picker the way a listener does, to check that a drag is written once, when it is let go of.
 *
 * Off unless a folder is named, like [PlayerBarRenderCheck]:
 *
 *     ./gradlew :test --tests "*PersonalisationRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class PersonalisationRenderCheck {

    private val folder: File? = System.getProperty("noctorium.renderBars")?.let(::File)?.also { it.mkdirs() }

    private val artist = Artist("judy-collins", "Judy Collins", ProviderType.YOUTUBE_MUSIC)

    private val track = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "dQw4w9WgXcQ",
        title = "Amazing Grace",
        artists = listOf(artist),
        album = Album("whales-and-nightingales", "Whales & Nightingales", listOf(artist), ProviderType.YOUTUBE_MUSIC),
        durationMs = 245_000,
        artworkUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
        sourceUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
    )

    @Test
    fun `the Sound page and the equaliser draw`() {
        val folder = folder ?: return
        withState { state ->
            panel(folder, "sound-off.png", state, 760, 790) { SoundSettingsPanel(it, state) }
            state.setEqualizerPreset(EqualizerPreset.ROCK)
            panel(folder, "sound-rock.png", state, 760, 790) { SoundSettingsPanel(it, state) }
            // The extremes, to see the handles and the readouts at both ends without clipping.
            state.updateEqualizer {
                copy(preset = EqualizerPreset.CUSTOM, customGains = listOf(12f, 9f, 4.5f, 0f, -3f, -7.5f, -12f, -6f, 2f, 12f), preampDb = -6f)
            }
            panel(folder, "sound-custom-extremes.png", state, 760, 790) { SoundSettingsPanel(it, state) }
            state.setTheme(ThemePreset.NOCTORIUM_DAY)
            panel(folder, "sound-custom-day.png", state, 760, 790) { SoundSettingsPanel(it, state) }
        }
    }

    @Test
    fun `dragging a band writes it once, when it is let go of`() {
        val folder = folder ?: return
        withState { state ->
            state.setEqualizerPreset(EqualizerPreset.ROCK)
            val rock = EqualizerPreset.ROCK.gains!!
            val width = 600
            val scene = scene(state, width, 260, padded = false) {
                val equalizer = state.settings.collectAsState().value.preferences.equalizer
                Box(Modifier.width(width.dp)) { EqualizerCurve(equalizer.gains, equalizer.enabled, state::setEqualizerBand) }
            }
            try {
                scene.render(0)
                val column = width / 10f
                // Band 4 (500 Hz) from wherever it is to the top of its column, and a little past it.
                val x = column * 4.5f
                scene.sendPointerEvent(PointerEventType.Move, Offset(x, 120f))
                scene.sendPointerEvent(PointerEventType.Press, Offset(x, 120f))
                listOf(100f, 70f, 40f, 10f, 2f).forEach { y ->
                    scene.sendPointerEvent(PointerEventType.Move, Offset(x + 20f, y))
                    scene.render(0)
                }
                // Mid-drag: drawn, but nothing written yet, so no settings save and no new filter for mpv.
                File(folder, "sound-curve-dragging.png").writeBytes(scene.render(0).encodeToData()!!.bytes)
                assertEquals(EqualizerPreset.ROCK, state.settings.value.preferences.equalizer.preset)
                scene.sendPointerEvent(PointerEventType.Release, Offset(x + 20f, 2f))
                scene.render(0)
                val after = state.settings.value.preferences.equalizer
                assertEquals(EqualizerPreset.CUSTOM, after.preset)
                assertEquals(12f, after.gains[4], "the band dragged to the top")
                // The rest of Rock stays where Rock had it, even though the pointer wandered sideways.
                assertEquals(rock.filterIndexed { i, _ -> i != 4 }, after.gains.filterIndexed { i, _ -> i != 4 })
                File(folder, "sound-curve-after.png").writeBytes(scene.render(0).encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `the accent picker draws, and recolours from a drag it writes once`() {
        val folder = folder ?: return
        withState { state ->
            state.setCustomAccent(0xFF2EC4B6)
            panel(folder, "colour-card-custom.png", state, 760, 1180) { ColourSettingsCard(it, state) }
            // A pale yellow on the Day theme's white page, which the picker should warn about.
            state.setTheme(ThemePreset.NOCTORIUM_DAY)
            state.setCustomAccent(0xFFFFE66D)
            panel(folder, "colour-card-custom-day-warning.png", state, 760, 1180) { ColourSettingsCard(it, state) }
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setCustomAccent(0xFF2EC4B6)
            state.setFont(FontChoice.SERIF)
            panel(folder, "colour-card-serif.png", state, 760, 1180) { ColourSettingsCard(it, state) }
            state.setFont(FontChoice.MONO)
            panel(folder, "colour-card-mono.png", state, 760, 1180) { ColourSettingsCard(it, state) }
            state.setFont(FontChoice.DEFAULT)

            val before = state.settings.value.preferences.customAccent
            val scene = scene(state, 560, 220) {
                val preferences = state.settings.collectAsState().value.preferences
                AccentColourPicker(preferences.customAccent, preferences.themeColours().background, state::setCustomAccent)
            }
            try {
                scene.render(0)
                // Across the square towards a saturated, bright corner: the picker starts at the top left.
                scene.sendPointerEvent(PointerEventType.Move, Offset(30f, 120f))
                scene.sendPointerEvent(PointerEventType.Press, Offset(30f, 120f))
                listOf(80f to 90f, 160f to 50f, 250f to 20f).forEach { (x, y) ->
                    scene.sendPointerEvent(PointerEventType.Move, Offset(x, y))
                    scene.render(0)
                }
                assertNotNull(accentPreview.value, "the window should follow the drag")
                assertEquals(before, state.settings.value.preferences.customAccent, "nothing is written mid-drag")
                scene.sendPointerEvent(PointerEventType.Release, Offset(250f, 20f))
                scene.render(0)
                scene.render(0)
                val chosen = state.settings.value.preferences
                assertNotEquals(before, chosen.customAccent)
                assertEquals(AccentPreset.CUSTOM, chosen.accent)
                assertNull(accentPreview.value, "the preview lets go once the colour is chosen")
                File(folder, "accent-picker-after-drag.png").writeBytes(scene.render(0).encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `the new settings draw`() {
        val folder = folder ?: return
        withState { state ->
            state.updateDesktop {
                copy(
                    hiddenPlayerButtons = setOf(PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES, PlayerButton.LYRICS),
                    hiddenDestinations = setOf(Destination.LINK, Destination.DOWNLOADS),
                    announceTracks = true,
                )
            }
            state.setHomePartHidden(HomePart.GREETING, true)
            state.setHomePartHidden(HomePart.SOUNDCLOUD, true)
            state.updateLyricsLook { LyricsLook(LyricsSize.LARGE, LyricsAlignment.START, dimOtherLines = false) }
            panel(folder, "settings-player-buttons.png", state, 760, 250) { PlayerButtonsSetting(it, state) }
            panel(folder, "settings-browsing.png", state, 760, 830) { BrowsingSettingsCard(it, state) }
            panel(folder, "settings-lyrics-look.png", state, 760, 600) { LyricsLookCard(it.lyrics, state) }
            state.updateLyricsLook { LyricsLook() }
            panel(folder, "settings-lyrics-look-default.png", state, 760, 600) { LyricsLookCard(it.lyrics, state) }
            panel(folder, "settings-startup-announce.png", state, 760, 640) { StartupSettingsPanel(it, state) }
        }
    }

    @Test
    fun `the player bars draw with buttons put away`() {
        val folder = folder ?: return
        val queue = QueueState(tracks = listOf(track), currentIndex = 0)
        val playback = PlaybackState(status = PlaybackStatus.PAUSED, track = track, positionMs = 83_000, durationMs = 245_000)
        withState { state ->
            state.updateDesktop { copy(hiddenPlayerButtons = setOf(PlayerButton.SHUFFLE, PlayerButton.SLEEP_TIMER, PlayerButton.LYRICS)) }
            PlayerBarStyle.entries.forEach { style ->
                state.setPlayerBarStyle(style)
                listOf(1280, 860).forEach { width ->
                    val scene = scene(state, width, 160, padded = false) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
                            PlayerBar(queue, playback, state)
                        }
                    }
                    try {
                        var image = scene.render(0)
                        repeat(20) { frame ->
                            Thread.sleep(100)
                            image = scene.render((frame + 1) * 100_000_000L)
                        }
                        File(folder, "bar-hidden-${style.name.lowercase()}-$width.png").writeBytes(image.encodeToData()!!.bytes)
                    } finally {
                        scene.close()
                    }
                }
            }
            // Everything that can be put away, put away: the transport alone, with download and add to playlist.
            state.updateDesktop { copy(hiddenPlayerButtons = PlayerButton.entries.toSet()) }
            state.setPlayerBarStyle(PlayerBarStyle.INLINE)
            val scene = scene(state, 1280, 120, padded = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.BottomCenter) {
                    PlayerBar(queue, playback, state)
                }
            }
            try {
                scene.render(0)
                File(folder, "bar-hidden-everything-inline.png").writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `the lyrics draw large, to the left and undimmed`() {
        val folder = folder ?: return
        val lines = listOf(
            "The streetlights hum a little tune" to 60_000L,
            "and every window keeps the time" to 66_000L,
            "a long way from the harbour wall" to 72_000L,
            "the boats are sleeping one and all" to 78_000L,
            "until the morning finds us singing" to 84_000L,
            "the same old song we always knew" to 90_000L,
            "and every word of it was true" to 96_000L,
            "so hum it low and hum it slow" to 102_000L,
            "the night is long and we can go" to 108_000L,
        )
        val outcome = LyricsProviderOutcome(
            LyricsProviderId.LRCLIB,
            LyricsProviderStatus.FOUND,
            LyricsResult(LyricsProviderId.LRCLIB, lines.map { (text, at) -> LyricLine(text, at) }, synced = true),
        )
        withState { state ->
            state.setTheme(ThemePreset.CRIMSON)
            listOf(
                "lyrics-default" to LyricsLook(),
                "lyrics-large-start-undimmed" to LyricsLook(LyricsSize.LARGE, LyricsAlignment.START, dimOtherLines = false),
                "lyrics-huge-centre" to LyricsLook(LyricsSize.HUGE, LyricsAlignment.CENTRE),
            ).forEach { (name, look) ->
                state.updateLyricsLook { look }
                // The panel beside the cover, and the wide sing-along column.
                panel(folder, "$name-panel.png", state, 420, 560, padded = false) { LyricsContent(outcome, 85_000, state, large = false) }
                panel(folder, "$name-singalong.png", state, 860, 560, padded = false) { LyricsContent(outcome, 85_000, state, large = true) }
            }
            // The whole screen in the sing-along layout, with whatever lyrics the network finds for the track.
            state.updateLyricsLook { LyricsLook(LyricsSize.LARGE, LyricsAlignment.START, dimOtherLines = false) }
            state.updateNowPlaying { NowPlayingPreferences(layout = NowPlayingLayout.SING_ALONG) }
            val queue = QueueState(tracks = listOf(track), currentIndex = 0)
            val playback = PlaybackState(status = PlaybackStatus.PLAYING, track = track, positionMs = 83_000, durationMs = 245_000)
            val scene = scene(state, 1280, 760, padded = false) { NowPlayingScreen(queue, playback, state) }
            try {
                var image = scene.render(0)
                repeat(60) { frame ->
                    Thread.sleep(100)
                    image = scene.render((frame + 1) * 100_000_000L)
                }
                File(folder, "now-playing-sing_along-large-start-undimmed.png").writeBytes(image.encodeToData()!!.bytes)
            } finally {
                scene.close()
            }
        }
    }

    @Test
    fun `Home draws with parts put away`() {
        val folder = folder ?: return
        fun song(n: Int, provider: ProviderType) = Track(
            provider = provider,
            id = "home-$n",
            title = "A song for the evening $n",
            artists = listOf(Artist("someone-$n", "Someone $n", provider)),
            durationMs = 200_000,
            sourceUrl = "https://example.invalid/$n",
        )
        val ui = AppUiState(
            pinnedTracks = (1..4).map { song(it, ProviderType.YOUTUBE_MUSIC) },
            recentTracks = (5..9).map { song(it, if (it % 2 == 0) ProviderType.SOUNDCLOUD else ProviderType.YOUTUBE_MUSIC) },
            homeSections = listOf(
                HomeSection("quick", "Quick picks", ProviderType.YOUTUBE_MUSIC, "From YouTube Music", (10..15).map { song(it, ProviderType.YOUTUBE_MUSIC) }),
                HomeSection("stream", "Your stream", ProviderType.SOUNDCLOUD, "From SoundCloud", (16..21).map { song(it, ProviderType.SOUNDCLOUD) }),
            ),
            homeLoading = false,
        )
        withState { state ->
            home(folder, "home-default.png", state, ui)
            state.setHomePartHidden(HomePart.GREETING, true)
            state.setHomePartHidden(HomePart.RECENT, true)
            state.setHomePartHidden(HomePart.SOUNDCLOUD, true)
            home(folder, "home-greeting-recent-soundcloud-hidden.png", state, ui)
            HomePart.entries.forEach { state.setHomePartHidden(it, true) }
            home(folder, "home-everything-hidden.png", state, ui)
            HomePart.entries.forEach { state.setHomePartHidden(it, false) }
            state.setHomePartHidden(HomePart.GREETING, true)
            home(folder, "home-nothing-yet.png", state, AppUiState(homeLoading = false))
            HomePart.entries.forEach { state.setHomePartHidden(it, false) }
            state.setFont(FontChoice.SERIF)
            home(folder, "home-serif.png", state, ui)
        }
    }

    // --- Drawing ---

    /**
     * A state for one check, with everything these checks change put back to its default before and after.
     *
     * Before as well as after, because settings are saved in the background and closing the state can cut the
     * last save short, so whatever an earlier check left in the file may still be there. The pause before
     * closing gives the saves of the reset their moment.
     */
    private fun withState(check: (AppState) -> Unit) {
        val state = desktopAppState()
        try {
            reset(state)
            check(state)
        } finally {
            reset(state)
            Thread.sleep(400)
            state.close()
        }
    }

    private fun reset(state: AppState) {
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAccent(AccentPreset.THEME)
        state.setFont(FontChoice.DEFAULT)
        state.updateEqualizer { EqualizerSettings() }
        state.updateLyricsLook { LyricsLook() }
        HomePart.entries.forEach { state.setHomePartHidden(it, false) }
        state.updateDesktop { copy(hiddenPlayerButtons = emptySet(), hiddenDestinations = emptySet(), announceTracks = false) }
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.updateNowPlaying { NowPlayingPreferences() }
        accentPreview.value = null
    }

    private fun home(folder: File, name: String, state: AppState, ui: AppUiState) {
        val scene = scene(state, 1100, 760, padded = false) { HomeScreen(ui, state) }
        try {
            scene.render(0)
            File(folder, name).writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
        } finally {
            scene.close()
        }
    }

    private fun panel(
        folder: File,
        name: String,
        state: AppState,
        width: Int,
        height: Int,
        padded: Boolean = true,
        content: @Composable (app.noctorium.settings.NoctoriumPreferences) -> Unit,
    ) {
        val scene = scene(state, width, height, padded) { content(state.settings.collectAsState().value.preferences) }
        try {
            // Twice: the first frame lays out, the second has what the first one's state changes asked for.
            scene.render(0)
            val file = File(folder, name)
            file.writeBytes(scene.render(100_000_000L).encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }

    /** The application's own theme, typeface included, on the page colour, as the window has them. */
    private fun scene(state: AppState, width: Int, height: Int, padded: Boolean = true, content: @Composable () -> Unit) =
        ImageComposeScene(width, height, Density(1f)) {
            val preferences = state.settings.collectAsState().value.preferences
            MaterialTheme(
                colorScheme = noctoriumColorScheme(preferences.themeColours(), Color(preferences.resolvedAccent(null))),
                typography = noctoriumTypography(preferences.font),
            ) {
                // Still, so each picture is the arrangement rather than a moment of the change into it.
                CompositionLocalProvider(LocalMotion provides false) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (padded) Box(Modifier.padding(20.dp)) { content() } else content()
                    }
                }
            }
        }
}
