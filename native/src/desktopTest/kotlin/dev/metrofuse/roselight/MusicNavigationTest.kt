package dev.metrofuse.roselight

import dev.metrofuse.api.CatalogAlbum
import dev.metrofuse.api.HomeItem
import dev.metrofuse.api.HomeShelf
import dev.metrofuse.api.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicNavigationTest {
    @Test
    fun sameTitleAlbumsFromDifferentArtistsKeepTheirOwnTracksAndArtwork() {
        val first = Track("a", "First", "Artist A", "Shared title", artwork = "file:/cover-a.jpg")
        val second = Track("b", "Second", "Artist B", "Shared title", artwork = "file:/cover-b.jpg")
        val third = Track("c", "Third", "Artist A", "Shared title")
        val groups = libraryGroups(listOf(first, second, third), "albums").associateBy { it.artist }
        assertEquals(listOf(first, third), groups.getValue("Artist A").tracks)
        assertEquals(second.artwork, groups.getValue("Artist B").cover)
    }

    @Test
    fun browsingTabsUseOnlyItemsSuppliedByTheActiveProvider() {
        val track = HomeItem(track = Track("track", "Track", "Artist", "Album"))
        val album = HomeItem(album = CatalogAlbum("album", "Album", "Artist"))
        val picks = HomeShelf("Picks", listOf(track, album))
        val radio = HomeShelf("Artist Radio", listOf(track))
        val available = listOf(picks, radio, HomeShelf("Empty", emptyList()))
        assertEquals(listOf(picks, radio), shelvesForPage(available, "home"))
        assertEquals(listOf(picks.copy(items = listOf(album))), shelvesForPage(available, "new"))
        assertEquals(listOf(radio), shelvesForPage(available, "radio"))
    }
}
