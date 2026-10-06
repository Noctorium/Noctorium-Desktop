package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import app.noctorium.ui.skins.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noctorium.domain.Track
import app.noctorium.settings.CoverStyle
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.ui.skins.SkinCover
import app.noctorium.ui.skins.SkinDesktop
import app.noctorium.ui.skins.skinned

/*
 * The parts of the now playing screen that are only drawing: the cover in each of its styles, what is
 * behind it, and the small picture of each layout that Settings and the screen's own menu offer.
 */

/** One turn of the record, in milliseconds. Slower than a real 33: it is a cover, not a turntable. */
private const val RECORD_TURN_MS = 12_000

/** How much of the record the cover takes, as a picture disc's label would. */
private const val RECORD_LABEL = .58f

/** Vinyl. Not quite black, so the grooves have something to be lighter than. */
private val VINYL = Color(0xFF111114)

/**
 * The cover, [size] across, drawn as [style] says.
 *
 * Rounded, square and circle are one shape with a different corner, so changing between them eases the corners
 * rather than swapping the picture. The record is its own drawing, and turns only while [playing].
 */
@Composable
internal fun NowPlayingCover(track: Track, playing: Boolean, style: CoverStyle, size: Dp, modifier: Modifier = Modifier) {
    if (style == CoverStyle.RECORD) {
        RecordCover(track, playing, size, modifier)
        return
    }
    // In a frame and casting no shadow under a Windows skin, square or round as chosen.
    if (style != CoverStyle.CIRCLE && skinned()) {
        SkinCover(track, size, modifier)
        return
    }
    val corner by animateDpAsState(
        when (style) {
            CoverStyle.SQUARE -> 0.dp
            CoverStyle.CIRCLE -> size / 2
            else -> size * .042f
        },
        motionSpec(),
        label = "cover-corner",
    )
    val shape = RoundedCornerShape(corner)
    Surface(modifier, shape = shape, shadowElevation = 22.dp, color = Color.Transparent) {
        RemoteArtwork(track.artworkUrl, track.provider, Modifier.size(size).clip(shape))
    }
}

/**
 * The cover as the label of a vinyl disc, turning while the music plays and stopping where it is when it
 * pauses. The grooves are rings, so it is the cover that shows the turn; the light on the vinyl stays put,
 * the way a lamp's reflection does on a real one.
 */
@Composable
private fun RecordCover(track: Track, playing: Boolean, size: Dp, modifier: Modifier) {
    val turning = playing && LocalMotion.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(turning) {
        if (!turning) return@LaunchedEffect
        while (true) {
            angle.animateTo(angle.value + 360f, tween(RECORD_TURN_MS, easing = LinearEasing))
            angle.snapTo(angle.value % 360f)
        }
    }
    Surface(modifier, shape = CircleShape, shadowElevation = 22.dp, color = VINYL) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) { drawGrooves() }
                RemoteArtwork(
                    track.artworkUrl,
                    track.provider,
                    Modifier.fillMaxSize(RECORD_LABEL).clip(CircleShape),
                )
            }
            Canvas(Modifier.fillMaxSize()) { drawSheen() }
            // The spindle hole, through the middle of the label.
            Box(
                Modifier
                    .size(size * .036f)
                    .clip(CircleShape)
                    .background(VINYL),
            )
        }
    }
}

