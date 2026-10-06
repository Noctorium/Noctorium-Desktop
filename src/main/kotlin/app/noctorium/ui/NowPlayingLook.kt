package app.noctorium.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.noctorium.domain.Track
import app.noctorium.settings.CoverStyle
import app.noctorium.settings.NowPlayingBackdrop
import app.noctorium.settings.NowPlayingLayout
import app.noctorium.ui.skins.SkinCover
import app.noctorium.ui.skins.SkinDesktop
import app.noctorium.ui.skins.skinned
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
 *
 * As a cover it is a picture disc, the label most of the record and turning slowly; on a turntable it is a
 * record, with a label the size a real one has, turning at 33 and a third.
 */
@Composable
private fun RecordCover(
    track: Track,
    playing: Boolean,
    size: Dp,
    modifier: Modifier,
    turnMs: Int = RECORD_TURN_MS,
    label: Float = RECORD_LABEL,
    shadow: Dp = 22.dp,
) {
    val turning = playing && LocalMotion.current
    val angle = remember { Animatable(0f) }
    LaunchedEffect(turning, turnMs) {
        if (!turning) return@LaunchedEffect
        while (true) {
            angle.animateTo(angle.value + 360f, tween(turnMs, easing = LinearEasing))
            angle.snapTo(angle.value % 360f)
        }
    }
    Surface(modifier, shape = CircleShape, shadowElevation = shadow, color = VINYL) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationZ = angle.value },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) { drawGrooves(label) }
                RemoteArtwork(
                    track.artworkUrl,
                    track.provider,
                    Modifier.fillMaxSize(label).clip(CircleShape),
                )
            }
            Canvas(Modifier.fillMaxSize()) { drawSheen(label) }
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

