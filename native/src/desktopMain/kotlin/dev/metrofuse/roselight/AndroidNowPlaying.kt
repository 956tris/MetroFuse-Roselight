package dev.metrofuse.roselight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.LyricsResult
import dev.metrofuse.api.PlaybackState
import dev.metrofuse.api.Player
import dev.metrofuse.api.PluginContext
import dev.metrofuse.api.Track
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.api.UiPlaybackControls
import dev.metrofuse.sdk.playbackFraction
import dev.metrofuse.sdk.PluginAlertDialog
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.add_queue
import dev.metrofuse.sdk.resources.close_player
import dev.metrofuse.sdk.resources.next
import dev.metrofuse.sdk.resources.pause
import dev.metrofuse.sdk.resources.play
import dev.metrofuse.sdk.resources.previous
import dev.metrofuse.sdk.resources.queue
import dev.metrofuse.sdk.resources.repeat
import dev.metrofuse.sdk.resources.shuffle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong
import kotlin.math.pow

private val sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
private val artworkShape = RoundedCornerShape(10.dp)
private val playerPrimary = Color(0xF0FFFFFF)
private val playerSecondary = Color(0x73FFFFFF)
private val playerTrack = Color(0x2EFFFFFF)
private val artworkEasing = Easing { fraction -> 1f - (1f - fraction).pow(2.4f) }
private enum class PlayerPage { Artwork, Lyrics, Queue }

internal fun playerTrack(playback: Track, metadata: Track): Track =
    if (metadata.id != playback.id) playback
    else metadata.copy(
        artwork = metadata.artwork.ifBlank { playback.artwork },
        duration = playback.duration.takeIf { it > 0 } ?: metadata.duration.coerceAtLeast(0),
    )

internal fun List<Track>.withAlbumArtwork(cover: String): List<Track> =
    if (cover.isBlank()) this else map { track ->
        if (track.artwork.isBlank()) track.copy(artwork = cover) else track
    }

