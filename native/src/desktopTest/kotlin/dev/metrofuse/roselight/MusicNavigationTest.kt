package dev.metrofuse.roselight

import dev.metrofuse.api.CatalogAlbum
import dev.metrofuse.api.HomeItem
import dev.metrofuse.api.HomeShelf
import dev.metrofuse.api.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class MusicNavigationTest {
    @Test
    fun albumQueueSuppliesMissingPlayerArtwork() {
        val first = Track("first", "First", "Artist", "Album")
        val second = first.copy(id = "second", artwork = "https://example.com/track.jpg")
        val cover = "https://example.com/album.jpg"
        val queue = listOf(first, second).withAlbumArtwork(cover)
        assertEquals(listOf(first.copy(artwork = cover), second), queue)
        assertEquals(cover, playerTrack(queue[0], first.copy(title = "Updated")).artwork)
        assertEquals("Updated", playerTrack(queue[0], first.copy(title = "Updated")).title)
        assertEquals(second, playerTrack(second, queue[0]))
    }

    @Test
    fun playerUsesPlaybackDurationAndFallsBackToMatchingMetadata() {
        val song = Track("song", "Song", "Artist", "Album")
        val measured = song.copy(duration = 198_523)
        assertEquals(198_523L, playerTrack(measured, song.copy(duration = 1)).duration)
        assertEquals(198_523L, playerTrack(song, measured).duration)
        assertEquals(0L, playerTrack(song, measured.copy(id = "previous")).duration)
    }

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