private fun DrawScope.drawGrooves(labelShare: Float = RECORD_LABEL) {
    val radius = size.minDimension / 2
    val label = radius * labelShare
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

private fun DrawScope.drawSheen(labelShare: Float = RECORD_LABEL) {
    val radius = size.minDimension / 2
    val label = radius * labelShare
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

// --- The turntable ---

/** A real record's label against the whole record: about a third, as on a twelve-inch. */
private const val TURNTABLE_LABEL = .36f

/** One turn at 33 and a third revolutions a minute. */
private const val TURNTABLE_TURN_MS = 1_800

/** How wide a turntable is for its depth: the platter to the left, the arm and its stand to the right. */
internal const val TURNTABLE_ASPECT = 1.32f

/** Where the record sits on the plinth, and how large it is, as shares of the plinth's depth. */
private const val TURNTABLE_RECORD = .42f
private const val TURNTABLE_CENTRE_X = .52f
private const val TURNTABLE_CENTRE_Y = .52f

/**
 * Where a turntable's arm points, worked out from how far through the song is.
 *
 * Lengths are in the record's radius, from the middle of the record, with y running down the screen as it is
 * drawn, and angles in degrees clockwise from pointing right. The arm pivots above and to the right of the
 * record and is a little longer than its pivot is from the spindle, as real arms are. The groove is a spiral of
 * even pitch, so it is the stylus's distance from the middle that moves evenly through a song, and the angle
 * follows from that by the triangle of pivot, spindle and stylus.
 */
internal object Tonearm {
    /** The pivot, from the middle of the record. */
    val PIVOT = Offset(1.2f, -.78f)

    /** From the pivot to the stylus. */
    const val LENGTH = 1.53f

    /** Where the music starts, just inside the edge, and the run-out groove it ends at, just outside the label. */
    const val LEAD_IN = .965f
    const val RUN_OUT = .42f

    /** On its stand, clear of the record. */
    const val REST_DEGREES = 92f

    /** The arm's angle a song [progress] of the way through, from 0 to 1; resting on its stand when null. */
    fun degrees(progress: Float?): Float {
        if (progress == null) return REST_DEGREES
        val radius = LEAD_IN + (RUN_OUT - LEAD_IN) * progress.coerceIn(0f, 1f)
        val reach = PIVOT.getDistance()
        // From the pivot towards the spindle, then turned in by the angle the triangle's sides allow.
        val towardsSpindle = atan2(-PIVOT.y, -PIVOT.x)
        val opening = acos(((reach * reach + LENGTH * LENGTH - radius * radius) / (2 * reach * LENGTH)).coerceIn(-1f, 1f))
        return Math.toDegrees((towardsSpindle - opening).toDouble()).toFloat()
    }

    /** Where the stylus is with the arm at [degrees], from the middle of the record: what [degrees] solves for. */
    fun stylus(degrees: Float): Offset {
        val angle = Math.toRadians(degrees.toDouble())
        return PIVOT + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * LENGTH
    }
}

/**
 * A turntable: a plinth, a platter with the record on it, the cover for its label, turning at 33 and a third
 * while the music plays, and an arm that crosses it as the song goes, from the edge at its start to the run-out
 * groove at its end. [progress] is how far through the song is, or null with nothing loaded, when the arm rests
 * on its stand. A seek swings the arm to its new place rather than jumping it there, unless nothing is to move.
 *
 * The plinth is a dark deck on a dark page and walnut on a pale one, where a black slab would be a hole in the
 * window; its light, beside the platter, is the accent, lit while the record turns. Drawn to be
 * [TURNTABLE_ASPECT] times as wide as it is deep, which is what [modifier] should give it.
 */
@Composable
internal fun Turntable(track: Track, playing: Boolean, progress: Float?, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val card = MaterialTheme.colorScheme.surfaceContainerHigh
    val light = MaterialTheme.colorScheme.background.luminance() > .5f
    val arm by animateFloatAsState(Tonearm.degrees(progress), motionSpec(700), label = "tonearm")
    BoxWithConstraints(modifier) {
        val depth = maxHeight
        val radius = depth * TURNTABLE_RECORD
        Canvas(Modifier.fillMaxSize()) { drawPlinth(light, card, accent, playing) }
        RecordCover(
            track,
            playing,
            radius * 2,
            Modifier.offset(depth * TURNTABLE_CENTRE_X - radius, depth * TURNTABLE_CENTRE_Y - radius),
            turnMs = TURNTABLE_TURN_MS,
            label = TURNTABLE_LABEL,
            shadow = 4.dp,
        )
        Canvas(Modifier.fillMaxSize()) { drawTonearm(arm) }
    }
}

/** The deck, the platter under the record, and the few controls that make it read as a turntable. */
private fun DrawScope.drawPlinth(light: Boolean, card: Color, accent: Color, playing: Boolean) {
    val depth = size.height
    val corner = CornerRadius(depth * .045f)
    // Its shadow on the page, and the deck itself.
    drawRoundRect(Color.Black.copy(alpha = if (light) .16f else .55f), Offset(0f, depth * .018f), size, corner)
    if (light) {
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF8C5D39), Color(0xFF65401F))), size = size, cornerRadius = corner)
        // The walnut's grain: long, faint, slightly wandering lines, darker than the wood.
        val grain = Path()
        repeat(22) { line ->
            val y = depth * (line + .5f) / 22f
            grain.moveTo(0f, y)
            var x = 0f
            while (x <= size.width) {
                x += size.width / 40f
                grain.lineTo(x, y + sin(x / size.width * 9f + line * 1.7f) * depth * .006f)
            }
        }
        clipPath(Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, corner)) }) {
            drawPath(grain, Color(0xFF3B2412).copy(alpha = .16f), style = Stroke(depth * .0035f))
        }
    } else {
        drawRoundRect(
            Brush.verticalGradient(listOf(card.mix(Color(0xFF3A383F), .6f), card.mix(Color(0xFF151418), .7f))),
            size = size,
            cornerRadius = corner,
        )
    }
    // A bevel: light catching the top edge.
    drawRoundRect(Color.White.copy(alpha = if (light) .22f else .09f), size = size, cornerRadius = corner, style = Stroke(depth * .006f))

    val centre = Offset(depth * TURNTABLE_CENTRE_X, depth * TURNTABLE_CENTRE_Y)
    val record = depth * TURNTABLE_RECORD
    // The platter: dark metal a little wider than the record, its rim catching the light, a ring of strobe dots.
    val platter = record * 1.07f
    drawCircle(Color(0xFF0C0C0E), platter, centre)
    drawCircle(
        Brush.sweepGradient(
            listOf(Color(0xFF5A5B60), Color(0xFFB9BBC0), Color(0xFF55565B), Color(0xFF9FA1A6), Color(0xFF5A5B60)),
            center = centre,
        ),
        platter,
        centre,
        style = Stroke(record * .035f),
    )
    repeat(90) { dot ->
        val angle = dot * 2 * Math.PI / 90
        drawCircle(
            Color(0xFFD4D5D9).copy(alpha = .55f),
            record * .008f,
            centre + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (platter - record * .035f),
        )
    }
    // The start button at the near left corner, and the light above it, lit in the accent while the record turns.
    val button = Offset(depth * .05f, depth * .86f)
    drawRoundRect(Color(0xFFB9BBC0), button, Size(depth * .11f, depth * .07f), CornerRadius(depth * .012f))
    drawRoundRect(Color.Black.copy(alpha = .25f), button, Size(depth * .11f, depth * .07f), CornerRadius(depth * .012f), style = Stroke(depth * .004f))
    val lamp = Offset(depth * .065f, depth * .1f)
    if (playing) drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .55f), Color.Transparent), lamp, depth * .045f), depth * .045f, lamp)
    drawCircle(if (playing) accent else Color(0xFF3A3A3E), depth * .014f, lamp)
    // The pitch slider down the right-hand edge, and its knob at the middle, where it plays at the right speed.
    val slot = Offset(size.width - depth * .1f, depth * .5f)
    drawRoundRect(Color.Black.copy(alpha = .45f), Offset(slot.x - depth * .008f, slot.y), Size(depth * .016f, depth * .36f), CornerRadius(depth * .008f))
    drawRoundRect(Color(0xFFB9BBC0), Offset(slot.x - depth * .03f, slot.y + depth * .15f), Size(depth * .06f, depth * .05f), CornerRadius(depth * .008f))
}

