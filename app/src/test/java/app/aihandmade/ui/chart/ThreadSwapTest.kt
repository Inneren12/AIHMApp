package app.aihandmade.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadSwapTest {

    // ── bandOf threshold edges ────────────────────────────────────────────────

    @Test fun bandOf_zero_isNear() = assertEquals(SwapBand.NEAR, bandOf(0.0))
    @Test fun bandOf_belowNearThreshold_isNear() = assertEquals(SwapBand.NEAR, bandOf(1.4))
    @Test fun bandOf_atNearThreshold_isSimilar() = assertEquals(SwapBand.SIMILAR, bandOf(1.5))
    @Test fun bandOf_belowSimilarThreshold_isSimilar() = assertEquals(SwapBand.SIMILAR, bandOf(3.9))
    @Test fun bandOf_atSimilarThreshold_isNoticeable() = assertEquals(SwapBand.NOTICEABLE, bandOf(4.0))
    @Test fun bandOf_belowNoticeableThreshold_isNoticeable() = assertEquals(SwapBand.NOTICEABLE, bandOf(7.9))
    @Test fun bandOf_atNoticeableThreshold_isVeryDifferent() = assertEquals(SwapBand.VERY_DIFFERENT, bandOf(8.0))
    @Test fun bandOf_large_isVeryDifferent() = assertEquals(SwapBand.VERY_DIFFERENT, bandOf(100.0))

    // ── nearestThreads ────────────────────────────────────────────────────────

    @Test fun nearestThreads_nZero_returnsEmpty() {
        val result = nearestThreads(0xFF_FF0000.toInt(), excludeCode = "321", n = 0)
        assertTrue(result.isEmpty())
    }

    @Test fun nearestThreads_negativeN_throws() {
        try {
            nearestThreads(0xFF_FF0000.toInt(), excludeCode = "321", n = -1)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-negative") == true)
        }
    }

    @Test fun nearestThreads_excludesCurrentCode() {
        // Use pure red — DMC 321 is close. Exclude it and verify it's absent.
        val results = nearestThreads(0xFF_FF0000.toInt(), excludeCode = "321", n = 6)
        assertTrue(results.none { it.ref.code == "321" })
    }

    @Test fun nearestThreads_returnsAtMostN() {
        val results = nearestThreads(0xFF_80_80_80.toInt(), excludeCode = "", n = 3)
        assertTrue(results.size <= 3)
    }

    @Test fun nearestThreads_sortedAscending() {
        val results = nearestThreads(0xFF_00_80_00.toInt(), excludeCode = "", n = 6)
        for (i in 1 until results.size) {
            assertTrue(results[i - 1].delta <= results[i].delta)
        }
    }

    @Test fun nearestThreads_deltaIsNonNegative() {
        nearestThreads(0xFF_FF_FF_00.toInt(), excludeCode = "", n = 6).forEach {
            assertTrue(it.delta >= 0.0)
        }
    }

    // ── searchThreads ─────────────────────────────────────────────────────────

    @Test fun searchThreads_limitZero_returnsEmpty() {
        val result = searchThreads("", limit = 0)
        assertTrue(result.isEmpty())
    }

    @Test fun searchThreads_negativeLimit_throws() {
        try {
            searchThreads("", limit = -1)
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("non-negative") == true)
        }
    }

    @Test fun searchThreads_emptyQuery_returnsUpToLimit() {
        val results = searchThreads("", limit = 10)
        assertEquals(10, results.size)
    }

    @Test fun searchThreads_byCodeCaseInsensitive() {
        // B5200 is the only alphanumeric DMC code; lower vs upper must return the same result.
        val lower = searchThreads("b5200", limit = 80)
        val upper = searchThreads("B5200", limit = 80)
        assertTrue(lower.any { it.code == "B5200" })
        assertEquals(lower.map { it.code }, upper.map { it.code })
    }

    @Test fun searchThreads_byNameCaseInsensitive() {
        // DMC 321 name is "Red"; search in upper case to confirm case-insensitive matching
        val resultsUpper = searchThreads("RED", limit = 80)
        val resultsLower = searchThreads("red", limit = 80)
        assertTrue(resultsUpper.any { it.code == "321" })
        assertEquals(resultsUpper.map { it.code }, resultsLower.map { it.code })
    }

    @Test fun searchThreads_unknownQuery_returnsEmpty() {
        val results = searchThreads("ZZZNOMATCH99999", limit = 80)
        assertTrue(results.isEmpty())
    }

    @Test fun searchThreads_resultsRespectLimit() {
        val all = searchThreads("", limit = 200)
        val limited = searchThreads("", limit = 5)
        assertTrue(all.size > limited.size)
        assertEquals(5, limited.size)
    }
}
