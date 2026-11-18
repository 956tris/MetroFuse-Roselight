package dev.metrofuse.roselight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.HomeItem
import dev.metrofuse.api.HomeShelf
import dev.metrofuse.api.NativePlugin
import dev.metrofuse.api.NativeScreen
import dev.metrofuse.api.PlaybackState
import dev.metrofuse.api.Player
import dev.metrofuse.api.PluginContext
import dev.metrofuse.api.SearchResult
import dev.metrofuse.api.Track
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.api.UiContext
import dev.metrofuse.api.UiPlaybackControls
import dev.metrofuse.sdk.PluginSeekBar
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.add_queue
import dev.metrofuse.sdk.resources.albums
import dev.metrofuse.sdk.resources.artists
import dev.metrofuse.sdk.resources.back
import dev.metrofuse.sdk.resources.close_player
import dev.metrofuse.sdk.resources.collection
import dev.metrofuse.sdk.resources.empty_home
import dev.metrofuse.sdk.resources.empty_library
import dev.metrofuse.sdk.resources.empty_search
import dev.metrofuse.sdk.resources.home
import dev.metrofuse.sdk.resources.library
import dev.metrofuse.sdk.resources.load_more
import dev.metrofuse.sdk.resources.music
import dev.metrofuse.sdk.resources.next
import dev.metrofuse.sdk.resources.no_results
import dev.metrofuse.sdk.resources.now_playing
import dev.metrofuse.sdk.resources.offline
import dev.metrofuse.sdk.resources.pause
import dev.metrofuse.sdk.resources.play
import dev.metrofuse.sdk.resources.playlists
import dev.metrofuse.sdk.resources.plugins
import dev.metrofuse.sdk.resources.previous
import dev.metrofuse.sdk.resources.queue
import dev.metrofuse.sdk.resources.repeat
import dev.metrofuse.sdk.resources.retry
import dev.metrofuse.sdk.resources.search
import dev.metrofuse.sdk.resources.settings
import dev.metrofuse.sdk.resources.shuffle
import dev.metrofuse.sdk.searchUiCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.stringResource

class RoselightPlugin : NativePlugin {
    override suspend fun start(context: PluginContext) {
        val pluginContext = context
        context.registerUi(
            "ui.app",
            object : NativeScreen {
                @Composable
                override fun Content(context: UiContext) = Roselight(context, context = pluginContext)
            },
        )
    }
}

private val accent: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val panel: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val muted: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val foreground: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

@Composable
private fun MusicTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val colors =
        if (dark) {
            darkColorScheme(
                primary = Color(0xFFFF496C),
                onPrimary = Color.White,
                background = Color(0xFF111112),
                surface = Color(0xFF171719),
                surfaceVariant = Color(0xFF262629),
                onSurface = Color(0xFFF6F5F7),
                onSurfaceVariant = Color(0xFFA5A4AB),
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD41943),
                onPrimary = Color.White,
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFBFBFC),
                surfaceVariant = Color(0xFFF0EFF1),
                onSurface = Color(0xFF18181A),
                onSurfaceVariant = Color(0xFF66656C),
            )
        }
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
    }
}

internal data class CollectionInfo(
    val kind: String,
    val id: String,
    val title: String,
    val subtitle: String,
    val artwork: String,
)

internal fun HomeItem.info(): CollectionInfo? =
    when {
        track != null -> track!!.let { CollectionInfo("track", it.id, it.title, it.artist, it.artwork) }
        album != null -> album!!.let { CollectionInfo("album", it.id, it.title, it.artist, it.artwork) }
        artist != null -> artist!!.let { CollectionInfo("artist", it.id, it.name, "", it.artwork) }
        playlist != null -> playlist!!.let { CollectionInfo("playlist", it.id, it.title, it.author, it.artwork) }
        else -> null
    }

