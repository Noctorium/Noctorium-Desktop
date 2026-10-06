package app.noctorium.ui.skins

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.domain.Playlist
import app.noctorium.domain.Track
import app.noctorium.settings.ThemeSkin
import app.noctorium.ui.ProviderBadge
import app.noctorium.ui.RemoteArtwork

/*
 * Pages of music under a Windows skin: the white pane a folder's contents sat in, its heading, the rows of its
 * lists and the thumbnails of its shelves.
 */

/**
 * A page for browsing -- Home, a search, the library, the queue -- set in the white pane a folder's view was,
 * under a Windows skin. Under the standard skin it is the page alone, exactly as it was.
 */
@Composable
fun BrowsingPane(content: @Composable () -> Unit) {
    if (!skin().isWindows) {
        content()
        return
    }
    ViewPane { content() }
}

/**
 * A screen's title. Under the standard skin the same bold line every screen had. Under 98 it is a folder's
 * name as its web view set it: large and bold, with a rule beneath it running from the title bar's navy out
 * into the white. Under XP, bold Trebuchet in Luna's blue.
 */
@Composable
fun ScreenTitle(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val skin = skin()
    if (!skin.isWindows) {
        androidx.compose.material3.Text(text, modifier, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = maxLines, overflow = overflow)
        return
    }
    val size = fontSize * .72f
    val pixels = with(LocalDensity.current) { size.toPx() }
    if (skin == ThemeSkin.WINDOWS_XP) {
        androidx.compose.material3.Text(
            text,
            modifier,
            maxLines = maxLines,
            overflow = overflow,
            style = TextStyle(
                fontFamily = SkinFonts.trebuchet,
                fontWeight = FontWeight.Bold,
                fontSize = size,
                color = Luna.GroupTitle,
            ).rasterisedFor(pixels),
        )
        return
    }
    // As wide as the title, and no narrower than a short rule wants to be.
    Column(modifier.widthIn(min = 200.dp).width(IntrinsicSize.Max)) {
        androidx.compose.material3.Text(
            text,
            maxLines = maxLines,
            overflow = overflow,
            style = TextStyle(fontFamily = SkinFonts.tahoma, fontWeight = FontWeight.Bold, fontSize = size, color = Classic.Text).rasterisedFor(pixels),
        )
        Spacer(Modifier.height(3.dp))
        HeadingRule()
    }
}

/**
 * A row of a list: under the standard skin the rounded tile every list had, exactly as it was; under a Windows
 * skin a plain line of the list that is highlighted the way pointing at an item selected it in the web style
 * both desktops offered -- navy, or Luna's blue, all the way across, with its writing white. [selected] keeps it
 * highlighted, for the song that is playing.
 */
@Composable
fun ListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    selected: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val skin = skin()
    if (!skin.isWindows) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, interactionSource = interactionSource, content = content)
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val lit = selected || hovered
    Box(
        modifier
            .fillMaxWidth()
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .drawBehind { if (lit) drawRect(skin.selection) },
    ) {
        Highlighted(lit) { content() }
    }
}

/**
 * The frame round a cover: under 98 a picture set into the page with a sunken edge, under XP a thumbnail on its
 * white mount inside a thin pale edge -- blue while the pointer is on it.
 */
internal fun Modifier.coverFrame(skin: ThemeSkin, hot: Boolean = false): Modifier = if (skin == ThemeSkin.WINDOWS_XP) {
    drawWithContent {
        drawRect(Luna.Window)
        drawContent()
        val p = pixel
        drawRect(if (hot) Luna.Selection else Luna.GroupEdge, Offset(p / 2, p / 2), Size(size.width - p, size.height - p), style = Stroke(if (hot) p * 2 else p))
    }.padding(3.dp)
} else {
    drawWithContent {
        drawContent()
        bevel(Bevel.Sunken)
    }
}

/**
 * A song on a shelf, as a thumbnail in a folder's view: the cover in its frame, the title centred beneath it and
 * the artist under that, both highlighted while the pointer is on it. The menu and a small play button come up
 * over the cover with the highlight, as the card's own do.
 */
@Composable
internal fun SkinTrackCard(
    track: Track,
    artist: String?,
    width: Dp,
    showBadge: Boolean,
    alwaysShowControls: Boolean,
    play: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val skin = skin()
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Column(
        Modifier
            .width(width)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Button, onClick = play),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(width).coverFrame(skin, hovered)) {
            RemoteArtwork(track.artworkUrl, track.provider, Modifier.fillMaxSize().then(if (skin == ThemeSkin.WINDOWS_98) Modifier.padding(2.dp) else Modifier))
            if (hovered) Box(Modifier.fillMaxSize().background(skin.selection.copy(alpha = .28f)))
            if (showBadge) Box(Modifier.align(Alignment.TopStart).padding(6.dp)) { ProviderBadge(track.provider, compact = true) }
            if (hovered || alwaysShowControls) {
                Box(Modifier.align(Alignment.TopEnd).padding(3.dp)) { CardButtonGround { menu() } }
                PushButton(play, Modifier.align(Alignment.BottomEnd).padding(6.dp).size(28.dp), minWidth = 0.dp, minHeight = 0.dp, padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Icon(Icons.Default.PlayArrow, "Play", Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Caption(track.title, artist, hovered, width)
    }
}

/**
 * An album, a playlist or an artist on a shelf, the same way: an artist round, as the card draws one, and the
 * rest square in their frame.
 */
@Composable
internal fun SkinPlaylistCard(playlist: Playlist, width: Dp, artist: Boolean, owner: String?, open: () -> Unit) {
    val skin = skin()
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Column(
        Modifier
            .width(width)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Button, onClick = open),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val picture = if (artist) Modifier.size(width).clip(CircleShape) else Modifier.size(width).coverFrame(skin, hovered)
        Box(picture) {
            if (playlist.artworkUrl != null) {
                RemoteArtwork(playlist.artworkUrl, playlist.provider, Modifier.fillMaxSize().then(if (!artist && skin == ThemeSkin.WINDOWS_98) Modifier.padding(2.dp) else Modifier))
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    if (artist) Icon(Icons.Default.Person, null, Modifier.size(width / 3), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (hovered) Box(Modifier.fillMaxSize().background(skin.selection.copy(alpha = .28f)))
        }
        Spacer(Modifier.height(5.dp))
        Caption(playlist.title, owner, hovered, width)
    }
}

/** A thumbnail's caption: centred, highlighted together while [lit]. */
@Composable
private fun Caption(title: String, detail: String?, lit: Boolean, width: Dp) {
    val skin = skin()
    Highlighted(lit) {
        Column(
            Modifier
                .widthIn(max = width)
                .background(if (lit) skin.selection else Color.Transparent)
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (detail != null) {
                Text(
                    detail,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** A face for a tool button that sits over a picture, so it can be seen against any cover. */
@Composable
private fun CardButtonGround(content: @Composable BoxScope.() -> Unit) {
    val skin = skin()
    Box(
        Modifier.drawBehind {
            drawRect(skin.face)
            if (skin == ThemeSkin.WINDOWS_98) bevel(Bevel.Raised) else drawRect(Luna.ButtonEdge, style = Stroke(pixel))
        },
        content = content,
    )
}

/** The rounded corners a cover or a thumbnail was going to have, or square under a Windows skin, where pictures were. */
@Composable
fun skinShape(standard: Shape): Shape = if (skin().isWindows) androidx.compose.ui.graphics.RectangleShape else standard
