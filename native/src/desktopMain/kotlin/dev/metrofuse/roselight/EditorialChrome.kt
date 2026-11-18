package dev.metrofuse.roselight

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.HomeItem
import dev.metrofuse.api.HomeShelf
import dev.metrofuse.api.UiArtworkEnvironment
import dev.metrofuse.sdk.resources.Res
import dev.metrofuse.sdk.resources.back
import dev.metrofuse.sdk.resources.settings
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AndroidPageHeader(
    title: String,
    source: String?,
    account: String,
    onChooseSource: () -> Unit,
    onSettings: () -> Unit,
    back: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(end = 38.dp, bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) {
            IconButton(onClick = back, modifier = Modifier.size(44.dp)) {
                Glyph("back", stringResource(Res.string.back), tint = colors.primary)
            }
        }
        Column(
            Modifier.weight(1f).then(
                if (source != null) Modifier.clickable(role = Role.Button, onClick = onChooseSource) else Modifier,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title, Modifier.semantics { heading() }, fontSize = 36.sp, lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (source != null) Glyph("down", "", Modifier.size(14.dp), colors.primary)
            }
            if (!source.isNullOrBlank()) {
                Text(source, color = colors.primary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (source != null) {
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant),
            ) {
                if (account.isBlank()) {
                    Glyph("profile", stringResource(Res.string.settings), Modifier.size(19.dp), colors.primary)
                } else {
                    Text(account.take(1).uppercase(), color = colors.primary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun EditorialHeading(title: String, modifier: Modifier = Modifier) {
    Text(
        title, modifier.fillMaxWidth().semantics { heading() }, fontSize = 23.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.35).sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun EditorialShelf(
    shelf: HomeShelf,
    page: String,
    featured: Boolean,
    wide: Boolean,
    artwork: UiArtworkEnvironment,
    open: (HomeItem) -> Unit,
) {
    val entries = shelf.items.mapNotNull { item -> item.info()?.let { item to it } }
    if (entries.isEmpty()) return
    val hero = featured && entries.any { (_, info) -> info.artwork.isNotBlank() }
    val radio = page == "radio"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = when {
            hero -> if (wide) minOf(maxWidth * 0.64f, 560.dp) else maxWidth * 0.91f
            radio -> if (wide) 160.dp else maxWidth * 0.46f
            wide -> 174.dp
            else -> maxWidth * 0.42f
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f))
            EditorialHeading(shelf.title)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(if (hero) 12.dp else 10.dp)) {
                items(entries) { (item, info) ->
                    val colors = MaterialTheme.colorScheme
                    val shape = when {
                        radio -> RoundedCornerShape(20.dp)
                        hero -> RoundedCornerShape(10.dp)
                        info.kind == "artist" -> CircleShape
                        else -> RoundedCornerShape(8.dp)
                    }
                    Column(
                        Modifier.width(cardWidth).clickable { open(item) },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(if (hero && !radio) 1.65f else 1f)
                                .clip(shape).background(colors.surfaceVariant)
                                .border(0.5.dp, colors.outlineVariant.copy(alpha = 0.45f), shape),
                        ) {
                            artwork.Artwork(info.artwork, null, Modifier.fillMaxSize())
                            if (hero && radio) {
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))))
                                Text(
                                    info.title, Modifier.align(Alignment.BottomStart).padding(16.dp), color = Color.White,
                                    fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (!hero || !radio) {
                            Text(
                                info.title, fontSize = if (hero) 19.sp else 15.sp, lineHeight = if (hero) 23.sp else 18.sp,
                                fontWeight = if (hero) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            if (info.subtitle.isNotBlank()) {
                                Text(info.subtitle, color = colors.onSurfaceVariant, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppleCatalogRow(
    info: CollectionInfo,
    artwork: UiArtworkEnvironment,
    active: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    open: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = if (info.kind == "artist") CircleShape else RoundedCornerShape(6.dp)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).clickable(enabled = enabled, onClick = open).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artwork.Artwork(info.artwork, null, Modifier.size(56.dp).clip(shape).background(colors.surfaceVariant))
            Column(Modifier.weight(1f)) {
                Text(
                    info.title, color = if (active) colors.primary else colors.onSurface,
                    fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (info.subtitle.isNotBlank()) {
                    Text(info.subtitle, color = colors.onSurfaceVariant, fontSize = 14.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            trailing?.invoke()
        }
        HorizontalDivider(Modifier.padding(start = 66.dp), color = colors.outlineVariant.copy(alpha = 0.62f))
    }
}
