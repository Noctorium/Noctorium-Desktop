package app.noctorium.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/*
 * The Line icon set: thin single strokes in one colour, the way a desktop's own icon theme draws a music player's
 * buttons.
 *
 * The shapes are Lucide's (https://lucide.dev), taken from lucide-react 1.50.0's icon nodes, with its circles,
 * rectangles and lines written out as paths. Lucide is under the ISC licence, and the icons it took from Feather
 * under the MIT licence; both notices are reproduced below and credited in the README.
 *
 *     ISC License
 *
 *     Copyright (c) 2026 Lucide Icons and Contributors
 *
 *     Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is
 *     hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE
 *     INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *     ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 *     DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 *     CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 *     The MIT License (MIT) (for the icons Lucide derived from the Feather project, which its LICENSE lists)
 *
 *     Copyright (c) 2013-present Cole Bemis
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *     associated documentation files (the "Software"), to deal in the Software without restriction, including
 *     without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *     following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in all copies or substantial
 *     portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *     LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 *     EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *     IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 *     USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
internal object LineIcons {
    /**
     * How thick the strokes are on Lucide's 24-point grid. Lucide draws at 2; a point and a half is nearer the
     * hairline a desktop icon theme is drawn in, and still holds at the eighteen points most buttons draw at.
     */
    const val STROKE = 1.5f

    private val built = ConcurrentHashMap<Pair<String, Boolean>, ImageVector>()

    /** The names there are shapes for. */
    val names: Set<String> get() = PATHS.keys

    /**
     * The icon called [name], built the first time it is asked for. [filled] fills its closed shapes as well, for
     * the few icons whose filling is what they say -- a heart for a song liked, a pin for one pinned -- beside the
     * outline that says the opposite.
     */
    fun get(name: String, filled: Boolean = false): ImageVector = built.getOrPut(name to filled) {
        val paths = requireNotNull(PATHS[name]) { "No line icon called $name" }
        ImageVector.Builder(
            name = "Line." + name + if (filled) ".Filled" else "",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { data ->
                addPath(
                    pathData = addPathNodes(data),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = STROKE,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }

    private val PATHS: Map<String, List<String>> = mapOf(
        "arrow-down-to-dot" to listOf(
            "M12 2v14",
            "m19 9-7 7-7-7",
            "M11 21a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z",
        ),
        "arrow-left" to listOf(
            "m12 19-7-7 7-7",
            "M19 12H5",
        ),
        "arrow-right-to-line" to listOf(
            "M17 12H3",
            "m11 18 6-6-6-6",
            "M21 5v14",
        ),
        "audio-lines" to listOf(
            "M2 10v3",
            "M6 6v11",
            "M10 3v18",
            "M14 8v7",
            "M18 5v13",
            "M22 10v3",
        ),
        "bell-ring" to listOf(
            "M10.268 21a2 2 0 0 0 3.464 0",
            "M22 8c0-2.3-.8-4.3-2-6",
            "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326",
            "M4 2C2.8 3.7 2 5.7 2 8",
        ),
        "car" to listOf(
            "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2",
            "M5 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
            "M9 17h6",
            "M15 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
        ),
        "cast" to listOf(
            "M2 8V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6",
            "M2 12a9 9 0 0 1 8 8",
            "M2 16a5 5 0 0 1 4 4",
            "M2 20L2.01 20",
        ),
        "check" to listOf(
            "M20 6 9 17l-5-5",
        ),
        "chevron-down" to listOf(
            "m6 9 6 6 6-6",
        ),
        "chevron-left" to listOf(
            "m15 18-6-6 6-6",
        ),
        "chevron-right" to listOf(
            "m9 18 6-6-6-6",
        ),
        "chevron-up" to listOf(
            "m18 15-6-6-6 6",
        ),
        "chevrons-up" to listOf(
            "m17 11-5-5-5 5",
            "m17 18-5-5-5 5",
        ),
        "circle" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
        ),
        "circle-alert" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M12 8L12 12",
            "M12 16L12.01 16",
        ),
        "circle-arrow-down" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M12 8v8",
            "m8 12 4 4 4-4",
        ),
        "circle-check" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "m16 9-5.5 5.5L8 12",
        ),
        "circle-check-big" to listOf(
            "M21.801 10A10 10 0 1 1 17 3.335",
            "m9 11 3 3L22 4",
        ),
        "circle-minus" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M8 12h8",
        ),
        "circle-play" to listOf(
            "M9 9.003a1 1 0 0 1 1.517-.859l4.997 2.997a1 1 0 0 1 0 1.718l-4.997 2.997A1 1 0 0 1 9 14.996z",
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
        ),
        "circle-stop" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M10 9h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
        ),
        "circle-user" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662",
        ),
        "clipboard-paste" to listOf(
            "M11 14h10",
            "M16 4h2a2 2 0 0 1 2 2v1.344",
            "m17 18 4-4-4-4",
            "M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 1.793-1.113",
            "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-2a1 1 0 0 1 1 -1z",
        ),
        "cloud" to listOf(
            "M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z",
        ),
        "cloud-off" to listOf(
            "M10.94 5.274A7 7 0 0 1 15.71 10h1.79a4.5 4.5 0 0 1 4.222 6.057",
            "M18.796 18.81A4.5 4.5 0 0 1 17.5 19H9A7 7 0 0 1 5.79 5.78",
            "m2 2 20 20",
        ),
        "cloud-upload" to listOf(
            "M12 13v8",
            "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242",
            "m8 17 4-4 4 4",
        ),
        "columns-3" to listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M9 3v18",
            "M15 3v18",
        ),
        "copy" to listOf(
            "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
            "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2",
        ),
        "disc-3" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M6 12c0-1.7.7-3.2 1.8-4.2",
            "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
            "M18 12c0 1.7-.7 3.2-1.8 4.2",
        ),
        "download" to listOf(
            "M12 15V3",
            "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
            "m7 10 5 5 5-5",
        ),
        "earth" to listOf(
            "M21.54 15H17a2 2 0 0 0-2 2v4.54",
            "M7 3.34V5a3 3 0 0 0 3 3a2 2 0 0 1 2 2c0 1.1.9 2 2 2a2 2 0 0 0 2-2c0-1.1.9-2 2-2h3.17",
            "M11 21.95V18a2 2 0 0 0-2-2a2 2 0 0 1-2-2v-1a2 2 0 0 0-2-2H2.05",
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
        ),
        "ellipsis-vertical" to listOf(
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z",
            "M11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z",
            "M11 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z",
        ),
        "external-link" to listOf(
            "M15 3h6v6",
            "M10 14 21 3",
            "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6",
        ),
        "folder-down" to listOf(
            "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z",
            "M12 10v6",
            "m15 13-3 3-3-3",
        ),
        "folder-open" to listOf(
            "m6 14 1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2",
        ),
        "gamepad-2" to listOf(
            "M6 11L10 11",
            "M8 9L8 13",
            "M15 12L15.01 12",
            "M18 10L18.01 10",
            "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z",
        ),
        "gauge" to listOf(
            "m12 14 4-4",
            "M3.34 19a10 10 0 1 1 17.32 0",
        ),
        "globe" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20",
            "M2 12h20",
        ),
        "hard-drive-download" to listOf(
            "M12 2v8",
            "m16 6-4 4-4-4",
            "M4 14h16a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-4a2 2 0 0 1 2 -2z",
            "M6 18h.01",
            "M10 18h.01",
        ),
        "headphones" to listOf(
            "M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3",
        ),
        "heart" to listOf(
            "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5",
        ),
        "heart-pulse" to listOf(
            "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5",
            "M3.22 13H9.5l.5-1 2 4.5 2-7 1.5 3.5h5.27",
        ),
        "history" to listOf(
            "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
            "M3 3v5h5",
            "M12 7v5l4 2",
        ),
        "hourglass" to listOf(
            "M5 22h14",
            "M5 2h14",
            "M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22",
            "M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2",
        ),
        "house" to listOf(
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
            "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        ),
        "id-card" to listOf(
            "M13 19a4 4 0 00-8 0",
            "M16 10h2",
            "M16 14h2",
            "M6 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
        ),
        "image" to listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M7 9a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
            "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21",
        ),
        "infinity" to listOf(
            "M6 16c5 0 7-8 12-8a4 4 0 0 1 0 8c-5 0-7-8-12-8a4 4 0 1 0 0 8",
        ),
        "info" to listOf(
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0z",
            "M12 16v-4",
            "M12 8h.01",
        ),
        "key-round" to listOf(
            "M2.586 17.414A2 2 0 0 0 2 18.828V21a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h1a1 1 0 0 0 1-1v-1a1 1 0 0 1 1-1h.172a2 2 0 0 0 1.414-.586l.814-.814a6.5 6.5 0 1 0-4-4z",
            "M16 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0z",
        ),
        "layout-grid" to listOf(
            "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
            "M15 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
            "M15 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
            "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
        ),
        "library" to listOf(
            "m16 6 4 14",
            "M12 6v14",
            "M8 8v12",
            "M4 4v16",
        ),
        "link" to listOf(
            "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71",
            "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71",
        ),
        "list-minus" to listOf(
            "M16 5H3",
            "M11 12H3",
            "M16 19H3",
            "M21 12h-6",
        ),
        "list-music" to listOf(
            "M16 5H3",
            "M11 12H3",
            "M11 19H3",
            "M21 16V5",
            "M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
        ),
        "list-plus" to listOf(
            "M16 5H3",
            "M11 12H3",
            "M16 19H3",
            "M18 9v6",
            "M21 12h-6",
        ),
        "list-start" to listOf(
            "M3 5h6",
            "M3 12h13",
            "M3 19h13",
            "m16 8-3-3 3-3",
            "M21 19V7a2 2 0 0 0-2-2h-6",
        ),
        "list-x" to listOf(
            "M16 5H3",
            "M11 12H3",
            "M16 19H3",
            "m15.5 9.5 5 5",
            "m20.5 9.5-5 5",
        ),
        "lock" to listOf(
            "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2z",
            "M7 11V7a5 5 0 0 1 10 0v4",
        ),
        "log-in" to listOf(
            "m10 17 5-5-5-5",
            "M15 12H3",
            "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4",
        ),
        "mic-vocal" to listOf(
            "m11 7.601-5.994 8.19a1 1 0 0 0 .1 1.298l.817.818a1 1 0 0 0 1.314.087L15.09 12",
            "M16.5 21.174C15.5 20.5 14.372 20 13 20c-2.058 0-3.928 2.356-6 2-2.072-.356-2.775-3.369-1.5-4.5",
            "M11 7a5 5 0 1 0 10 0a5 5 0 1 0 -10 0z",
        ),
        "monitor" to listOf(
            "M4 3h16a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z",
            "M8 21L16 21",
            "M12 17L12 21",
        ),
        "monitor-smartphone" to listOf(
            "M18 8V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h8",
            "M10 19v-3.96 3.15",
            "M7 19h5",
            "M18 12h2a2 2 0 0 1 2 2v6a2 2 0 0 1 -2 2h-2a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2z",
        ),
        "moon" to listOf(
            "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401",
        ),
        "mouse-pointer-click" to listOf(
            "M14 4.1 12 6",
            "m5.1 8-2.9-.8",
            "m6 12-1.9 2",
            "M7.2 2.2 8 5.1",
            "M9.037 9.69a.498.498 0 0 1 .653-.653l11 4.5a.5.5 0 0 1-.074.949l-4.349 1.041a1 1 0 0 0-.74.739l-1.04 4.35a.5.5 0 0 1-.95.074z",
        ),
        "music" to listOf(
            "M9 18V5l12-2v13",
            "M3 18a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M15 16a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
        ),
        "network" to listOf(
            "M17 16h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
            "M3 16h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
            "M10 2h4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z",
            "M5 16v-3a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v3",
            "M12 12V8",
        ),
        "palette" to listOf(
            "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z",
            "M13 6.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0z",
            "M17 10.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0z",
            "M6 12.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0z",
            "M8 7.5a0.5 0.5 0 1 0 1 0a0.5 0.5 0 1 0 -1 0z",
        ),
        "panel-right" to listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M15 3v18",
        ),
        "pause" to listOf(
            "M15 3h3a1 1 0 0 1 1 1v16a1 1 0 0 1 -1 1h-3a1 1 0 0 1 -1 -1v-16a1 1 0 0 1 1 -1z",
            "M6 3h3a1 1 0 0 1 1 1v16a1 1 0 0 1 -1 1h-3a1 1 0 0 1 -1 -1v-16a1 1 0 0 1 1 -1z",
        ),
        "pencil" to listOf(
            "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
            "m15 5 4 4",
        ),
        "pin" to listOf(
            "M12 17v5",
            "M9 10.76a2 2 0 0 1-1.11 1.79l-1.78.9A2 2 0 0 0 5 15.24V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.76a2 2 0 0 0-1.11-1.79l-1.78-.9A2 2 0 0 1 15 10.76V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z",
        ),
        "pipette" to listOf(
            "m12 9-8.414 8.414A2 2 0 0 0 3 18.828v1.344a2 2 0 0 1-.586 1.414A2 2 0 0 1 3.828 21h1.344a2 2 0 0 0 1.414-.586L15 12",
            "m18 9 .4.4a1 1 0 1 1-3 3l-3.8-3.8a1 1 0 1 1 3-3l.4.4 3.4-3.4a1 1 0 1 1 3 3z",
            "m2 22 .414-.414",
        ),
        "play" to listOf(
            "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z",
        ),
        "plus" to listOf(
            "M5 12h14",
            "M12 5v14",
        ),
        "power" to listOf(
            "M12 2v10",
            "M18.4 6.6a9 9 0 1 1-12.77.04",
        ),
        "puzzle" to listOf(
            "M15.39 4.39a1 1 0 0 0 1.68-.474 2.5 2.5 0 1 1 3.014 3.015 1 1 0 0 0-.474 1.68l1.683 1.682a2.414 2.414 0 0 1 0 3.414L19.61 15.39a1 1 0 0 1-1.68-.474 2.5 2.5 0 1 0-3.014 3.015 1 1 0 0 1 .474 1.68l-1.683 1.682a2.414 2.414 0 0 1-3.414 0L8.61 19.61a1 1 0 0 0-1.68.474 2.5 2.5 0 1 1-3.014-3.015 1 1 0 0 0 .474-1.68l-1.683-1.682a2.414 2.414 0 0 1 0-3.414L4.39 8.61a1 1 0 0 1 1.68.474 2.5 2.5 0 1 0 3.014-3.015 1 1 0 0 1-.474-1.68l1.683-1.682a2.414 2.414 0 0 1 3.414 0z",
        ),
        "radio" to listOf(
            "M16.247 7.761a6 6 0 0 1 0 8.478",
            "M19.075 4.933a10 10 0 0 1 0 14.134",
            "M4.925 19.067a10 10 0 0 1 0-14.134",
            "M7.753 16.239a6 6 0 0 1 0-8.478",
            "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
        ),
        "refresh-ccw" to listOf(
            "M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
            "M3 3v5h5",
            "M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16",
            "M16 16h5v5",
        ),
        "refresh-cw" to listOf(
            "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
            "M21 3v5h-5",
            "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
            "M8 16H3v5",
        ),
        "repeat" to listOf(
            "m17 2 4 4-4 4",
            "M3 11v-1a4 4 0 0 1 4-4h14",
            "m7 22-4-4 4-4",
            "M21 13v1a4 4 0 0 1-4 4H3",
        ),
        "repeat-1" to listOf(
            "m17 2 4 4-4 4",
            "M3 11v-1a4 4 0 0 1 4-4h14",
            "m7 22-4-4 4-4",
            "M21 13v1a4 4 0 0 1-4 4H3",
            "M11 10h1v4",
        ),
        "rotate-ccw" to listOf(
            "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
            "M3 3v5h5",
        ),
        "rows-3" to listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
            "M21 9H3",
            "M21 15H3",
        ),
        "save" to listOf(
            "M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
            "M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7",
            "M7 3v4a1 1 0 0 0 1 1h7",
        ),
        "search" to listOf(
            "m21 21-4.34-4.34",
            "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0z",
        ),
        "settings" to listOf(
            "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
        ),
        "shapes" to listOf(
            "M8.3 10a.7.7 0 0 1-.626-1.079L11.4 3a.7.7 0 0 1 1.198-.043L16.3 8.9a.7.7 0 0 1-.572 1.1Z",
            "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-5a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z",
            "M14 17.5a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0 -7 0z",
        ),
        "share-2" to listOf(
            "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
            "M8.59 13.51L15.42 17.49",
            "M15.41 6.51L8.59 10.49",
        ),
        "shield" to listOf(
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        ),
        "shuffle" to listOf(
            "m18 14 4 4-4 4",
            "m18 2 4 4-4 4",
            "M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22",
            "M2 6h1.972a4 4 0 0 1 3.6 2.2",
            "M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45",
        ),
        "skip-back" to listOf(
            "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z",
            "M3 20V4",
        ),
        "skip-forward" to listOf(
            "M21 4v16",
            "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z",
        ),
        "sliders-horizontal" to listOf(
            "M10 5H3",
            "M12 19H3",
            "M14 3v4",
            "M16 17v4",
            "M21 12h-9",
            "M21 19h-5",
            "M21 5h-7",
            "M8 10v4",
            "M8 12H3",
        ),
        "sliders-vertical" to listOf(
            "M10 8h4",
            "M12 21v-9",
            "M12 8V3",
            "M17 16h4",
            "M19 12V3",
            "M19 21v-5",
            "M3 14h4",
            "M5 10V3",
            "M5 21v-7",
        ),
        "smartphone" to listOf(
            "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2z",
            "M12 18h.01",
        ),
        "sparkles" to listOf(
            "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
            "M20 2v4",
            "M22 4h-4",
            "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z",
        ),
        "speaker" to listOf(
            "M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2z",
            "M12 6h.01",
            "M8 14a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z",
            "M12 14h.01",
        ),
        "square" to listOf(
            "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2z",
        ),
        "square-arrow-out-up-right" to listOf(
            "M21 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h6",
            "m21 3-9 9",
            "M15 3h6v6",
        ),
        "tablet" to listOf(
            "M6 2h12a2 2 0 0 1 2 2v16a2 2 0 0 1 -2 2h-12a2 2 0 0 1 -2 -2v-16a2 2 0 0 1 2 -2z",
            "M12 18L12.01 18",
        ),
        "trash-2" to listOf(
            "M10 11v6",
            "M14 11v6",
            "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
            "M3 6h18",
            "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2",
        ),
        "trending-up" to listOf(
            "M16 7h6v6",
            "m22 7-8.5 8.5-5-5L2 17",
        ),
        "triangle-alert" to listOf(
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
            "M12 9v4",
            "M12 17h.01",
        ),
        "tv" to listOf(
            "m17 2-5 5-5-5",
            "M4 7h16a2 2 0 0 1 2 2v11a2 2 0 0 1 -2 2h-16a2 2 0 0 1 -2 -2v-11a2 2 0 0 1 2 -2z",
        ),
        "type" to listOf(
            "M12 4v16",
            "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2",
            "M9 20h6",
        ),
        "unlink" to listOf(
            "m18.84 12.25 1.72-1.71h-.02a5.004 5.004 0 0 0-.12-7.07 5.006 5.006 0 0 0-6.95 0l-1.72 1.71",
            "m5.17 11.75-1.71 1.71a5.004 5.004 0 0 0 .12 7.07 5.006 5.006 0 0 0 6.95 0l1.71-1.71",
            "M8 2L8 5",
            "M2 8L5 8",
            "M16 19L16 22",
            "M19 16L22 16",
        ),
        "user" to listOf(
            "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
            "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z",
        ),
        "user-plus" to listOf(
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
            "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z",
            "M19 8L19 14",
            "M22 11L16 11",
        ),
        "users" to listOf(
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
            "M16 3.128a4 4 0 0 1 0 7.744",
            "M22 21v-2a4 4 0 0 0-3-3.87",
            "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0z",
        ),
        "volume-2" to listOf(
            "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
            "M16 9a5 5 0 0 1 0 6",
            "M19.364 18.364a9 9 0 0 0 0-12.728",
        ),
        "volume-x" to listOf(
            "M11 4.702a.7.7 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.7.7 0 0 0 11 19.298z",
            "m16.5 14.5 5-5",
            "m16.5 9.5 5 5",
        ),
        "x" to listOf(
            "M18 6 6 18",
            "m6 6 12 12",
        ),
        "zap" to listOf(
            "M15.914 4a1.5 1.5 0 00-2.474-1.561l-9 9A1.5 1.5 0 005.5 14h4.002a.5.5 0 01.471.666L8.086 20a1.5 1.5 0 002.475 1.56l9-9A1.5 1.5 0 0018.5 10h-3.997a.5.5 0 01-.472-.667z",
        ),
    )
}
