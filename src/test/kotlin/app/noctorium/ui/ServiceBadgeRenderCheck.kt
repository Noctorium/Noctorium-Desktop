package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.domain.ProviderType
import app.noctorium.settings.ServiceBadgeStyle
import app.noctorium.settings.ThemePreset
import app.noctorium.ui.skins.Surface
import app.noctorium.ui.skins.Text
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Draws the service badges to pictures, off screen, for looking at: every service's badge with its mark and with its
 * letters, compact and in full, on a cover and in a row of a list, and the choice between them in Settings -- in the
 * standard theme, a light one, 98, Noctorium 98 and XP, at the screen's size and at twice and three times it, where a
 * mark that is blurred or off centre shows. Off unless a folder is named, like [PlayerBarRenderCheck]; the pictures go
 * to `service-badges/` under it:
 *
 *     ./gradlew :test --tests "*ServiceBadgeRenderCheck*" -Dnoctorium.renderBars=build/bars
 */
class ServiceBadgeRenderCheck {

    private val themes = listOf(
        ThemePreset.NOCTORIUM_NIGHT,
        ThemePreset.NOCTORIUM_DAY,
        ThemePreset.WINDOWS_98,
        ThemePreset.WINDOWS_98_NOCTORIUM,
        ThemePreset.WINDOWS_XP,
    )

    @Test
    fun `the service badges draw, with marks and with letters`() {
        val folder = System.getProperty("noctorium.renderBars")?.let { File(it, "service-badges") }?.also { it.mkdirs() } ?: return
        themes.forEach { theme ->
            listOf(1, 2, 3).forEach { scale ->
                draw(folder, "badges-${theme.name.lowercase()}-${scale}x.png", theme, 680, 330, scale) { Sheet() }
            }
            // The screen's own pixels, four times over: whether a mark is crisp and centred at the size it is seen.
            magnified(File(folder, "badges-${theme.name.lowercase()}-1x.png"), File(folder, "badges-${theme.name.lowercase()}-1x-pixels.png"))
            draw(folder, "picker-${theme.name.lowercase()}.png", theme, 720, 520, 1) {
                Column(Modifier.padding(20.dp)) {
                    IconSetPicker(app.noctorium.settings.IconSet.STANDARD) {}
                    ServiceBadgeStylePicker(ServiceBadgeStyle.MARKS) {}
                }
            }
        }
    }

    /** Each way of drawing the badges, one row of every service each, then the badges where they are found. */
    @Composable
    private fun Sheet() {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceBadgeStyle.entries.forEach { style ->
                listOf(true, false).forEach { compact ->
                    Labelled("${style.displayName}${if (compact) "" else ", in full"}") {
                        CompositionLocalProvider(LocalServiceBadgeStyle provides style) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                (if (compact) ProviderType.entries else ProviderType.entries.take(4)).forEach { ProviderBadge(it, compact) }
                            }
                        }
                    }
                }
            }
            Labelled("Where they are") {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ServiceBadgeStyle.entries.forEach { style ->
                        CompositionLocalProvider(LocalServiceBadgeStyle provides style) {
                            Cover(ProviderType.YOUTUBE_MUSIC)
                            ListRow(ProviderType.SOUNDCLOUD)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Labelled(label: String, content: @Composable () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
            content()
        }
    }

    /** A card's cover with the badge in its corner, as Home and the library draw it. */
    @Composable
    private fun Cover(provider: ProviderType) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2E3A59), Color(0xFFB98A5E)))),
        ) {
            Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { ProviderBadge(provider, compact = true) }
        }
    }

    /** A row of a list: the badge, then a made-up song's title and artist. */
    @Composable
    private fun ListRow(provider: ProviderType) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProviderBadge(provider, compact = true)
            Column(Modifier.padding(start = 8.dp)) {
                Text("Harbour lights", fontSize = 13.sp)
                Text("The Night Ferries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    /** The badges' rows of [from], cut out and blown up four times without smoothing, so each pixel is a square. */
    private fun magnified(from: File, to: File) {
        val sheet = ImageIO.read(from).getSubimage(120, 10, 400, 180)
        val big = BufferedImage(sheet.width * 4, sheet.height * 4, BufferedImage.TYPE_INT_ARGB)
        AffineTransformOp(AffineTransform.getScaleInstance(4.0, 4.0), AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(sheet, big)
        ImageIO.write(big, "png", to)
    }

    private fun draw(folder: File, name: String, theme: ThemePreset, width: Int, height: Int, scale: Int, content: @Composable () -> Unit) {
        val scene = ImageComposeScene(width * scale, height * scale, Density(scale.toFloat())) {
            BadgeTheme(theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
            }
        }
        try {
            val file = File(folder, name)
            // A few frames in, so that anything easing in on its first frame has arrived.
            var image = scene.render(0)
            repeat(4) { frame -> image = scene.render((frame + 1) * 100_000_000L) }
            file.writeBytes(image.encodeToData()!!.bytes)
            assertTrue(file.length() > 1_000, "nothing was drawn for $name")
        } finally {
            scene.close()
        }
    }
}
