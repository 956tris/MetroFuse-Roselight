package dev.metrofuse.roselight

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.HomeShelf
import dev.metrofuse.api.PlaybackState
import dev.metrofuse.api.Player
import dev.metrofuse.api.Track
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.api.UiPlaybackControls
import dev.metrofuse.sdk.PluginSeekBar
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.next
import dev.metrofuse.sdk.resources.pause
import dev.metrofuse.sdk.resources.play
import dev.metrofuse.sdk.resources.previous
import dev.metrofuse.sdk.resources.queue
import dev.metrofuse.sdk.resources.repeat
import dev.metrofuse.sdk.resources.shuffle
import org.jetbrains.compose.resources.stringResource

private data class Destination(
    val id: String,
    val title: String,
    val icon: String = id,
)

private val destinations =
    listOf(
        Destination("home", "Home"),
        Destination("new", "New"),
        Destination("radio", "Radio"),
        Destination("library", "Library"),
    )
private val libraryLinks =
    listOf(
        Destination("recent", "Recently Added", "library"),
        Destination("artists", "Artists", "profile"),
        Destination("albums", "Albums", "library"),
        Destination("songs", "Songs", "note"),
        Destination("playlists", "Playlists", "library"),
    )

internal fun pageLabel(
    page: String,
    section: String,
): String =
    when (page) {
        "library" -> libraryLinks.firstOrNull { it.id == section }?.title ?: "Library"
        "collection" -> "Collection"
        "queue" -> "Playing Next"
        "search" -> "Search"
        else -> destinations.firstOrNull { it.id == page }?.title ?: "Home"
    }

internal data class LibraryGroup(
    val title: String,
    val artist: String,
    val tracks: List<Track>,
) {
    val cover: String get() = tracks.firstOrNull { it.artwork.isNotBlank() }?.artwork.orEmpty()
}

internal fun libraryGroups(
    tracks: List<Track>,
    section: String,
): List<LibraryGroup> =
    when (section) {
        "artists" -> {
            tracks.filter { it.artist.isNotBlank() }.groupBy { it.artist }.map { (artist, songs) ->
                LibraryGroup(artist, "", songs)
            }
        }

        "recent" -> {
            tracks.groupBy { (it.album.ifBlank { it.title }) to it.artist }.map { (key, songs) ->
                LibraryGroup(key.first, key.second, songs)
            }
        }

        "albums" -> {
            tracks.filter { it.album.isNotBlank() }.groupBy { it.album to it.artist }.map { (key, songs) ->
                LibraryGroup(key.first, key.second, songs)
            }
        }

        else -> {
            emptyList()
        }
    }.let { if (section == "recent") it else it.sortedBy { group -> group.title.lowercase() } }

internal fun shelvesForPage(
    shelves: List<HomeShelf>,
    page: String,
): List<HomeShelf> =
    when (page) {
        "new" -> {
            shelves.mapNotNull { shelf ->
                val items = shelf.items.filter { it.album != null }
                shelf.copy(items = items).takeIf { items.isNotEmpty() }
            }
        }

        "radio" -> {
            shelves.filter { shelf ->
                shelf.items.isNotEmpty() &&
                    (shelf.title.contains("radio", ignoreCase = true) || shelf.title.contains("station", ignoreCase = true))
            }
        }

        else -> {
            shelves.filter { it.items.isNotEmpty() }
        }
    }

@Composable
internal fun DesktopSidebar(
    page: String,
    section: String,
    query: String,
    select: (String) -> Unit,
    selectSection: (String) -> Unit,
    search: (String) -> Unit,
    settings: () -> Unit,
    includePlaylists: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.width(242.dp).fillMaxHeight().background(colors.surfaceVariant)) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.padding(start = 8.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Glyph("note", "Roselight", Modifier.size(24.dp), colors.primary)
                Spacer(Modifier.width(6.dp))
                Text("Roselight Music", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            SearchInput(query, search, Modifier.fillMaxWidth(), compact = true)
            Spacer(Modifier.height(10.dp))
            destinations.take(3).forEach { destination -> SidebarItem(destination, page == destination.id) { select(destination.id) } }
            Text("Library", Modifier.padding(start = 10.dp, top = 20.dp, bottom = 3.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            LibraryLinks(if (page == "library") section else "", selectSection, includePlaylists)
            Spacer(Modifier.height(10.dp))
            SidebarItem(Destination("settings", "Settings", "settings"), false, settings)
        }
        Spacer(Modifier.fillMaxHeight().width(1.dp).background(colors.outlineVariant.copy(alpha = 0.45f)))
    }
}

@Composable
internal fun LibraryLinks(
    section: String,
    select: (String) -> Unit,
    includePlaylists: Boolean,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        libraryLinks.filter { includePlaylists || it.id != "playlists" }.forEach { destination ->
            SidebarItem(destination, destination.id == section) { select(destination.id) }
        }
    }
}