/**
 * The arm at [degrees]: its shadow first, then the stand it rests on, the counterweight behind the pivot, the
 * tube, and the headshell turned in at its end with the stylus at the point [Tonearm.degrees] solved for.
 */
private fun DrawScope.drawTonearm(degrees: Float) {
    val depth = size.height
    val radius = depth * TURNTABLE_RECORD
    val centre = Offset(depth * TURNTABLE_CENTRE_X, depth * TURNTABLE_CENTRE_Y)
    fun at(point: Offset) = centre + point * radius
    val pivot = at(Tonearm.PIVOT)
    val tip = at(Tonearm.stylus(degrees))
    val angle = Math.toRadians(degrees.toDouble())
    val along = Offset(cos(angle).toFloat(), sin(angle).toFloat())
    // The headshell is turned in towards the spindle, as an offset headshell is, and the tube meets its back.
    val turn = Math.toRadians(degrees.toDouble() + 24.0)
    val shellAlong = Offset(cos(turn).toFloat(), sin(turn).toFloat())
    val shellBack = tip - shellAlong * (radius * .2f)

    // The stand, where the stylus is when the arm rests.
    val rest = pivot + Offset(cos(Math.toRadians(Tonearm.REST_DEGREES.toDouble())).toFloat(), sin(Math.toRadians(Tonearm.REST_DEGREES.toDouble())).toFloat()) * (Tonearm.LENGTH * radius * .74f)
    drawCircle(Color.Black.copy(alpha = .4f), radius * .055f, rest + Offset(radius * .01f, radius * .02f))
    drawCircle(Color(0xFF2E2E33), radius * .05f, rest)
    drawCircle(Color(0xFF6B6C72), radius * .028f, rest)

    fun arm(colour: Color?, offset: Offset) {
        val shade = colour ?: Color.Black.copy(alpha = .32f)
        // The counterweight behind the pivot.
        drawLine(colour ?: shade, pivot - along * (radius * .09f) + offset, pivot - along * (radius * .27f) + offset, radius * .13f, StrokeCap.Round)
        // The tube.
        drawLine(colour?.let { Color(0xFF8E9096) } ?: shade, pivot + offset, shellBack + offset, radius * .05f, StrokeCap.Round)
        drawLine(colour?.let { Color(0xFFD5D7DC) } ?: shade, pivot + offset, shellBack + offset, radius * .026f, StrokeCap.Round)
        // The headshell, and the finger lift off its side.
        drawLine(colour?.let { Color(0xFF222226) } ?: shade, shellBack + offset, tip + offset, radius * .09f, StrokeCap.Round)
        val lift = Offset(-shellAlong.y, shellAlong.x)
        drawLine(colour?.let { Color(0xFFB9BBC0) } ?: shade, shellBack + shellAlong * (radius * .06f) + offset, shellBack + shellAlong * (radius * .06f) + lift * (radius * .09f) + offset, radius * .018f, StrokeCap.Round)
    }
    arm(null, Offset(radius * .025f, radius * .045f))
    arm(Color(0xFF2C2C31), Offset.Zero)
    // A highlight along the counterweight, and the pivot's base and cap.
    drawLine(Color.White.copy(alpha = .18f), pivot - along * (radius * .1f), pivot - along * (radius * .26f), radius * .03f, StrokeCap.Round)
    drawCircle(Color(0xFF1E1E22), radius * .16f, pivot)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF8E9096), Color(0xFF3E3F44)), pivot, radius * .13f), radius * .13f, pivot)
    drawCircle(Color(0xFFD5D7DC), radius * .055f, pivot)
    // The spindle through the record's hole.
    drawCircle(Color(0xFFC9CBD0), radius * .024f, centre)
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
 * its rows. Enough to tell eleven arrangements apart at a glance, which their names alone are not.
 */