private fun DrawScope.drawGrooves() {
    val radius = size.minDimension / 2
    val label = radius * RECORD_LABEL
    drawCircle(VINYL, radius)
    var ring = radius * .975f
    var index = 0
    while (ring > label + radius * .03f) {
        // Every fifth groove a little brighter, which is what makes them read as grooves at a glance.
        drawCircle(
            Color.White.copy(alpha = if (index % 5 == 0) .075f else .035f),
            ring,
            style = Stroke(width = 1f),
        )
        ring -= radius * .017f
        index++
    }
    // The bright edge of the disc, and the run-out ring around the label.
    drawCircle(Color.White.copy(alpha = .14f), radius * .995f, style = Stroke(width = 1.4f))
    drawCircle(Color.White.copy(alpha = .10f), label + radius * .012f, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawSheen() {
    val radius = size.minDimension / 2
    val label = radius * RECORD_LABEL
    val light = Color.White.copy(alpha = .11f)
    drawCircle(
        brush = Brush.sweepGradient(
            0f to Color.Transparent,
            .08f to light,
            .17f to Color.Transparent,
            .5f to Color.Transparent,
            .58f to light.copy(alpha = .08f),
            .67f to Color.Transparent,
            1f to Color.Transparent,
            center = center,
        ),
        radius = (radius + label) / 2,
        style = Stroke(width = radius - label),
    )
}

/**
 * What is behind the now playing screen.
 *
 * The wash is what the screen always had: two colours from the cover, fading into the theme's background.
 * The blurred cover is the cover itself, far larger and out of focus, pulled back toward the background so
 * that writing in the theme's own colours still reads on top of it. Plain draws nothing, and the page shows.
 */
@Composable
internal fun NowPlayingBackground(track: Track, backdrop: NowPlayingBackdrop) {
    val background = MaterialTheme.colorScheme.background
    when (backdrop) {
        // Under a Windows skin, the desktop the windows stand on.
        NowPlayingBackdrop.PLAIN -> if (skinned()) SkinDesktop()
        NowPlayingBackdrop.WASH -> {
            val palette = rememberArtworkPalette(
                track.artworkUrl,
                track.provider,
                fallback = ArtworkPalette(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
            )
            val near by animateColorAsState(palette.primary, tween(700), label = "ambient")
            val far by animateColorAsState(palette.secondary, tween(700), label = "ambientDeep")
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(near.copy(alpha = .42f), far.copy(alpha = .30f), background)),
                ),
            )
        }
        NowPlayingBackdrop.COVER -> Box(Modifier.fillMaxSize().background(background).clipToBounds()) {
            RemoteArtwork(
                track.artworkUrl,
                track.provider,
                Modifier
                    .fillMaxSize()
                    // Larger than the screen, so the blur's soft edge falls outside it.
                    .graphicsLayer {
                        scaleX = 1.25f
                        scaleY = 1.25f
                        alpha = .62f
                    }
                    .blur(72.dp),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(background.copy(alpha = .30f), background.copy(alpha = .55f), background.copy(alpha = .86f)),
                    ),
                ),
            )
        }
    }
}

/**
 * A small picture of [layout]: the cover in the accent, the writing as bars, the panel as a pale block with
 * its rows. Enough to tell six arrangements apart at a glance, which their names alone are not.
 */
