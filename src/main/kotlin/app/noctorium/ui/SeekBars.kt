package app.noctorium.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import app.noctorium.settings.ProgressBarStyle
import app.noctorium.settings.SeekBar
import app.noctorium.settings.WindowsXpColours
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Every drawn seek bar, in one place.
 *
 * Kept apart from the composable that owns the gestures because these are the only part that differs
 * between the styles. Seeking, the two times, the width, what counts as unknown -- all of that is
 * written once above and cannot quietly work in one style and not another, which is the failure this
 * shape is chosen to make impossible.
 *
 * [phase] runs 0..1 and is one full wavelength of travel. [amplitude] is 0..1 and is faded rather than
 * stopped, so a paused wave settles into the flat line the other styles draw instead of freezing
 * mid-crest, which reads as a rendering fault rather than as a paused song.
 */
internal fun DrawScope.drawSeekBar(
    style: ProgressBarStyle,
    fraction: Float,
    /** Whether to draw the handle. False while a track is unseekable, where a handle would be a lie. */
    showHead: Boolean,
    track: Color,
    filled: Color,
    phase: Float,
    amplitude: Float,
    /** The song the bars are drawn for, by its queue key. Null while there is none, which lays them level. */
    songKey: String? = null,
    /** How long the song is, for the ruler's ticks. Zero or less while that is not known. */
    durationMs: Long = 0,
    /**
     * How brightly the neon's spark burns, from 0 to 1. It breathes between the two while the music plays and
     * rests at full when it does not, so a paused spark is a still one rather than one caught mid-breath.
     */
    spark: Float = 1f,
    /** The pointer is on the handle or dragging it, which XP answered by lighting its trackbar's thumb. */
    hot: Boolean = false,
    /** The page the bar is on: whether the neon brightens, and whether a Luna well can be XP's white. */
    page: Color = Color.Black,
    /** A card on that page, which a Luna well is filled with where the page is dark. */
    card: Color = Color.DarkGray,
    /**
     * The face of the Classic bar's slab: 98's grey, and under the 98 skin the face of the palette in force, so
     * Noctorium 98's slab is its own violet rather than a grey one standing on it.
     */
    classicFace: Color = Color(SeekBar.CLASSIC_FACE),
) {
    val centreY = size.height / 2f
    val head = (size.width * fraction.coerceIn(0f, 1f))
    val lightPage = page.luminance() > .5f
    when (style) {
        ProgressBarStyle.MATERIAL -> Unit // Drawn by Material's own slider, never here.

        ProgressBarStyle.MINIMAL -> {
            val thickness = SeekBar.LINE_DP.dp.toPx()
            drawLine(track, Offset(0f, centreY), Offset(size.width, centreY), thickness, StrokeCap.Round)
            if (head > 0f) {
                drawLine(filled, Offset(0f, centreY), Offset(head, centreY), thickness, StrokeCap.Round)
            }
            if (showHead) drawCircle(filled, SeekBar.DOT_RADIUS_DP.dp.toPx(), Offset(head, centreY))
        }

        ProgressBarStyle.WAVE -> {
            val thickness = SeekBar.LINE_DP.dp.toPx()
            // The part still to come stays flat: a wave on both sides would say nothing about progress.
            drawLine(track, Offset(head, centreY), Offset(size.width, centreY), thickness, StrokeCap.Round)
            if (head > 0f) {
                val peak = SeekBar.WAVE_AMPLITUDE_DP.dp.toPx() * amplitude
                val wavelength = SeekBar.WAVE_LENGTH_DP.dp.toPx().coerceAtLeast(1f)
                val path = Path().apply {
                    moveTo(0f, centreY)
                    // A sample every couple of pixels. Finer buys nothing at this amplitude and this is
                    // redrawn on every frame of the travel.
                    var x = 0f
                    while (x <= head) {
                        val turns = (x / wavelength) - phase
                        lineTo(x, centreY + sin(turns * 2f * Math.PI.toFloat()) * peak)
                        x += 2f
                    }
                    lineTo(head, centreY + sin(((head / wavelength) - phase) * 2f * Math.PI.toFloat()) * peak)
                }
                drawPath(path, filled, style = Stroke(width = thickness, cap = StrokeCap.Round))
            }
            if (showHead) drawCircle(filled, SeekBar.DOT_RADIUS_DP.dp.toPx(), Offset(head, centreY))
        }

        ProgressBarStyle.SEGMENTS -> {
            // The count follows the width, so a block is the same size on a phone and in a window.
            val count = (size.width / SeekBar.SEGMENT_PITCH_DP.dp.toPx()).toInt().coerceAtLeast(4)
            val pitch = size.width / count
            val width = (pitch * (1f - SeekBar.SEGMENT_GAP_RATIO)).coerceAtLeast(1f)
            val height = SeekBar.CAPSULE_DP.dp.toPx()
            // A gentle rounding rather than half the width. At this size half the width is a pill, and a
            // row of pills is a row of dots -- which is a nice enough bar but not the one called Segments.
            val corner = height * .25f
            val radius = CornerRadius(corner, corner)
            repeat(count) { index ->
                val left = index * pitch
                // Lit once the head has reached the block, rather than once it has passed it: a bar that
                // waits for a block to finish before lighting it reads as running behind the music.
                drawRoundRect(
                    color = if (left < head) filled else track,
                    topLeft = Offset(left, centreY - height / 2f),
                    size = Size(width, height),
                    cornerRadius = radius,
                )
            }
        }

        ProgressBarStyle.CAPSULE -> {
            val height = SeekBar.CAPSULE_DP.dp.toPx()
            val radius = CornerRadius(height / 2f, height / 2f)
            drawRoundRect(track, Offset(0f, centreY - height / 2f), Size(size.width, height), radius)
            if (head > 0f) {
                // Never narrower than it is tall, or the filled end stops being a capsule and becomes a
                // lens: at a few seconds into a long track the two corner radii meet and pinch.
                drawRoundRect(
                    color = filled,
                    topLeft = Offset(0f, centreY - height / 2f),
                    size = Size(head.coerceAtLeast(height), height),
                    cornerRadius = radius,
                )
            }
        }

        ProgressBarStyle.CLASSIC -> {
            /*
             * Windows 98's progress bar with its trackbar's thumb on top. The well is sunk into the page --
             * dark edge above and to the left, light below and to the right, the way that desktop lit every
             * control from the top left -- and fills from the left with square blocks. Black and white for
             * the bevel rather than the theme's own colours, because that is what the bevels were, and they
             * read on a dark page as well as on the grey one they were made for.
             */
            val height = SeekBar.CLASSIC_WELL_DP.dp.toPx()
            val top = centreY - height / 2f
            val edge = 1.dp.toPx().coerceAtLeast(1f)
            val shadow = Color.Black.copy(alpha = .55f)
            val light = Color.White.copy(alpha = .85f)
            drawRect(track, Offset(0f, top), Size(size.width, height))
            drawRect(shadow, Offset(0f, top), Size(size.width, edge))
            drawRect(shadow, Offset(0f, top), Size(edge, height))
            drawRect(light, Offset(0f, top + height - edge), Size(size.width, edge))
            drawRect(light, Offset(size.width - edge, top), Size(edge, height))

            val inset = edge * 2f
            val block = SeekBar.CLASSIC_BLOCK_DP.dp.toPx()
            val step = block + SeekBar.CLASSIC_BLOCK_GAP_DP.dp.toPx()
            val end = size.width - inset
            var left = inset
            // Lit once the head has reached the block, as the other blocky bar does.
            while (left < head && left < end) {
                drawRect(filled, Offset(left, top + inset), Size(minOf(block, end - left), height - inset * 2f))
                left += step
            }

            if (showHead) {
                val width = SeekBar.CLASSIC_THUMB_WIDTH_DP.dp.toPx()
                val tall = SeekBar.CLASSIC_THUMB_HEIGHT_DP.dp.toPx().coerceAtMost(size.height)
                val x = (head - width / 2f).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
                val y = centreY - tall / 2f
                // Raised rather than sunk: the same two edges, the other way round.
                drawRect(classicFace, Offset(x, y), Size(width, tall))
                drawRect(light, Offset(x, y), Size(width, edge))
                drawRect(light, Offset(x, y), Size(edge, tall))
                drawRect(Color.Black.copy(alpha = .7f), Offset(x, y + tall - edge), Size(width, edge))
                drawRect(Color.Black.copy(alpha = .7f), Offset(x + width - edge, y), Size(edge, tall))
            }
        }

        ProgressBarStyle.BARS -> drawBars(head, track, filled, songKey.takeIf { durationMs > 0 })

        ProgressBarStyle.BEADS -> {
            // As many as fit at the pitch, spread evenly, so a bead is a bead on every screen as a block is.
            val count = (size.width / SeekBar.BEAD_PITCH_DP.dp.toPx()).toInt().coerceAtLeast(4)
            val step = size.width / count
            val radius = SeekBar.BEAD_RADIUS_DP.dp.toPx()
            // The bead the song has reached rather than one riding loose along the string: the string is the
            // song in beads, and the larger one is which of them is playing.
            val reached = if (showHead) (fraction.coerceIn(0f, 1f) * count).toInt().coerceAtMost(count - 1) else -1
            repeat(count) { index ->
                val centre = Offset((index + .5f) * step, centreY)
                when {
                    index == reached -> {
                        val large = SeekBar.BEAD_HEAD_RADIUS_DP.dp.toPx()
                        drawCircle(
                            Brush.radialGradient(listOf(filled.copy(alpha = .38f), filled.copy(alpha = 0f)), centre, large * 2.2f),
                            large * 2.2f,
                            centre,
                        )
                        drawCircle(filled, large, centre)
                    }
                    index < reached || (reached < 0 && centre.x <= head) -> drawCircle(filled, radius, centre)
                    else -> drawCircle(track, radius, centre)
                }
            }
        }

        ProgressBarStyle.NEON -> {
            val line = SeekBar.NEON_LINE_DP.dp.toPx()
            val glow = SeekBar.NEON_GLOW_DP.dp.toPx()
            // Lifted towards white on a dark page, the way a lit tube is paler than its glow. On a pale page
            // that would wash the line out, so there it stays the accent and the glow does the work.
            val tube = if (lightPage) filled else filled.mix(Color.White, .38f)
            // What is still to come, in dashes anchored to the bar rather than to the head: dashes that set
            // off from the spark would crawl along as the song plays, and the eye follows anything that moves.
            val start = head + glow * .6f
            if (start < size.width) {
                val dash = SeekBar.NEON_DASH_DP.dp.toPx()
                val gap = SeekBar.NEON_DASH_GAP_DP.dp.toPx()
                drawLine(
                    track,
                    Offset(start, centreY),
                    Offset(size.width, centreY),
                    line,
                    StrokeCap.Round,
                    PathEffect.dashPathEffect(floatArrayOf(dash, gap), start % (dash + gap)),
                )
            }
            if (head > 0f) {
                // The glow as strokes laid one inside the other, widest and faintest first: a blur would do the
                // same at many times the cost, on the one control redrawn for the length of every song.
                NEON_GLOW.forEach { (spread, alpha) ->
                    drawLine(
                        filled.copy(alpha = alpha * (if (lightPage) .8f else 1f)),
                        Offset(0f, centreY),
                        Offset(head, centreY),
                        line + glow * 2f * spread,
                        StrokeCap.Round,
                    )
                }
                drawLine(tube, Offset(0f, centreY), Offset(head, centreY), line, StrokeCap.Round)
            }
            if (showHead) {
                val at = Offset(head, centreY)
                val halo = glow * (.9f + .3f * spark)
                drawCircle(
                    Brush.radialGradient(
                        0f to tube.copy(alpha = .7f + .3f * spark),
                        .28f to filled.copy(alpha = .45f + .3f * spark),
                        1f to filled.copy(alpha = 0f),
                        center = at,
                        radius = halo,
                    ),
                    halo,
                    at,
                )
                drawCircle(Color.White, line * 1.6f, at)
            }
        }

        ProgressBarStyle.RULER -> {
            /*
             * A rule along the foot, the ticks standing on it, and the pointer above the tallest of them,
             * the three stacked about the middle so the times either side still line up with the whole.
             */
            val hairline = 1.dp.toPx().coerceAtLeast(1f)
            val minor = SeekBar.RULER_TICK_DP.dp.toPx()
            val major = SeekBar.RULER_MAJOR_TICK_DP.dp.toPx()
            val pointer = SeekBar.RULER_POINTER_DP.dp.toPx()
            val gap = 2.dp.toPx()
            val top = centreY - (pointer + gap + major) / 2f
            val baseline = top + pointer + gap + major
            // A tick every eight points or so at most: closer than that and they merge into a grey band.
            val ticks = SeekBar.rulerTicks(durationMs, (size.width / 8.dp.toPx()).toInt())
            ticks.forEach { tick ->
                val x = (tick.fraction * size.width).roundToInt().toFloat() + .5f
                val played = x <= head
                drawLine(
                    when {
                        played -> filled
                        tick.major -> track.copy(alpha = (track.alpha * 1.9f).coerceAtMost(1f))
                        else -> track
                    },
                    Offset(x, baseline),
                    Offset(x, baseline - if (tick.major) major else minor),
                    if (tick.major) hairline * 1.5f else hairline,
                )
            }
            drawLine(track, Offset(0f, baseline), Offset(size.width, baseline), hairline)
            if (head > 0f) drawLine(filled, Offset(0f, baseline), Offset(head, baseline), hairline * 2f)
            if (showHead) {
                // A needle from the pointer down to the rule, faint, so the place can be read off the ticks.
                drawLine(filled.copy(alpha = .45f), Offset(head, top + pointer), Offset(head, baseline), hairline)
                val half = pointer / 2f
                val x = head.coerceIn(half, size.width - half)
                drawPath(
                    Path().apply {
                        moveTo(x - half, top)
                        lineTo(x + half, top)
                        lineTo(x, top + pointer)
                        close()
                    },
                    filled,
                )
            }
        }

        ProgressBarStyle.LUNA -> drawLuna(head, centreY, showHead, hot, lightPage, card)
    }
}

