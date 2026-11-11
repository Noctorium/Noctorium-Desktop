package app.noctorium.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.noctorium.core.AppState
import app.noctorium.core.AppUiState
import app.noctorium.core.SearchKind
import app.noctorium.domain.Track
import app.noctorium.radio.RadioCountry
import app.noctorium.ui.icons.Icon
import app.noctorium.ui.skins.DropdownMenu
import app.noctorium.ui.skins.DropdownMenuItem
import app.noctorium.ui.skins.ListRow
import app.noctorium.ui.skins.OutlinedTextField
import app.noctorium.ui.skins.Text
import app.noctorium.ui.skins.classicScrollbar
import app.noctorium.ui.skins.skinShape

/*
 * Radio in the search: the search box's own small menu says Music or Radio -- Music unless the listener picks the
 * other -- and with Radio chosen the same box finds stations from anywhere in the world, from Radio Browser, with a
 * country to narrow them to where the service chips would be. Ctrl+F (⌘F) pressed again in the quick search bar
 * switches between the two, so nobody who lives on the keyboard has to reach for the menu.
 */

/** The icon each kind is drawn with: a note for songs, a set for stations. */
internal fun SearchKind.icon() = if (this == SearchKind.RADIO) Icons.Default.Radio else Icons.Default.MusicNote

/** What each kind of search finds, under its name in the menu. */
internal fun SearchKind.about(): String = when (this) {
    SearchKind.MUSIC -> "Songs, artists and albums from your services"
    SearchKind.RADIO -> "Live stations from every country"
}

/**
 * The search's Music / Radio switch: the kind chosen, as a word and a caret above the box -- not in it, so the box is
 * only a search box -- opening a menu of the two. No fill and no border: it reads as a heading's quiet companion,
 * not as a button, and is lit only while the pointer is on it.
 */
@Composable
internal fun SearchKindPicker(kind: SearchKind, choose: (SearchKind) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { open = true }
                .padding(start = 4.dp, end = 0.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(kind.icon(), null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(kind.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ArrowDropDown, "Music or radio", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }) {
            SearchKind.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option.displayName, fontWeight = FontWeight.SemiBold)
                            Text(option.about(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = {
                        open = false
                        choose(option)
                    },
                    leadingIcon = { Icon(option.icon(), null) },
                    trailingIcon = { if (option == kind) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

/** "Germany", or "Everywhere" for no country. */
internal fun countryName(code: String?, countries: List<RadioCountry>): String =
    code?.let { c -> countries.firstOrNull { it.code == c }?.name ?: c } ?: "Everywhere"

/** The countries [filter] picks out, by name or by code, as the picker lists them. */
internal fun filterCountries(countries: List<RadioCountry>, filter: String): List<RadioCountry> {
    val wanted = filter.trim().lowercase()
    if (wanted.isEmpty()) return countries
    return countries.filter { it.name.lowercase().contains(wanted) || it.code.lowercase() == wanted }
}

/**
 * Where the radio search looks: everywhere, or one country, picked from all of them -- a couple of hundred, so the
 * menu has a box at its top to type a name into.
 */
@Composable
internal fun RadioCountryPicker(ui: AppUiState, choose: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f))
                .clickable { open = true; filter = "" }
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A globe, not a flag: Windows draws flag emoji as two letters, which says less than the name beside it.
            Icon(Icons.Default.Public, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(7.dp))
            Text(countryName(ui.radioCountry, ui.radioCountries), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Icon(Icons.Default.ArrowDropDown, "Country", Modifier.size(20.dp))
        }
        DropdownMenu(open, { open = false }, Modifier.width(300.dp).heightIn(max = 460.dp)) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Find a country") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp).tracksTyping(),
            )
            if (filter.isBlank()) {
                DropdownMenuItem(
                    text = { Text("Everywhere") },
                    onClick = { open = false; choose(null) },
                    leadingIcon = { Icon(Icons.Default.Public, null) },
                    trailingIcon = { if (ui.radioCountry == null) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
            val shown = filterCountries(ui.radioCountries, filter)
            if (ui.radioCountries.isEmpty()) {
                Text("Fetching the countries…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
            } else if (shown.isEmpty()) {
                Text("No country called “$filter”.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
            }
            shown.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(country.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${country.stations}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { open = false; choose(country.code) },
                    leadingIcon = { Text(country.code, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(22.dp)) },
                    trailingIcon = { if (ui.radioCountry == country.code) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

/** What the station list is headed with: the most listened-to somewhere, or the stations matching the words. */
internal fun radioHeading(ui: AppUiState): String {
    val where = countryName(ui.radioCountry, ui.radioCountries)
    return when {
        ui.searchQuery.isBlank() && ui.radioCountry == null -> "Most listened-to around the world"
        ui.searchQuery.isBlank() -> "Most listened-to in $where"
        ui.radioCountry == null -> "Stations for “${ui.searchQuery.trim()}”"
        else -> "Stations for “${ui.searchQuery.trim()}” in $where"
    }
}

/** The search page with Radio chosen: the stations found, or why there are none. */
@Composable
internal fun RadioResults(ui: AppUiState, state: AppState, playing: Track?) {
    when {
        ui.searchLoading && ui.radioStations.isEmpty() -> app.noctorium.ui.skins.LinearProgressIndicator(Modifier.fillMaxWidth())
        ui.errorMessage != null -> Text(ui.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
        ui.radioStations.isEmpty() && !ui.searchLoading -> Text(
            "No stations found. Try another name, a kind of music such as “jazz”, or Everywhere.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        else -> {
            Text(radioHeading(ui), fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Text(
                "From Radio Browser, an open list of the world's stations. Click one to tune in.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (ui.searchLoading) app.noctorium.ui.skins.LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 6.dp))
            val list = rememberLazyListState()
            LazyColumn(Modifier.classicScrollbar(list), state = list, verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = chromePadding()) {
                items(ui.radioStations, key = { it.queueKey }) { station ->
                    StationRow(station, onAir = playing?.queueKey == station.queueKey) { state.playStation(station) }
                }
            }
        }
    }
}

/** One station: its logo, its name, where it is from and what it plays, and its badge. */
@Composable
internal fun StationRow(station: Track, onAir: Boolean, play: () -> Unit) {
    ListRow(
        onClick = play,
        color = if (onAir) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .36f).solidOnGlow(),
        shape = RoundedCornerShape(12.dp),
        selected = onAir,
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteArtwork(station.artworkUrl, station.provider, Modifier.size(48.dp).clip(skinShape(RoundedCornerShape(9.dp))))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(station.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(station.artistLine, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onAir) {
                Text("ON AIR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 10.dp))
            }
            ProviderBadge(station.provider)
        }
    }
}
