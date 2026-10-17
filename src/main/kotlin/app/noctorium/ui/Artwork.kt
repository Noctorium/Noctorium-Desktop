package app.noctorium.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import app.noctorium.ui.icons.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import app.noctorium.domain.ProviderType
import app.noctorium.settings.ArtworkColour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * A cover, loaded only when it is on screen and decoded to the size it will actually be drawn at.
 *
 * The box measures itself first, so a row thumbnail and the now playing hero ask the loader for very different
 * sizes from the same address. Anything already decoded appears on the first frame; anything new fades in, so a
 * list fills rather than flickering.
 */
@Composable
fun RemoteArtwork(
    url: String?,
    provider: ProviderType,
    modifier: Modifier = Modifier,
) {
    // What sits there until the cover arrives, and forever for a track that has none. Taken from the
    // theme rather than fixed: it used to be two violets, which read as a stray Noctorium tile on any
    // theme that is not violet -- a lilac square in the middle of Gruvbox. Which service a track came
    // from is the badge's job, so the placeholder no longer tries to say it in a shade of purple.
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier.background(Brush.linearGradient(listOf(scheme.surfaceVariant, scheme.primaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        val targetPx = artworkSizeBucket(
            with(density) {
                val widest = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
                val tallest = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
                maxOf(widest, tallest)
            },
        )
        val artwork by produceState<ImageBitmap?>(ArtworkLoader.cached(url), url, targetPx) {
            val address = url?.takeIf(String::isNotBlank) ?: return@produceState
            ArtworkLoader.cached(address)?.let { value = it }
            value = ArtworkLoader.load(address, targetPx) ?: value
        }

        Crossfade(artwork, animationSpec = tween(220), label = "artwork") { image ->
            if (image != null) {
                Image(
                    image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        // The theme's writing colour, not white: on a light theme a white mark on a pale
                        // placeholder is a blank square.
                        tint = scheme.onSurfaceVariant.copy(alpha = .72f),
                        modifier = Modifier.fillMaxSize(.32f),
                    )
                }
            }
        }
    }
}

/** Colours sampled from a cover, used to tint the Now Playing backdrop the way the artwork does. */
data class ArtworkPalette(val primary: Color, val secondary: Color)

/**
 * What a cover's pixels were found to hold: [colour], the colour the cover is known by, as core's
 * [ArtworkColour.of] finds it on every player; and [companion], the runner-up, for the far end of a backdrop's
 * gradient. Both null for a cover with nothing vivid in it.
 */
internal data class ArtworkSample(val colour: Long?, val companion: Long?)

private object PaletteCache {
    val samples = ConcurrentHashMap<String, ArtworkSample>()
}

/**
 * The colours a cover was found to hold, sampled once and kept by address; null with no cover, and until a new
 * one has been sampled the last one stays, so a song changing eases from one colour to the next rather than
 * flashing through the theme's.
 */
@Composable
private fun rememberArtworkSample(url: String?): ArtworkSample? {
    val sample by produceState(initialValue = url?.let { PaletteCache.samples[it] }, key1 = url) {
        val key = url ?: run { value = null; return@produceState }
        PaletteCache.samples[key]?.let { value = it; return@produceState }
        // Hue sampling needs no detail, so a small decode is both enough and quick — and it means the backdrop
        // no longer waits for the full-size cover to arrive.
        val image = ArtworkLoader.cached(key) ?: ArtworkLoader.load(key, 128) ?: return@produceState
        value = withContext(Dispatchers.Default) {
            runCatching { sampleArtwork(image) }.getOrDefault(ArtworkSample(null, null))
        }.also { PaletteCache.samples[key] = it }
    }
    return sample
}

/**
 * The colour a cover is known by, as core's [ArtworkColour.of] finds it: what "Match the artwork" paints the
 * accent and what Afterglow lights the page with, which are the same colour on every player because they are the
 * same sums. Null with no cover, before it has been sampled, and for a cover with nothing vivid in it.
 */
@Composable
fun rememberArtworkColour(url: String?): Long? = rememberArtworkSample(url)?.colour

/**
 * The two colours that carry a cover: the colour it is known by, and a deeper companion for the far end of the
 * gradient -- the next most common vivid hue, or the first taken most of the way to black.
 *
 * [fallback] is what is answered while nothing has been sampled and for a track with no cover at all. It is
 * handed in rather than fixed because a constant violet meant that a backdrop with nothing playing was
 * Noctorium's own colour whatever theme was on. The caller knows the theme; this does not.
 */
@Composable
fun rememberArtworkPalette(url: String?, provider: ProviderType, fallback: ArtworkPalette): ArtworkPalette {
    val sample = rememberArtworkSample(url)
    // A cover with no colour in it at all -- a black-and-white photograph, a plain sleeve. The theme's
    // own colours are a better answer than a violet nobody chose.
    val colour = sample?.colour ?: return fallback
    val primary = Color(colour)
    val secondary = sample.companion?.let(::Color) ?: Color(primary.red * .45f, primary.green * .45f, primary.blue * .45f)
    return ArtworkPalette(primary, secondary)
}

/** [image]'s colour as core finds it, and its runner-up. */
internal fun sampleArtwork(image: ImageBitmap): ArtworkSample {
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels)
    return ArtworkSample(ArtworkColour.of(pixels, image.width, image.height), artworkCompanion(pixels, image.width, image.height))
}

/**
 * The second most common vivid hue in a cover, counted as [ArtworkColour.of] counts the first: the same pixels,
 * the same twelve hues, the same passing over of the nearly black, the nearly white and the washed out. Only the
 * desktop's backdrops have a far end to colour, so the runner-up is counted here rather than in core; the colour
 * that matters -- the one the accent and the glow agree on -- is core's.
 */
internal fun artworkCompanion(pixels: IntArray, width: Int, height: Int): Long? {
    val step = maxOf(1, minOf(width, height) / 48)
    // Twelve hue buckets is enough to separate "pink cover" from "teal cover" without chasing gradients.
    val buckets = Array(12) { floatArrayOf(0f, 0f, 0f, 0f) }
    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            val pixel = pixels[y * width + x]
            val red = (pixel shr 16 and 0xFF) / 255f
            val green = (pixel shr 8 and 0xFF) / 255f
            val blue = (pixel and 0xFF) / 255f
            val max = maxOf(red, green, blue)
            val min = minOf(red, green, blue)
            val saturation = if (max <= 0f) 0f else (max - min) / max
            if (max > 0.15f && max < 0.97f && saturation > 0.22f) {
                val bucket = buckets[(hueOf(red, green, blue) / 30f).toInt().coerceIn(0, 11)]
                bucket[0] += red
                bucket[1] += green
                bucket[2] += blue
                bucket[3] += 1f
            }
        }
    }
    val runnerUp = buckets.filter { it[3] > 0f }.sortedByDescending { it[3] }.getOrNull(1) ?: return null
    return Color(runnerUp[0] / runnerUp[3], runnerUp[1] / runnerUp[3], runnerUp[2] / runnerUp[3]).toArgb().toLong() and 0xFFFFFFFFL
}

private fun hueOf(red: Float, green: Float, blue: Float): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min
    if (delta <= 0f) return 0f
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return if (hue < 0f) hue + 360f else hue
}