@Composable
internal fun AndroidMiniPlayer(
    player: Player,
    playback: PlaybackState,
    track: Track,
    artwork: UiArtworkEnvironment,
    dark: Boolean,
    artworkModifier: Modifier,
    open: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    GlassFrame(dark, cornerRadius = 26, modifier = Modifier.fillMaxWidth()) {
        if (track.artwork.isNotBlank()) {
            Box(Modifier.matchParentSize()) {
                artwork.Artwork(track.artwork, null, Modifier.fillMaxSize().blur(26.dp))
                Box(Modifier.fillMaxSize().background(if (dark) Color(0xCC1C1C1E) else Color(0xCCF5F4F8)))
            }
        } else {
            Box(Modifier.matchParentSize().background(glassFill(dark)))
        }
        Row(
            Modifier.fillMaxWidth().height(androidMiniPlayerHeight)
                .clickable(onClick = open).padding(start = 8.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Crossfade(
                track.artwork to track.title,
                modifier = Modifier.size(44.dp).then(artworkModifier).clip(RoundedCornerShape(10.dp)),
                label = "mini player artwork",
            ) { (url, title) -> artwork.Artwork(url, title, Modifier.fillMaxSize()) }
            Column(Modifier.weight(1f).padding(start = 10.dp, end = 6.dp)) {
                Text(
                    track.title,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (track.artist.isNotBlank()) {
                    Text(
                        track.artist,
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PlayerButton(
                if (playback.playing) PlayerControlArt.MiniPause else PlayerControlArt.MiniPlay,
                stringResource(if (playback.playing) Res.string.pause else Res.string.play),
                "toggle" in player.capabilities,
                tint = colors.onSurface,
                action = player::toggle,
            )
            Spacer(Modifier.width(6.dp))
            PlayerButton(
                PlayerControlArt.MiniNext,
                stringResource(Res.string.next),
                "next" in player.capabilities,
                tint = colors.onSurface,
                action = player::next,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.playerArtworkTransition(trackId: String, visibility: AnimatedVisibilityScope): Modifier =
    Modifier.sharedElement(
        rememberSharedContentState("roselight-player-cover:$trackId"),
        animatedVisibilityScope = visibility,
        boundsTransform = BoundsTransform { _, _ -> tween(390, easing = FastOutSlowInEasing) },
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AndroidNowPlaying(
    expanded: Boolean,
    player: Player,
    playback: PlaybackState,
    track: Track,
    artwork: UiArtworkEnvironment,
    context: PluginContext,
    artworkTransition: SharedTransitionScope,
    close: () -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        modifier = Modifier.fillMaxSize(),
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 360f)) { it } + fadeIn(tween(170)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(210, delayMillis = 45)),
    ) {
        val sheet = this
        var page by remember { mutableStateOf(PlayerPage.Artwork) }
        var seeking by remember { mutableStateOf(false) }
        var drag by remember { mutableFloatStateOf(0f) }
        var dragging by remember { mutableStateOf(false) }
        val dismiss by rememberUpdatedState(close)
        val closeLabel = stringResource(Res.string.close_player)
        val dismissDistance = with(LocalDensity.current) { 96.dp.toPx() }
        val settledDrag by animateFloatAsState(drag, spring(stiffness = 470f), label = "player drag")
        LaunchedEffect(expanded) {
            if (expanded) drag = 0f
        }
        val dragToClose = Modifier.pointerInput(dismissDistance) {
            detectVerticalDragGestures(
                onDragStart = { dragging = true },
                onDragEnd = {
                    dragging = false
                    if (drag >= dismissDistance) dismiss() else drag = 0f
                },
                onDragCancel = {
                    dragging = false
                    drag = 0f
                },
            ) { change, delta ->
                change.consume()
                drag = (drag + delta).coerceAtLeast(0f)
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = if (dragging) drag else settledDrag
                clip = true
                shape = sheetShape
            }.background(Color.Black),
        ) {
            val controlsHeight = (maxHeight * 0.345f).coerceIn(
                minOf(186.dp, maxHeight * 0.62f),
                maxHeight * 0.62f,
            )
            PlayerBackdrop(track.artwork, artwork, Modifier.matchParentSize())
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = close,
                    modifier = dragToClose.height(32.dp).width(64.dp),
                ) {
                    Box(
                        Modifier.size(36.dp, 4.dp)
                            .background(playerSecondary, CircleShape)
                            .semantics { contentDescription = closeLabel },
                    )
                }
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(220)))
                    },
                    label = "artwork and queue",
                ) { visiblePage ->
                    when (visiblePage) {
                        PlayerPage.Artwork -> PlayerArtwork(
                            track,
                            artwork,
                            playback.playing,
                            seeking,
                            dragToClose,
                            artworkTransition.playerArtworkTransition(track.id, sheet),
                        )
                        PlayerPage.Lyrics, PlayerPage.Queue -> Column(Modifier.fillMaxSize()) {
                            SecondaryPlayerHeader(track, player, artwork) { page = PlayerPage.Artwork }
                            Box(Modifier.weight(1f)) {
                                if (visiblePage == PlayerPage.Lyrics) {
                                    PlayerLyrics(track, playback, player, context)
                                } else {
                                    PlayerQueue(playback, player, artwork)
                                }
                            }
                        }
                    }
                }
                if (page == PlayerPage.Artwork) Row(
                    Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = track.title to track.artist,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            fadeIn(tween(160)).togetherWith(fadeOut(tween(160)))
                        },
                        label = "track information",
                    ) { (title, artist) ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(title, Modifier.basicMarquee(iterations = 1), color = playerPrimary, fontWeight = FontWeight.Medium, fontSize = 20.sp, maxLines = 1)
                            Text(artist, Modifier.basicMarquee(iterations = 1), color = playerSecondary, fontSize = 20.sp, maxLines = 1)
                        }
                    }
                    PlayerMore(player, track)
                }
                Column(Modifier.fillMaxWidth().height(controlsHeight)) {
                    PlayerTimeline(player, playback, track, Modifier.padding(horizontal = 32.dp)) { seeking = it }
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        PlayerTransport(player, playback.playing)
                    }
                    Box(Modifier.fillMaxWidth().height(60.dp)) {
                        val lyrics = page == PlayerPage.Lyrics
                        val queue = page == PlayerPage.Queue
                        PlayerButton(
                            if (lyrics) PlayerControlArt.LyricsSelected else PlayerControlArt.Lyrics,
                            "Lyrics",
                            true,
                            modifier = Modifier.align(Alignment.Center).offset(x = (-103).dp),
                            width = 60.dp,
                            height = 60.dp,
                            selected = lyrics,
                        ) {
                            page = if (lyrics) PlayerPage.Artwork else PlayerPage.Lyrics
                        }
                        PlayerButton(
                            if (queue) PlayerControlArt.QueueSelected else PlayerControlArt.Queue,
                            stringResource(Res.string.queue),
                            true,
                            modifier = Modifier.align(Alignment.Center).offset(x = 103.dp),
                            width = 60.dp,
                            height = 60.dp,
                            selected = queue,
                        ) {
                            page = if (queue) PlayerPage.Artwork else PlayerPage.Queue
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SecondaryPlayerHeader(track: Track, player: Player, artwork: UiArtworkEnvironment, closePage: () -> Unit) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        artwork.Artwork(
            track.artwork,
            "Album artwork",
            Modifier.size(60.dp).clip(shape).background(Color(0xFF27272A))
                .border(0.5.dp, Color(0x1A000000), shape).clickable(onClick = closePage),
        )
        Column(Modifier.weight(1f).clickable(onClick = closePage).padding(start = 8.dp)) {
            Text(
                track.title, Modifier.basicMarquee(iterations = 1), color = playerPrimary, fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
            Text(track.artist, Modifier.basicMarquee(iterations = 1), color = playerSecondary, fontSize = 17.sp, maxLines = 1)
        }
        PlayerMore(player, track)
    }
}

@Composable
private fun PlayerBackdrop(url: String, artwork: UiArtworkEnvironment, modifier: Modifier) {
    val motion = rememberInfiniteTransition(label = "album color motion")
    val mainRotation = motion.animateFloat(0f, -360f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "center")
    val upperRotation = motion.animateFloat(0f, 360f, infiniteRepeatable(tween(90_000, easing = LinearEasing)), label = "upper")
    val lowerRotation = motion.animateFloat(0f, 360f, infiniteRepeatable(tween(70_000, easing = LinearEasing)), label = "lower")
    Crossfade(
        url,
        modifier = modifier,
        animationSpec = tween(1000, easing = CubicBezierEasing(0f, 0f, 0.3f, 1f)),
        label = "artwork backdrop",
    ) { image ->
        Box(Modifier.fillMaxSize()) {
            if (image.isNotBlank()) {
                BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp)).blur(64.dp), contentAlignment = Alignment.Center) {
                    val edge = maxOf(maxHeight, maxWidth) * 1.3f
                    val backdrop = Modifier.requiredSize(edge)
                    artwork.Artwork(image, null, backdrop.graphicsLayer { rotationZ = mainRotation.value })
                    artwork.Artwork(image, null, backdrop.graphicsLayer {
                        rotationZ = upperRotation.value
                        translationX = -0.95f * maxWidth.toPx()
                        translationY = -0.7f * maxHeight.toPx()
                    })
                    artwork.Artwork(image, null, backdrop.graphicsLayer {
                        rotationZ = lowerRotation.value
                        translationX = -0.5f * maxWidth.toPx()
                        translationY = 0.7f * maxHeight.toPx()
                    })
                }
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0x4D000000),
                        0.5f to Color(0x59000000),
                        1f to Color(0x80000000),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun PlayerArtwork(
    track: Track,
    artwork: UiArtworkEnvironment,
    playing: Boolean,
    seeking: Boolean,
    drag: Modifier,
    sharedArtwork: Modifier,
) {
    val targetScale = when {
        !playing -> 0.85f
        seeking -> 0.95f
        else -> 1f
    }
    val scale by animateFloatAsState(targetScale, tween(300, easing = artworkEasing), label = "artwork scale")
    val elevation by animateDpAsState(if (playing) 15.dp else 8.dp, tween(300, easing = artworkEasing), label = "artwork elevation")
    BoxWithConstraints(
        Modifier.fillMaxSize().then(drag).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val edge = minOf(maxWidth, maxHeight, 560.dp)
        Crossfade(
            track.artwork to track.title,
            modifier = Modifier.size(edge).then(sharedArtwork).graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(elevation, artworkShape)
                .clip(artworkShape)
                .border(0.5.dp, Color(0x1A000000), artworkShape)
                .background(Color(0xFF27272A)),
            animationSpec = tween(310),
            label = "track artwork",
        ) { (url, title) -> artwork.Artwork(url, title, Modifier.fillMaxSize()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTimeline(
    player: Player,
    playback: PlaybackState,
    track: Track,
    modifier: Modifier,
    onScrubbing: (Boolean) -> Unit,
) {
    val duration = track.duration.takeIf { it > 0 } ?: playback.track.duration.coerceAtLeast(0)
    val seekEnabled = "seek" in player.capabilities && duration > 0
    var scrub by remember(track.id, playback.playbackInstanceId, duration, seekEnabled) { mutableStateOf<Float?>(null) }
    val fraction = scrub ?: playbackFraction(playback.position, duration)
    val sliderState = remember { SliderState() }
    SideEffect { sliderState.value = fraction }
    val position = scrub?.let { (it.toDouble() * duration).roundToLong() } ?: playback.position.coerceAtLeast(0)
    val interactions = remember { MutableInteractionSource() }
    val dragged by interactions.collectIsDraggedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val interacting = dragged || pressed
    SideEffect { onScrubbing(interacting) }
    val timeColor = when {
        !seekEnabled -> Color(0x14FFFFFF)
        dragged || pressed -> playerSecondary
        else -> playerTrack
    }
    Column(modifier.fillMaxWidth()) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Ltr,
            LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        ) {
            Slider(
            state = sliderState,
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { player.seek((it.toDouble() * duration).roundToLong()) }
                scrub = null
            },
            enabled = seekEnabled,
            modifier = Modifier.fillMaxWidth().height(27.dp),
            interactionSource = interactions,
            thumb = {},
            track = { state ->
                Canvas(Modifier.fillMaxWidth().height(7.dp)) {
                    val height = 7.dp.toPx()
                    val radius = CornerRadius(3.5.dp.toPx())
                    val top = (size.height - height) / 2f
                    val filled = size.width * state.value
                    drawRoundRect(playerTrack, Offset(0f, top), Size(size.width, height), radius)
                    if (filled > 0f) {
                        val filledColor = if (dragged || pressed) playerPrimary else playerSecondary
                        drawRoundRect(filledColor, Offset(0f, top), Size(filled, height), radius)
                    }
                }
            },
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val timeStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
                Text(formatTime(position), color = timeColor, style = timeStyle)
                if (duration > 0) {
                    Text("-" + formatTime((duration - position).coerceAtLeast(0)), color = timeColor, style = timeStyle)
                }
            }
        }
    }
}

@Composable
private fun PlayerTransport(player: Player, playing: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        PlayerButton(PlayerControlArt.Previous, stringResource(Res.string.previous), "previous" in player.capabilities, width = 68.dp, height = 61.dp, action = player::previous)
        PlayerButton(
            if (playing) PlayerControlArt.Pause else PlayerControlArt.Play,
            stringResource(if (playing) Res.string.pause else Res.string.play),
            "toggle" in player.capabilities,
            width = 66.dp,
            height = 66.dp,
            action = player::toggle,
        )
        PlayerButton(PlayerControlArt.Next, stringResource(Res.string.next), "next" in player.capabilities, width = 68.dp, height = 61.dp, action = player::next)
    }
}

@Composable
private fun PlayerButton(
    icon: PlayerControlArt,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 48.dp,
    selected: Boolean? = null,
    tint: Color = playerPrimary,
    action: () -> Unit,
) {
    IconButton(
        onClick = action,
        enabled = enabled,
        modifier = modifier.size(width, height).semantics { selected?.let { this.selected = it } },
    ) {
        PlayerControlIcon(icon, label, if (enabled) tint else tint.copy(alpha = 0.35f))
    }
}

@Composable
private fun PlayerMore(player: Player, track: Track) {
    val controls = player as? UiPlaybackControls
    val canStartRadio = controls != null && "radio" in player.capabilities
    val canQueue = "addToQueue" in player.capabilities
    if (!canStartRadio && !canQueue) return
    var showOptions by remember(track.id) { mutableStateOf(false) }
    PlayerButton(PlayerControlArt.More, "More", true, width = 44.dp) { showOptions = true }
    if (showOptions) {
        PluginAlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text(track.title) },
            text = {
                Column {
                    if (canStartRadio) {
                        TextButton(onClick = {
                            showOptions = false
                            controls.startTrackRadio()
                        }) { Text("Start Radio") }
                    }
                    if (canQueue) {
                        TextButton(onClick = {
                            showOptions = false
                            player.addToQueue(listOf(track))
                        }) { Text(stringResource(Res.string.add_queue)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptions = false }) { Text("Close") }
            },
            dismissButton = {},
        )
    }
}

@Composable
private fun PlayerQueue(playback: PlaybackState, player: Player, artwork: UiArtworkEnvironment) {
    val controls = player as? UiPlaybackControls
    val indexed = playback.currentIndex?.takeIf { it in playback.queue.indices }
    val current = indexed ?: playback.queue.indexOfFirst { it.id == playback.track.id }
    val firstUpcoming = current + 1
    val upcoming = playback.queue.drop(firstUpcoming)
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.queue), Modifier.weight(1f), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            PlayerButton(PlayerControlArt.Shuffle, stringResource(Res.string.shuffle), controls != null && "shuffle" in player.capabilities) {
                controls?.toggleShuffle()
            }
            PlayerButton(PlayerControlArt.Repeat, stringResource(Res.string.repeat), controls != null && "repeat" in player.capabilities) {
                controls?.cycleRepeat()
            }
        }
        if (upcoming.isEmpty()) {
            Text("No upcoming tracks", color = playerSecondary, modifier = Modifier.padding(top = 14.dp))
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
        ) {
            itemsIndexed(upcoming) { index, track ->
                Row(
                    Modifier.fillMaxWidth().height(52.dp).clickable(enabled = controls != null && "queueIndex" in player.capabilities) {
                        controls?.selectQueueIndex(firstUpcoming + index)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    artwork.Artwork(track.artwork, null, Modifier.size(42.dp).clip(RoundedCornerShape(5.dp)))
                    Column {
                        Text(track.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = playerSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerLyrics(
    track: Track,
    playback: PlaybackState,
    player: Player,
    context: PluginContext,
    modifier: Modifier = Modifier,
) {
    var lyrics by remember(track.id) { mutableStateOf<LyricsResult?>(null) }
    var loading by remember(track.id) { mutableStateOf(true) }
    LaunchedEffect(track.id, context) {
        try {
            val response = context.callService("ui.lyrics/v1", buildJsonObject {})
            if (response["trackId"]?.jsonPrimitive?.contentOrNull == track.id) {
                lyrics = response["lyrics"]?.let { Json.decodeFromJsonElement(LyricsResult.serializer(), it) }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            lyrics = null
        } finally {
            loading = false
        }
    }
    val lines = lyrics?.lines.orEmpty()
    if (lines.isEmpty()) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(if (loading) "Loading lyrics…" else "Lyrics unavailable", color = playerSecondary, fontSize = 17.sp)
        }
        return
    }

    val wordRanges = remember(lines) { lines.map(::lyricWordRanges) }
    val timed = lines.any { it.lyricStartMs() != null }
    val position = lyricPosition(playback)
    val active by remember(lines, position) {
        derivedStateOf {
            val time = position.value
            lines.indexOfLast { line -> line.lyricStartMs()?.let { it <= time } == true }
        }
    }
    val list = rememberLazyListState()
    val dragging by list.interactionSource.collectIsDraggedAsState()
    var follow by remember(track.id) { mutableStateOf(true) }
    LaunchedEffect(dragging, track.id) {
        if (dragging) {
            follow = false
        } else if (!follow) {
            delay(4_000)
            follow = true
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val top = maxHeight * 0.16f
        LaunchedEffect(active, follow, track.id) {
            if (follow && active >= 0) list.animateScrollToItem(active)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent, 0.10f to Color.Black, 0.86f to Color.Black, 1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            state = list,
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = top),
            verticalArrangement = Arrangement.spacedBy(if (timed) 32.dp else 12.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                val highlighted = !timed || index == active
                val alpha by animateFloatAsState(if (highlighted) 1f else 0.45f, tween(240), label = "lyric emphasis")
                val scale by animateFloatAsState(if (highlighted) 1f else 0.98f, tween(240), label = "lyric scale")
                val time = line.lyricStartMs()
                KaraokeLine(
                    line = line,
                    words = wordRanges[index],
                    active = index == active,
                    timed = timed,
                    position = position,
                    modifier = Modifier.clickable(enabled = time != null && "seek" in player.capabilities) {
                        if (time != null) {
                            player.seek(time)
                            follow = true
                        }
                    }.graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
            if (timed) item { Spacer(Modifier.height(maxHeight - top)) }
        }
    }
}
