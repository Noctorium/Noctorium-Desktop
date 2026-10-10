package app.noctorium.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import app.noctorium.core.AppState
import app.noctorium.playback.AudioAnalysis
import app.noctorium.playback.AudioAnalysisKind
import app.noctorium.playback.Visualiser
import app.noctorium.settings.ThemeSkin
import app.noctorium.settings.VisualiserStyle
import app.noctorium.ui.skins.Classic
import app.noctorium.ui.skins.Luna
import app.noctorium.ui.skins.bevel
import app.noctorium.ui.skins.Bevel
import app.noctorium.ui.skins.pixel
import app.noctorium.ui.skins.skin
import app.noctorium.ui.skins.towards
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.floor
import kotlin.math.max

/*
 * The visualiser: the sound playing, measured by the player itself (see Visualiser in the core), drawn as bars from
 * the bass up or as each channel's level. Never a pattern standing in for it: where the player cannot see the
 * sound it draws less -- a level meter where only levels can be had -- or nothing at all.
 *
 * The player measures only while something is watching, and only this watches: it is composed only where a
 * visualiser is on screen, so with none on screen nothing is measured.
 */

/**
 * Where the visualiser reads the sound from: the player's own, unless something provides another -- a picture of
 * the screen drawn off screen, which has no music playing to measure.
 */
internal val LocalAudioAnalysis = staticCompositionLocalOf<StateFlow<AudioAnalysis?>?> { null }

/** How fast a bar falls once the sound under it has gone, in heights a second: all the way down in about half a second. */
internal const val VISUALISER_FALL_PER_SECOND = 2.2f

/** How long a peak's cap stays where the bar left it before it too falls, and how fast it falls then. */
internal const val VISUALISER_PEAK_HOLD_MS = 450L
internal const val VISUALISER_PEAK_FALL_PER_SECOND = .9f

/**
 * One step of the bars' motion: up to the new level at once, and down no faster than [fallPerSecond] allows, so the
 * twenty or so frames a second the player measures read as movement rather than as a flicker.
 */
internal fun approach(shown: Float, measured: Float, elapsedSeconds: Float, fallPerSecond: Float = VISUALISER_FALL_PER_SECOND): Float =
    if (measured >= shown) measured else max(measured, shown - fallPerSecond * elapsedSeconds)

/**
 * What [style] draws while the player can give [available] at best: bars, a level meter, or nothing ([VisualiserStyle.OFF]).
 * A frame holding less than that -- levels while mpv is still finding out how many channels a song has -- is drawn as
 * what it holds; see [AudioAnalysis.kind].
 */
internal fun visualiserShown(style: VisualiserStyle, available: AudioAnalysisKind): VisualiserStyle = style.shownWith(available)

/** The bars and caps as they stand on screen, eased towards each frame the player measures. */
private class Motion(bands: Int, channels: Int) {
    val bands = FloatArray(bands)
    val caps = FloatArray(bands)
    val capHeld = LongArray(bands)
    val rms = FloatArray(channels)
    val peaks = FloatArray(channels)

    fun step(frame: AudioAnalysis?, elapsedSeconds: Float, nowMs: Long) {
        for (i in bands.indices) {
            val measured = frame?.bands?.getOrNull(i) ?: 0f
            bands[i] = approach(bands[i], measured, elapsedSeconds)
            if (bands[i] >= caps[i]) {
                caps[i] = bands[i]
                capHeld[i] = nowMs
            } else if (nowMs - capHeld[i] > VISUALISER_PEAK_HOLD_MS) {
                caps[i] = approach(caps[i], bands[i], elapsedSeconds, VISUALISER_PEAK_FALL_PER_SECOND)
            }
        }
        for (c in rms.indices) {
            rms[c] = approach(rms[c], frame?.rms?.getOrNull(c) ?: 0f, elapsedSeconds)
            peaks[c] = approach(peaks[c], frame?.peaks?.getOrNull(c) ?: 0f, elapsedSeconds, VISUALISER_PEAK_FALL_PER_SECOND)
        }
    }
}

/**
 * The visualiser in [style], from the player's sound: [VisualiserStyle.BARS] or [VisualiserStyle.LEVEL_METER],
 * drawn as the skin in force draws it -- the accent on the page, or the green of a player of the time on a dark
 * pane under 98 and XP. Draws nothing for [VisualiserStyle.OFF], and watches nothing then either.
 */
@Composable
internal fun AudioVisualiser(style: VisualiserStyle, state: AppState, modifier: Modifier = Modifier) =
    AudioVisualiser(style, LocalAudioAnalysis.current ?: state.audioAnalysis, modifier)