@Composable
internal fun NowPlayingLayoutPicture(layout: NowPlayingLayout, selected: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val writing = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = h * .045f

        fun bar(x: Float, y: Float, length: Float, alpha: Float = .32f, thick: Float = stroke, colour: Color = writing) =
            drawRoundRect(colour.copy(alpha = alpha), Offset(x, y), Size(length, thick), CornerRadius(thick / 2))

        fun title(x: Float, y: Float, length: Float) = bar(x, y, length, alpha = .62f, thick = stroke * 1.4f)

        fun cover(x: Float, y: Float, side: Float) = drawRoundRect(
            accent.copy(alpha = if (selected) .9f else .6f),
            Offset(x, y),
            Size(side, side),
            CornerRadius(side * .1f),
        )

        fun seek(x: Float, y: Float, length: Float) {
            bar(x, y, length, alpha = .2f, thick = stroke * .7f)
            bar(x, y, length * .42f, alpha = 1f, thick = stroke * .7f, colour = accent)
        }

        fun transport(x: Float, y: Float) {
            listOf(0f, 1f, 2f).forEach { i ->
                drawCircle(writing.copy(alpha = if (i == 1f) .6f else .32f), h * (if (i == 1f) .045f else .032f), Offset(x + i * h * .12f, y))
            }
        }

        fun panel(x: Float, y: Float, pw: Float, ph: Float, rows: Boolean = true) {
            drawRoundRect(writing.copy(alpha = .09f), Offset(x, y), Size(pw, ph), CornerRadius(h * .06f))
            if (!rows) return
            var row = y + ph * .1f
            while (row + h * .08f < y + ph) {
                drawRoundRect(writing.copy(alpha = .18f), Offset(x + pw * .08f, row), Size(h * .07f, h * .07f), CornerRadius(h * .015f))
                bar(x + pw * .08f + h * .1f, row + h * .015f, pw * .5f, alpha = .2f)
                row += h * .115f
            }
        }

        when (layout) {
            // Split and Turntable are pictured as side by side until they have pictures of their own.
            NowPlayingLayout.SIDE_BY_SIDE, NowPlayingLayout.PANEL_LEFT, NowPlayingLayout.SPLIT, NowPlayingLayout.TURNTABLE -> {
                val left = layout != NowPlayingLayout.PANEL_LEFT
                val heroX = if (left) w * .07f else w * .45f
                title(heroX, h * .1f, w * .3f)
                bar(heroX, h * .2f, w * .18f)
                seek(heroX, h * .3f, w * .48f)
                val side = h * .5f
                cover(heroX + (w * .48f - side) / 2, h * .4f, side)
                panel(if (left) w * .62f else w * .07f, h * .08f, w * .31f, h * .84f)
            }
            NowPlayingLayout.STAGE -> {
                val side = h * .7f
                val x = w * .12f
                cover(x, h * .15f, side)
                val column = x + side + w * .07f
                title(column, h * .3f, w * .28f)
                bar(column, h * .41f, w * .17f)
                seek(column, h * .56f, w * .32f)
                transport(column + h * .04f, h * .7f)
            }
            // Full cover, Cover flow and Big type are pictured as Focus until they have pictures of their own.
            NowPlayingLayout.FOCUS, NowPlayingLayout.IMMERSIVE, NowPlayingLayout.COVER_FLOW, NowPlayingLayout.POSTER -> {
                val side = h * .46f
                cover((w - side) / 2, h * .08f, side)
                title((w - w * .3f) / 2, h * .62f, w * .3f)
                bar((w - w * .2f) / 2, h * .72f, w * .2f)
                seek((w - w * .44f) / 2, h * .84f, w * .44f)
            }
            NowPlayingLayout.BANNER -> {
                val side = h * .3f
                cover(w * .07f, h * .08f, side)
                val column = w * .07f + side + w * .05f
                title(column, h * .1f, w * .34f)
                bar(column, h * .2f, w * .2f)
                seek(column, h * .3f, w * .5f)
                panel(w * .07f, h * .48f, w * .86f, h * .44f)
            }
            NowPlayingLayout.SING_ALONG -> {
                val side = h * .38f
                val x = w * .07f
                cover(x + (w * .25f - side) / 2, h * .12f, side)
                title(x, h * .6f, w * .22f)
                bar(x, h * .7f, w * .14f)
                seek(x, h * .8f, w * .25f)
                val px = w * .38f
                val pw = w * .55f
                panel(px, h * .08f, pw, h * .84f, rows = false)
                listOf(.3f, .38f, .26f, .34f, .22f).forEachIndexed { i, length ->
                    val singing = i == 2
                    val lineLength = pw * length * (if (singing) 1.5f else 1.2f)
                    bar(
                        px + (pw - lineLength) / 2,
                        h * (.2f + i * .13f),
                        lineLength,
                        alpha = if (singing) 1f else .26f,
                        thick = stroke * (if (singing) 1.6f else 1.1f),
                        colour = if (singing) accent else writing,
                    )
                }
            }
        }
    }
}
