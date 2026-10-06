package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.settings.ThemeSkin
import kotlin.math.roundToInt

/*
 * Windows themselves: the title bar with its caption buttons, the frame round a window, and the things a
 * window's face held -- group boxes, tabs, the white pane a folder's contents sat in, a tooltip.
 */

/** How tall a title bar is: 98's eighteen pixels, Luna's twenty-five. */
internal fun ThemeSkin.titleBarHeight(): Dp = if (this == ThemeSkin.WINDOWS_XP) 25.dp else 18.dp

/**
 * A title bar: the window's icon -- Noctorium's mark -- its title in bold white, and a close button at the far
 * end when there is something for it to close.
 *
 * 98's runs from the navy of an active window to the light blue of its end, left to right; Luna's is lit from
 * above, its deep blue between a bright rim and a darker foot, and its top corners round, as the window's are.
 * Its title is in Trebuchet with a shadow under it. [title] is whatever the caller writes there: it is laid out
 * on one line however long it is, and what does not fit is cut off at the close button, as a long title was.
 */
@Composable
internal fun TitleBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    onClose: (() -> Unit)? = null,
) {
    val skin = skin()
    val xp = skin == ThemeSkin.WINDOWS_XP
    val height = skin.titleBarHeight()
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind { if (xp) drawLunaTitle(active) else drawClassicTitle(active) }
            .padding(start = if (xp) 5.dp else 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource("noctorium-mark.png"),
            null,
            Modifier.size(16.dp).clip(RoundedCornerShape(if (xp) 3.dp else 0.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(if (xp) 5.dp else 3.dp))
        val pixels = with(LocalDensity.current) { (if (xp) 13.sp else 11.sp).toPx() }
        val style = TextStyle(
            fontFamily = if (xp) SkinFonts.trebuchet else SkinFonts.classic,
            fontWeight = FontWeight.Bold,
            fontSize = if (xp) 13.sp else 11.sp,
            color = if (xp) Luna.TitleText else if (active) Classic.TitleText else Classic.Face,
            shadow = if (xp) Shadow(Color(0xFF0A1E7A).copy(alpha = .75f), Offset(1f, 1f), 1.5f) else null,
        ).rasterisedFor(if (xp) 17f else pixels)
        Box(Modifier.weight(1f).clipToBounds()) {
            Box(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
                CompositionLocalProvider(LocalContentColor provides (style.color)) {
                    ProvideTextStyle(style) { title() }
                }
            }
        }
        if (onClose != null) CaptionClose(onClose)
    }
}

private fun DrawScope.drawClassicTitle(active: Boolean) {
    drawRect(
        Brush.horizontalGradient(
            if (active) listOf(Classic.Title, Classic.TitleEnd) else listOf(Classic.InactiveTitle, Classic.InactiveTitleEnd),
        ),
    )
}

private fun DrawScope.drawLunaTitle(active: Boolean) {
    val top = if (active) Luna.TitleTop else Luna.InactiveTitle.towards(Luna.Window, .35f)
    val middle = if (active) Luna.Title else Luna.InactiveTitle
    val low = if (active) Luna.TitleLow else Luna.InactiveTitle.towards(Luna.Window, .15f)
    val foot = if (active) Luna.TitleFoot else Luna.InactiveTitle.towards(Color.Black, .1f)
    drawRect(Brush.verticalGradient(0f to top, .12f to middle, .3f to middle, .82f to low, 1f to low))
    drawRect(foot, Offset(0f, size.height - pixel), Size(size.width, pixel))
}

/**
 * The close button at the end of a title bar: 98's sixteen by fourteen raised slab with a black cross, Luna's
 * twenty-one pixel square in its red-orange with a white edge and a white cross.
 */
@Composable
private fun CaptionClose(onClose: () -> Unit) {
    val skin = skin()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val xp = skin == ThemeSkin.WINDOWS_XP
    Canvas(
        Modifier
            .size(if (xp) 21.dp else 16.dp, if (xp) 21.dp else 14.dp)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Button, onClick = onClose)
            .semantics { contentDescription = "Close" },
    ) {
        val p = pixel
        if (xp) {
            val radius = CornerRadius(3.dp.toPx())
            val face = when {
                pressed -> Luna.Close.towards(Color.Black, .25f)
                hovered -> Luna.Close.towards(Luna.Window, .2f)
                else -> Luna.Close
            }
            drawRoundRect(Brush.verticalGradient(listOf(face.towards(Luna.Window, .25f), face, face.towards(Color.Black, .1f))), cornerRadius = radius)
            drawRoundRect(Luna.Window, topLeft = Offset(p / 2, p / 2), size = Size(size.width - p, size.height - p), cornerRadius = radius, style = Stroke(p))
            val inset = size.width * .3f
            val stroke = 2.dp.toPx().coerceAtLeast(p * 2)
            drawLine(Luna.Window, Offset(inset, inset), Offset(size.width - inset, size.height - inset), stroke, StrokeCap.Square)
            drawLine(Luna.Window, Offset(size.width - inset, inset), Offset(inset, size.height - inset), stroke, StrokeCap.Square)
        } else {
            drawRect(Classic.Face)
            bevel(if (pressed) Bevel.Pressed else Bevel.Raised)
            pixelArtCentred(Art.close, mapOf('#' to Classic.Text), if (pressed) Offset(p, p) else Offset.Zero)
        }
    }
}

