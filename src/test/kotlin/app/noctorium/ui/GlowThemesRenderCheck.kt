package app.noctorium.ui

import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.domain.Album
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.PlaybackContext
import app.noctorium.domain.PlaybackOrigin
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.lyrics.LyricLine
import app.noctorium.lyrics.LyricsProviderId
import app.noctorium.lyrics.LyricsProviderOutcome
import app.noctorium.lyrics.LyricsProviderStatus
import app.noctorium.lyrics.LyricsResult
import app.noctorium.lyrics.LyricsUiState
import app.noctorium.playback.PlaybackState
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.PlaybackToolInstaller
import app.noctorium.playback.RepeatMode
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.settings.NowPlayingPreferences
import app.noctorium.settings.PlayerBarPosition
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SurfaceStyle
import app.noctorium.settings.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import java.io.File
import kotlin.test.Test

/**
 * Draws the Glow themes to pictures, off screen -- Afterglow, Cinder, Velvet and Smoke: Home, Now playing in Sing
 * along and three other layouts, the backdrops, the theme picker, and settings pages, the queue and the player bars
 * in Velvet, whose white accent is the one most likely to lose something -- with Night beside them for comparison.
 * Off unless a folder is named, like [PlayerBarRenderCheck]; the pictures go to `glow/` under it:
 *
 *     ./gradlew :test --tests "*GlowThemesRenderCheck*" -Dnoctorium.renderBars=build/bars
 *
 * The songs are made up, and their covers painted here: one with a red bar down a dark sleeve, one in blues, and
 * one with no cover at all, which is what Afterglow's light is looked at with -- the cover's red, the cover's blue,
 * and the theme's own red with nothing to follow.
 *
 * Every window has an application of its own. Closing a window closes the application it showed, as the real one
 * does when it quits, and a second window on the same one would show a player that has stopped listening to its
 * queue -- shuffle turned on and the bar never hearing of it.
 *
 * Run it with `--no-configuration-cache`: a cached configuration keeps the folders the last run was given.
 */
class GlowThemesRenderCheck {

    @Test
    fun `the Glow themes draw`() {
        val root = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        try {
            listOf(ThemePreset.AFTERGLOW, ThemePreset.CINDER, ThemePreset.VELVET, ThemePreset.SMOKE, ThemePreset.NOCTORIUM_NIGHT).forEach { theme ->
                val folder = File(root, "glow/${theme.name.lowercase()}")
                window(folder, theme, GlowSongs.red) { tour, state ->
                    tour.go(Destination.HOME)
                    tour.capture("home")
                    tour.go(Destination.NOW_PLAYING)
                    listOf(NowPlayingLayout.SING_ALONG, NowPlayingLayout.SIDE_BY_SIDE, NowPlayingLayout.STAGE, NowPlayingLayout.POSTER).forEach { layout ->
                        state.updateNowPlaying { copy(layout = layout) }
                        tour.capture("now-playing-${layout.name.lowercase().replace('_', '-')}", frames = 14)
                    }
                    // The backdrops, in Sing along: the wash as chosen, which a Glow theme draws as Plain, its light and
                    // all; Plain itself; and the blurred cover, which covers the light as it covers the page.
                    state.updateNowPlaying { copy(layout = NowPlayingLayout.SING_ALONG) }
                    listOf(NowPlayingBackdrop.WASH, NowPlayingBackdrop.PLAIN, NowPlayingBackdrop.COVER).forEach { backdrop ->
                        state.setNowPlayingBackdrop(backdrop)
                        tour.capture("backdrop-${backdrop.name.lowercase()}", frames = 14)
                    }
                }
                if (theme == ThemePreset.AFTERGLOW) {
                    // The light follows the cover: blue for the blue one, the theme's red for none.
                    listOf("blue" to GlowSongs.blue, "nocover" to GlowSongs.uncovered).forEach { (name, song) ->
                        window(folder, theme, song) { tour, state ->
                            state.updateNowPlaying { copy(layout = NowPlayingLayout.SING_ALONG) }
                            tour.go(Destination.NOW_PLAYING)
                            tour.capture("now-playing-sing-along-$name", frames = 20)
                            tour.go(Destination.HOME)
                            tour.capture("home-$name", frames = 14)
                        }
                    }
                }
                if (theme == ThemePreset.VELVET || theme == ThemePreset.NOCTORIUM_NIGHT) {
                    window(folder, theme, GlowSongs.red) { tour, state -> whiteAccent(tour, state) }
                    // The themes, whole, in a window tall enough for every family.
                    window(folder, theme, GlowSongs.red, height = 1700) { tour, _ ->
                        tour.settingsPage("THEME")
                        tour.capture("settings-theme")
                    }
                }
            }
        } finally {
            // The settings as the other tests expect to find them, once, rather than after every window: put back after
            // each, devices would be looked for between one window and the next.
            desktopAppState().also { state -> restore(state); state.close() }
        }
    }