@Composable
private fun SidebarItem(
    destination: Destination,
    selected: Boolean,
    click: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) colors.onSurface.copy(alpha = 0.10f) else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, onClick = click)
            .height(34.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Glyph(destination.icon, "", Modifier.size(17.dp), colors.primary)
        Text(destination.title, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable
private fun GlassFrame(
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    val rim =
        if (dark) {
            listOf(Color(0x65FFFFFF), Color(0x10FFFFFF), Color(0x3AFFFFFF))
        } else {
            listOf(Color.White, Color(0x55A3A0AD), Color.White)
        }
    Box(modifier.shadow(10.dp, shape).clip(shape)) {
        content()
        Box(Modifier.matchParentSize().border(1.dp, Brush.verticalGradient(rim), shape))
    }
}

@Composable
internal fun MiniPlayer(
    player: Player,
    playback: PlaybackState,
    track: Track,
    artwork: UiArtworkEnvironment,
    dark: Boolean,
    open: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    GlassFrame(dark, Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(glassFill(dark))
                .clickable(onClick = open)
                .padding(start = 8.dp, end = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Crossfade(track.artwork to track.title, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) { (url, title) ->
                artwork.Artwork(url, title, Modifier.fillMaxSize())
            }
            TrackMetadata(track, Modifier.weight(1f))
            TransportButton(if (playback.playing) "pause" else "play", "toggle" in player.capabilities, player::toggle, colors.onSurface)
            TransportButton("next", "next" in player.capabilities, player::next, colors.onSurface)
        }
        if (track.artwork.isNotBlank()) {
            Box(Modifier.matchParentSize().alpha(0.14f)) {
                artwork.Artwork(track.artwork, null, Modifier.fillMaxSize().blur(26.dp))
            }
        }
    }
}

@Composable
internal fun GlassDock(
    page: String,
    condensed: Boolean,
    dark: Boolean,
    go: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val height by animateDpAsState(if (condensed) 47.dp else 60.dp, spring(dampingRatio = 0.85f, stiffness = 395f))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassFrame(dark, Modifier.weight(1f)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(glassFill(dark))
                    .padding(4.dp),
            ) {
                destinations.forEach { destination ->
                    val selected = destination.id == page
                    val tint by animateColorAsState(if (selected) colors.primary else colors.onSurface)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (selected) colors.onSurface.copy(alpha = 0.085f) else Color.Transparent)
                            .semantics { this.selected = selected }
                            .clickable(role = Role.Tab) { go(destination.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Glyph(destination.icon, if (condensed) destination.title else "", Modifier.size(21.dp), tint)
                        if (!condensed) {
                            Text(
                                destination.title,
                                color = tint,
                                fontSize = 10.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
        GlassFrame(dark) {
            Box(
                Modifier
                    .size(height)
                    .background(glassFill(dark))
                    .semantics { selected = page == "search" }
                    .clickable(role = Role.Tab) { go("search") },
                contentAlignment = Alignment.Center,
            ) {
                Glyph("search", "Search", Modifier.size(24.dp), if (page == "search") colors.primary else colors.onSurface)
            }
        }
    }
}

private fun glassFill(dark: Boolean): Brush =
    Brush.verticalGradient(
        if (dark) {
            listOf(Color(0xED45454B), Color(0xE9242428), Color(0xEB2C2C30))
        } else {
            listOf(Color(0xF5FFFFFF), Color(0xD7F5F4F8), Color(0xE7FFFFFF))
        },
    )

@Composable
internal fun DesktopPlayer(
    player: Player,
    playback: PlaybackState,
    track: Track,
    artwork: UiArtworkEnvironment,
    expand: () -> Unit,
    queue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val hasTrack = track.id.isNotBlank()
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(61.dp)
                .background(colors.surface)
                .padding(start = 10.dp, end = 46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val controls = player as? UiPlaybackControls
            TransportButton("shuffle", controls != null && "shuffle" in player.capabilities, {
                controls?.toggleShuffle()
            }, colors.onSurfaceVariant, compact = true)
            TransportButton("previous", "previous" in player.capabilities, player::previous, colors.onSurface, compact = true)
            TransportButton(
                if (playback.playing) "pause" else "play",
                hasTrack && "toggle" in player.capabilities,
                player::toggle,
                colors.onSurface,
                compact = true,
            )
            TransportButton("next", "next" in player.capabilities, player::next, colors.onSurface, compact = true)
            TransportButton("repeat", controls != null && "repeat" in player.capabilities, {
                controls?.cycleRepeat()
            }, colors.onSurfaceVariant, compact = true)
            Box(
                Modifier
                    .weight(1f)
                    .height(45.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant),
            ) {
                if (hasTrack) {
                    Row(Modifier.fillMaxSize().clickable(onClick = expand), verticalAlignment = Alignment.CenterVertically) {
                        artwork.Artwork(track.artwork, track.title, Modifier.size(45.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                track.title,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                track.artist,
                                color = colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth()
                            .padding(start = 46.dp)
                            .height(14.dp),
                    ) { PluginSeekBar(player) }
                } else {
                    Glyph("note", "", Modifier.align(Alignment.Center).size(20.dp), colors.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            IconButton(onClick = queue, enabled = hasTrack) { Glyph("queue", stringResource(Res.string.queue), Modifier.size(19.dp)) }
        }
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun TransportButton(
    name: String,
    enabled: Boolean,
    click: () -> Unit,
    tint: Color,
    compact: Boolean = false,
) {
    val label =
        stringResource(
            when (name) {
                "previous" -> Res.string.previous
                "next" -> Res.string.next
                "pause" -> Res.string.pause
                "shuffle" -> Res.string.shuffle
                "repeat" -> Res.string.repeat
                else -> Res.string.play
            },
        )
    IconButton(onClick = click, enabled = enabled, modifier = Modifier.size(if (compact) 27.dp else 44.dp)) {
        Glyph(name, label, Modifier.size(if (compact) 18.dp else 22.dp), if (enabled) tint else tint.copy(alpha = 0.35f))
    }
}
