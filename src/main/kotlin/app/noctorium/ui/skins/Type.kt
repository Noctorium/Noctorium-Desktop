package app.noctorium.ui.skins

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import app.noctorium.settings.FontChoice
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.fontFamily
import app.noctorium.ui.noctoriumTypography
import org.jetbrains.skia.FontMgr

/**
 * The typefaces the two desktops were set in, found among those already installed.
 *
 * Asked for by name rather than shipped: Microsoft's fonts are Microsoft's to hand out, and Windows and macOS
 * both carry them anyway. A machine without one falls to the next, and without any to the system's own, so the
 * worst that happens is a 98 window in the default sans.
 */
internal object SkinFonts {
    /** MS Sans Serif was a bitmap; Microsoft Sans Serif is the same letters as an outline font. */
    val classic: FontFamily by lazy { installed("Microsoft Sans Serif", "Tahoma") }

    /** XP's interface font, and 98's for anything large enough that the bitmap face would have broken up. */
    val tahoma: FontFamily by lazy { installed("Tahoma", "Microsoft Sans Serif") }

    /** Luna's title bars. */
    val trebuchet: FontFamily by lazy { installed("Trebuchet MS", "Tahoma") }

    /**
     * The heavy face posters and covers of the time were set in: Arial Black, which both desktops carried; Tahoma
     * at its boldest where it is missing. Windows files Arial Black under Arial now, as its heaviest weight, so it
     * is looked for there first and by its own name after.
     */
    @OptIn(ExperimentalTextApi::class)
    val black: FontFamily by lazy {
        when {
            hasWeight("Arial", 900) -> FontFamily(SystemFont("Arial", FontWeight.Black))
            isInstalled("Arial Black") -> FontFamily(SystemFont("Arial Black", FontWeight.Black))
            isInstalled("Tahoma") -> FontFamily(SystemFont("Tahoma", FontWeight.Bold))
            else -> FontFamily.Default
        }
    }

    private fun hasWeight(family: String, weight: Int): Boolean = runCatching {
        val styles = FontMgr.default.matchFamily(family)
        (0 until styles.count()).any { styles.getStyle(it).weight == weight }
    }.getOrDefault(false)

    private fun isInstalled(family: String): Boolean =
        runCatching { FontMgr.default.matchFamily(family).count() > 0 }.getOrDefault(false)

    @OptIn(ExperimentalTextApi::class)
    private fun installed(vararg names: String): FontFamily {
        val name = names.firstOrNull(::isInstalled) ?: return FontFamily.Default
        return FontFamily(
            SystemFont(name, FontWeight.Normal),
            SystemFont(name, FontWeight.Bold),
            SystemFont(name, FontWeight.Normal, FontStyle.Italic),
            SystemFont(name, FontWeight.Bold, FontStyle.Italic),
        )
    }
}

/**
 * Below this size, in pixels, writing is drawn the way those desktops drew it: on the pixel grid and without
 * smoothing, every stem a whole pixel. Font smoothing in 98 and XP only ever reached larger sizes -- each font's
 * own table said where -- and the core fonts all left their small sizes crisp, which is the look itself.
 */
private const val SMOOTH_FROM_PX = 17f

@OptIn(ExperimentalTextApi::class)
private val Crisp = PlatformTextStyle(
    null,
    PlatformParagraphStyle(FontRasterizationSettings(FontSmoothing.None, FontHinting.Full, subpixelPositioning = false, autoHintingForced = false)),
)

@OptIn(ExperimentalTextApi::class)
private val Smooth = PlatformTextStyle(
    null,
    PlatformParagraphStyle(FontRasterizationSettings(FontSmoothing.AntiAlias, FontHinting.Full, subpixelPositioning = false, autoHintingForced = false)),
)

/** The skin's typeface for ordinary writing, or null under the standard skin. */
internal fun ThemeSkin.bodyFamily(): FontFamily? = when (this) {
    ThemeSkin.STANDARD -> null
    ThemeSkin.WINDOWS_98 -> SkinFonts.classic
    ThemeSkin.WINDOWS_XP -> SkinFonts.tahoma
}

