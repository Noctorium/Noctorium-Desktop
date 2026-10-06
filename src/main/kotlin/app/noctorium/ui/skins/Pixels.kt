package app.noctorium.ui.skins

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.floor

/*
 * Drawing at the resolution Windows 98 was drawn at: whole pixels.
 *
 * Every edge of that desktop is one pixel wide and lands on a pixel, and a bevel drawn half a pixel off is a
 * grey smear rather than an edge. So nothing here is placed or sized in fractions: lines are [pixel] wide, and
 * the small pictures -- the tick, the radio button, the trackbar's pointer, the arrows -- are pixel art, drawn
 * a pixel at a time from the same maps those controls were bitmaps of.
 */

/**
 * One pixel of the old desktop: a device pixel, or a whole number of them where the screen is dense enough
 * that a single one would be a hairline -- two on a doubled display, never one and a half.
 */
internal val DrawScope.pixel: Float get() = floor(density).coerceAtLeast(1f)

/**
 * A two-line edge, as every control of the time was framed: an outer and an inner line, each lit on its top
 * and left and shaded on its bottom and right.
 */
internal class Bevel(
    val outerLight: Color,
    val outerDark: Color,
    val innerLight: Color,
    val innerDark: Color,
) {
    companion object {
        /** A button at rest, standing out of the face. */
        val Raised = Bevel(Classic.Highlight, Classic.DarkShadow, Classic.Light, Classic.Shadow)

        /** A button held down: the raised edge the other way round. */
        val Pressed = Bevel(Classic.DarkShadow, Classic.Highlight, Classic.Shadow, Classic.Light)

        /** A field or a list, set into the face. */
        val Sunken = Bevel(Classic.Shadow, Classic.Highlight, Classic.DarkShadow, Classic.Light)

        /** A window's own frame, which is a raised edge with its outer light a step softer. */
        val Window = Bevel(Classic.Light, Classic.DarkShadow, Classic.Highlight, Classic.Shadow)

        /** A group box's groove: a sunken line beside a raised one. */
        val Etched = Bevel(Classic.Shadow, Classic.Highlight, Classic.Highlight, Classic.Shadow)
    }
}

/**
 * One line round the rectangle from [left], [top] to [right], [bottom]: [light] along the top and the left,
 * [dark] along the bottom and the right. The two far corners belong to the dark side, as they did.
 */
internal fun DrawScope.edge(
    light: Color,
    dark: Color,
    left: Float = 0f,
    top: Float = 0f,
    right: Float = size.width,
    bottom: Float = size.height,
) {
    val p = pixel
    val width = right - left
    val height = bottom - top
    if (width <= 0f || height <= 0f) return
    drawRect(light, Offset(left, top), Size((width - p).coerceAtLeast(0f), p))
    drawRect(light, Offset(left, top), Size(p, (height - p).coerceAtLeast(0f)))
    drawRect(dark, Offset(left, bottom - p), Size(width, p))
    drawRect(dark, Offset(right - p, top), Size(p, height))
}

/** A full two-line [bevel] round the rectangle, the inner line a pixel inside the outer. */
internal fun DrawScope.bevel(
    bevel: Bevel,
    left: Float = 0f,
    top: Float = 0f,
    right: Float = size.width,
    bottom: Float = size.height,
) {
    val p = pixel
    edge(bevel.outerLight, bevel.outerDark, left, top, right, bottom)
    edge(bevel.innerLight, bevel.innerDark, left + p, top + p, right - p, bottom - p)
}

/** The one-line edge a toolbar button rises to under the pointer, and a status bar's panes are sunk by. */
internal fun DrawScope.thinRaised(left: Float = 0f, top: Float = 0f, right: Float = size.width, bottom: Float = size.height) =
    edge(Classic.Highlight, Classic.Shadow, left, top, right, bottom)

internal fun DrawScope.thinSunken(left: Float = 0f, top: Float = 0f, right: Float = size.width, bottom: Float = size.height) =
    edge(Classic.Shadow, Classic.Highlight, left, top, right, bottom)

/** A line cut into the face: shadow, with a highlight a pixel below it. [length] runs along x from [x]. */
internal fun DrawScope.etchedLine(x: Float, y: Float, length: Float) {
    val p = pixel
    drawRect(Classic.Shadow, Offset(x, y), Size(length, p))
    drawRect(Classic.Highlight, Offset(x, y + p), Size(length, p))
}

/** The same standing upright: shadow, with a highlight a pixel to its right. */
internal fun DrawScope.etchedUpright(x: Float, y: Float, length: Float) {
    val p = pixel
    drawRect(Classic.Shadow, Offset(x, y), Size(p, length))
    drawRect(Classic.Highlight, Offset(x + p, y), Size(p, length))
}

/**
 * The dotted rectangle round whatever has the keyboard: every other pixel, starting from the corner, the way
 * DrawFocusRect left it.
 */
internal fun DrawScope.focusDots(inset: Float, colour: Color = Classic.Text) {
    val p = pixel
    val left = inset
    val top = inset
    val right = size.width - inset - p
    val bottom = size.height - inset - p
    if (right <= left || bottom <= top) return
    var x = left
    while (x <= right) {
        drawRect(colour, Offset(x, top), Size(p, p))
        drawRect(colour, Offset(x, bottom), Size(p, p))
        x += p * 2
    }
    var y = top + p * 2
    while (y < bottom) {
        drawRect(colour, Offset(left, y), Size(p, p))
        drawRect(colour, Offset(right, y), Size(p, p))
        y += p * 2
    }
}

