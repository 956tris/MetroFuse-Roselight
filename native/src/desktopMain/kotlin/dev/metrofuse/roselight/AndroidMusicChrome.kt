package dev.metrofuse.roselight

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.Player
import dev.metrofuse.api.Track
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.sdk.PluginAlertDialog
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.add_queue
import dev.metrofuse.sdk.resources.back
import dev.metrofuse.sdk.resources.home
import dev.metrofuse.sdk.resources.play
import dev.metrofuse.sdk.resources.search
import dev.metrofuse.sdk.resources.shuffle
import org.jetbrains.compose.resources.stringResource

internal val androidTabHeight = 60.dp
internal val androidMiniPlayerHeight = 58.dp

@Composable
internal fun AndroidNavigationTabs(
    page: String,
    condensed: Boolean,
    dark: Boolean,
    go: (String) -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val height by animateDpAsState(if (condensed) 48.dp else androidTabHeight, spring(dampingRatio = 0.85f, stiffness = 395f))
    val primaryTabs =
        listOf(
            Triple("home", stringResource(Res.string.home), PlayerControlArt.TabHome),
            Triple("new", "New", PlayerControlArt.TabNew),
            Triple("radio", "Radio", PlayerControlArt.TabRadio),
            Triple("library", "Library", PlayerControlArt.TabLibrary),
        )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassFrame(dark, cornerRadius = 30, modifier = Modifier.weight(1f)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(glassFill(dark))
                    .padding(4.dp),
            ) {
                primaryTabs.forEach { (id, label, icon) ->
                    val active = id == page
                    val tint by animateColorAsState(if (active) palette.primary else palette.onSurface)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(if (active) palette.onSurface.copy(alpha = 0.085f) else Color.Transparent)
                            .semantics { selected = active }
                            .clickable(role = Role.Tab) { go(id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        PlayerControlIcon(icon, if (condensed) label else null, tint, Modifier.size(23.dp))
                        if (!condensed) {
                            Text(
                                label,
                                color = tint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 12.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
        GlassFrame(dark, cornerRadius = 30) {
            val searchActive = page == "search"
            val searchTint by animateColorAsState(if (searchActive) palette.primary else palette.onSurface)
            Box(
                Modifier
                    .size(height)
                    .background(glassFill(dark))
                    .semantics { selected = searchActive }
                    .clickable(role = Role.Tab) { go("search") },
                contentAlignment = Alignment.Center,
            ) {
                PlayerControlIcon(PlayerControlArt.TabSearch, stringResource(Res.string.search), searchTint, Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun AndroidAlbumTile(
    title: String,
    subtitle: String,
    cover: String,
    artwork: UiArtworkEnvironment,
    modifier: Modifier,
    open: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(5.dp)
    Column(modifier.clickable(onClick = open).padding(bottom = 12.dp)) {
        artwork.Artwork(
            cover,
            null,
            Modifier.fillMaxWidth().aspectRatio(1f).clip(shape).background(palette.surfaceVariant)
                .border(0.5.dp, palette.outlineVariant.copy(alpha = 0.48f), shape),
        )
        Spacer(Modifier.height(4.dp))
        Text(title, color = palette.onSurface, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = palette.onSurfaceVariant, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AndroidCollectionHeader(
    info: CollectionInfo,
    artist: Boolean,
    tracks: List<Track>,
    player: Player,
    artwork: UiArtworkEnvironment,
    back: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val canPlay = tracks.isNotEmpty() && "playTracks" in player.capabilities
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            IconButton(onClick = back, modifier = Modifier.align(Alignment.CenterStart)) {
                Glyph("back", stringResource(Res.string.back), tint = palette.primary)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            val edge = (maxWidth - 112.dp).coerceIn(minOf(maxWidth, 120.dp), minOf(maxWidth, 320.dp))
            val shape = if (artist) CircleShape else RoundedCornerShape(10.dp)
            artwork.Artwork(
                info.artwork,
                null,
                Modifier.size(edge).clip(shape).background(palette.surfaceVariant)
                    .border(0.5.dp, palette.outlineVariant.copy(alpha = 0.48f), shape),
            )
        }
        Spacer(Modifier.height(15.dp))
        Text(
            info.title, fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        if (info.subtitle.isNotBlank()) {
            Text(info.subtitle, color = palette.primary, fontSize = 18.sp, lineHeight = 23.sp, textAlign = TextAlign.Center, maxLines = 2)
        }
        if (canPlay) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shuffle = stringResource(Res.string.shuffle)
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(palette.surfaceVariant)
                        .clickable(role = Role.Button, onClickLabel = shuffle) { player.playTracks(tracks.shuffled()) },
                    contentAlignment = Alignment.Center,
                ) { PlayerControlIcon(PlayerControlArt.Shuffle, null, palette.primary, Modifier.size(23.dp)) }
                val play = stringResource(Res.string.play)
                Row(
                    Modifier.size(160.dp, 44.dp).clip(CircleShape).background(palette.primary)
                        .clickable(role = Role.Button) { player.playTracks(tracks) },
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerControlIcon(PlayerControlArt.Play, null, palette.onPrimary, Modifier.size(17.dp))
                    Text(play, color = palette.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                if ("addToQueue" in player.capabilities) {
                    val add = stringResource(Res.string.add_queue)
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(palette.surfaceVariant)
                            .clickable(role = Role.Button) { player.addToQueue(tracks) },
                        contentAlignment = Alignment.Center,
                    ) { Glyph("add", add, Modifier.size(22.dp), palette.primary) }
                } else {
                    Spacer(Modifier.size(44.dp))
                }
            }
        }
        HorizontalDivider(color = palette.outlineVariant)
    }
}

@Composable
internal fun AndroidAlbumTrack(track: Track, number: Int, active: Boolean, player: Player, play: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    var showOptions by remember(track.id) { mutableStateOf(false) }
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(enabled = "playTracks" in player.capabilities, onClick = play),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                number.toString(), Modifier.size(32.dp, 20.dp), color = palette.onSurfaceVariant,
                fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1,
            )
            Text(
                track.title, Modifier.weight(1f).padding(vertical = 8.dp),
                color = if (active) palette.primary else palette.onSurface,
                fontSize = 16.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if ("addToQueue" in player.capabilities) {
                IconButton(onClick = { showOptions = true }, modifier = Modifier.size(48.dp)) {
                    PlayerControlIcon(PlayerControlArt.More, "More", palette.onSurfaceVariant, Modifier.size(24.dp))
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = 32.dp), color = palette.outlineVariant.copy(alpha = 0.7f))
    }
    if (showOptions) {
        PluginAlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text(track.title) },
            text = { Text(track.artist) },
            confirmButton = {
                TextButton(onClick = {
                    player.addToQueue(listOf(track))
                    showOptions = false
                }) { Text(stringResource(Res.string.add_queue)) }
            },
            dismissButton = { TextButton(onClick = { showOptions = false }) { Text("Close") } },
        )
    }
}
