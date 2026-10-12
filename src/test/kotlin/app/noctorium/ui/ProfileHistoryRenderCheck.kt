package app.noctorium.ui

import androidx.compose.ui.geometry.Offset
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.history.HistoryEntry
import app.noctorium.history.PlayHistoryRepository
import app.noctorium.settings.ThemePreset
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test

/**
 * The profile page with a history in it, drawn off screen for looking at: several days of made-up listening from
 * every service, long titles among them, in a wide window and a narrow one, under the day theme and Windows 98, with
 * the filter typed into, a song's menu and the question before clearing -- and with nothing in it. Off unless a
 * folder is named, like the other checks:
 *
 *     ./gradlew :test --tests "*ProfileHistoryRenderCheck*" -Dnoctorium.renderBars=build/history
 *
 * The history is written to the test folder's own file before each window's state reads it, and an empty one after.
 * The songs and artists are invented, with WindowTour's painted covers. Each window has a state of its own, since
 * the window closes its state when it goes, as Noctorium's does when it quits.
 */
class ProfileHistoryRenderCheck {

    @Test
    fun `the profile page and its history draw`() {
        val folder = System.getProperty("noctorium.renderBars")?.let(::File) ?: return
        val history = madeUpHistory()
        try {
            profile(folder, history) { tour, _ -> tour.capture("profile-history") }
            profile(folder, history, height = 2200) { tour, _ -> tour.capture("profile-history-tall") }
            profile(folder, history, width = 760, height = 900) { tour, _ -> tour.capture("profile-history-narrow") }
            profile(folder, history, theme = ThemePreset.NOCTORIUM_DAY) { tour, _ -> tour.capture("profile-history-day") }
            profile(folder, history, theme = ThemePreset.WINDOWS_98) { tour, _ -> tour.capture("profile-history-98") }
            profile(folder, history, height = 1100) { tour, _ ->
                // The filter, typed into as somebody would.
                tour.click(Offset(FILTER_X, FILTER_Y))
                tour.type("harbour")
                tour.capture("profile-history-filtered")
            }
            profile(folder, history, height = 1100) { tour, _ ->
                // A song's menu, from the ⋮ that shows on the line the pointer is on.
                tour.hover(Offset(600f, ROW_Y))
                tour.settle(2)
                tour.click(Offset(MENU_X, ROW_Y))
                tour.capture("profile-history-menu")
            }
            profile(folder, history, height = 1100) { tour, _ ->
                tour.click(Offset(CLEAR_X, CLEAR_Y))
                tour.capture("profile-history-clear")
            }
            profile(folder, history) { tour, state ->
                tour.ui { it.copy(destination = Destination.HOME) }
                tour.settle(2)
                openSettingsSearch(state, "recently played")
                tour.capture("settings-search-recently-played")
                tour.ui { it.copy(destination = Destination.HOME) }
                tour.settle(2)
                state.navigate(Destination.SETTINGS)
                tour.capture("settings-home")
            }
            profile(folder, emptyList()) { tour, _ -> tour.capture("profile-history-empty") }
        } finally {
            PlayHistoryRepository().save(emptyList())
        }
    }