    /**
     * What a white accent could lose, drawn in Velvet and, for comparison, Night: the player bars with shuffle and
     * repeat on, the queue's chips, settings pages of switches and choices, and the window under glass, where the
     * page beneath the panes is the light.
     */
    private fun whiteAccent(tour: WindowTour, state: AppState) {
        state.queue.setShuffle(true)
        state.queue.setRepeat(RepeatMode.ALL)
        tour.go(Destination.HOME)
        listOf(PlayerBarStyle.INLINE, PlayerBarStyle.CENTERED, PlayerBarStyle.SLIM, PlayerBarStyle.TOOLBAR, PlayerBarStyle.TASKBAR).forEach { style ->
            state.setPlayerBarStyle(style)
            tour.capture("bar-${style.name.lowercase()}", crop = Rect.makeXYWH(0f, 800f - 130f, 1280f, 130f))
        }
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        listOf(ProgressBarStyle.MATERIAL, ProgressBarStyle.SLIDER, ProgressBarStyle.NEON).forEach { style ->
            state.setProgressBarStyle(style)
            tour.capture("bar-seek-${style.name.lowercase()}", crop = Rect.makeXYWH(0f, 800f - 130f, 1280f, 130f))
        }
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        tour.go(Destination.QUEUE)
        tour.capture("queue")
        state.setSurfaceStyle(SurfaceStyle.GLASS)
        tour.go(Destination.HOME)
        tour.capture("home-glass")
        tour.go(Destination.NOW_PLAYING)
        tour.capture("now-playing-glass", frames = 14)
        state.setSurfaceStyle(SurfaceStyle.SOLID)
        state.queue.setShuffle(false)
        state.queue.setRepeat(RepeatMode.OFF)
        listOf("PLAYBACK_QUEUE", "NOW_PLAYING", "STARTUP").forEach { page ->
            tour.settingsPage(page)
            tour.capture("settings-${page.lowercase().replace('_', '-')}")
        }
    }

    /**
     * A window of its own, in [theme], with [playing] paused in it, for [draw] to take pictures of. The song goes in
     * before the window opens, so its first frame has it -- the player bar's cover too, which otherwise keeps the
     * last song's while the next one loads.
     */
    private fun window(folder: File, theme: ThemePreset, playing: Track, height: Int = 800, draw: (WindowTour, AppState) -> Unit) {
        val state = desktopAppState()
        val tour = WindowTour(state, folder)
        try {
            prepare(state)
            state.setTheme(theme)
            fill(tour, state, playing)
            tour.open(height = height)
            waitForTools()
            fill(tour, state, playing)
            tour.settle(4)
            draw(tour, state)
        } finally {
            tour.close()
            state.close()
        }
    }

    /** The made-up songs in the queue, [playing] among them and paused a little way in, and Home filled with them. */
    private fun fill(tour: WindowTour, state: AppState, playing: Track) {
        val queue = listOf(playing) + GlowSongs.others.take(6)
        state.queue.playQueue(queue, 0, PlaybackContext(ProviderType.YOUTUBE_MUSIC, PlaybackOrigin.HOME))
        tour.playback(PlaybackState(status = PlaybackStatus.PAUSED, track = playing, positionMs = 12_000, durationMs = playing.durationMs ?: 110_000))
        tour.ui {
            it.copy(
                recentTracks = GlowSongs.others.drop(2),
                pinnedTracks = listOf(GlowSongs.red, GlowSongs.blue, GlowSongs.others[0], GlowSongs.uncovered),
                homeSections = listOf(
                    HomeSection("glow-quick", "Listening right now", ProviderType.YOUTUBE_MUSIC, "From YouTube Music", GlowSongs.others),
                ),
                homeLoading = false,
                errorMessage = null,
            )
        }
        lyrics(state, playing)
    }

    /**
     * Synced lyrics for [song], put where the lyrics panel looks, so it shows words rather than asking the lyric
     * sources about a song none of them has heard of. Keyed as AppState keys a lookup, which is what keeps it from
     * asking anyway.
     */
    private fun lyrics(state: AppState, song: Track) {
        @Suppress("UNCHECKED_CAST")
        val flow = AppState::class.java.getDeclaredField("mutableLyrics").apply { isAccessible = true }.get(state) as MutableStateFlow<LyricsUiState>
        val lines = GlowSongs.words.mapIndexed { index, words -> LyricLine(words, startTimeMs = index * 5_000L) }
        val result = LyricsResult(LyricsProviderId.LRCLIB, lines, synced = true)
        flow.value = LyricsUiState(
            trackKey = "${song.queueKey}|${song.title}|${song.artistLine}|${song.durationMs ?: 0}",
            loading = false,
            outcomes = listOf(LyricsProviderOutcome(LyricsProviderId.LRCLIB, LyricsProviderStatus.FOUND, result)),
            selectedProvider = LyricsProviderId.LRCLIB,
        )
    }