/** The same, from [source]: the player's, or what a test hands it. Watched only while this is on screen. */
@Composable
internal fun AudioVisualiser(style: VisualiserStyle, source: StateFlow<AudioAnalysis?>, modifier: Modifier = Modifier) {
    if (style == VisualiserStyle.OFF) return
    val frame by source.collectAsState()
    val latest by rememberUpdatedState(frame)
    val motion = remember { Motion(Visualiser.BAND_COUNT, 2) }
    // Bumped every frame the screen draws while anything is moving, which is what redraws the canvas.
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(motion) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val elapsed = ((now - last) / 1e9f).coerceIn(0f, .1f)
                last = now
                motion.step(latest, elapsed, now / 1_000_000)
                tick = now
            }
        }
    }
    val accent = MaterialTheme.colorScheme.primary
    val quiet = ink(.1f)
    val skin = skin()
    Canvas(modifier) {
        // Read so that each frame's tick redraws it.
        tick
        val shown = if (style == VisualiserStyle.BARS && (latest?.bands?.isNotEmpty() != false)) VisualiserStyle.BARS else VisualiserStyle.LEVEL_METER
        when (skin) {
            ThemeSkin.STANDARD -> {
                if (shown == VisualiserStyle.BARS) drawBands(motion, accent, quiet, accent.copy(alpha = .9f)) else drawMeters(motion, accent, quiet)
            }
            ThemeSkin.WINDOWS_98, ThemeSkin.WINDOWS_XP -> {
                // A dark pane set into the face, as a player of the time drew its picture of the sound on.
                val xp = skin == ThemeSkin.WINDOWS_XP
                drawRect(if (xp) Color(0xFF0B1A33) else Color.Black)
                // A player of the time's green; Noctorium 98's own violet on its night palette.
                val lit = when {
                    xp -> Luna.Progress.towards(Color.White, .15f)
                    Classic.palette.dark -> Classic.Selection.towards(Color.White, .25f)
                    else -> Color(0xFF34D058)
                }
                val dim = lit.copy(alpha = .14f)
                val cap = if (xp) Color.White else Classic.TitleEnd.towards(Color.White, .3f)
                val margin = if (xp) 3.dp.toPx() else 4.dp.toPx()
                inset(margin) {
                    if (shown == VisualiserStyle.BARS) drawBands(motion, lit, dim, cap, square = true) else drawMeters(motion, lit, dim, square = true)
                }
                if (xp) {
                    drawRect(Luna.FieldEdge, Offset(pixel / 2, pixel / 2), Size(size.width - pixel, size.height - pixel), style = Stroke(pixel))
                } else {
                    bevel(Bevel.Sunken)
                }
            }
        }
    }
}

/** Ten bars from the bass up, each with a cap at the highest it reached a moment ago, over a faint track for each. */
private fun DrawScope.drawBands(motion: Motion, colour: Color, track: Color, cap: Color, square: Boolean = false) {
    val count = motion.bands.size
    val gap = if (square) floor(2.dp.toPx()) else 3.dp.toPx()
    val width = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)
    val capHeight = 2.dp.toPx()
    val usable = size.height - capHeight - 1.dp.toPx()
    val corner = if (square) CornerRadius.Zero else CornerRadius(width * .18f)
    for (i in 0 until count) {
        val left = i * (width + gap)
        drawRoundRect(track, Offset(left, 0f), Size(width, size.height), corner)
        val height = usable * motion.bands[i].coerceIn(0f, 1f)
        if (height > 0f) {
            drawRoundRect(
                Brush.verticalGradient(listOf(colour, colour.copy(alpha = .55f)), startY = size.height - height, endY = size.height),
                Offset(left, size.height - height),
                Size(width, height),
                corner,
            )
        }
        val capY = size.height - capHeight - usable * motion.caps[i].coerceIn(0f, 1f)
        if (motion.caps[i] > .01f) drawRect(cap, Offset(left, capY), Size(width, capHeight))
    }
}

/**
 * Each channel's level as a mixing desk's meter: a row of segments lit from the left as far as the sound's strength,
 * the loudest few in amber and red, and a mark where the peak is.
 */
private fun DrawScope.drawMeters(motion: Motion, colour: Color, track: Color, square: Boolean = false) {
    val channels = motion.rms.size
    val gap = 4.dp.toPx()
    val rowHeight = ((size.height - gap * (channels - 1)) / channels).coerceAtMost(16.dp.toPx())
    val top = (size.height - rowHeight * channels - gap * (channels - 1)) / 2f
    val segment = 4.dp.toPx()
    val pitch = segment + (if (square) 2.dp.toPx() else 1.5.dp.toPx())
    val count = (size.width / pitch).toInt().coerceAtLeast(4)
    for (c in 0 until channels) {
        val y = top + c * (rowHeight + gap)
        val level = motion.rms[c].coerceIn(0f, 1f)
        for (s in 0 until count) {
            val at = (s + .5f) / count
            val lit = at <= level
            val hue = when {
                at > .92f -> Color(0xFFE5484D)
                at > .8f -> Color(0xFFF5A524)
                else -> colour
            }
            drawRect(if (lit) hue else track, Offset(s * pitch, y), Size(segment, rowHeight))
        }
        val peak = motion.peaks[c].coerceIn(0f, 1f)
        if (peak > .01f) drawRect(colour.copy(alpha = .9f), Offset((peak * count).toInt().coerceAtMost(count - 1) * pitch, y), Size(segment, rowHeight))
    }
}
