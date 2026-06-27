package app.aihandmade.core.quant

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ClusterMedoidEdgeTest {

    private fun sampleSet(
        L: FloatArray = floatArrayOf(0.3f, 0.5f, 0.7f),
        a: FloatArray = floatArrayOf(0f, 0f, 0f),
        b: FloatArray = floatArrayOf(0f, 0f, 0f),
        weight: FloatArray = floatArrayOf(1f, 1f, 1f),
    ) = SampleSet(
        index = intArrayOf(0, 1, 2),
        L = L,
        a = a,
        b = b,
        weight = weight,
        sourceWidth = 3,
        sourceHeight = 1,
    )

    // ---------- collectCluster ----------

    @Test
    fun collectClusterRejectsNegativeBinRadius() {
        assertThrows(IllegalArgumentException::class.java) {
            collectCluster(sampleSet(), BinIndex(12, 12, 12), binRadius = -1)
        }
    }

    @Test
    fun collectClusterRejectsNegativeLBinIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            collectCluster(sampleSet(), BinIndex(-1, 12, 12))
        }
    }

    @Test
    fun collectClusterRejectsOutOfRangeABinIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            collectCluster(sampleSet(), BinIndex(12, 24, 12))
        }
    }

    @Test
    fun collectClusterRejectsOutOfRangeBBinIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            collectCluster(sampleSet(), BinIndex(12, 12, 24))
        }
    }

    @Test
    fun collectClusterRejectsNegativeBBinIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            collectCluster(sampleSet(), BinIndex(12, 12, -1))
        }
    }

    // ---------- weightedMedoid ----------

    @Test
    fun medoidRejectsNegativeClusterIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(), intArrayOf(-1))
        }
    }

    @Test
    fun medoidRejectsOutOfRangeClusterIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(), intArrayOf(0, 1, 3))
        }
    }

    @Test
    fun medoidRejectsDuplicateClusterIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(), intArrayOf(0, 1, 1, 2))
        }
    }

    @Test
    fun medoidRejectsUnsortedClusterIndices() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(), intArrayOf(2, 0, 1))
        }
    }

    @Test
    fun medoidRejectsNaNWeight() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(weight = floatArrayOf(1f, Float.NaN, 1f)), intArrayOf(0, 1, 2))
        }
    }

    @Test
    fun medoidRejectsInfiniteWeight() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(
                sampleSet(weight = floatArrayOf(1f, Float.POSITIVE_INFINITY, 1f)),
                intArrayOf(0, 1, 2),
            )
        }
    }

    @Test
    fun medoidRejectsNegativeWeight() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(weight = floatArrayOf(1f, -0.1f, 1f)), intArrayOf(0, 1, 2))
        }
    }

    @Test
    fun medoidRejectsNaNL() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(L = floatArrayOf(0.3f, Float.NaN, 0.7f)), intArrayOf(0, 1, 2))
        }
    }

    @Test
    fun medoidRejectsInfiniteA() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(
                sampleSet(a = floatArrayOf(0f, Float.NEGATIVE_INFINITY, 0f)),
                intArrayOf(0, 1, 2),
            )
        }
    }

    @Test
    fun medoidRejectsNaNB() {
        assertThrows(IllegalArgumentException::class.java) {
            weightedMedoid(sampleSet(b = floatArrayOf(0f, 0f, Float.NaN)), intArrayOf(0, 1, 2))
        }
    }
}
