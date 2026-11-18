package dev.metrofuse.roselight

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun MobileLibraryLinks(onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        listOf("artists" to "Artists", "albums" to "Albums", "songs" to "Songs").forEach { (section, title) ->
            val icon =
                when (section) {
                    "artists" -> "profile"
                    "albums" -> "library"
                    else -> "note"
                }
            Row(
                Modifier.fillMaxWidth().height(49.dp).clickable { onSelect(section) },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(icon, "", Modifier.size(23.dp), colors.primary)
                Text(title, Modifier.weight(1f), fontSize = 19.sp)
                Glyph("forward", "", Modifier.size(14.dp), colors.onSurfaceVariant.copy(alpha = 0.65f))
            }
            HorizontalDivider(Modifier.padding(start = 37.dp), color = colors.outlineVariant.copy(alpha = 0.75f))
        }
    }
}

@Composable
internal fun SearchScopes(
    catalogTitle: String,
    librarySelected: Boolean,
    selectLibrary: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val outer = RoundedCornerShape(9.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(outer)
            .background(colors.surfaceVariant)
            .padding(3.dp),
    ) {
        listOf(false to catalogTitle, true to "Library").forEach { (local, label) ->
            val selected = local == librarySelected
            val background by animateColorAsState(if (selected) colors.surface else colors.surfaceVariant)
            Box(
                Modifier
                    .weight(1f)
                    .height(31.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(background)
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab) { selectLibrary(local) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

private val categories =
    listOf(
        Triple("Hits", 0xFFDA3052, 0xFFFA7857),
        Triple("Pop", 0xFFC444E7, 0xFF6D55E4),
        Triple("Hip-Hop", 0xFF384BCC, 0xFF6179FD),
        Triple("Dance", 0xFFC74C80, 0xFFF874AA),
        Triple("Rock", 0xFF9B3646, 0xFFDF5343),
        Triple("Electronic", 0xFF188A9B, 0xFF47CCB6),
    )

@Composable
internal fun SearchBrowse(onCategory: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Browse Categories", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        categories.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (name, start, stop) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(Color(start), Color(stop))))
                            .clickable { onCategory(name) },
                    ) {
                        CategoryArtwork()
                        Text(
                            name,
                            Modifier.align(Alignment.BottomStart).padding(10.dp),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CategoryArtwork() {
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0x25000000), Color.Transparent))))
    Glyph("note", "", Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).size(61.dp), Color.White.copy(alpha = 0.23f))
}
