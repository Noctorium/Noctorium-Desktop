package app.noctorium.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import app.noctorium.core.AppState
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track

/**
 * The artist's name under a song, made into the way to their page: pressing it opens the artist on the service the song
 * came from -- YouTube Music's artist for a YouTube Music song, the SoundCloud profile for a SoundCloud one -- in the
 * library, as their songs. Underlined while the pointer is on it, so it reads as a link only when it is about to be one.
 *
 * Nothing for no song, a station or a file on this computer, which have no artist to open.
 */
@Composable
internal fun Modifier.opensArtistOf(track: Track?, state: AppState): Modifier {
    if (track == null || track.provider == ProviderType.RADIO || track.provider == ProviderType.LOCAL || track.artistLine.isBlank()) {
        return this
    }
    val pointer = remember { MutableInteractionSource() }
    val hovered by pointer.collectIsHoveredAsState()
    return this
        .pointerHoverIcon(PointerIcon.Hand)
        .hoverable(pointer)
        .drawBehind {
            if (hovered) {
                val y = size.height - 1f
                drawLine(Color.Gray.copy(alpha = .8f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
        }
        .clickable(interactionSource = pointer, indication = null, role = Role.Button, onClickLabel = "Open the artist") {
            state.openArtistOf(track)
        }
}