/** The neon's glow: how far each stroke spreads, as a share of the whole glow, and how strong it is. */
private val NEON_GLOW = listOf(1f to .07f, .62f to .1f, .36f to .16f, .16f to .3f)

/** How tall the bars' reflection stands against the bars themselves, and how strongly it is drawn. */
private const val BAR_REFLECTION = .3f
private const val BAR_REFLECTION_ALPHA = .5f

/** Every bar's height when there is no song to give them one: a level row, which is a song not yet begun. */
private const val BARS_LEVEL = .3f

/**
 * SoundCloud's kind of bar: a row of upright bars standing on a line a little below the middle, each with a
 * short faint reflection under it, lit from the left as the song plays.
 *
 * Every bar is a whole number of pixels wide and starts on a whole pixel, since bars three pixels wide that
 * straddle pixels come out as a smear of two greys rather than a row. Tallest where the bar has room, and
 * shorter, rather than spilling over, where it is squeezed into less.
 */
private fun DrawScope.drawBars(head: Float, track: Color, filled: Color, songKey: String?) {
    val count = (size.width / SeekBar.BARS_PITCH_DP.dp.toPx()).toInt().coerceAtLeast(8)
    val step = size.width / count
    val width = (SeekBar.BARS_WIDTH_DP.dp.toPx()).roundToInt().coerceAtLeast(1).toFloat()
    val gap = 1.dp.toPx().coerceAtLeast(1f)
    val tallest = minOf(SeekBar.BARS_HEIGHT_DP.dp.toPx(), (size.height - gap) / (1f + BAR_REFLECTION))
    val baseline = ((size.height - gap - tallest * (1f + BAR_REFLECTION)) / 2f + tallest).roundToInt().toFloat()
    val heights = songKey?.let { SeekBar.barHeights(it, count) }
    fun bar(left: Float, height: Float, colour: Color) {
        drawRect(colour, Offset(left, baseline - height), Size(width, height))
        val reflection = height * BAR_REFLECTION
        drawRect(
            Brush.verticalGradient(
                listOf(colour.copy(alpha = colour.alpha * BAR_REFLECTION_ALPHA), colour.copy(alpha = 0f)),
                startY = baseline + gap,
                endY = baseline + gap + reflection,
            ),
            Offset(left, baseline + gap),
            Size(width, reflection),
        )
    }
    repeat(count) { index ->
        val left = floor(index * step + (step - width) / 2f)
        val height = (tallest * (heights?.get(index) ?: BARS_LEVEL)).roundToInt().coerceAtLeast(1).toFloat()
        when {
            left + width <= head -> bar(left, height, filled)
            left >= head -> bar(left, height, track)
            // The bar the song is in the middle of, lit as far as the song has got.
            else -> {
                bar(left, height, track)
                clipRect(right = head) { bar(left, height, filled) }
            }
        }
    }
}

