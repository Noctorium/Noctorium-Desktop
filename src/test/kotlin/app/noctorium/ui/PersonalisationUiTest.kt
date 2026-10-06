package app.noctorium.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import app.noctorium.core.AppUiState
import app.noctorium.core.Destination
import app.noctorium.core.ProviderFilter
import app.noctorium.domain.Artist
import app.noctorium.domain.HomeSection
import app.noctorium.domain.ProviderType
import app.noctorium.domain.Track
import app.noctorium.settings.AccentPreset
import app.noctorium.settings.FontChoice
import app.noctorium.settings.HomePart
import app.noctorium.settings.PlayerBarStyle
import app.noctorium.settings.PlayerButton
import app.noctorium.settings.parseHexColour
import app.noctorium.settings.toHexColour
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalisationUiTest {

    // --- The player bar ---

    @Test
    fun `with nothing hidden every layout draws what it always drew`() {
        val all = PlayerButton.entries.toSet()
        assertEquals(all, playerBarButtons(PlayerBarStyle.INLINE, narrow = false, emptySet()))
        assertEquals(all, playerBarButtons(PlayerBarStyle.INLINE, narrow = true, emptySet()))
        // Stacked never had a lyrics button, and its compact form leaves the volume behind the window edge.
        assertEquals(all - PlayerButton.LYRICS, playerBarButtons(PlayerBarStyle.STACKED, narrow = false, emptySet()))
        assertEquals(all - PlayerButton.LYRICS - PlayerButton.VOLUME, playerBarButtons(PlayerBarStyle.STACKED, narrow = true, emptySet()))
        listOf(PlayerBarStyle.CENTERED, PlayerBarStyle.SLIM, PlayerBarStyle.SLIM_LEFT, PlayerBarStyle.SPOTLIGHT).forEach { style ->
            assertEquals(all, playerBarButtons(style, narrow = false, emptySet()), "$style, wide")
            assertEquals(all - PlayerButton.LYRICS, playerBarButtons(style, narrow = true, emptySet()), "$style, narrow")
        }
    }

    @Test
    fun `Floating and Display give things up in turn as they narrow`() {
        val all = PlayerButton.entries.toSet()
        listOf(PlayerBarStyle.FLOATING, PlayerBarStyle.DISPLAY).forEach { style ->
            assertEquals(all, playerBarButtons(style, narrow = false, emptySet()), "$style, wide")
            assertEquals(all - PlayerButton.LYRICS, playerBarButtons(style, narrow = true, emptySet()), "$style, narrow")
            assertEquals(
                setOf(PlayerButton.LIKE, PlayerButton.QUEUE, PlayerButton.VOLUME, PlayerButton.DEVICES),
                playerBarButtons(style, narrow = true, emptySet(), tight = true),
                "$style, tight",
            )
            // A timer that is running stays, however tight.
            assertTrue(PlayerButton.SLEEP_TIMER in playerBarButtons(style, narrow = true, emptySet(), setOf(PlayerButton.SLEEP_TIMER), tight = true))
        }
    }

    @Test
    fun `the Island opens to the volume, and shows what is under way`() {
        assertEquals(setOf(PlayerButton.VOLUME), playerBarButtons(PlayerBarStyle.ISLAND, narrow = false, emptySet()))
        assertEquals(setOf(PlayerButton.VOLUME), playerBarButtons(PlayerBarStyle.ISLAND, narrow = true, emptySet()))
        assertTrue(playerBarButtons(PlayerBarStyle.ISLAND, narrow = false, setOf(PlayerButton.VOLUME)).isEmpty())
        assertEquals(
            setOf(PlayerButton.VOLUME, PlayerButton.SLEEP_TIMER),
            playerBarButtons(PlayerBarStyle.ISLAND, narrow = false, emptySet(), setOf(PlayerButton.SLEEP_TIMER)),
        )
    }

    @Test
    fun `only Floating and the Island float over the page`() {
        assertEquals(setOf(PlayerBarStyle.FLOATING, PlayerBarStyle.ISLAND), PlayerBarStyle.entries.filter { it.floats }.toSet())
    }

    @Test
    fun `a lifted bar is as round as the corner setting, and never past a pill`() {
        assertEquals(0f, liftedCorner(app.noctorium.settings.CornerStyle.SHARP, FLOATING_HEIGHT).value)
        assertEquals(28f, liftedCorner(app.noctorium.settings.CornerStyle.SOFT, FLOATING_HEIGHT).value)
        assertEquals(FLOATING_HEIGHT.value / 2, liftedCorner(app.noctorium.settings.CornerStyle.ROUND, FLOATING_HEIGHT).value)
    }

    @Test
    fun `a hidden button is drawn by no layout at any width`() {
        PlayerButton.entries.forEach { button ->
            PlayerBarStyle.entries.forEach { style ->
                listOf(false, true).forEach { narrow ->
                    val shown = playerBarButtons(style, narrow, setOf(button))
                    assertFalse(button in shown, "$button is still on $style (narrow: $narrow)")
                }
            }
        }
    }

    @Test
    fun `hiding some buttons leaves the others where they were`() {
        val hidden = setOf(PlayerButton.SHUFFLE, PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES)
        assertEquals(
            setOf(PlayerButton.REPEAT, PlayerButton.LIKE, PlayerButton.LYRICS, PlayerButton.QUEUE, PlayerButton.VOLUME),
            playerBarButtons(PlayerBarStyle.INLINE, narrow = false, hidden),
        )
        assertTrue(playerBarButtons(PlayerBarStyle.SLIM, narrow = false, PlayerButton.entries.toSet()).isEmpty())
    }

    @Test
    fun `a hidden sleep timer or Connect still shows while it is in use`() {
        val hidden = setOf(PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES, PlayerButton.SHUFFLE)
        PlayerBarStyle.entries.forEach { style ->
            val idle = playerBarButtons(style, narrow = false, hidden)
            assertFalse(PlayerButton.SLEEP_TIMER in idle || PlayerButton.DEVICES in idle, "$style draws them idle")
            val busy = playerBarButtons(style, narrow = false, hidden, inUse = setOf(PlayerButton.SLEEP_TIMER, PlayerButton.DEVICES))
            assertTrue(PlayerButton.SLEEP_TIMER in busy && PlayerButton.DEVICES in busy, "$style hides them in use")
            assertFalse(PlayerButton.SHUFFLE in busy, "in use brings back only what is in use")
        }
        // In use does not add a button the layout never had.
        assertEquals(playerBarButtons(PlayerBarStyle.STACKED, narrow = true, emptySet()), playerBarButtons(PlayerBarStyle.STACKED, narrow = true, emptySet(), PlayerButton.entries.toSet()))
    }

    // --- Home ---

    private fun track(id: String, provider: ProviderType) = Track(
        provider = provider,
        id = id,
        title = "Song $id",
        artists = listOf(Artist("a-$id", "Artist $id", provider)),
        durationMs = 200_000,
        sourceUrl = "https://example.invalid/$id",
    )

    private val youTube = HomeSection("yt", "Quick picks", ProviderType.YOUTUBE_MUSIC, tracks = listOf(track("1", ProviderType.YOUTUBE_MUSIC)))
    private val video = HomeSection("ytv", "Videos", ProviderType.YOUTUBE_VIDEO, tracks = listOf(track("2", ProviderType.YOUTUBE_VIDEO)))
    private val soundCloud = HomeSection("sc", "Your stream", ProviderType.SOUNDCLOUD, tracks = listOf(track("3", ProviderType.SOUNDCLOUD)))

    private val ui = AppUiState(
        recentTracks = listOf(track("r1", ProviderType.YOUTUBE_MUSIC), track("r2", ProviderType.SOUNDCLOUD)),
        pinnedTracks = listOf(track("p1", ProviderType.SOUNDCLOUD)),
        homeSections = listOf(youTube, video, soundCloud),
        homeLoading = false,
    )

    @Test
    fun `Home with nothing put away is Home as it was`() {
        val home = homeContent(ui, emptySet())
        assertTrue(home.greeting)
        assertEquals(ui.pinnedTracks, home.pinned)
        assertEquals(ui.recentTracks, home.recent)
        assertEquals(listOf(youTube, video, soundCloud), home.sections)
        assertFalse(home.nothingToShow)
    }

    @Test
    fun `each part of Home can be put away by itself`() {
        assertFalse(homeContent(ui, setOf(HomePart.GREETING)).greeting)
        assertTrue(homeContent(ui, setOf(HomePart.PINNED)).pinned.isEmpty())
        assertTrue(homeContent(ui, setOf(HomePart.RECENT)).recent.isEmpty())
        // YouTube Music's part takes the YouTube video rows with it; the core helper decides that.
        assertEquals(listOf(soundCloud), homeContent(ui, setOf(HomePart.YOUTUBE_MUSIC)).sections)
        assertEquals(listOf(youTube, video), homeContent(ui, setOf(HomePart.SOUNDCLOUD)).sections)
    }

    @Test
    fun `the filter at the top still applies to what is left`() {
        val home = homeContent(ui.copy(providerFilter = ProviderFilter.SOUNDCLOUD), setOf(HomePart.RECENT))
        assertEquals(listOf(soundCloud), home.sections)
        assertEquals(ui.pinnedTracks, home.pinned)
        assertTrue(home.recent.isEmpty())
    }

    @Test
    fun `placeholders stand in only for rows that could still appear`() {
        val loading = ui.copy(homeLoading = true)
        assertTrue(homeContent(loading, setOf(HomePart.SOUNDCLOUD)).loading)
        // Bandcamp's rows can still come while it is on, so the placeholders stay until it is put away too --
        // and Spotify's and VK's, now that they have rows of their own.
        assertTrue(homeContent(loading, setOf(HomePart.SOUNDCLOUD, HomePart.YOUTUBE_MUSIC)).loading)
        assertTrue(homeContent(loading, setOf(HomePart.SOUNDCLOUD, HomePart.YOUTUBE_MUSIC, HomePart.BANDCAMP)).loading)
        assertFalse(homeContent(loading, SERVICE_HOME_PARTS).loading)
        assertFalse(homeContent(loading.copy(providerFilter = ProviderFilter.BANDCAMP), setOf(HomePart.BANDCAMP)).loading)
        assertFalse(homeContent(loading.copy(providerFilter = ProviderFilter.SOUNDCLOUD), setOf(HomePart.SOUNDCLOUD)).loading)
        assertFalse(homeContent(loading.copy(providerFilter = ProviderFilter.SPOTIFY), setOf(HomePart.SPOTIFY)).loading)
        assertTrue(homeContent(loading.copy(providerFilter = ProviderFilter.VK), setOf(HomePart.SPOTIFY)).loading)
    }

    @Test
    fun `Home with everything put away knows it has nothing to show`() {
        assertTrue(homeContent(ui, HomePart.entries.toSet()).nothingToShow)
        // Parts still on but with nothing in them count too, once the greeting has gone.
        val empty = AppUiState(homeLoading = false)
        assertTrue(homeContent(empty, setOf(HomePart.GREETING)).nothingToShow)
        // With the greeting there is always something on the page, as there always was.
        assertFalse(homeContent(empty, emptySet()).nothingToShow)
    }

    // --- The sidebar ---

    @Test
    fun `Home and Settings stay in the sidebar whatever is asked`() {
        val everything = Destination.entries.toSet()
        assertTrue(sidebarShows(Destination.HOME, everything))
        assertTrue(sidebarShows(Destination.SETTINGS, everything))
        HIDEABLE_DESTINATIONS.forEach { destination ->
            assertFalse(sidebarShows(destination, setOf(destination)), "$destination was not hidden")
            assertTrue(sidebarShows(destination, emptySet()), "$destination was hidden unasked")
        }
        assertEquals(Destination.entries - Destination.HOME - Destination.SETTINGS, HIDEABLE_DESTINATIONS)
    }

    // --- Announcing tracks ---

    @Test
    fun `a track is announced once, when it starts, and only while nobody is looking`() {
        val announcer = TrackAnnouncer()
        // Resolving is not playing yet.
        assertFalse(announcer.shouldAnnounce("a", playing = false, enabled = true, windowInFront = false))
        assertTrue(announcer.shouldAnnounce("a", playing = true, enabled = true, windowInFront = false))
        // Paused and carried on, or told again: still the same start.
        assertFalse(announcer.shouldAnnounce("a", playing = false, enabled = true, windowInFront = false))
        assertFalse(announcer.shouldAnnounce("a", playing = true, enabled = true, windowInFront = false))
        // The next song, while the window is in front: not announced, and not announced later either.
        assertFalse(announcer.shouldAnnounce("b", playing = true, enabled = true, windowInFront = true))
        assertFalse(announcer.shouldAnnounce("b", playing = true, enabled = true, windowInFront = false))
        // A different song again is.
        assertTrue(announcer.shouldAnnounce("a", playing = true, enabled = true, windowInFront = false))
    }

    @Test
    fun `nothing is announced while announcing is off, and switching it on mid-song stays quiet`() {
        val announcer = TrackAnnouncer()
        assertFalse(announcer.shouldAnnounce("a", playing = true, enabled = false, windowInFront = false))
        assertFalse(announcer.shouldAnnounce("a", playing = true, enabled = true, windowInFront = false))
        assertTrue(announcer.shouldAnnounce("b", playing = true, enabled = true, windowInFront = false))
        assertFalse(announcer.shouldAnnounce(null, playing = true, enabled = true, windowInFront = false))
    }

    // --- Colour ---

    @Test
    fun `colours survive the trip through hue, saturation and value`() {
        val colours = listOf("#000000", "#FFFFFF", "#808080", "#FF0000", "#00FF00", "#0000FF", "#B47CFF", "#E0243F", "#5FE3B0", "#123456", "#FEDCBA") +
            AccentPreset.entries.mapNotNull { it.argb?.toHexColour() }
        colours.forEach { hex ->
            val argb = parseHexColour(hex)!!
            assertEquals(hex, argb.toHsv().toArgb().toHexColour(), "round trip of $hex")
        }
    }

    @Test
    fun `hue, saturation and value mean what a picker means by them`() {
        val red = parseHexColour("#FF0000")!!.toHsv()
        assertEquals(0f, red.hue)
        assertEquals(1f, red.saturation)
        assertEquals(1f, red.value)
        assertEquals(120f, parseHexColour("#00FF00")!!.toHsv().hue)
        assertEquals(240f, parseHexColour("#0000FF")!!.toHsv().hue)
        assertEquals(0f, parseHexColour("#808080")!!.toHsv().saturation)
        assertEquals("#FFFFFF", Hsv(200f, 0f, 1f).toArgb().toHexColour())
        assertEquals("#000000", Hsv(200f, 1f, 0f).toArgb().toHexColour())
        // A hue of 360 is a hue of 0, and anything out of range is kept in it.
        assertEquals(Hsv(0f, 1f, 1f).toArgb(), Hsv(360f, 1f, 1f).toArgb())
        assertEquals(0xFFFFFFFFL, Hsv(10f, -1f, 2f).toArgb())
    }

    // --- The equaliser ---

    @Test
    fun `gains are labelled and snapped the way the curve shows them`() {
        assertEquals("0", formatGain(0f))
        assertEquals("0", formatGain(-0.1f))
        assertEquals("+3", formatGain(3f))
        assertEquals("+2.5", formatGain(2.4f))
        assertEquals("−12", formatGain(-12f))
        assertEquals("−1.5", formatGain(-1.5f))
        assertEquals(2.5f, snapGain(2.3f))
        assertEquals(12f, snapGain(30f))
        assertEquals(-12f, snapGain(-13f))
    }

    // --- The typeface ---

    @Test
    fun `the default typeface is Material's own scale, untouched`() {
        assertEquals(Typography(), noctoriumTypography(FontChoice.DEFAULT))
        assertEquals(FontFamily.Serif, noctoriumTypography(FontChoice.SERIF).bodyLarge.fontFamily)
        assertEquals(FontFamily.Monospace, noctoriumTypography(FontChoice.MONO).labelSmall.fontFamily)
        assertEquals(FontFamily.Monospace, noctoriumTypography(FontChoice.MONO).displayLarge.fontFamily)
    }
}