/**
 * A window: a frame, a title bar and a face to put things on. 98's frame is its bevel with a pixel of face
 * inside it; Luna's is a blue border under a title bar whose top corners are round.
 */
@Composable
internal fun SkinWindow(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    active: Boolean = true,
    face: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val skin = skin()
    if (skin == ThemeSkin.WINDOWS_XP) {
        val shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
        Column(
            modifier
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Luna.Title, Luna.Frame)))
                .padding(start = 3.dp, end = 3.dp, bottom = 3.dp),
        ) {
            TitleBar(title, Modifier.padding(horizontal = 0.dp), active, onClose)
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .background(face ?: Luna.Face),
                content = content,
            )
        }
        return
    }
    Column(
        modifier
            .drawBehind {
                drawRect(face ?: Classic.Face)
                bevel(Bevel.Window)
            }
            .padding(3.dp),
    ) {
        TitleBar(title, active = active, onClose = onClose)
        Column(Modifier.fillMaxWidth().weight(1f, fill = false).padding(top = 1.dp), content = content)
    }
}

/**
 * A group box: an etched frame under 98 and Luna's soft-cornered one under XP, round a set of options that
 * belong together, with [GroupLegend] -- its title -- set into the top edge.
 */
@Composable
internal fun GroupBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val skin = skin()
    Column(
        modifier
            .drawBehind {
                val line = (LEGEND_LINE.toPx() / pixel).roundToInt() * pixel
                if (skin == ThemeSkin.WINDOWS_XP) {
                    drawRoundRect(
                        Luna.GroupEdge,
                        topLeft = Offset(pixel / 2, line + pixel / 2),
                        size = Size(size.width - pixel, size.height - line - pixel),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(pixel),
                    )
                } else {
                    bevel(Bevel.Etched, 0f, line, size.width, size.height)
                }
            }
            .padding(start = 11.dp, end = 11.dp, top = GROUP_TOP, bottom = 11.dp),
        content = content,
    )
}

/** Where a group box's top edge runs, down from its top: the middle of the line its title is written on. */
private val LEGEND_LINE = 7.dp

/** How far below the top edge a group box's contents begin. */
private val GROUP_TOP = 18.dp

/**
 * A group box's title, set into its top edge with a little of the face behind it to break the line, the way
 * a group box's caption sat. It takes no room of its own: what follows it starts where the box's contents do.
 */
