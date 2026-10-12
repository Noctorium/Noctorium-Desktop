package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.domain.ProviderType
import app.noctorium.settings.ServiceBadgeStyle
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.icons.markVector
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.skinCorners
import kotlin.math.roundToInt

/*
 * The badge that says which service a song, an album or a playlist comes from: a small chip in the service's colour,
 * with the service's own mark on it, or the letters -- YTM, SC, SP -- it had before there were marks, as the listener
 * chooses on the Icons card, under Look › Theme and colours ([ServiceBadgeStyle]).
 */

/** How the badges are drawn, provided at the window's root from the settings. Marks wherever nothing provides it. */
val LocalServiceBadgeStyle = staticCompositionLocalOf { ServiceBadgeStyle.MARKS }

/** The size the letters are written at. */
private val LETTER_SIZE = 9.sp

/**
 * How big the mark is drawn before the service's name, on a badge in full: a little taller than the letters'
 * capitals, which is what it takes for a shape with no letters in it to be made out beside them. In type's units
 * rather than the screen's, so it grows with the letters when the text is made bigger.
 */
private val MARK_BESIDE_NAME = 11.sp

/** How much of the letters' line a compact badge's mark takes up, the rest being the chip round it. */
private const val MARK_SHARE = .75f

/**
 * The smallest a compact badge's mark is drawn, wherever the letters' line is short -- a Windows skin's -- since a
 * mark much smaller than this is a blur: Spotify's waves run together, and VK's letters are gone.
 */
private val MARK_AT_LEAST = 14.sp

/**
 * [provider]'s badge. [compact] is the one on a card or in most rows, with the service's mark alone or its short
 * name; the other, at the end of a song's row in the lists of songs with room for it, has the service's name in
 * full, after its mark.
 *
 * Drawn either way, the chip is the same: the same colour, the same corners under every skin, and the same height,
 * so changing the setting moves nothing round it. A mark says less than the letters to somebody who does not know it,
 * so it carries the service's name for a screen reader, and as a hint when it is pointed at.
 */
@Composable
internal fun ProviderBadge(provider: ProviderType, compact: Boolean = false) {
    val label = provider.badgeLabel(compact)
    val chip = Modifier
        .clip(skinCorners(RoundedCornerShape(6.dp)))
        .background(provider.badgeColour().copy(alpha = .9f))
    if (LocalServiceBadgeStyle.current == ServiceBadgeStyle.LETTERS) {
        BadgeLetters(label, chip.padding(horizontal = if (compact) 6.dp else 7.dp, vertical = 4.dp))
        return
    }
    HoverHint(provider.displayName) {
        val named = chip.semantics(mergeDescendants = true) { contentDescription = provider.displayName }
        if (compact) {
            CompactMark(provider, label, named.padding(4.dp))
        } else {
            Row(
                named.padding(horizontal = 7.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val mark = with(LocalDensity.current) { MARK_BESIDE_NAME.toDp() }
                Icon(provider.markVector(), null, Modifier.size(mark), tint = Color.White)
                BadgeLetters(label)
            }
        }
    }
}

/**
 * A compact badge's mark, in a square as tall as the badge's [letters] would be. The letters are measured and never
 * drawn, so that the chip is exactly the height it is with them, in whatever typeface, size and line height the place
 * it is in writes: a row of a list, a Windows skin's smaller type, the text made bigger.
 */
@Composable
private fun CompactMark(provider: ProviderType, letters: String, modifier: Modifier) {
    Layout(
        content = {
            BadgeLetters(letters)
            Icon(provider.markVector(), null, tint = Color.White)
        },
        modifier = modifier,
    ) { (line, mark), _ ->
        val side = line.measure(Constraints()).height
        // As much smaller than the square as leaves the same whole pixels each side of it, so it is not half a pixel
        // off its middle.
        val size = maxOf((side * MARK_SHARE).roundToInt(), MARK_AT_LEAST.roundToPx()).coerceAtMost(side)
            .let { it - (side - it) % 2 }
        val drawn = mark.measure(Constraints.fixed(size, size))
        layout(side, side) { drawn.place((side - size) / 2, (side - size) / 2) }
    }
}

@Composable
private fun BadgeLetters(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Color.White, fontSize = LETTER_SIZE, fontWeight = FontWeight.Bold, modifier = modifier)
}

/** Each service's colour, the badge's whichever way it is drawn. */
internal fun ProviderType.badgeColour(): Color = when (this) {
    ProviderType.YOUTUBE_MUSIC -> Color(0xFF8B2AB8)
    ProviderType.YOUTUBE_VIDEO -> Color(0xFFB32C35)
    ProviderType.SOUNDCLOUD -> Color(0xFFC45A16)
    ProviderType.SPOTIFY -> Color(0xFF1DB954)
    ProviderType.BANDCAMP -> Color(0xFF408294)
    ProviderType.VK -> Color(0xFF0077FF)
    ProviderType.LOCAL -> Color(0xFF4F46E5)
}

/** What the badge says in letters: the short name on a [compact] one, the name in full on the other. */
internal fun ProviderType.badgeLabel(compact: Boolean): String = when (this) {
    ProviderType.YOUTUBE_MUSIC -> if (compact) "YTM" else "YT MUSIC"
    ProviderType.YOUTUBE_VIDEO -> if (compact) "YT" else "YOUTUBE"
    ProviderType.SOUNDCLOUD -> if (compact) "SC" else "SOUNDCLOUD"
    ProviderType.SPOTIFY -> if (compact) "SP" else "SPOTIFY"
    ProviderType.BANDCAMP -> if (compact) "BC" else "BANDCAMP"
    ProviderType.VK -> if (compact) "VK" else "VK MUSIC"
    ProviderType.LOCAL -> "LOCAL"
}
