package app.noctorium.core

import app.noctorium.desktopAppState
import app.noctorium.domain.ProviderType
import app.noctorium.playback.PlaybackStatus
import app.noctorium.playback.YtDlpService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pasting a link and hearing the song, through the desktop's whole AppState, the real yt-dlp and mpv.
 *
 * What a unit test cannot show is the part that matters most: that a link known only by its address comes
 * out the other end as a track with a title, an artist and a cover, playing. Off unless asked for:
 *
 *     ./gradlew :test --tests "*LiveLinkTest*" -Dnoctorium.live=true
 */
class LiveLinkTest {

    private val live = System.getProperty("noctorium.live") == "true"

    private suspend fun AppState.playsFromLink(pasted: String) {
        openLink(pasted)
        withTimeout(60_000) {
            while (playback.value.status != PlaybackStatus.PLAYING) {
                check(playback.value.status != PlaybackStatus.ERROR) { "did not play: ${playback.value.errorMessage}" }
                check(linkState.value.status != LinkStatus.FAILED) { "the link failed: ${linkState.value.message}" }
                delay(200)
            }
        }
        val playing = playback.value.track!!
        assertTrue(playing.artists.isNotEmpty(), "playing with no artist: $playing")
        assertTrue(playing.title != "Opening link", "the placeholder title reached the player")
        // The link's own record picks the details up as well, which is what keeps its screen right later.
        withTimeout(10_000) { while (linkState.value.tracks.firstOrNull()?.artists.isNullOrEmpty()) delay(100) }
        assertEquals(playing.queueKey, linkState.value.tracks.single().queueKey)
    }

    @Test
    fun `a YouTube share link plays, dressed as the song it is`() = runBlocking {
        assumeTrue(live, "live test: -Dnoctorium.live=true")
        val state = desktopAppState()
        try {
            state.playsFromLink("listen to this https://youtu.be/PzYrr7K1dvU?si=AbCdEf123 !!")
            assertEquals(ProviderType.YOUTUBE_VIDEO, state.playback.value.track!!.provider)
        } finally {
            state.close()
        }
    }

    @Test
    fun `a SoundCloud link plays`() = runBlocking {
        assumeTrue(live, "live test: -Dnoctorium.live=true")
        // A real track's address, found rather than written down, so the test does not rot when one
        // upload is taken down.
        val address = YtDlpService().search(ProviderType.SOUNDCLOUD, "burial archangel", 1).first().sourceUrl
        val state = desktopAppState()
        try {
            state.playsFromLink("$address?utm_source=clipboard&utm_medium=text")
            assertEquals(ProviderType.SOUNDCLOUD, state.playback.value.track!!.provider)
        } finally {
            state.close()
        }
    }
}