@Composable
internal fun GroupLegend(text: String, background: Color = LocalPane.current ?: MaterialTheme.colorScheme.background) {
    val skin = skin()
    val colour = if (skin == ThemeSkin.WINDOWS_XP) Luna.GroupTitle else Classic.Text
    Text(
        text,
        color = colour,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                // From where the contents begin back up to where the edge runs, less half the title's height.
                val lift = (GROUP_TOP - LEGEND_LINE).roundToPx() + placeable.height / 2
                layout(placeable.width, (placeable.height - lift).coerceAtLeast(0)) {
                    placeable.place(-4.dp.roundToPx(), -lift)
                }
            }
            .background(background)
            .padding(horizontal = 3.dp),
    )
}

/**
 * The white pane a folder's contents sat in, set into the window's face; under XP flat, edged on its left where
 * it met the task pane.
 */
@Composable
internal fun ViewPane(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val skin = skin()
    if (skin == ThemeSkin.WINDOWS_XP) {
        CompositionLocalProvider(LocalPane provides Luna.Window) {
            Box(
                modifier
                    .fillMaxSize()
                    .background(Luna.Window)
                    .drawBehind { drawRect(Luna.FieldEdge.copy(alpha = .55f), Offset.Zero, Size(pixel, size.height)) },
                content = content,
            )
        }
        return
    }
    CompositionLocalProvider(LocalPane provides Classic.Window) {
        Box(
            modifier
                .fillMaxSize()
                .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
                .drawWithContent {
                    drawRect(Classic.Window)
                    drawContent()
                    bevel(Bevel.Sunken)
                }
                .padding(2.dp),
            content = content,
        )
    }
}

/**
 * The colour behind whatever is drawn below, where that is not the window's face: the white of a
 * [ViewPane], which a group box's title breaks the box's edge with.
 */
internal val LocalPane = staticCompositionLocalOf<Color?> { null }

/**
 * A strip of tabs over a page, the chosen one standing taller and joined to the page beneath it.
 *
 * 98's tabs were raised slabs with their top corners clipped; Luna's were flat beige with the chosen one white
 * and an orange line along its top.
 */
@Composable
internal fun <T> TabStrip(tabs: List<T>, selected: T, name: (T) -> String, choose: (T) -> Unit, modifier: Modifier = Modifier) {
    val skin = skin()
    Row(modifier.height(22.dp), verticalAlignment = Alignment.Bottom) {
        Spacer(Modifier.width(2.dp))
        tabs.forEach { tab ->
            val chosen = tab == selected
            val source = remember(tab) { MutableInteractionSource() }
            val hovered by source.collectIsHoveredAsState()
            Box(
                Modifier
                    .height(if (chosen) 22.dp else 20.dp)
                    .layout { measurable, constraints ->
                        // The chosen tab reaches two pixels further each way, over its neighbours' edges.
                        val spread = if (chosen) 2.dp.roundToPx() else 0
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth))
                        layout((placeable.width - spread * 2).coerceAtLeast(0), placeable.height) { placeable.place(-spread, 0) }
                    }
                    .hoverable(source)
                    .clickable(source, indication = null, role = Role.Tab) { choose(tab) }
                    .drawBehind { if (skin == ThemeSkin.WINDOWS_XP) drawLunaTab(chosen, hovered) else drawClassicTab(chosen) }
                    .padding(horizontal = if (chosen) 10.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(name(tab), fontSize = 11.sp, color = if (skin == ThemeSkin.WINDOWS_XP) Luna.Text else Classic.Text, maxLines = 1)
            }
        }
    }
}

private fun DrawScope.drawClassicTab(chosen: Boolean) {
    val p = pixel
    val w = size.width
    val h = size.height
    drawRect(Classic.Face)
    // Left and top in the light, the right in shadow; the top corners clipped by a pixel, and no bottom edge on
    // the chosen one, which runs into the page.
    drawRect(Classic.Highlight, Offset(0f, p * 2), Size(p, h - p * 2))
    drawRect(Classic.Highlight, Offset(p, p), Size(p, p))
    drawRect(Classic.Highlight, Offset(p * 2, 0f), Size(w - p * 4, p))
    drawRect(Classic.Light, Offset(p, p * 2), Size(p, h - p * 2))
    drawRect(Classic.Light, Offset(p * 2, p), Size(w - p * 4, p))
    drawRect(Classic.DarkShadow, Offset(w - p, p * 2), Size(p, h - p * 2))
    drawRect(Classic.DarkShadow, Offset(w - p * 2, p), Size(p, p))
    drawRect(Classic.Shadow, Offset(w - p * 2, p * 2), Size(p, h - p * 2))
    if (!chosen) drawRect(Classic.Highlight, Offset(0f, h - p), Size(w, p))
}

