package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.noctorium.settings.ThemeSkin

/**
 * The desktop behind the windows: what the now playing screen shows under a Windows skin when its backdrop is
 * plain.
 *
 * 98's is the teal it was installed with, and nothing else. XP's is a blue sky over a rolling green hill,
 * drawn here from a handful of shapes: the photograph everybody remembers belongs to its photographer, and a
 * sky, two hills and some cloud are enough to put anybody in mind of it.
 */
@Composable
internal fun SkinDesktop(modifier: Modifier = Modifier) {
    val skin = skin()
    Canvas(modifier.fillMaxSize()) {
        if (skin == ThemeSkin.WINDOWS_XP) drawHillside() else drawRect(Classic.Desktop)
    }
}

private fun DrawScope.drawHillside() {
    val w = size.width
    val h = size.height
    // The sky, deepest overhead and paling towards the horizon.
    drawRect(Brush.verticalGradient(0f to Luna.Sky, .62f to Luna.SkyLow, 1f to Luna.SkyLow))
    // Cloud: soft white drifts, brightest at their hearts and gone at their edges.
    listOf(
        Triple(.18f, .2f, .16f),
        Triple(.32f, .16f, .11f),
        Triple(.62f, .1f, .14f),
        Triple(.78f, .24f, .19f),
        Triple(.9f, .14f, .09f),
        Triple(.47f, .32f, .1f),
    ).forEach { (x, y, r) ->
        val centre = Offset(w * x, h * y)
        val radius = w * r
        drawOval(
            Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.White.copy(alpha = .18f), Color.Transparent), centre, radius),
            topLeft = Offset(centre.x - radius, centre.y - radius * .42f),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * .84f),
        )
    }
    // A farther hill, darker and lower, behind the near one's shoulder.
    val far = Path().apply {
        moveTo(w * .42f, h)
        cubicTo(w * .62f, h * .66f, w * .86f, h * .6f, w, h * .63f)
        lineTo(w, h)
        close()
    }
    drawPath(far, Brush.verticalGradient(listOf(Luna.Hill.towards(Luna.HillShade, .55f), Luna.HillShade), startY = h * .6f, endY = h))
    // The near hill: one long smooth rise, lit along its crest and shading into the foreground.
    val near = Path().apply {
        moveTo(0f, h * .74f)
        cubicTo(w * .22f, h * .58f, w * .5f, h * .6f, w * .74f, h * .78f)
        cubicTo(w * .86f, h * .87f, w * .95f, h * .9f, w, h * .92f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        near,
        Brush.verticalGradient(
            0f to Luna.Hill.towards(Color.White, .18f),
            .25f to Luna.Hill,
            1f to Luna.HillShade,
            startY = h * .58f,
            endY = h,
        ),
    )
    // Sun on the near slope.
    drawPath(
        near,
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = .22f), Color.Transparent),
            center = Offset(w * .3f, h * .62f),
            radius = w * .35f,
        ),
    )
}