/**
 * Material's type scale in the skin's typeface and at the sizes of the time, where the scale is used unasked:
 * a label, a menu, a row with no size of its own. Eleven pixels was eight points on a 96 dpi screen, the size
 * both desktops wrote everything in; the larger styles stay large, for what was meant to be.
 *
 * A typeface the listener chose for themselves is kept, and only the sizes follow the skin. The standard skin
 * is Material's own scale, exactly as it was.
 */
internal fun skinTypography(skin: ThemeSkin, font: FontChoice): Typography {
    if (!skin.isWindows) return noctoriumTypography(font)
    val family = font.fontFamily() ?: skin.bodyFamily()
    val base = Typography()
    fun TextStyle.sized(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = copy(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        letterSpacing = 0.sp,
        platformStyle = if (size >= SMOOTH_FROM_PX) Smooth else Crisp,
    )
    return Typography(
        displayLarge = base.displayLarge.sized(40, 46, FontWeight.Bold),
        displayMedium = base.displayMedium.sized(34, 40, FontWeight.Bold),
        displaySmall = base.displaySmall.sized(28, 34, FontWeight.Bold),
        headlineLarge = base.headlineLarge.sized(26, 32, FontWeight.Bold),
        headlineMedium = base.headlineMedium.sized(22, 28, FontWeight.Bold),
        headlineSmall = base.headlineSmall.sized(18, 24, FontWeight.Bold),
        titleLarge = base.titleLarge.sized(16, 20, FontWeight.Bold),
        titleMedium = base.titleMedium.sized(12, 16, FontWeight.Bold),
        titleSmall = base.titleSmall.sized(11, 14, FontWeight.Bold),
        bodyLarge = base.bodyLarge.sized(12, 16),
        bodyMedium = base.bodyMedium.sized(11, 15),
        bodySmall = base.bodySmall.sized(11, 14),
        labelLarge = base.labelLarge.sized(11, 14),
        labelMedium = base.labelMedium.sized(11, 14),
        labelSmall = base.labelSmall.sized(10, 13),
    )
}

/**
 * [style] as a skin writes it: crisp below [SMOOTH_FROM_PX] and smoothed above, and in the two weights the
 * period had. A semibold or medium heading was not a thing either desktop could draw, so those come out as
 * the regular face, the way a 98 list showed every name.
 */
@Composable
private fun periodStyle(style: TextStyle, fontSize: TextUnit, fontWeight: FontWeight?): Pair<TextStyle, FontWeight?> {
    val size = if (fontSize != TextUnit.Unspecified) fontSize else style.fontSize
    val pixels = if (size.type == TextUnitType.Sp) with(LocalDensity.current) { size.toPx() } else 14f
    val raster = if (pixels >= SMOOTH_FROM_PX) Smooth else Crisp
    return style.copy(platformStyle = raster, fontWeight = style.fontWeight?.let(::periodWeight)) to fontWeight?.let(::periodWeight)
}

private fun periodWeight(weight: FontWeight): FontWeight = if (weight >= FontWeight.Bold) FontWeight.Bold else FontWeight.Normal

/**
 * Material's Text, written the way the skin in force writes. Under the standard skin it is Material's own,
 * untouched; see [periodStyle] for what a Windows skin changes.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.Text(
            text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
            lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style,
        )
        return
    }
    val (period, weight) = periodStyle(style, fontSize, fontWeight)
    androidx.compose.material3.Text(
        text, modifier, color, fontSize, fontStyle, weight, fontFamily, letterSpacing, textDecoration, textAlign,
        lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, period,
    )
}

/** The same for writing with styles inside it. */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.Text(
            text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration, textAlign,
            lineHeight, overflow, softWrap, maxLines, minLines, inlineContent, onTextLayout, style,
        )
        return
    }
    val (period, weight) = periodStyle(style, fontSize, fontWeight)
    androidx.compose.material3.Text(
        text, modifier, color, fontSize, fontStyle, weight, fontFamily, letterSpacing, textDecoration, textAlign,
        lineHeight, overflow, softWrap, maxLines, minLines, inlineContent, onTextLayout, period,
    )
}

/** A style for writing the skin draws itself -- title bars, headings -- crisp or smooth by its size. */
internal fun TextStyle.rasterisedFor(pixels: Float): TextStyle = copy(platformStyle = if (pixels >= SMOOTH_FROM_PX) Smooth else Crisp)
