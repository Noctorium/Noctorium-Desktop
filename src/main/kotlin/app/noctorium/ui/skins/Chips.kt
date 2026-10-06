package app.noctorium.ui.skins

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ChipElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noctorium.settings.ThemeSkin
import kotlin.math.floor

/*
 * Chips and progress bars.
 *
 * A chip that is chosen or not was, on those desktops, a button that stayed pressed in while it was on: a
 * toolbar's Bold, a view's Details. So a filter chip is a push button that latches here, and an assist chip is
 * simply a push button. The progress bar is 98's sunken well filling with navy blocks, and Luna's rounded white
 * one filling with green.
 */

/** Material's filter chip; under a Windows skin a push button that stays pressed in while [selected]. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.FilterChip(selected, onClick, label, modifier, enabled, leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource)
        return
    }
    PushButton(
        onClick,
        modifier.heightIn(min = 23.dp),
        enabled,
        interactionSource,
        latched = selected,
        padding = PaddingValues(horizontal = 8.dp),
        minWidth = 0.dp,
    ) {
        if (leadingIcon != null) {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leadingIcon() }
            Spacer(Modifier.width(4.dp))
        }
        label()
        if (trailingIcon != null) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { trailingIcon() }
        }
    }
}

/** Material's assist chip; a push button under a Windows skin. */
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
) {
    if (!skin().isWindows) {
        androidx.compose.material3.AssistChip(onClick, label, modifier, enabled, leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource)
        return
    }
    PushButton(onClick, modifier, enabled, interactionSource, padding = PaddingValues(horizontal = 8.dp), minWidth = 0.dp) {
        if (leadingIcon != null) {
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { leadingIcon() }
            Spacer(Modifier.width(4.dp))
        }
        label()
        if (trailingIcon != null) {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) { trailingIcon() }
        }
    }
}

/** Material's progress bar for a known amount; the skin's own under a Windows skin. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.LinearProgressIndicator(progress, modifier, color, trackColor, strokeCap, gapSize)
        return
    }
    // The well is the height it was, whatever height Material's line was given.
    Canvas(Modifier.height(14.dp).then(modifier).fillMaxWidth()) { drawBlocks(skin, 0f, progress().coerceIn(0f, 1f)) }
}

/**
 * Material's progress bar for an amount nobody knows yet; under a Windows skin a few blocks running along the
 * well, the way later Windows said "working" when it could not say how far.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
    gapSize: Dp = ProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.LinearProgressIndicator(modifier, color, trackColor, strokeCap, gapSize)
        return
    }
    val running = rememberInfiniteTransition(label = "marquee")
    val along by running.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "marqueeAlong")
    Canvas(Modifier.height(14.dp).then(modifier).fillMaxWidth()) {
        val chunk = .22f
        val start = along * (1f + chunk) - chunk
        drawBlocks(skin, start.coerceAtLeast(0f), (start + chunk).coerceAtMost(1f))
    }
}

/** The well, and the blocks filling it from [from] to [to] of the way along. */
private fun DrawScope.drawBlocks(skin: ThemeSkin, from: Float, to: Float) {
    val p = pixel
    if (skin == ThemeSkin.WINDOWS_XP) {
        val radius = CornerRadius(3.dp.toPx())
        drawRoundRect(Luna.Window, cornerRadius = radius)
        drawRoundRect(Luna.TabEdge, Offset(p / 2, p / 2), Size(size.width - p, size.height - p), radius, style = Stroke(p))
        val inset = p * 3
        val block = 6 * p
        val step = block + 2 * p
        val height = size.height - inset * 2
        val end = size.width - inset
        var x = inset + floor((from * (end - inset)) / step) * step
        val stop = inset + to * (end - inset)
        val green = Brush.verticalGradient(listOf(Luna.ProgressLight, Luna.Progress, Luna.Progress, Luna.ProgressLight), startY = inset, endY = inset + height)
        while (x < stop && x < end) {
            drawRect(green, Offset(x, inset), Size(minOf(block, end - x), height))
            x += step
        }
        return
    }
    thinSunken()
    val inset = p * 2
    val block = 8 * p
    val step = block + 2 * p
    val end = size.width - inset
    var x = inset + floor((from * (end - inset)) / step) * step
    val stop = inset + to * (end - inset)
    while (x < stop && x < end) {
        drawRect(Classic.Selection, Offset(x, inset), Size(minOf(block, end - x), size.height - inset * 2))
        x += step
    }
}
