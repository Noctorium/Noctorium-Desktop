package app.noctorium.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.noctorium.ui.skins.focusDots
import app.noctorium.ui.skins.selection
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.skinned
import kotlinx.coroutines.delay

/**
 * Where the settings are on a page, for the settings search to land on one.
 *
 * Every setting a page draws with the shared controls -- [ToggleRow], [ChoiceRow], [ToggleChips], [CardHeading] --
 * marks its place by its own label, and the few drawn some other way say theirs with [SettingAnchor]. Asked to
 * show one ([target]), the page scrolls it into view and lights it up for a moment. [record] hears every one a
 * page draws, which is how a test checks that each has an entry in [SettingsIndex], so a new setting cannot be
 * left out of the search.
 *
 * Outside Settings there is nothing to mark, and the controls are drawn exactly as they always were.
 */
internal class SettingsAnchors(
    /** The label of the setting to show, or null for none. */
    val target: String? = null,
    /** Counts the times one was asked for, so asking for the same one again shows it again. */
    val request: Int = 0,
    val record: ((String) -> Unit)? = null,
)

internal val LocalSettingsAnchors = staticCompositionLocalOf<SettingsAnchors?> { null }

/** How long a setting found by the search stays lit before it fades, and how long the fade takes. */
private const val HIGHLIGHT_HOLD_MS = 1_600L
private const val HIGHLIGHT_FADE_MS = 900

/** Long enough for a page sliding in to have arrived, so the scroll is measured on the page where it ends up. */
private const val PAGE_ARRIVES_MS = 360L

/**
 * Marks [content] as the setting called [name] on this page: scrolled to and lit up when the settings search
 * chose it. Nothing at all outside Settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingAnchor(name: String?, content: @Composable () -> Unit) {
    val anchors = LocalSettingsAnchors.current
    if (anchors == null || name == null) {
        content()
        return
    }
    anchors.record?.invoke(name)
    val requester = remember { BringIntoViewRequester() }
    val glow = remember { Animatable(0f) }
    val measured = remember { arrayOf(IntSize.Zero) }
    val density = LocalDensity.current
    val moving = LocalMotion.current
    val targeted = anchors.target == name
    LaunchedEffect(anchors.request, targeted) {
        if (!targeted) {
            glow.snapTo(0f)
            return@LaunchedEffect
        }
        delay(if (moving) PAGE_ARRIVES_MS else 40L)
        // A little room above, and more below, so the setting lands with what follows it in view rather than
        // pressed against the bottom edge.
        val (width, height) = measured[0]
        with(density) {
            runCatching { requester.bringIntoView(Rect(0f, -24.dp.toPx(), width.toFloat(), height + 220.dp.toPx())) }
        }
        glow.snapTo(1f)
        delay(HIGHLIGHT_HOLD_MS)
        if (moving) glow.animateTo(0f, tween(HIGHLIGHT_FADE_MS)) else glow.snapTo(0f)
    }
    val accent = MaterialTheme.colorScheme.primary
    val skin = skin()
    val classic = skinned()
    Box(
        Modifier
            .onSizeChanged { measured[0] = it }
            .bringIntoViewRequester(requester)
            .drawBehind {
                val amount = glow.value
                if (amount <= 0f) return@drawBehind
                // Drawn a little outside the setting, so its words are not pressed against the edge of the light.
                val outset = 7.dp.toPx()
                val topLeft = Offset(-outset, -outset * .7f)
                val lit = Size(size.width + outset * 2, size.height + outset * 1.4f)
                if (classic) {
                    // The way a dialog of the time showed the control with the keyboard: a dotted rectangle, here
                    // round a faint wash of the selection colour.
                    drawRect(skin.selection.copy(alpha = .16f * amount), topLeft, lit)
                    if (amount > .5f) focusDots(-outset * .7f, skin.selection)
                } else {
                    drawRoundRect(accent.copy(alpha = .14f * amount), topLeft, lit, CornerRadius(10.dp.toPx()))
                    drawRoundRect(accent.copy(alpha = .75f * amount), topLeft, lit, CornerRadius(10.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                }
            },
    ) { content() }
}
