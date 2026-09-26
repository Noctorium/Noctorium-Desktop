package app.noctorium.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.noctorium.settings.ThemeColours
import app.noctorium.settings.ThemePreset

/** Noctorium's own violet, for the places that are the brand rather than the theme: the mark, the icon. */
val NoctoriumPurpleStrong = Color(0xFF8B5CF6)
val NoctoriumLavender = Color(0xFFD8B4FE)

/** The theme a window starts in before the settings have been read. */
val DefaultTheme: ThemeColours = ThemePreset.NOCTORIUM_NIGHT.colours!!

/**
 * The writing colour at [alpha]: white on a dark theme, near-black on a light one.
 *
 * For hairlines, hover tints, dim captions and the other places that used to say `Color.White.copy(alpha)`
 * when the page was always black. On a pale page those vanished -- the time under the seek bar was the
 * first to go -- because a translucent white on white is white.
 */
@Composable
fun ink(alpha: Float): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

/**
 * Blends toward [other]; used to derive a whole scheme from a theme's six colours.
 *
 * Opacity is blended along with the rest. It used to be dropped -- the result was always fully opaque --
 * which cost nothing while every colour here was opaque anyway, and turned two of the glass surfaces
 * back into solid ones the moment they were not.
 */
internal fun Color.mix(other: Color, ratio: Float): Color = Color(
    red = red + (other.red - red) * ratio,
    green = green + (other.green - green) * ratio,
    blue = blue + (other.blue - blue) * ratio,
    alpha = alpha + (other.alpha - alpha) * ratio,
)

/**
 * Material's palette, worked out from a theme's six colours and the accent in force.
 *
 * The theme says where the page, a panel and a card sit and what writing looks like on them; the accent
 * is whatever the listener chose, which may be the theme's own or the cover's. Containers, outlines and
 * the colour of writing on the accent are derived rather than hand-picked, so a theme written as six
 * numbers -- or sampled from a cover -- comes out coherent, and a light theme comes out as light rather
 * than as a dark theme with a white page.
 */
fun noctoriumColorScheme(theme: ThemeColours, accent: Color): ColorScheme {
    val background = Color(theme.background)
    val panel = Color(theme.panel)
    val card = Color(theme.card)
    val text = Color(theme.text)
    val subtext = Color(theme.subtext)
    // Writing on the accent itself: dark on a bright accent, light on a deep one.
    val onAccent = if (accent.luminance() > .35f) accent.mix(Color.Black, .84f) else Color.White
    val error = if (theme.light) Color(0xFFB3261E) else Color(0xFFFF6B81)
    val errorContainer = if (theme.light) Color(0xFFFFDAD6) else Color(0xFF3D0713)
    val scheme = if (theme.light) ::lightColorScheme else ::darkColorScheme
    return scheme(
        /* primary = */ accent,
        /* onPrimary = */ onAccent,
        /* primaryContainer = */ accent.mix(background, .74f),
        /* onPrimaryContainer = */ accent.mix(text, .55f),
        /* inversePrimary = */ accent.mix(text, .3f),
        /* secondary = */ accent.mix(text, .38f),
        /* onSecondary = */ background,
        /* secondaryContainer = */ accent.mix(background, .82f),
        /* onSecondaryContainer = */ text,
        /* tertiary = */ accent.mix(subtext, .5f),
        /* onTertiary = */ background,
        /* tertiaryContainer = */ card,
        /* onTertiaryContainer = */ text,
        /* background = */ background,
        /* onBackground = */ text,
        /* surface = */ background,
        /* onSurface = */ text,
        /* surfaceVariant = */ card,
        /* onSurfaceVariant = */ subtext,
        /* surfaceTint = */ accent,
        /* inverseSurface = */ text,
        /* inverseOnSurface = */ background,
        /* error = */ error,
        /* onError = */ if (theme.light) Color.White else Color(0xFF2A0008),
        /* errorContainer = */ errorContainer,
        /* onErrorContainer = */ if (theme.light) Color(0xFF410002) else Color(0xFFFFDAD6),
        /* outline = */ subtext.mix(background, .45f),
        /* outlineVariant = */ subtext.mix(background, .72f),
        /* scrim = */ Color.Black,
        /* surfaceBright = */ card,
        /* surfaceDim = */ background,
        /* surfaceContainer = */ panel,
        /* surfaceContainerHigh = */ card,
        /* surfaceContainerHighest = */ card.mix(text, .06f),
        /* surfaceContainerLow = */ panel.mix(background, .5f),
        /* surfaceContainerLowest = */ background,
    )
}
