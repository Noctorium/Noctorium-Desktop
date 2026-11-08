package app.noctorium.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class ShadowChaosRenderCheck {
    @Test
    fun `bundled Shadow artwork renders at desktop and phone sizes`() {
        val bytes = assertNotNull(javaClass.getResourceAsStream("/themes/shadow_chaos.png")).use { it.readBytes() }
        val picture = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        for ((width, height) in listOf(1200 to 760, 390 to 844)) {
            val scene = ImageComposeScene(width, height, Density(1f)) {
                Canvas(Modifier.fillMaxSize()) { drawChaosBackdrop(picture, size) }
            }
            try {
                val rendered = assertNotNull(scene.render().encodeToData())
                File("build/shadow-chaos-previews").apply { mkdirs() }
                    .resolve("shadow-chaos-${width}x${height}.png").writeBytes(rendered.bytes)
            } finally {
                scene.close()
            }
        }
    }
}