/**
 * Windows XP's progress bar under its trackbar's thumb, as Classic is 98's.
 *
 * A white well with rounded ends and a grey edge, shaded faintly inside along the top, filling from the left
 * with XP's green blocks -- green whatever the accent, since the green is what makes it XP's. On a dark page
 * the well is the card instead of white, which would otherwise be the brightest thing in the window.
 */
private fun DrawScope.drawLuna(head: Float, centreY: Float, showHead: Boolean, hot: Boolean, lightPage: Boolean, card: Color) {
    val height = SeekBar.LUNA_WELL_DP.dp.toPx()
    val top = centreY - height / 2f
    val edge = 1.dp.toPx().coerceAtLeast(1f)
    val corner = CornerRadius(3.dp.toPx())
    val well = RoundRect(0f, top, size.width, top + height, corner)
    drawPath(Path().apply { addRoundRect(well) }, if (lightPage) Color.White else card)
    clipPath(Path().apply { addRoundRect(well) }) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.Black.copy(alpha = if (lightPage) .09f else .22f), Color.Transparent),
                startY = top,
                endY = top + height * .45f,
            ),
            Offset(0f, top),
            Size(size.width, height * .45f),
        )
        // The blocks inside the edge and a pixel clear of it, as XP left them.
        val inset = edge * 2f
        val block = SeekBar.LUNA_BLOCK_DP.dp.toPx()
        val step = block + SeekBar.LUNA_BLOCK_GAP_DP.dp.toPx()
        val green = Brush.verticalGradient(
            0f to Color(WindowsXpColours.PROGRESS_LIGHT),
            .5f to Color(WindowsXpColours.PROGRESS),
            1f to Color(WindowsXpColours.PROGRESS_LIGHT),
            startY = top + inset,
            endY = top + height - inset,
        )
        val end = size.width - inset
        var left = inset
        // Lit once the head reaches a block, like the other blocky bars.
        while (left < head && left < end) {
            drawRect(green, Offset(left, top + inset), Size(minOf(block, end - left), height - inset * 2f))
            left += step
        }
    }
    drawRoundRect(
        if (lightPage) Color(LUNA_WELL_EDGE) else Color.White.copy(alpha = .28f),
        Offset(edge / 2f, top + edge / 2f),
        Size(size.width - edge, height - edge),
        CornerRadius(corner.x - edge / 2f),
        style = Stroke(edge),
    )
    if (!showHead) return

    // The thumb: a slab with rounded shoulders and a pointed foot, as XP's trackbar had with its ticks below.
    val width = SeekBar.LUNA_THUMB_WIDTH_DP.dp.toPx()
    val tall = SeekBar.LUNA_THUMB_HEIGHT_DP.dp.toPx().coerceAtMost(size.height)
    val x = (head - width / 2f).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
    val y = centreY - tall / 2f
    val shoulder = 2.dp.toPx()
    val foot = width / 2f
    val thumb = Path().apply {
        moveTo(x + shoulder, y)
        lineTo(x + width - shoulder, y)
        quadraticTo(x + width, y, x + width, y + shoulder)
        lineTo(x + width, y + tall - foot)
        lineTo(x + width / 2f, y + tall)
        lineTo(x, y + tall - foot)
        lineTo(x, y + shoulder)
        quadraticTo(x, y, x + shoulder, y)
        close()
    }
    drawPath(
        thumb,
        Brush.verticalGradient(listOf(Color.White, Color(LUNA_THUMB_FOOT)), startY = y, endY = y + tall),
    )
    if (hot) {
        // XP's hot rim: orange just inside the edge, as its buttons had when pointed at. Drawn twice as wide
        // as wanted and cut to the thumb, so all of it falls inside.
        clipPath(thumb) { drawPath(thumb, Color(WindowsXpColours.HOT), style = Stroke(edge * 3.6f)) }
    }
    drawPath(thumb, Color(LUNA_THUMB_EDGE), style = Stroke(edge))
}

/** The grey round XP's progress well, read off Luna's own bitmap. */
private const val LUNA_WELL_EDGE = 0xFF8D8D8D

/** Luna's trackbar thumb: the grey its face fades to, and the dark blue-grey it is edged in. */
private const val LUNA_THUMB_FOOT = 0xFFDCDCD6
private const val LUNA_THUMB_EDGE = 0xFF4C6080
