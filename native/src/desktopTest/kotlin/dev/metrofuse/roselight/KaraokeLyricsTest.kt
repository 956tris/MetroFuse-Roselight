package dev.metrofuse.roselight

import dev.metrofuse.api.LyricsLine
import dev.metrofuse.api.LyricsWord
import kotlin.test.Test
import kotlin.test.assertEquals

class KaraokeLyricsTest {
    @Test
    fun onlyProviderWordTimingsDriveWordHighlights() {
        val lines = listOf(
            LyricsLine("One more", 1_000),
            LyricsLine("Don’t go", 3_000, listOf(LyricsWord("Don't", 3_200, 3_700), LyricsWord("go", 3_900, 4_500))),
            LyricsLine("Untimed"),
        )
        val (lineOnly, exact, untimed) = lines.map(::lyricWordRanges)
        assertEquals(emptyList(), lineOnly)
        assertEquals(3_200L, exact.first().startMs)
        assertEquals(4_500L, exact.last().endMs)
        assertEquals(emptyList(), untimed)
    }
    @Test
    fun repeatedWordsKeepSeparateRangesAndSweepAcrossTheirInterval() {
        val line = LyricsLine(
            "Go, go!",
            words = listOf(LyricsWord("go", 100, 300), LyricsWord("go", 400, 600)),
        )
        val (first, second) = lyricWordRanges(line)
        assertEquals(0 to 2, first.start to first.end)
        assertEquals(4 to 6, second.start to second.end)
        assertEquals(0f, first.progress(99))
        assertEquals(0.5f, first.progress(200))
        assertEquals(1f, first.progress(300))
        assertEquals(0f, second.progress(300))
        assertEquals(0.5f, second.progress(500))
    }
}
