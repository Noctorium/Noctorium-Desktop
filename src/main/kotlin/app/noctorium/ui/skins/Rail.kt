package app.noctorium.ui.skins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.Destination
import app.noctorium.domain.Track
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.RemoteArtwork
import app.noctorium.ui.sidebarIcon
import app.noctorium.ui.sidebarLabel
import app.noctorium.ui.sidebarShows

/**
 * The sidebar under a Windows skin, in place of the rail of rounded items.
 *
 * Under 98 it is a toolbar stood on its end, the way Outlook Express kept its folders and Internet Explorer its
 * buttons: a picture over a small label, flat until pointed at, raised a pixel then, and pressed in with the
 * checkerboard behind it for the page that is open. Under XP it is Explorer's task pane: a blue column holding
 * pale panels, each under a header, of links that underline when pointed at -- and, last, the Details panel,
 * which is what is playing.
 */
@Composable
internal fun SkinRail(selected: Destination, hidden: Set<Destination>, playing: Track?, navigate: (Destination) -> Unit) {
    if (skin() == ThemeSkin.WINDOWS_XP) TaskPane(selected, hidden, playing, navigate) else ToolbarRail(selected, hidden, navigate)
}

// --- 98: the toolbar ---

@Composable
private fun ToolbarRail(selected: Destination, hidden: Set<Destination>, navigate: (Destination) -> Unit) {
    @Composable
    fun item(destination: Destination) {
        if (sidebarShows(destination, hidden)) ToolbarItem(destination, selected == destination) { navigate(destination) }
    }
    Column(
        Modifier
            .width(86.dp)
            .fillMaxHeight()
            .background(Classic.Face)
            .padding(start = 4.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Gripper()
        Spacer(Modifier.height(4.dp))
        Throbber()
        Spacer(Modifier.height(6.dp))
        item(Destination.HOME)
        item(Destination.SEARCH)
        item(Destination.LINK)
        item(Destination.LIBRARY)
        item(Destination.DOWNLOADS)
        if (sidebarShows(Destination.NOW_PLAYING, hidden) || sidebarShows(Destination.QUEUE, hidden)) {
            EtchedSeparator(Modifier.padding(vertical = 5.dp))
            item(Destination.NOW_PLAYING)
            item(Destination.QUEUE)
        }
        Spacer(Modifier.weight(1f))
        EtchedSeparator(Modifier.padding(vertical = 5.dp))
        item(Destination.SETTINGS)
    }
}

/** The two raised ridges at the head of a toolbar band, which it was dragged by. */
@Composable
private fun Gripper() {
    Canvas(Modifier.fillMaxWidth().height(7.dp)) {
        val p = pixel
        listOf(0f, p * 4).forEach { y ->
            thinRaised(p * 2, y, size.width - p * 2, y + p * 3)
        }
    }
}

/**
 * The square at the end of Internet Explorer's toolbar where its logo turned while a page loaded -- here
 * Noctorium's mark, on the black of the darkest shadow, set into the face.
 */
@Composable
private fun Throbber() {
    Box(
        Modifier
            .size(48.dp, 40.dp)
            .drawBehind {
                drawRect(Classic.DarkShadow)
                thinSunken()
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource("noctorium-mark.png"), "Noctorium", Modifier.size(32.dp), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun ToolbarItem(destination: Destination, chosen: Boolean, action: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val down = pressed || chosen
    Column(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Tab, onClick = action)
            .drawBehind {
                when {
                    down -> {
                        if (chosen && !pressed) checkerboard(topLeft = Offset(pixel, pixel), size = Size(size.width - pixel * 2, size.height - pixel * 2))
                        thinSunken()
                    }
                    hovered -> thinRaised()
                }
            }
            .offset(if (down) 1.dp else 0.dp, if (down) 1.dp else 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(destination.sidebarIcon(), null, Modifier.size(24.dp), tint = if (chosen || hovered) Classic.Selection else Classic.Text)
        Spacer(Modifier.height(2.dp))
        Text(destination.sidebarLabel(), fontSize = 11.sp, color = Classic.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// --- XP: the task pane ---

@Composable
private fun TaskPane(selected: Destination, hidden: Set<Destination>, playing: Track?, navigate: (Destination) -> Unit) {
    @Composable
    fun ColumnScope.link(destination: Destination) {
        if (sidebarShows(destination, hidden)) TaskLink(destination, selected == destination) { navigate(destination) }
    }
    Column(
        Modifier
            .width(206.dp)
            .fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Luna.TaskPaneTop, Luna.TaskPaneFoot)))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TaskPanel("Noctorium", special = true) {
            link(Destination.HOME)
            link(Destination.SEARCH)
            link(Destination.LINK)
            link(Destination.LIBRARY)
            link(Destination.DOWNLOADS)
        }
        if (sidebarShows(Destination.NOW_PLAYING, hidden) || sidebarShows(Destination.QUEUE, hidden)) {
            TaskPanel("Playing") {
                link(Destination.NOW_PLAYING)
                link(Destination.QUEUE)
            }
        }
        TaskPanel("Other places") { link(Destination.SETTINGS) }
        TaskPanel("Details") {
            if (playing == null) {
                Text("Nothing is playing.", fontSize = 11.sp, color = Luna.Text)
            } else {
                RemoteArtwork(playing.artworkUrl, playing.provider, Modifier.size(64.dp).background(Luna.Window).padding(1.dp))
                Spacer(Modifier.height(6.dp))
                Text(playing.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Luna.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(playing.artistLine, fontSize = 11.sp, color = Luna.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                playing.album?.title?.takeIf { it.isNotBlank() }?.let { album ->
                    Text(album, fontSize = 11.sp, color = Luna.Text.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/**
 * One of the task pane's panels: a header with its title and the round button that folded it away, over a
 * pale body. The first panel of a folder was its special one, with a deep blue header and white writing.
 */
@Composable
private fun TaskPanel(title: String, special: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(25.dp)
                .drawBehind {
                    val radius = CornerRadius(4.dp.toPx())
                    val header = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, topLeftCornerRadius = radius, topRightCornerRadius = radius))
                    }
                    drawPath(
                        header,
                        if (special) {
                            Brush.horizontalGradient(listOf(Luna.TaskPanelTitle.towards(Luna.Frame, .45f), Luna.TaskPanelTitle))
                        } else {
                            Brush.horizontalGradient(listOf(Luna.Window, Luna.TaskPanel.towards(Luna.TaskPanelTitle, .12f)))
                        },
                    )
                }
                .padding(start = 11.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (special) Luna.Window else Luna.TaskPanelTitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            // The chevron button that rolled the panel up. Drawn, not offered: the panels here stay open.
            Box(
                Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(if (special) Luna.Window.copy(alpha = .25f) else Luna.Window)
                    .drawBehind { drawCircle(if (special) Luna.Window.copy(alpha = .6f) else Luna.FieldEdge, style = Stroke(pixel)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowUp, null, Modifier.size(12.dp), tint = if (special) Luna.Window else Luna.TaskPanelTitle)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (special) Luna.TaskPanel.towards(Luna.Window, .45f) else Luna.TaskPanel)
                .drawBehind {
                    drawRect(Luna.Window, Offset.Zero, Size(pixel, size.height))
                    drawRect(Luna.Window, Offset(size.width - pixel, 0f), Size(pixel, size.height))
                    drawRect(Luna.Window, Offset(0f, size.height - pixel), Size(size.width, pixel))
                }
                .padding(start = 11.dp, end = 8.dp, top = 9.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun TaskLink(destination: Destination, chosen: Boolean, action: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(if (chosen) Luna.Selection.towards(Luna.Window, .78f) else Color.Transparent)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Tab, onClick = action)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(destination.sidebarIcon(), null, Modifier.size(16.dp), tint = Luna.TaskPanelTitle)
        Spacer(Modifier.width(6.dp))
        Text(
            destination.sidebarLabel(),
            fontSize = 11.sp,
            color = if (hovered) Luna.TaskPanelTitle.towards(Luna.TitleTop, .5f) else if (chosen) Luna.Text else Luna.TaskPanelTitle,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (hovered) TextDecoration.Underline else null,
            textAlign = TextAlign.Start,
            maxLines = 1,
        )
    }
}
