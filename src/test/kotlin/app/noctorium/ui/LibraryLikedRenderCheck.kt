package app.noctorium.ui

import app.noctorium.core.Destination
import app.noctorium.desktopAppState
import app.noctorium.domain.Playlist
import app.noctorium.domain.ProviderType
import app.noctorium.settings.ThemePreset
import java.io.File
import kotlin.test.Test

/**
 * The library with each service's liked songs heading it as wide cards, and the Browsing page's Home layout. Off
 * unless a folder is named:
 *
 *     ./gradlew :test --tests "*LibraryLikedRenderCheck*" -Dnoctorium.renderLiked=build/liked
 */
class LibraryLikedRenderCheck {

    @Test
    fun `liked songs head the library and Home layout is offered`() {
        val root = System.getProperty("noctorium.renderLiked")?.let(::File) ?: return
        val state = desktopAppState()
        val tour = WindowTour(state, root)
        try {
            state.setAnimations(false)
            state.setUpdateCheckOnLaunch(false)
            state.setTheme(ThemePreset.NOCTORIUM_NIGHT)
            tour.open(1280, 800)
            tour.fill()
            val liked = listOf(
                Playlist("LM", "Liked Music", ProviderType.YOUTUBE_MUSIC, trackCount = 412),
                Playlist("likes", "Liked tracks", ProviderType.SOUNDCLOUD, trackCount = 87),
            )
            tour.library { it.copy(playlists = it.playlists + liked) }
            tour.go(Destination.LIBRARY)
            tour.capture("library-liked", frames = 12)

            tour.settingsPage("BROWSING", "Home layout")
            tour.capture("settings-home-layout", frames = 12)
        } finally {
            tour.close()
            state.close()
        }
    }
}
