package dev.metrofuse.roselight

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.sp
import dev.metrofuse.api.LyricsLine
import dev.metrofuse.api.PlaybackState
import kotlin.time.TimeSource

@Composable
internal fun lyricPosition(playback: PlaybackState): State<Long> =
    produceState(
        playback.position,
        playback.track.id,
        playback.playbackInstanceId,
        playback.position,
        playback.playing,
        playback.track.duration,
    ) {
        val start = TimeSource.Monotonic.markNow()
        val anchor = playback.position.coerceAtLeast(0L)
        val duration = playback.track.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        value = anchor.coerceAtMost(duration)
        if (playback.playing) {
            while (true) {
                withFrameNanos {
                    val elapsed = start.elapsedNow().inWholeMilliseconds
                    value = anchor.coerceAtMost(duration) + elapsed.coerceAtMost((duration - anchor).coerceAtLeast(0))
                }
            }
        }
    }

internal data class LyricWordRange(val start: Int, val end: Int, val startMs: Long, val endMs: Long) {
    fun progress(position: Long): Float = when {
        position < startMs -> 0f
        position >= endMs -> 1f
        else -> ((position - startMs).toDouble() / (endMs - startMs)).toFloat()
    }
}

internal fun lyricWordRanges(line: LyricsLine): List<LyricWordRange> {
    val lineText = line.text.lyricPunctuation()
    var cursor = 0
    return line.words.mapNotNull { word ->
        val text = word.text.trim()
        if (text.isEmpty() || word.endMs <= word.startMs) return@mapNotNull null
        val start = lineText.indexOf(text.lyricPunctuation(), cursor, ignoreCase = true)
        if (start < 0) return@mapNotNull null
        cursor = start + text.length
        LyricWordRange(start, cursor, word.startMs, word.endMs)
    }
}

private fun String.lyricPunctuation() = replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')

internal fun LyricsLine.lyricStartMs(): Long? =
    words.firstOrNull { it.endMs > it.startMs }?.startMs ?: timeMs

private data class WordInk(val path: Path, val bounds: Rect, val rtl: Boolean)
private data class TimedInk(val word: LyricWordRange, val pieces: List<WordInk>)

private fun TextLayoutResult.wordInk(word: LyricWordRange): TimedInk {
    val pieces = (getLineForOffset(word.start)..getLineForOffset(word.end - 1)).mapNotNull { line ->
        val start = maxOf(word.start, getLineStart(line))
        val end = minOf(word.end, getLineEnd(line, visibleEnd = true))
        if (end <= start) return@mapNotNull null
        val path = getPathForRange(start, end)
        WordInk(path, path.getBounds(), getBidiRunDirection(start) == ResolvedTextDirection.Rtl)
    }
    return TimedInk(word, pieces)
}

@Composable
internal fun KaraokeLine(
    line: LyricsLine,
    words: List<LyricWordRange>,
    active: Boolean,
    timed: Boolean,
    position: State<Long>,
    modifier: Modifier = Modifier,
) {
    var layout by remember(line.text) { mutableStateOf<TextLayoutResult?>(null) }
    val measured = layout
    val ink = remember(measured, words, active) {
        if (active && measured != null) words.map(measured::wordInk) else emptyList()
    }
    val highlight = active && words.isNotEmpty()
    Text(
        text = line.text,
        modifier = modifier.fillMaxWidth().drawWithContent {
            drawContent()
            if (measured != null && ink.isNotEmpty()) {
                val time = position.value
                for ((word, pieces) in ink) {
                    var remaining = pieces.sumOf { it.bounds.width.toDouble() }.toFloat() * word.progress(time)
                    for ((path, bounds, rtl) in pieces) {
                        if (remaining <= 0f) break
                        val width = minOf(remaining, bounds.width)
                        val left = if (rtl) bounds.right - width else bounds.left
                        clipPath(path) {
                            clipRect(left = left, top = bounds.top, right = left + width, bottom = bounds.bottom) {
                                drawText(measured, color = Color.White)
                            }
                        }
                        remaining -= width
                    }
                }
            }
        },
        color = if (highlight) Color.White.copy(alpha = 0.45f) else Color.White,
        fontSize = if (timed) 34.sp else 22.sp,
        lineHeight = if (timed) 40.sp else 26.sp,
        fontWeight = FontWeight.Bold,
        onTextLayout = { layout = it },
    )
}