@Composable
private fun Roselight(
    ui: UiContext,
    context: PluginContext,
) {
    val environment = checkNotNull(ui.extensions as? UiArtworkEnvironment) { "Roselight requires the host UI artwork environment" }
    val playback by ui.player.state.collectAsState()
    val metadata by ui.metadata.collectAsState()
    val current = if (metadata.id == playback.track.id) metadata else playback.track
    val shelves by environment.homeShelves.collectAsState(emptyList())
    var accountName by remember { mutableStateOf("") }
    var frontendId by remember { mutableStateOf("") }
    var frontendTitle by remember { mutableStateOf("") }
    var frontendBridge by remember { mutableStateOf(false) }
    var showFrontends by remember { mutableStateOf(false) }
    val pluginStates by environment.pluginStates.collectAsState(emptyList())
    LaunchedEffect(shelves, pluginStates, frontendId) {
        try {
            val source = context.callService("ui.source/v1", buildJsonObject {})
            accountName = source["accountName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            frontendId = source["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            frontendTitle = source["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            frontendBridge =
                runCatching { context.callService("ui.frontends/v1", buildJsonObject {}) }
                    .onFailure { if (it is CancellationException) throw it }
                    .isSuccess
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            accountName = ""
        }
    }
    val scope = rememberCoroutineScope()
    val codec = remember { Json { ignoreUnknownKeys = true } }
    var page by remember { mutableStateOf("home") }
    var librarySection by remember { mutableStateOf("recent") }
    var expanded by remember { mutableStateOf(false) }
    var searchRetry by remember { mutableIntStateOf(0) }
    var librarySearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResult(emptyList())) }
    var offset by remember { mutableIntStateOf(0) }
    val library by remember(offset) { environment.library(offset, 100) }.collectAsState(emptyList())
    var collection by remember { mutableStateOf<CollectionInfo?>(null) }
    var collectionOrigin by remember { mutableStateOf("home") }
    var collectionTracks by remember { mutableStateOf(emptyList<Track>()) }
    var searching by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    val loading =
        if (page == "search") {
            searching
        } else if (page == "collection") {
            resolving
        } else {
            false
        }
    var collectionGeneration by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(page, query, searchRetry, frontendId, librarySearch) {
        if (page != "search") return@LaunchedEffect
        results = SearchResult(emptyList())
        failed = false
        if (query.isBlank()) {
            searching = false
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        try {
            results =
                if (librarySearch && frontendBridge) {
                    val response =
                        context.callService(
                            "ui.search/v1",
                            buildJsonObject {
                                put("query", query)
                                put("scope", "library")
                            },
                        )
                    codec.decodeFromJsonElement<SearchResult>(response.getValue("results"))
                } else {
                    context.searchUiCatalog(query)
                }
        } catch (
            cancelled: CancellationException,
        ) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            searching = false
        }
    }

    fun navigate(target: String) {
        job?.cancel()
        searching = false
        resolving = false
        collectionGeneration++
        failed = false
        if (target == "library" && page != "library") {
            librarySection = "recent"
            offset = 0
        }
        page = target
    }

    fun open(info: CollectionInfo) {
        if (page != "collection") collectionOrigin = page
        val generation = ++collectionGeneration
        job?.cancel()
        collection = info
        collectionTracks = emptyList()
        page = "collection"
        resolving = true
        failed = false
        job =
            scope.launch {
                try {
                    val item =
                        context
                            .callService(
                                "ui.collection/v1",
                                buildJsonObject {
                                    put("kind", info.kind)
                                    put("id", info.id)
                                },
                            )["item"]
                            ?.jsonObject
                    if (generation != collectionGeneration) return@launch
                    collectionTracks = item?.get("tracks")?.let { codec.decodeFromJsonElement<List<Track>>(it) }.orEmpty()
                    failed = item == null
                    if (item != null) {
                        collection =
                            info.copy(
                                title =
                                    item["title"]?.jsonPrimitive?.contentOrNull ?: item["name"]?.jsonPrimitive?.contentOrNull ?: info.title,
                                subtitle =
                                    item["artist"]?.jsonPrimitive?.contentOrNull ?: item["author"]?.jsonPrimitive?.contentOrNull
                                        ?: info.subtitle,
                                artwork = item["artwork"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: info.artwork,
                            )
                    }
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: Exception) {
                    if (generation == collectionGeneration) failed = true
                } finally {
                    if (generation == collectionGeneration) resolving = false
                }
            }
    }

    fun openItem(item: HomeItem) {
        if (item.track != null) {
            if ("playTracks" in ui.player.capabilities) ui.player.playTracks(listOf(item.track!!))
        } else {
            item.info()?.let(::open)
        }
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    val dark = isSystemInDarkTheme()
    val stream = remember(page) { LazyListState() }
    var compactDock by remember(page) { mutableStateOf(false) }
    val pageMotion = remember { Animatable(1f) }
    LaunchedEffect(page, librarySection, frontendId) {
        pageMotion.snapTo(0f)
        pageMotion.animateTo(1f, tween(235))
    }
    LaunchedEffect(stream) {
        var previous = 0L
        snapshotFlow { stream.firstVisibleItemIndex.toLong() * 100_000 + stream.firstVisibleItemScrollOffset }.collect { position ->
            if (position != previous) compactDock = position > previous && position > 24
            previous = position
        }
    }
    MusicTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = foreground) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = environment.platform == "desktop" && maxWidth >= 720.dp
                if (showFrontends) {
                    FrontendPicker(context, { showFrontends = false }) { chosen ->
                        frontendId = chosen.selected
                        frontendTitle =
                            chosen.choices
                                .firstOrNull { it.id == chosen.selected }
                                ?.title
                                .orEmpty()
                        offset = 0
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    if (wide) {
                        DesktopSidebar(page, librarySection, query, { navigate(it) }, { section ->
                            navigate("library")
                            librarySection = section
                            offset = 0
                        }, { input ->
                            query = input
                            if (page != "search") navigate("search")
                        }, { ui.navigate("settings") })
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        if (wide) {
                            DesktopPlayer(
                                ui.player,
                                playback,
                                current,
                                environment,
                                { expanded = current.id.isNotBlank() },
                                { navigate("queue") },
                            )
                        }
                        LazyColumn(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = 0.70f + 0.30f * pageMotion.value
                                    translationY = 9.dp.toPx() * (1f - pageMotion.value)
                                },
                            state = stream,
                            contentPadding =
                                PaddingValues(
                                    start = if (wide) 36.dp else 20.dp,
                                    end = if (wide) 36.dp else 20.dp,
                                    top = if (wide) 24.dp else 18.dp,
                                    bottom =
                                        if (wide) {
                                            32.dp
                                        } else if (current.id.isBlank()) {
                                            88.dp
                                        } else {
                                            153.dp
                                        },
                                ),
                            verticalArrangement = Arrangement.spacedBy(if (wide) 28.dp else 20.dp),
                        ) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(end = if (wide) 0.dp else 38.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (page == "home") {
                                        Column(
                                            Modifier.weight(1f).clickable { showFrontends = true },
                                            verticalArrangement = Arrangement.spacedBy(1.dp),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    "Home",
                                                    fontSize = 34.sp,
                                                    lineHeight = 42.sp,
                                                    letterSpacing = (-1.1).sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Glyph("down", "Choose music source", Modifier.size(15.dp), accent)
                                            }
                                            Text(
                                                frontendTitle.ifBlank { stringResource(Res.string.offline) },
                                                color = accent,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    } else {
                                        if (!wide && page == "library" && librarySection != "recent") {
                                            IconButton(onClick = {
                                                librarySection = "recent"
                                                offset = 0
                                            }) {
                                                Glyph("back", stringResource(Res.string.back), tint = accent)
                                            }
                                        }
                                        val title =
                                            if (!wide && page == "library" &&
                                                librarySection == "recent"
                                            ) {
                                                "Library"
                                            } else {
                                                pageLabel(page, librarySection)
                                            }
                                        Text(
                                            title,
                                            Modifier.weight(1f),
                                            fontSize = if (wide) 36.sp else 34.sp,
                                            lineHeight = 42.sp,
                                            letterSpacing = (-1.1).sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    if (!wide && page == "home") {
                                        Surface(onClick = { ui.navigate("settings") }, shape = CircleShape, color = panel) {
                                            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                                if (accountName.isNotBlank()) {
                                                    Text(accountName.take(1).uppercase(), color = accent, fontWeight = FontWeight.Bold)
                                                } else {
                                                    Glyph("profile", stringResource(Res.string.settings), Modifier.size(17.dp), accent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (page == "library" && !wide && librarySection == "recent") {
                                item {
                                    MobileLibraryLinks { section ->
                                        librarySection = section
                                        offset = 0
                                    }
                                }
                                item {
                                    Text("Recently Added", fontSize = 25.sp, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            when (page) {
                                "home", "new", "radio" -> {
                                    val filtered =
                                        shelvesForPage(shelves, page).ifEmpty {
                                            if (page == "home" && frontendId.isEmpty() && library.isNotEmpty()) {
                                                listOf(HomeShelf("Recently Added", library.take(16).map { HomeItem(track = it) }))
                                            } else {
                                                emptyList()
                                            }
                                        }
                                    val featureShelf = filtered.firstOrNull()
                                    if (featureShelf != null) {
                                        item {
                                            FeaturedShelf(featureShelf, wide, environment, ::openItem)
                                        }
                                    }
                                    if (page == "radio" && "radio" in ui.player.capabilities && current.id.isNotBlank()) {
                                        item {
                                            TextButton(onClick = { (ui.player as? UiPlaybackControls)?.startTrackRadio() }) {
                                                Text("Start a station from this song")
                                            }
                                        }
                                    }
                                    if (filtered.isEmpty()) {
                                        item {
                                            Text(
                                                if (page == "radio") {
                                                    "Your source has no featured stations."
                                                } else if (page == "new") {
                                                    "Your source has no featured releases."
                                                } else {
                                                    stringResource(Res.string.empty_home)
                                                },
                                                color = muted,
                                            )
                                            TextButton(onClick = { ui.navigate("plugins") }) { Text(stringResource(Res.string.plugins)) }
                                        }
                                    }
                                    filtered.drop(1).forEach { shelf ->
                                        item {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text(
                                                    shelf.title,
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    items(shelf.items) { entry ->
                                                        entry.info()?.let { info ->
                                                            Column(
                                                                Modifier
                                                                    .width(if (wide) 174.dp else 152.dp)
                                                                    .clickable { openItem(entry) },
                                                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                                            ) {
                                                                environment.Artwork(
                                                                    info.artwork,
                                                                    info.title,
                                                                    Modifier
                                                                        .size(if (wide) 174.dp else 152.dp)
                                                                        .clip(
                                                                            if (info.kind ==
                                                                                "artist"
                                                                            ) {
                                                                                CircleShape
                                                                            } else {
                                                                                RoundedCornerShape(8.dp)
                                                                            },
                                                                        ).background(panel),
                                                                )
                                                                Text(
                                                                    info.title,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                )
                                                                if (info.subtitle.isNotBlank()) {
                                                                    Text(
                                                                        info.subtitle,
                                                                        color = muted,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    if (page == "search") {
                                        if (!wide) {
                                            item { SearchInput(query, { query = it }, Modifier.fillMaxWidth()) }
                                        }
                                        if (frontendBridge && frontendId.isNotEmpty()) {
                                            item {
                                                SearchScopes(frontendTitle.ifBlank { "Catalog" }, librarySearch) { librarySearch = it }
                                            }
                                        }
                                        if (!wide && query.isBlank() && !librarySearch && frontendId.isNotEmpty()) {
                                            item { SearchBrowse { query = it } }
                                        }
                                    }
                                    if (page == "collection") {
                                        item {
                                            collection?.let { info ->
                                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                    IconButton(
                                                        onClick = { navigate(collectionOrigin) },
                                                    ) { Glyph("back", stringResource(Res.string.back)) }
                                                    environment.Artwork(
                                                        info.artwork,
                                                        info.title,
                                                        Modifier
                                                            .size(
                                                                if (wide) 240.dp else 220.dp,
                                                            ).align(Alignment.CenterHorizontally)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(panel),
                                                    )
                                                    Text(
                                                        info.title,
                                                        style = MaterialTheme.typography.headlineLarge,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    if (info.subtitle.isNotBlank()) Text(info.subtitle, color = muted)
                                                    if (collectionTracks.isNotEmpty()) {
                                                        Row(
                                                            Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                        ) {
                                                            Text(
                                                                formatTime(collectionTracks.sumOf { it.duration }),
                                                                color = muted,
                                                                style = MaterialTheme.typography.bodySmall,
                                                            )
                                                            PlayButton(
                                                                false,
                                                                "playTracks" in ui.player.capabilities,
                                                                { ui.player.playTracks(collectionTracks) },
                                                                56,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                                    if (failed) {
                                        item {
                                            TextButton(onClick = {
                                                if (page ==
                                                    "collection"
                                                ) {
                                                    collection?.let(::open)
                                                } else {
                                                    searchRetry++
                                                }
                                            }) { Text(stringResource(Res.string.retry)) }
                                        }
                                    }
                                    if (page == "search") {
                                        items(
                                            results.albums,
                                        ) { album ->
                                            CatalogRow(
                                                environment,
                                                CollectionInfo("album", album.id, album.title, album.artist, album.artwork),
                                                ::open,
                                            )
                                        }
                                        items(
                                            results.artists,
                                        ) { artist ->
                                            CatalogRow(
                                                environment,
                                                CollectionInfo("artist", artist.id, artist.name, "", artist.artwork),
                                                ::open,
                                            )
                                        }
                                        items(
                                            results.playlists,
                                        ) { playlist ->
                                            CatalogRow(
                                                environment,
                                                CollectionInfo("playlist", playlist.id, playlist.title, playlist.author, playlist.artwork),
                                                ::open,
                                            )
                                        }
                                    }
                                    val groups = if (page == "library") libraryGroups(library, librarySection) else emptyList()
                                    if (groups.isNotEmpty()) {
                                        val columns = if (wide) 4 else 2
                                        items(groups.chunked(columns)) { row ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                row.forEach { group ->
                                                    Column(
                                                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                                                            collectionOrigin = "library"
                                                            navigate("collection")
                                                            collection = CollectionInfo("local", "", group.title, group.artist, group.cover)
                                                            collectionTracks = group.tracks
                                                        },
                                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                                    ) {
                                                        val shape =
                                                            if (librarySection ==
                                                                "artists"
                                                            ) {
                                                                CircleShape
                                                            } else {
                                                                RoundedCornerShape(8.dp)
                                                            }
                                                        environment.Artwork(
                                                            group.cover,
                                                            group.title,
                                                            Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(1f)
                                                                .clip(shape)
                                                                .background(panel),
                                                        )
                                                        Text(group.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        if (group.artist.isNotBlank()) {
                                                            Text(
                                                                group.artist,
                                                                color = muted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                            )
                                                        }
                                                    }
                                                }
                                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                    val tracks =
                                        when (page) {
                                            "queue" -> {
                                                playback.queue
                                            }

                                            "search" -> {
                                                results.tracks
                                            }

                                            "collection" -> {
                                                collectionTracks
                                            }

                                            else -> {
                                                when (librarySection) {
                                                    "artists", "albums", "recent" -> emptyList()
                                                    "songs" -> library.sortedBy { it.title.lowercase() }
                                                    else -> library
                                                }
                                            }
                                        }
                                    if (tracks.isEmpty() && groups.isEmpty() && !loading &&
                                        !failed &&
                                        !(page == "search" && !wide && query.isBlank() && !librarySearch && frontendId.isNotEmpty())
                                    ) {
                                        item {
                                            Text(
                                                stringResource(
                                                    if (page ==
                                                        "search"
                                                    ) {
                                                        if (query.isBlank()) Res.string.empty_search else Res.string.no_results
                                                    } else {
                                                        Res.string.empty_library
                                                    },
                                                ),
                                                color = muted,
                                            )
                                        }
                                    }
                                    items(tracks.indices.toList()) { index ->
                                        val track = tracks[index]
                                        Row(
                                            Modifier.fillMaxWidth().clickable(
                                                enabled =
                                                    if (page ==
                                                        "queue"
                                                    ) {
                                                        "queueIndex" in ui.player.capabilities
                                                    } else {
                                                        "playTracks" in ui.player.capabilities
                                                    },
                                            ) {
                                                if (page ==
                                                    "queue"
                                                ) {
                                                    (ui.player as? UiPlaybackControls)?.selectQueueIndex(index)
                                                } else {
                                                    ui.player.playTracks(tracks, index)
                                                }
                                            },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            environment.Artwork(
                                                track.artwork,
                                                track.title,
                                                Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(panel),
                                            )
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    track.title,
                                                    color =
                                                        if (track.id ==
                                                            playback.track.id
                                                        ) {
                                                            accent
                                                        } else {
                                                            foreground
                                                        },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    listOf(track.artist, track.album)
                                                        .filter {
                                                            it.isNotBlank()
                                                        }.joinToString(
                                                            " · ",
                                                        ),
                                                    color = muted,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            if (track.duration >
                                                0
                                            ) {
                                                Text(formatTime(track.duration), color = muted, style = MaterialTheme.typography.labelSmall)
                                            }
                                            if ("addToQueue" in
                                                ui.player.capabilities
                                            ) {
                                                IconButton(
                                                    onClick = { ui.player.addToQueue(listOf(track)) },
                                                ) { Glyph("add", stringResource(Res.string.add_queue)) }
                                            }
                                        }
                                    }
                                    if (page == "library") {
                                        item {
                                            Row {
                                                if (offset >
                                                    0
                                                ) {
                                                    TextButton(
                                                        onClick = { offset = (offset - 100).coerceAtLeast(0) },
                                                    ) { Text(stringResource(Res.string.previous)) }
                                                }
                                                if (library.size ==
                                                    100
                                                ) {
                                                    TextButton(onClick = { offset += 100 }) { Text(stringResource(Res.string.load_more)) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !wide && !expanded,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = fadeIn(tween(210)) + slideInVertically(tween(240)) { it / 3 },
                    exit = fadeOut(tween(150)) + slideOutVertically(tween(210)) { it / 4 },
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        if (current.id.isNotBlank()) {
                            MiniPlayer(ui.player, playback, current, environment, dark) { expanded = true }
                        }
                        GlassDock(page, compactDock && page != "search", dark, ::navigate)
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(240)) + slideInVertically(tween(320)) { it / 3 },
                    exit = fadeOut(tween(210)) + slideOutVertically(tween(275)) { it / 3 },
                ) {
                    MusicTheme(true) {
                        Box(Modifier.fillMaxSize().background(Color(0xFF24161D))) {
                            environment.Artwork(
                                current.artwork,
                                null,
                                Modifier.matchParentSize().blur(64.dp).alpha(0.62f),
                            )
                            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x33101218), Color(0xED151319)))))
                            Column(
                                Modifier.fillMaxSize().padding(horizontal = if (wide) 120.dp else 24.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(end = if (wide) 0.dp else 28.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = { expanded = false }) { Glyph("down", stringResource(Res.string.close_player)) }
                                    Text(
                                        stringResource(Res.string.now_playing),
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                    )
                                    IconButton(onClick = {
                                        expanded = false
                                        navigate("queue")
                                    }) { Glyph("queue", stringResource(Res.string.queue)) }
                                }
                                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    val artSize = minOf(maxWidth, maxHeight, 440.dp)
                                    Crossfade(
                                        current.artwork to current.title,
                                        modifier = Modifier.size(artSize).clip(RoundedCornerShape(12.dp)).background(panel),
                                    ) { (url, title) ->
                                        environment.Artwork(url, title, Modifier.fillMaxSize())
                                    }
                                }
                                TrackMetadata(current, Modifier.fillMaxWidth(), true)
                                PluginSeekBar(ui.player)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(formatTime(playback.position), color = muted, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        "-" + formatTime(current.duration - playback.position),
                                        color = muted,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    val controls = ui.player as? UiPlaybackControls
                                    IconButton(
                                        onClick = { controls?.toggleShuffle() },
                                        enabled =
                                            controls != null && "shuffle" in ui.player.capabilities,
                                    ) { Glyph("shuffle", stringResource(Res.string.shuffle)) }
                                    IconButton(onClick = ui.player::previous, enabled = "previous" in ui.player.capabilities) {
                                        Glyph("previous", stringResource(Res.string.previous), Modifier.size(32.dp))
                                    }
                                    IconButton(
                                        onClick = ui.player::toggle,
                                        enabled = "toggle" in ui.player.capabilities,
                                        modifier = Modifier.size(65.dp),
                                    ) {
                                        Glyph(
                                            if (playback.playing) "pause" else "play",
                                            stringResource(if (playback.playing) Res.string.pause else Res.string.play),
                                            Modifier.size(46.dp),
                                        )
                                    }
                                    IconButton(onClick = ui.player::next, enabled = "next" in ui.player.capabilities) {
                                        Glyph("next", stringResource(Res.string.next), Modifier.size(32.dp))
                                    }
                                    IconButton(onClick = {
                                        controls?.cycleRepeat()
                                    }, enabled = controls != null && "repeat" in ui.player.capabilities) {
                                        Glyph(
                                            "repeat",
                                            stringResource(Res.string.repeat),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchInput(
    query: String,
    search: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier
            .height(if (compact) 34.dp else 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(panel)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Glyph("search", stringResource(Res.string.search), Modifier.size(if (compact) 15.dp else 18.dp), muted)
        BasicTextField(
            query,
            search,
            Modifier.weight(
                1f,
            ),
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                    color = foreground,
                    fontSize = if (compact) 13.sp else 16.sp,
                ),
            cursorBrush = SolidColor(accent),
            decorationBox = { input ->
                Box {
                    if (query.isEmpty()) Text(stringResource(Res.string.search), color = muted, fontSize = if (compact) 13.sp else 16.sp)
                    input()
                }
            },
        )
    }
}

@Composable
private fun CatalogRow(
    environment: UiArtworkEnvironment,
    info: CollectionInfo,
    open: (CollectionInfo) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable {
            open(info)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        environment.Artwork(
            info.artwork,
            info.title,
            Modifier
                .size(56.dp)
                .clip(
                    if (info.kind ==
                        "artist"
                    ) {
                        CircleShape
                    } else {
                        RoundedCornerShape(8.dp)
                    },
                ).background(panel),
        )
        Column {
            Text(info.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (info.subtitle.isNotBlank()) Text(info.subtitle, color = muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun TrackMetadata(
    track: Track,
    modifier: Modifier,
    large: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            track.title,
            style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track.artist,
            color = muted,
            style = if (large) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlayButton(
    playing: Boolean,
    enabled: Boolean,
    click: () -> Unit,
    size: Int,
    color: Color = accent,
) {
    Surface(onClick = click, enabled = enabled, shape = RoundedCornerShape(14.dp), color = color, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Glyph(
                if (playing) "pause" else "play",
                stringResource(if (playing) Res.string.pause else Res.string.play),
                Modifier.size((size / 2).dp),
                Color.Black,
            )
        }
    }
}

internal fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

@Composable
internal fun Glyph(
    name: String,
    description: String,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val path =
        when (name) {
            "new" -> {
                "M12 2 14.7 8.2 21 9 16.5 13.7 17.8 20 12 16.8 6.2 20 7.5 13.7 3 9 9.3 8.2Z"
            }

            "radio" -> {
                "M12 10a2 2 0 1 0 0 4 2 2 0 0 0 0-4ZM6.3 6.3a8 8 0 0 0 0 11.4l1.4-1.4a6 6 0 0 1 0-8.6Z" +
                    "M17.7 6.3l-1.4 1.4a6 6 0 0 1 0 8.6l1.4 1.4a8 8 0 0 0 0-11.4Z"
            }

            "note" -> {
                "M9 5v11a3.5 3.5 0 1 1-2-3.2V4l13-3v12a3.5 3.5 0 1 1-2-3.2V4Z"
            }

            "home" -> {
                "M12 3 2 11h3v10h5v-6h4v6h5V11h3Z"
            }

            "search" -> {
                "M10 2a8 8 0 1 0 4.9 14.3L21 22l1-1-5.7-6.1A8 8 0 0 0 10 2Zm0 2a6 6 0 1 1 0 12 6 6 0 0 1 0-12Z"
            }

            "library" -> {
                "M3 3h2v18H3Zm5 0h2v18H8Zm5 1 2-.5 5 17-2 .5Z"
            }

            "play" -> {
                "M7 3v18l15-9Z"
            }

            "pause" -> {
                "M6 4h4v16H6Zm8 0h4v16h-4Z"
            }

            "next" -> {
                "M5 4v16l12-8ZM18 4h3v16h-3Z"
            }

            "previous" -> {
                "M19 4v16L7 12ZM3 4h3v16H3Z"
            }

            "profile" -> {
                "M12 2a5 5 0 1 0 0 10 5 5 0 0 0 0-10ZM3 22v-3c0-7 18-7 18 0v3Z"
            }

            "check" -> {
                "M3 12l2-2 5 5L19 6l2 2-11 11Z"
            }

            "forward" -> {
                "M9 4l-1.7 1.7 6.3 6.3-6.3 6.3L9 20l8-8Z"
            }

            "lyrics" -> {
                "M4 4h16v12h-8l-5 4v-4H4Zm3 4v2h10V8Zm0 4v2h7v-2Z"
            }

            "down" -> {
                "m3 8 9 9 9-9-2-2-7 7-7-7Z"
            }

            "close" -> {
                "m5 4 7 7 7-7 1 1-7 7 7 7-1 1-7-7-7 7-1-1 7-7-7-7Z"
            }

            "back" -> {
                "m12 3 2 2-6 6h14v2H8l6 6-2 2-9-9Z"
            }

            "queue" -> {
                "M3 4h18v2H3Zm0 6h18v2H3Zm0 6h12v2H3Zm14-2 6 4-6 4Z"
            }

            "add" -> {
                "M11 3h2v8h8v2h-8v8h-2v-8H3v-2h8Z"
            }

            "shuffle" -> {
                "M3 5h4l10 12h2v-3l5 4-5 4v-3h-3L6 7H3Zm0 12h4l3-4 2 2-4 4H3Zm10-8 3-4h3V2l5 4-5 4V7h-2l-3 4Z"
            }

            "repeat" -> {
                "M5 4h13V1l5 4-5 4V6H5v5H3V6Zm14 16H6v3l-5-4 5-4v3h13v-5h2v5Z"
            }

            "expand" -> {
                "M3 3h7v2H5v5H3Zm11 0h7v7h-2V5h-5ZM3 14h2v5h5v2H3Zm16 0h2v7h-7v-2h5Z"
            }

            "brand" -> {
                "M3 17V7h3l6 7 6-7h3v10h-3v-6l-6 7-6-7v6Z"
            }

            "settings" -> {
                "M10 2h4l.7 3 2 .9 2.8-.9 2 3.5-2.2 2v2.9l2.2 2-2 3.5-2.8-.9-2 .9-.7 3" +
                    "h-4l-.7-3-2-.9-2.8.9-2-3.5 2.2-2v-2.9l-2.2-2 2-3.5 2.8.9 2-.9Zm2 6a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z"
            }

            else -> {
                "M4 3h16v18H4Zm2 2v14h12V5Zm2 3h8v2H8Zm0 5h8v2H8Z"
            }
        }
    val vector =
        remember(name) {
            ImageVector
                .Builder(
                    name,
                    24.dp,
                    24.dp,
                    24f,
                    24f,
                ).addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.White))
                .build()
        }
    Icon(vector, description, modifier, tint = tint)
}
