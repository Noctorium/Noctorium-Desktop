package app.noctorium.ui

import androidx.compose.ui.input.key.Key
import app.noctorium.core.AppUiState
import app.noctorium.core.SearchKind
import app.noctorium.domain.Artist
import app.noctorium.domain.ProviderType
import app.noctorium.domain.SearchResults
import app.noctorium.domain.Track
import app.noctorium.radio.RadioCountry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Radio in the search, without drawing it: the bar's list, the Ctrl+F switch, the country picker's filter. */
class RadioSearchTest {

    private fun station(id: String) = Track(
        ProviderType.RADIO, id, "Station $id", listOf(Artist("radio-country:FR", "France", ProviderType.RADIO)),
        sourceUrl = "https://stream.example/$id",
    )

    private val song = Track(ProviderType.YOUTUBE_MUSIC, "s", "Song", emptyList(), sourceUrl = "https://music.youtube.com/watch?v=s")

    @Test
    fun `music is what the search is until radio is picked`() {
        assertEquals(SearchKind.MUSIC, AppUiState().searchKind)
    }

    @Test
    fun `with radio chosen the bar lists stations, even before anything is typed`() {
        val ui = AppUiState(
            searchKind = SearchKind.RADIO,
            searchQuery = "",
            radioStations = (1..10).map { station("$it") },
            searchResults = SearchResults(tracks = listOf(song)),
        )
        val listed = quickResults("", ui)
        assertEquals(QUICK_RESULTS, listed.size)
        assertTrue(listed.all { it.provider == ProviderType.RADIO })
        assertTrue(quickResults("jazz", ui).isEmpty(), "results for other words are not shown as these")
    }

    @Test
    fun `with music chosen the bar lists songs as it always did`() {
        val ui = AppUiState(searchQuery = "song", searchResults = SearchResults(tracks = listOf(song)), radioStations = listOf(station("1")))
        assertEquals(listOf(song), quickResults("song", ui))
        assertTrue(quickResults("", ui).isEmpty())
    }

    @Test
    fun `Ctrl+F in the bar switches, Cmd+F on a Mac`() {
        assertTrue(isKindSwitch(Key.F, ctrl = true, meta = false, mac = false))
        assertTrue(isKindSwitch(Key.F, ctrl = false, meta = true, mac = true))
        assertFalse(isKindSwitch(Key.F, ctrl = false, meta = false, mac = false), "a plain f is typing")
        assertFalse(isKindSwitch(Key.G, ctrl = true, meta = false, mac = false))
    }

    @Test
    fun `countries are found by name or code, and none means everywhere`() {
        val countries = listOf(RadioCountry("FR", "France", 2000), RadioCountry("JP", "Japan", 300), RadioCountry("ZA", "South Africa", 200))
        assertEquals(listOf("France"), filterCountries(countries, "fra").map { it.name })
        assertEquals(listOf("Japan"), filterCountries(countries, "jp").map { it.name })
        assertEquals(3, filterCountries(countries, "  ").size)
        assertEquals("Everywhere", countryName(null, countries))
        assertEquals("South Africa", countryName("ZA", countries))
    }

    @Test
    fun `the heading says where and what`() {
        val countries = listOf(RadioCountry("FR", "France", 2000))
        assertEquals("Most listened-to around the world", radioHeading(AppUiState(searchKind = SearchKind.RADIO)))
        assertEquals("Most listened-to in France", radioHeading(AppUiState(radioCountry = "FR", radioCountries = countries)))
        assertEquals("Stations for “jazz” in France", radioHeading(AppUiState(searchQuery = "jazz ", radioCountry = "FR", radioCountries = countries)))
    }
}