@Composable
internal fun NowPlayingLayoutPicture(layout: NowPlayingLayout, selected: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val writing = MaterialTheme.colorScheme.onSurface
    val page = MaterialTheme.colorScheme.background
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
            NowPlayingLayout.SIDE_BY_SIDE, NowPlayingLayout.PANEL_LEFT -> {
                val left = layout == NowPlayingLayout.SIDE_BY_SIDE
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
            NowPlayingLayout.FOCUS -> {
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
            NowPlayingLayout.IMMERSIVE -> {
                // The cover is the whole screen, with the page's own colour rising from the foot under the writing.
                val corner = CornerRadius(h * .06f)
                drawRoundRect(accent.copy(alpha = if (selected) .85f else .55f), Offset.Zero, size, corner)
                drawRoundRect(
                    Brush.verticalGradient(0f to Color.Transparent, .5f to Color.Transparent, 1f to page.copy(alpha = .9f)),
                    Offset.Zero,
                    size,
                    corner,
                )
                bar(w * .07f, h * .52f, w * .44f, alpha = .8f, thick = stroke * 2f)
                bar(w * .07f, h * .65f, w * .2f)
                transport(w * .07f + h * .04f, h * .82f)
                seek(w * .07f + h * .32f, h * .815f, w * .5f)
            }
            NowPlayingLayout.SPLIT -> {
                // The cover the left half, edge to edge, and the track, the controls and the panel on the right.
                val corner = CornerRadius(h * .06f)
                // Rounded only on the outside: the half meets the rest along a straight edge.
                drawPath(
                    Path().apply {
                        addRoundRect(RoundRect(0f, 0f, w * .5f, h, corner, CornerRadius.Zero, CornerRadius.Zero, corner))
                    },
                    accent.copy(alpha = if (selected) .9f else .6f),
                )
                title(w * .57f, h * .1f, w * .28f)
                bar(w * .57f, h * .2f, w * .17f)
                seek(w * .57f, h * .3f, w * .36f)
                panel(w * .57f, h * .4f, w * .36f, h * .52f)
            }
            NowPlayingLayout.COVER_FLOW -> {
                // The one playing faces forward, the rest of the queue turned towards it either side, each over a
                // faint reflection, with the track and the controls under the row.
                val side = h * .4f
                val top = h * .07f
                val middle = (w - side) / 2
                listOf(3, 2, 1).forEach { step ->
                    val fade = .78f - step * .14f
                    val narrow = side * .34f
                    val drop = side * .1f
                    listOf(-1, 1).forEach { direction ->
                        val inner = if (direction < 0) middle - side * .06f - (step - 1) * narrow * .62f else middle + side + side * .06f + (step - 1) * narrow * .62f
                        val outer = inner + direction * narrow
                        val sleeve = Path().apply {
                            moveTo(inner, top)
                            lineTo(outer, top + drop)
                            lineTo(outer, top + side - drop)
                            lineTo(inner, top + side)
                            close()
                        }
                        drawPath(sleeve, accent.copy(alpha = fade * (if (selected) 1f else .75f)))
                        drawPath(sleeve, page.copy(alpha = .08f + step * .1f))
                    }
                }
                cover(middle, top, side)
                drawRoundRect(
                    Brush.verticalGradient(listOf(accent.copy(alpha = .3f), Color.Transparent), startY = top + side + h * .015f, endY = top + side * 1.3f),
                    Offset(middle, top + side + h * .015f),
                    Size(side, side * .28f),
                    CornerRadius(side * .1f),
                )
                title((w - w * .3f) / 2, h * .66f, w * .3f)
                bar((w - w * .18f) / 2, h * .76f, w * .18f)
                seek((w - w * .44f) / 2, h * .87f, w * .44f)
            }
            NowPlayingLayout.TURNTABLE -> {
                // A deck with the record on it and the arm across it, the controls under it, the panel beside it.
                val deckX = w * .07f
                val deckY = h * .1f
                val deckW = w * .5f
                val deckH = deckW / TURNTABLE_ASPECT
                drawRoundRect(writing.copy(alpha = .14f), Offset(deckX, deckY), Size(deckW, deckH), CornerRadius(h * .03f))
                val radius = deckH * TURNTABLE_RECORD
                val centre = Offset(deckX + deckH * TURNTABLE_CENTRE_X, deckY + deckH * TURNTABLE_CENTRE_Y)
                drawCircle(writing.copy(alpha = .55f), radius, centre)
                drawCircle(accent.copy(alpha = if (selected) .95f else .7f), radius * .38f, centre)
                val pivot = centre + Tonearm.PIVOT * radius
                drawLine(writing.copy(alpha = .7f), pivot, centre + Tonearm.stylus(Tonearm.degrees(.4f)) * radius, h * .025f, StrokeCap.Round)
                drawCircle(writing.copy(alpha = .7f), h * .03f, pivot)
                seek(deckX, deckY + deckH + h * .12f, deckW)
                transport(deckX + deckW / 2 - h * .12f, deckY + deckH + h * .25f)
                panel(w * .64f, h * .08f, w * .29f, h * .84f)
            }
            NowPlayingLayout.POSTER -> {
                // No cover: the title as large as it will go, the artist lighter under it, and the controls.
                val x = w * .08f
                val thick = h * .11f
                listOf(.72f, .58f, .34f).forEachIndexed { line, length ->
                    bar(x, h * .1f + line * thick * 1.25f, w * length, alpha = if (selected) 1f else .85f, thick = thick, colour = accent)
                }
                bar(x, h * .58f, w * .4f, alpha = .55f, thick = stroke * 1.6f)
                bar(x, h * .68f, w * .16f, alpha = .3f, thick = stroke * .8f)
                transport(x + h * .04f, h * .85f)
                seek(x + h * .32f, h * .845f, w * .52f)
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
