package app.noctorium.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.noctorium.domain.ProviderType
import app.noctorium.domain.ServiceMarks
import java.util.EnumMap

/*
 * The services' marks as vectors, for the badges that say where a song comes from. The shapes are Base's
 * [ServiceMarks] -- Simple Icons' (CC0), and Material's folder for music on this computer -- and the phone builds
 * the same vectors from the same text.
 *
 * None of them is in any icon set: a service's mark is the service's, whatever style the buttons are drawn in, so
 * [Icon] finds nothing to swap it for and draws it as it is.
 */

private val built = EnumMap<ProviderType, ImageVector>(ProviderType::class.java)

/** [this] service's mark, filled in one colour on a 24-point square, built the first time it is asked for. */
internal fun ProviderType.markVector(): ImageVector = synchronized(built) {
    built.getOrPut(this) {
        ImageVector.Builder(
            name = "Mark.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = ServiceMarks.VIEWPORT,
            viewportHeight = ServiceMarks.VIEWPORT,
        ).apply {
            addPath(pathData = addPathNodes(markPath(this@markVector)), fill = SolidColor(Color.Black))
        }.build()
    }
}

/** [provider]'s mark as the text it is drawn from: Base's, with its arcs' flags written out. See [separatedArcFlags]. */
internal fun markPath(provider: ProviderType): String = separatedArcFlags(ServiceMarks.path(provider))

/**
 * [path] with every arc's two flags standing apart from what follows them.
 *
 * SVG lets the flags -- one digit each -- run straight into the next number, and Simple Icons writes them that way to
 * save room: Apple Music's mark has `a9.23 9.23 0 00-.24-2.19`. Compose's path parser reads `00` as one number, so
 * every figure after the first arc lands one place along and the mark comes out a scribble twice the size of its
 * square. Written out with a space between each of an arc's figures it reads the same in every parser. Nothing else
 * about the path changes.
 */
internal fun separatedArcFlags(path: String): String {
    val out = StringBuilder(path.length + 64)
    var command = ' '
    // Which of the current command's figures comes next, counted from its letter: an arc's are rx, ry, the rotation,
    // the two flags, then x and y, over again for each arc the letter is followed by.
    var figure = 0
    var at = 0
    while (at < path.length) {
        val char = path[at]
        when {
            char.isLetter() -> {
                command = char
                figure = 0
                out.append(char)
                at++
            }
            char == ',' || char.isWhitespace() -> at++
            else -> {
                val flag = (command == 'a' || command == 'A') && (figure % 7 == 3 || figure % 7 == 4)
                val end = if (flag) at + 1 else numberEnd(path, at)
                if (out.isNotEmpty() && !out.last().isLetter()) out.append(' ')
                out.append(path, at, end)
                figure++
                at = end
            }
        }
    }
    return out.toString()
}

/** Where the number starting at [start] in [path] ends: its sign, its digits, one point, and an exponent. */
private fun numberEnd(path: String, start: Int): Int {
    var at = start
    if (at < path.length && (path[at] == '-' || path[at] == '+')) at++
    var point = false
    while (at < path.length) {
        val char = path[at]
        when {
            char.isDigit() -> at++
            char == '.' && !point -> { point = true; at++ }
            (char == 'e' || char == 'E') && at > start -> {
                at++
                if (at < path.length && (path[at] == '-' || path[at] == '+')) at++
            }
            else -> break
        }
    }
    return at
}
