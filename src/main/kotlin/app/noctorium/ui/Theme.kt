package app.noctorium.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.noctorium.settings.FontChoice
import app.noctorium.settings.ThemeColours
import app.noctorium.settings.ThemePreset
import app.noctorium.settings.onAccent

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
    // Writing on the accent itself, as every player works it out: dark on a bright accent, white on a deep one.
    val onAccent = accent.onAccent()
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

/**
 * The colour of writing and icons laid on this accent: core's [onAccent], which the phone, the web and the
 * terminal take as well.
 *
 * It was worked out here, inline, with the same sums, so nothing written on an accent Noctorium already had
 * changes. It matters now because three of the Glow themes have a white accent, and anything that still wrote
 * white on the accent wrote nothing at all.
 */
fun Color.onAccent(): Color = Color(onAccent(toArgb().toLong() and 0xFFFFFFFFL))

/**
 * Whether this accent has no colour to speak of: a white, a grey or a black, as Afterglow's, Velvet's and
 * Smoke's white is. Saturation, as a hue wheel has it, under [PLAIN_ACCENT_SATURATION].
 */
internal fun Color.isPlainAccent(): Boolean {
    val brightest = maxOf(red, green, blue)
    val darkest = minOf(red, green, blue)
    return brightest <= 0f || (brightest - darkest) / brightest < PLAIN_ACCENT_SATURATION
}

/**
 * Below this, an accent reads as a white or a grey rather than as a colour. The palest accent a theme shipped
 * with before the Glow themes, Rosé Pine's, is near twice it, and a cover's colour is never under it, since
 * "Match the artwork" only ever takes a vivid one.
 */
internal const val PLAIN_ACCENT_SATURATION = .15f

/**
 * Whether the accent in force is plain -- see [isPlainAccent] -- and so cannot be told from the writing beside
 * it by its colour.
 *
 * Much of what is on, chosen or playing in Noctorium was shown by the accent against the writing: a lit shuffle
 * button against the quiet ones beside it. A white accent beside near-white writing leaves that a brighter grey
 * against a grey, so where the accent was the only sign, this asks for a second one, and an accent with a
 * colour, which never needed it, does not get it.
 */
@Composable
@ReadOnlyComposable
fun plainAccent(): Boolean = MaterialTheme.colorScheme.primary.isPlainAccent()

/**
 * A dot under a toggle that is on -- shuffle, repeat, Devices -- while the accent in force is [plainAccent].
 *
 * The music players that draw their controls white mark a lit one the same way, and it needs no explaining.
 * Nothing is drawn under an accent with a colour, which says it already.
 */
@Composable
fun Modifier.litMark(lit: Boolean): Modifier {
    if (!lit || !plainAccent()) return this
    val colour = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        val radius = LIT_MARK_RADIUS.dp.toPx()
        drawCircle(colour, radius, Offset(size.width / 2f, size.height - radius - LIT_MARK_INSET.dp.toPx()))
    }
}

/** The lit dot's size, and how far it sits above the foot of its button. */
private const val LIT_MARK_RADIUS = 1.75f
private const val LIT_MARK_INSET = 1.5f

/** The typeface a [FontChoice] means here, or null for the system's own, which is what Material uses unasked. */
fun FontChoice.fontFamily(): FontFamily? = when (this) {
    FontChoice.DEFAULT -> null
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
}

/**
 * Material's type scale, set in the chosen typeface.
 *
 * Every style is given the family rather than only the body ones, because the menus, the buttons and the
 * chips each take a different style from the scale, and a typeface that stopped at the edge of a button
 * would look like a mistake. The default choice returns Material's own scale untouched, which is exactly
 * what the window used before there was a choice. Sizes are left alone: the text size setting already
 * scales every size there is, through the density.
 */
fun noctoriumTypography(font: FontChoice): Typography {
    val base = Typography()
    val family = font.fontFamily() ?: return base
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
