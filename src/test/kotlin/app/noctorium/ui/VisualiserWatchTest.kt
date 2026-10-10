package app.noctorium.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.playback.AudioAnalysis
import app.noctorium.settings.VisualiserStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The player measures the sound only while something watches it, and the visualiser is the only thing that does:
 * it watches while it is on screen and drawing, and not otherwise -- not off screen, and not while it is set to Off.
 */
class VisualiserWatchTest {

    @Test
    fun `the sound is watched only while a visualiser is on screen`() {
        val sound = MutableStateFlow<AudioAnalysis?>(AudioAnalysis(bands = List(10) { .5f }, peaks = listOf(.7f, .7f), rms = listOf(.5f, .5f)))
        var shown by mutableStateOf(true)
        var style by mutableStateOf(VisualiserStyle.BARS)
        val scene = ImageComposeScene(240, 80, Density(1f)) {
            if (shown) AudioVisualiser(style, sound, Modifier.size(200.dp, 60.dp))
        }
        try {
            var time = 0L
            fun frame() {
                repeat(3) {
                    scene.render(time)
                    time += 16_000_000L
                }
            }
            frame()
            assertEquals(1, sound.subscriptionCount.value, "drawing bars, it watches")
            style = VisualiserStyle.LEVEL_METER
            frame()
            assertEquals(1, sound.subscriptionCount.value, "drawing a meter, it watches")
            style = VisualiserStyle.OFF
            frame()
            assertEquals(0, sound.subscriptionCount.value, "set to Off, nothing is watched")
            style = VisualiserStyle.BARS
            frame()
            assertEquals(1, sound.subscriptionCount.value)
            shown = false
            frame()
            assertEquals(0, sound.subscriptionCount.value, "off screen, nothing is watched")
        } finally {
            scene.close()
        }
    }
}