    /** A window of its own, opened at the profile page with [history] in it, for [draw] to look at. */
    private fun profile(
        folder: File,
        history: List<HistoryEntry>,
        width: Int = 1280,
        height: Int = 800,
        theme: ThemePreset = ThemePreset.NOCTORIUM_NIGHT,
        draw: (WindowTour, AppState) -> Unit,
    ) {
        PlayHistoryRepository().save(history)
        val state = desktopAppState()
        val tour = WindowTour(state, folder)
        try {
            waitFor("the history to be read") { state.history.value.size == history.size }
            // Nothing playing, whatever the test folder's kept queue holds: the player bar is not what is looked at.
            state.clearQueue()
            state.setAnimations(false)
            state.setTheme(theme)
            tour.open(width, height)
            tour.settingsPage("PROFILE")
            tour.settle(4)
            draw(tour, state)
        } finally {
            // Put back while the state is still running to save it: the window going closes it.
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            state.setAnimations(true)
            tour.close()
            state.close()
        }
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!done() && System.currentTimeMillis() < until) Thread.sleep(20)
        check(done()) { "Waited ten seconds for $what" }
    }

    /** Six days of listening, newest first: the last half hour, last night, earlier this week, last month, last year. */
    private fun madeUpHistory(): List<HistoryEntry> {
        val covers = Fixtures.tracks.drop(2).take(9).map { it.artworkUrl }
        fun song(n: Int, title: String, artist: String, provider: ProviderType, second: String? = null) = Track(
            provider = provider,
            id = "history-$n",
            title = title,
            artists = listOfNotNull(artist.takeIf(String::isNotEmpty), second).map { Artist(it.lowercase().replace(' ', '-'), it, provider) },
            durationMs = 180_000L + n * 7_000L,
            artworkUrl = covers[n % covers.size],
            sourceUrl = "https://example.invalid/history/$n",
        )
        val songs = listOf(
            song(1, "Harbour Lights", "Night Tram", ProviderType.YOUTUBE_MUSIC),
            song(2, "Sodium Lamps", "The Lamplighters", ProviderType.SOUNDCLOUD),
            song(3, "Under the Viaduct (Extended Mix for the Last Train Home, Remastered in 2026)", "Signal Box", ProviderType.SPOTIFY, "Morning Ferry"),
            song(4, "Slow Ferry", "Morning Ferry", ProviderType.BANDCAMP),
            song(5, "Paper Moonrise", "Paper Satellites", ProviderType.VK),
            song(6, "Static on the Line — live at the Corn Exchange, second night, with the string section", "Signal Box", ProviderType.YOUTUBE_VIDEO),
            song(7, "Window Seat", "Night Tram", ProviderType.YOUTUBE_MUSIC),
            song(8, "Low Tide Radio", "Morning Ferry", ProviderType.SOUNDCLOUD),
            song(9, "Platform Nine", "Night Tram", ProviderType.SPOTIFY),
            song(10, "Kitchen Radio Demo 3", "", ProviderType.LOCAL),
            song(11, "Harbour at Dusk", "The Lamplighters", ProviderType.BANDCAMP, "Paper Satellites"),
        )
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        fun at(daysAgo: Long, hour: Int, minute: Int) = today.minusDays(daysAgo).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val plays = buildList {
            (0 until 6).forEach { i -> add(HistoryEntry(songs[i % songs.size], now - (4L + i * 4L) * 60_000L)) }
            listOf(23 to 41, 22 to 58, 22 to 54, 21 to 12, 20 to 47, 9 to 3).forEachIndexed { i, (h, m) -> add(HistoryEntry(songs[(i + 3) % songs.size], at(1, h, m))) }
            listOf(18 to 30, 18 to 26, 7 to 55).forEachIndexed { i, (h, m) -> add(HistoryEntry(songs[(i + 6) % songs.size], at(3, h, m))) }
            listOf(13 to 15, 13 to 11).forEachIndexed { i, (h, m) -> add(HistoryEntry(songs[(i + 8) % songs.size], at(5, h, m))) }
            listOf(16 to 0, 15 to 56, 15 to 52).forEachIndexed { i, (h, m) -> add(HistoryEntry(songs[(i + 1) % songs.size], at(24, h, m))) }
            add(HistoryEntry(songs[10], at(400, 19, 20)))
        }
        return plays.sortedByDescending { it.playedAt }
    }

    private companion object {
        // Where things are on the page in a window 1280 wide, read off the pictures: the filter box, the first song's
        // line and its ⋮, and Clear history.
        const val FILTER_X = 500f
        const val FILTER_Y = 490f
        const val ROW_Y = 627f
        const val MENU_X = 924f
        const val CLEAR_X = 840f
        const val CLEAR_Y = 490f
    }
}
