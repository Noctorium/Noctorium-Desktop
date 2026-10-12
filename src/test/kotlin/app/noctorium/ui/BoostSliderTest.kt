package app.noctorium.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.skins.LocalSkin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The booster's slider, clicked rather than dragged, off screen.
 *
 * A click on the track is a press and a release with nothing drawn between them, and the slider's finish was
 * reading the level as last drawn -- the old one -- so a click moved the slider and not the player. Clicked once
 * at three quarters of the way, the level written has to be the one the click chose, under the standard skin's
 * slider and under the Windows skins' trackbar alike.
 */
@OptIn(ExperimentalComposeUiApi::class)
class BoostSliderTest {

    private fun clicked(skin: ThemeSkin, atFraction: Float): List<Int> {
        val written = mutableListOf<Int>()
        val width = 400
        val scene = ImageComposeScene(width, 120, Density(1f)) {
            MaterialTheme {
                CompositionLocalProvider(LocalSkin provides skin, LocalMotion provides false) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 0.dp)) { BoostSlider(200) { written += it } }
                }
            }
        }
        try {
            scene.render(0)
            // The track runs across the whole width, a thumb's half in from each end; the slider sits under the label.
            val at = Offset(width * atFraction, 42f)
            scene.sendPointerEvent(PointerEventType.Press, at)
            scene.sendPointerEvent(PointerEventType.Release, at)
            scene.render(100_000_000L)
        } finally {
            scene.close()
        }
        return written
    }

    @Test
    fun `a click on the track writes the level it chose`() {
        listOf(ThemeSkin.STANDARD, ThemeSkin.WINDOWS_98).forEach { skin ->
            val written = clicked(skin, .75f)
            assertEquals(1, written.size, "$skin: one write for one click, not none: $written")
            assertTrue(written.single() in 650..850, "$skin: about three quarters of the way to 1000%: $written")
        }
    }
}
