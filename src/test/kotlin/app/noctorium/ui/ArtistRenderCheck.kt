package app.noctorium.ui

import app.noctorium.core.AppState
import app.noctorium.core.Destination
import app.noctorium.core.SearchKind
import app.noctorium.desktopAppState
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.ThemePreset
import java.io.File
import kotlin.test.Test

/**
 * Artists' pages and Artists in Search, drawn off screen from the real services: a YouTube Music artist, a SoundCloud
 * profile, and an Artists search. Off unless a folder is named, and it needs the network:
 *
 *     ./gradlew :test --tests "*ArtistRenderCheck*" -Dnoctorium.renderArtists=build/artists
 */
class ArtistRenderCheck {

    private val youTubeSong = Track(
        ProviderType.YOUTUBE_MUSIC, "FGBhQbmPwH8", "One More Time",
        listOf(Artist("YOUTUBE_MUSIC:Daft Punk", "Daft Punk", ProviderType.YOUTUBE_MUSIC)),
        sourceUrl = "https://music.youtube.com/watch?v=FGBhQbmPwH8",
    )
    private val soundCloudSong = Track(
        ProviderType.SOUNDCLOUD, "flume/never-be-like-you-feat-kai", "Never Be Like You",
        listOf(Artist("SOUNDCLOUD:Flume", "Flume", ProviderType.SOUNDCLOUD)),
        sourceUrl = "https://soundcloud.com/flume/never-be-like-you-feat-kai",
    )

    @Test
    fun `artists' pages and Artists in Search draw`() {
        val root = System.getProperty("noctorium.renderArtists")?.let(::File) ?: return
        val state = desktopAppState()
        val tour = WindowTour(state, root)
        try {
            state.setAnimations(false)
            state.setUpdateCheckOnLaunch(false)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            tour.open(1280, 800)
            tour.fill()

            state.openArtistOf(youTubeSong)
            waitForPage(tour, state, "Daft Punk")
            tour.capture("artist-youtube-music", frames = 20)

            state.openArtistOf(soundCloudSong)
            waitForPage(tour, state, "Flume")
            tour.capture("artist-soundcloud", frames = 20)

            state.closePlaylist()
            state.navigate(Destination.SEARCH)
            state.setSearchKind(SearchKind.ARTISTS)
            state.search("odesza")
            repeat(150) {
                if (!state.ui.value.searchLoading && state.ui.value.searchResults.playlists.isNotEmpty()) return@repeat
                tour.settle(2)
            }
            tour.capture("search-artists", frames = 20)

            // An artist opened from Search, and Back: Search again, not the library.
            // As a card does: the page, then the library it is shown in.
            state.openPlaylist(state.ui.value.searchResults.playlists.first { it.provider == ProviderType.SOUNDCLOUD })
            state.navigate(Destination.LIBRARY)
            waitForPage(tour, state, "ODESZA")
            tour.capture("artist-from-search", frames = 30)
            state.closePlaylist()
            tour.capture("back-from-artist", frames = 6)

            tour.open(760, 620)
            state.openArtistOf(youTubeSong)
            waitForPage(tour, state, "Daft Punk")
            tour.capture("artist-narrow", frames = 30)
        } finally {
            state.setSearchKind(SearchKind.MUSIC)
            state.closePlaylist()
            tour.close()
            state.close()
        }
    }

    private fun waitForPage(tour: WindowTour, state: AppState, title: String) {
        repeat(150) {
            val library = state.library.value
            if (library.openPlaylist?.title == title && !library.openPlaylistLoading) return
            tour.settle(2)
        }
    }
}