/** The checkerboard of the two colours, a pixel a square, that scroll bars' tracks and latched buttons were filled with. */
internal fun DrawScope.checkerboard(
    first: Color = Classic.Highlight,
    second: Color = Classic.Face,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
) {
    drawRect(checkerBrush(pixel.toInt(), first, second), topLeft, size)
}

private val checkerBrushes = HashMap<Triple<Int, Color, Color>, Brush>()

private fun checkerBrush(unit: Int, first: Color, second: Color): Brush = checkerBrushes.getOrPut(Triple(unit, first, second)) {
    val tile = ImageBitmap(unit * 2, unit * 2)
    val canvas = Canvas(tile)
    val one = Paint().apply { color = first }
    val two = Paint().apply { color = second }
    val step = unit.toFloat()
    canvas.drawRect(0f, 0f, step, step, one)
    canvas.drawRect(step, step, step * 2, step * 2, one)
    canvas.drawRect(step, 0f, step * 2, step, two)
    canvas.drawRect(0f, step, step, step * 2, two)
    ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
}

/**
 * Pixel art: [rows] of characters, each one pixel, coloured by [palette] and left clear where a character has
 * no colour there. Drawn with its top left at [x], [y], a run of one colour at a time.
 */
internal fun DrawScope.pixelArt(rows: List<String>, palette: Map<Char, Color>, x: Float, y: Float) {
    val p = pixel
    rows.forEachIndexed { row, line ->
        var column = 0
        while (column < line.length) {
            val colour = palette[line[column]]
            var end = column + 1
            while (end < line.length && line[end] == line[column]) end++
            if (colour != null) drawRect(colour, Offset(x + column * p, y + row * p), Size((end - column) * p, p))
            column = end
        }
    }
}

/** How wide and tall a piece of pixel art is, in pixels of [pixel]'s size. */
internal fun DrawScope.artSize(rows: List<String>): Size = Size(rows.maxOf { it.length } * pixel, rows.size * pixel)

/** Pixel art centred in what is being drawn, landing on whole pixels. */
internal fun DrawScope.pixelArtCentred(rows: List<String>, palette: Map<Char, Color>, offset: Offset = Offset.Zero) {
    val art = artSize(rows)
    val x = floor((size.width - art.width) / 2f / pixel) * pixel + offset.x
    val y = floor((size.height - art.height) / 2f / pixel) * pixel + offset.y
    pixelArt(rows, palette, x, y)
}

/** The pictures the controls of the time were made of, as maps of their pixels. */
internal object Art {
    /** The tick in a checkbox and beside a chosen menu item. */
    val tick = listOf(
        "......#",
        ".....##",
        "#...###",
        "##.###.",
        "#####..",
        ".###...",
        "..#....",
    )

    /**
     * The radio button's round well: shadow and black round its top and left, white and light grey round
     * its bottom and right, white inside.
     */
    val radio = listOf(
        "....ssss....",
        "..sskkkkss..",
        ".skkwwwwkkh.",
        ".skwwwwwwlh.",
        "skwwwwwwwwlh",
        "skwwwwwwwwlh",
        "skwwwwwwwwlh",
        "skwwwwwwwwlh",
        ".skwwwwwwlh.",
        ".sllwwwwllh.",
        "..hhllllhh..",
        "....hhhh....",
    )

    /** The dot in a chosen radio button, four pixels across with its corners off. */
    val radioDot = listOf(
        ".##.",
        "####",
        "####",
        ".##.",
    )

    /** The cross on a window's close button. */
    val close = listOf(
        "##....##",
        ".##..##.",
        "..####..",
        "...##...",
        "..####..",
        ".##..##.",
        "##....##",
    )

    /** The triangles on scroll bar buttons and beside a menu item that opens another. */
    val up = listOf(
        "...#...",
        "..###..",
        ".#####.",
        "#######",
    )
    val down = up.reversed()
    val right = listOf(
        "#...",
        "##..",
        "###.",
        "####",
        "###.",
        "##..",
        "#...",
    )
    val left = right.map { it.reversed() }

    /** XP's arrows, which were chevrons rather than solid triangles. */
    val chevronUp = listOf(
        "...#...",
        "..###..",
        ".##.##.",
        "##...##",
    )
    val chevronDown = chevronUp.reversed()
    val chevronLeft = listOf(
        "...#",
        "..##",
        ".##.",
        "##..",
        ".##.",
        "..##",
        "...#",
    )
    val chevronRight = chevronLeft.map { it.reversed() }

    /**
     * The trackbar's pointer: a raised slab eleven pixels wide whose foot comes to a point, lit on the left as
     * everything was. `w` highlight, `l` light, `f` face, `s` shadow, `k` dark shadow.
     */
    val trackbarThumb = buildList {
        add("wwwwwwwwwwk")
        add("wllllllllsk")
        repeat(14) { add("wlfffffffsk") }
        add(".wlfffffsk.")
        add("..wlfffsk..")
        add("...wlfsk...")
        add("....wsk....")
        add(".....k.....")
    }

    /** The colours of [radio] and [trackbarThumb], by the letters they are written in. */
    val bevelPalette: Map<Char, Color> = mapOf(
        'w' to Classic.Highlight,
        'h' to Classic.Highlight,
        'l' to Classic.Light,
        'f' to Classic.Face,
        's' to Classic.Shadow,
        'k' to Classic.DarkShadow,
    )
}