    /** Settings that would otherwise make two drawings of the same screen differ: motion, devices, updates. */
    private fun prepare(state: AppState) {
        state.setAnimations(false)
        state.setConnectEnabled(false)
        state.setUpdateCheckOnLaunch(false)
        state.setProgressBarStyle(ProgressBarStyle.MINIMAL)
        state.setPlayerBarStyle(PlayerBarStyle.INLINE)
        state.setPlayerBarPosition(PlayerBarPosition.BOTTOM)
        state.setSurfaceStyle(SurfaceStyle.SOLID)
        state.updateNowPlaying { NowPlayingPreferences() }
        state.setNowPlayingBackdrop(NowPlayingBackdrop.WASH)
    }

    private fun restore(state: AppState) {
        prepare(state)
        state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
        state.setAnimations(true)
        state.setConnectEnabled(true)
        state.setUpdateCheckOnLaunch(true)
    }

    /** Waits out a first run's fetching of yt-dlp and mpv, whose banner would otherwise be in every picture. */
    private fun waitForTools() {
        val until = System.currentTimeMillis() + 180_000
        while (PlaybackToolInstaller.state.value.installing != null && System.currentTimeMillis() < until) Thread.sleep(500)
    }
}

/** Made-up songs for the Glow pictures, with covers painted rather than fetched. */
private object GlowSongs {
    private val covers: File = File(System.getProperty("user.dir"), "build/glow-covers").apply { mkdirs() }

    private fun artist(name: String) = Artist(name.lowercase().replace(' ', '-'), name, ProviderType.YOUTUBE_MUSIC)

    private fun song(n: Int, title: String, artist: String, cover: String?, album: String? = null) = Track(
        provider = ProviderType.YOUTUBE_MUSIC,
        id = "glow-$n",
        title = title,
        artists = listOf(artist(artist)),
        album = album?.let { Album("glow-album-$n", it, listOf(artist(artist)), ProviderType.YOUTUBE_MUSIC) },
        durationMs = 110_000L + n * 9_000L,
        artworkUrl = cover,
        sourceUrl = "https://example.invalid/glow/$n",
    )

    /** A dark sleeve, a pale shape on it, and a red bar down the middle: a cover whose only colour is the red. */
    val red = song(1, "One More Evening", "Mira Holloway", cover("red") { size, canvas ->
        canvas.drawPaint(Paint().apply { shader = Shader.makeLinearGradient(0f, 0f, size, size, intArrayOf(0xFF2A2A2A.toInt(), 0xFF0E0E0E.toInt()), null, GradientStyle.DEFAULT) })
        canvas.drawCircle(size * .42f, size * .46f, size * .26f, Paint().apply { color = 0xFF3C3A39.toInt() })
        canvas.drawRect(Rect.makeXYWH(size * .42f, size * .15f, size * .17f, size * .62f), Paint().apply { color = 0xFFC2261A.toInt() })
    }, album = "Lamplight")

    /** Deep blues, a paler disc. */
    val blue = song(2, "Tidewater", "The Quiet Pines", cover("blue") { size, canvas ->
        canvas.drawPaint(Paint().apply { shader = Shader.makeLinearGradient(0f, 0f, size, size, intArrayOf(0xFF1C3F9E.toInt(), 0xFF0A1433.toInt()), null, GradientStyle.DEFAULT) })
        canvas.drawCircle(size * .5f, size * .55f, size * .24f, Paint().apply { color = 0xFF4F86E8.toInt() })
    })

    /** No cover at all. */
    val uncovered = song(3, "Paper Room", "Lumen Hart", null)

    val others: List<Track> = listOf(
        Triple("Neon Orchard", "Saffron Kites", 0xFFD93279),
        Triple("Glasshouse", "Corin Vale", 0xFF9AA0B8),
        Triple("Summer Static", "Juno Pell", 0xFFE8BE2E),
        Triple("Low Orbit", "Oda Marsh", 0xFF8A8A80),
        Triple("Lantern Field", "Ivo Lark", 0xFF6E5A50),
        Triple("Slow Signal", "The Night Ferries", 0xFF2E9C8C),
        Triple("Wild Plum", "Tamsin Rowe", 0xFF7A3C8E),
    ).mapIndexed { index, (title, artist, colour) ->
        song(10 + index, title, artist, cover("plain-$index") { size, canvas ->
            canvas.drawPaint(Paint().apply { color = colour.toInt() })
            canvas.drawCircle(size * .5f, size * .5f, size * .3f, Paint().apply { color = 0x33FFFFFF })
        })
    }

    /** Words for the made-up song, one line every five seconds. */
    val words = listOf(
        "Streetlights hum the long way home",
        "Every window keeps a light for you",
        "Stay a while, the night is warm",
        "Every window keeps a light for you",
        "We counted stars we could not see",
        "Tell me this is not the last time",
        "The radio plays on without us",
        "Stay a while, the night is warm",
    )

    private fun cover(name: String, paint: (Float, org.jetbrains.skia.Canvas) -> Unit): String {
        val file = File(covers, "$name.png")
        val size = 320
        val surface = Surface.makeRasterN32Premul(size, size)
        paint(size.toFloat(), surface.canvas)
        file.writeBytes(checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)).bytes)
        return file.toURI().toString()
    }
}

