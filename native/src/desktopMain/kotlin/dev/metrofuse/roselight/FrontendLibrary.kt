package dev.metrofuse.roselight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.metrofuse.api.HomeItem
import dev.metrofuse.api.LibraryPage
import dev.metrofuse.api.Player
import dev.metrofuse.api.PluginContext
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.empty_library
import dev.metrofuse.sdk.resources.load_more
import dev.metrofuse.sdk.resources.retry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource

internal class FrontendLibrary(
    private val context: PluginContext,
    private val owner: String,
    private val likedSongsOnly: Boolean = false,
) {
    var items by mutableStateOf(emptyList<HomeItem>())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var hostUpdateRequired by mutableStateOf(false)
        private set
    var continuation by mutableStateOf<String?>(null)
        private set

    suspend fun load() {
        if (loading) return
        loading = true
        failed = false
        hostUpdateRequired = false
        try {
            val response = context.callService("ui.library/v1", buildJsonObject {
                put("owner", owner)
                if (likedSongsOnly) put("likedSongsOnly", true)
                continuation?.let { put("continuation", it) }
            })
            check(response["owner"]?.jsonPrimitive?.contentOrNull == owner)
            val page = response["page"]?.let { Json.decodeFromJsonElement(LibraryPage.serializer(), it) }
            items = (items + page?.items.orEmpty()).distinctBy { it.info()?.let { info -> info.kind to info.id } }
            continuation = page?.continuation
            loaded = true
            failed = page?.complete == false && continuation == null
        } catch (error: CancellationException) {
            currentCoroutineContext().ensureActive()
            continuation = null
            failed = true
        } catch (error: IllegalArgumentException) {
            hostUpdateRequired = error.message == "Missing service ui.library/v1"
            failed = !hostUpdateRequired
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }

    suspend fun retry() {
        if (loading) return
        if (continuation == null) items = emptyList()
        load()
    }
}

internal fun CollectionInfo.appearsInLibrarySection(section: String): Boolean = when (section) {
    "albums" -> kind == "album"
    "artists" -> kind == "artist"
    "playlists" -> kind == "playlist"
    "songs" -> kind == "track"
    else -> true
}

internal fun LazyListScope.providerLibrary(
    library: FrontendLibrary,
    section: String,
    wide: Boolean,
    player: Player,
    artwork: UiArtworkEnvironment,
    open: (CollectionInfo) -> Unit,
    load: () -> Unit,
    retry: () -> Unit,
) {
    val selected = library.items.mapNotNull(HomeItem::info).filter { it.appearsInLibrarySection(section) }
    val rows = selected.filter { it.kind !in setOf("album", "playlist") }
    val collections = selected.filter { it.kind in setOf("album", "playlist") }
    val songs = if (section == "songs" || section == "recent") library.items.mapNotNull { it.track } else emptyList()
    val columns = if (wide) 4 else 2
    items(collections.chunked(columns)) { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { info ->
                AndroidAlbumTile(info.title, info.subtitle, info.artwork, artwork, Modifier.weight(1f)) {
                    open(info)
                }
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    items(rows, key = { "provider-library:${it.kind}:${it.id}" }) { info ->
        val songIndex = if (info.kind == "track") songs.indexOfFirst { it.id == info.id } else -1
        AppleCatalogRow(
            info,
            artwork,
            enabled = info.kind != "track" || (songIndex >= 0 && "playTracks" in player.capabilities),
        ) {
            if (songIndex >= 0) player.playTracks(songs, songIndex) else open(info)
        }
    }
    item {
        when {
            library.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            library.hostUpdateRequired -> Text(
                "Update MetroFuse to see this provider’s library in Roselight.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            library.failed -> TextButton(onClick = retry) { Text(stringResource(Res.string.retry)) }
            library.continuation != null -> TextButton(onClick = load) { Text(stringResource(Res.string.load_more)) }
            library.loaded && selected.isEmpty() -> Text(
                stringResource(Res.string.empty_library),
                Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