private fun DrawScope.drawLunaTab(chosen: Boolean, hovered: Boolean) {
    val p = pixel
    val radius = CornerRadius(3.dp.toPx())
    val outline = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height + radius.x, topLeftCornerRadius = radius, topRightCornerRadius = radius))
    }
    val fill = if (chosen) Brush.verticalGradient(listOf(Luna.Window, Luna.Window)) else Brush.verticalGradient(listOf(Luna.Window, Luna.Face.towards(Luna.Window, .35f)))
    drawPath(outline, fill)
    drawPath(outline, Luna.TabEdge, style = Stroke(p))
    if (chosen || hovered) {
        drawRoundRect(Luna.TabChosen, Offset(0f, 0f), Size(size.width, p * 3), CornerRadius(p * 2))
    }
    if (!chosen) drawRect(Luna.TabEdge, Offset(0f, size.height - p), Size(size.width, p))
}

/**
 * The page under a [TabStrip]: 98's raised panel with the chosen tab joined to its top edge, Luna's white page
 * edged in the tabs' grey.
 */
@Composable
internal fun TabPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val skin = skin()
    Column(
        modifier.drawBehind {
            if (skin == ThemeSkin.WINDOWS_XP) {
                drawRect(Luna.Window)
                drawRect(Luna.TabEdge, Offset(pixel / 2, pixel / 2), Size(size.width - pixel, size.height - pixel), style = Stroke(pixel))
            } else {
                drawRect(Classic.Face)
                edge(Classic.Highlight, Classic.DarkShadow)
                edge(Classic.Light, Classic.Shadow, pixel, pixel, size.width - pixel, size.height - pixel)
            }
        },
        content = content,
    )
}

/** A tooltip: the pale yellow box with a thin black edge, and its words in the small type. */
@Composable
internal fun TooltipBox(text: String) {
    val skin = skin()
    Box(
        Modifier
            .background(if (skin == ThemeSkin.WINDOWS_XP) Luna.Tooltip else Classic.Tooltip)
            .drawBehind { edge(Classic.Text, Classic.Text) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Text(text, fontSize = 11.sp, color = Classic.Text)
    }
}

/**
 * The line under a heading on a folder's page in 98's web view: the title bar's colours running out into the
 * white. Two pixels deep.
 */
@Composable
internal fun HeadingRule(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawRect(
            Brush.horizontalGradient(0f to Classic.Title, .45f to Classic.TitleEnd, 1f to Classic.Window),
            size = Size(size.width, (2 * pixel).coerceAtMost(size.height)),
        )
    }
}

/** A rule for the status of a pane or a toolbar's end: the etched line between two sets of things. */
@Composable
internal fun EtchedSeparator(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val skin = skin()
    Canvas(if (vertical) modifier.width(2.dp) else modifier.fillMaxWidth().height(2.dp)) {
        if (skin == ThemeSkin.WINDOWS_XP) {
            if (vertical) drawRect(Luna.GroupEdge, Offset.Zero, Size(pixel, size.height)) else drawRect(Luna.GroupEdge, Offset.Zero, Size(size.width, pixel))
        } else if (vertical) {
            etchedUpright(0f, 0f, size.height)
        } else {
            etchedLine(0f, 0f, size.width)
        }
    }
}

/** The face of whatever the skin puts things on: 98's grey, Luna's beige. */
internal val ThemeSkin.face: Color get() = if (this == ThemeSkin.WINDOWS_XP) Luna.Face else Classic.Face

/** The colour of a selection, for highlighting a row or an item. */
internal val ThemeSkin.selection: Color get() = if (this == ThemeSkin.WINDOWS_XP) Luna.Selection else Classic.Selection
