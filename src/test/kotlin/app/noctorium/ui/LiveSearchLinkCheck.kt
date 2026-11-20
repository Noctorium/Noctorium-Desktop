package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.playback.PlaybackStatus
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A music link typed into the real window's Search page, and into the Ctrl+F bar, then Enter: the song plays, through
 * the desktop's whole AppState, yt-dlp and mpv. Off unless asked for:
 *
 *     ./gradlew :test --tests "*LiveSearchLinkCheck*" -Dnoctorium.live=true
 */
class LiveSearchLinkCheck {

    private fun WindowTour.playsAfterEnter(state: AppState, link: String, name: String) {
        val before = state.playback.value.track?.queueKey
        type(link)
        settle(2)
        press(Key.Enter, '\n')
        val until = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < until && !(state.playback.value.track?.queueKey != before && state.playback.value.status in setOf(PlaybackStatus.PLAYING, PlaybackStatus.ERROR))) {
            settle(4)
        }
        capture(name)
        val now = state.playback.value
        println("LINKCHECK $name: ${now.status} ${now.track?.provider} ${now.track?.title} — ${now.track?.artistLine} ${now.errorMessage ?: ""} | link ${state.linkState.value.status} ${state.linkState.value.message ?: ""}")
        assertEquals(PlaybackStatus.PLAYING, now.status, "$name: ${now.errorMessage} / ${state.linkState.value.message}")
        kotlin.test.assertNotEquals(before, now.track?.queueKey, "$name: the link's song never started")
    }

    @Test
    fun `a link and Enter plays, on the Search page and in the Ctrl+F bar`() {
        assumeTrue(System.getProperty("noctorium.live") == "true", "live test: -Dnoctorium.live=true")
        val state = desktopAppState()
        state.finishSetup()
        val tour = WindowTour(state, File("build/link-check"))
        try {
            // The Ctrl+F bar first, as the window shows it over Home: the window, once closed, closes the app with it.
            tour.open(content = { androidx.compose.material3.MaterialTheme { QuickSearchBar(state, {}) } })
            tour.settle()
            tour.playsAfterEnter(state, "https://music.youtube.com/watch?v=PzYrr7K1dvU", "quick-search-youtube")
            tour.open()
            state.search("")
            tour.go(Destination.SEARCH)
            tour.settle()
            tour.playsAfterEnter(state, "https://soundcloud.com/flume/never-be-like-you-feat-kai", "search-page-soundcloud")
        } finally {
            tour.close()
            state.close()
        }
    }
}
