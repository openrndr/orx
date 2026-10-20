package org.openrndr.extra.math.eigenmodes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GridEigenModesTest {

    /**
     * Analytic Dirichlet eigenvalues of the 5-point Laplacian on a full n1 x n2 grid of
     * interior points, i.e. lambda_pq = 4 - 2*cos(p*pi/(n1+1)) - 2*cos(q*pi/(n2+1)).
     */
    private fun analyticSpectrum(n1: Int, n2: Int): List<Double> {
        val values = ArrayList<Double>(n1 * n2)
        for (p in 1..n1) {
            for (q in 1..n2) {
                values.add(4.0 - 2.0 * cos(p * PI / (n1 + 1)) - 2.0 * cos(q * PI / (n2 + 1)))
            }
        }
        return values.sorted()
    }

    @Test
    fun laplacianOfFullRectangleMatchesAnalyticSpectrum() {
        val width = 4
        val height = 5
        val mask = BooleanArray(width * height) { true }

        val modes = extractEigenModes(mask, width, height, count = 6)
        val expected = analyticSpectrum(width, height)

        for (i in modes.indices) {
            assertTrue(
                abs(modes[i].eigenvalue - expected[i]) < 1e-4,
                "mode $i: expected eigenvalue ${expected[i]}, got ${modes[i].eigenvalue}"
            )
        }
    }

    @Test
    fun modesAreAscendingAndFundamentalHasUnitRatio() {
        val width = 6
        val height = 6
        val mask = BooleanArray(width * height) { true }

        val modes = extractEigenModes(mask, width, height, count = 5)

        assertTrue(abs(modes.first().frequencyRatio - 1.0) < 1e-9)
        for (i in 1 until modes.size) {
            assertTrue(modes[i].eigenvalue >= modes[i - 1].eigenvalue - 1e-6, "eigenvalues must be ascending")
            assertTrue(modes[i].frequencyRatio >= 1.0 - 1e-6, "frequency ratio must be >= 1 for all but the fundamental")
        }
    }

    @Test
    fun valuesAreZeroOutsideTheMask() {
        val width = 8
        val height = 8
        val mask = BooleanArray(width * height) { index ->
            val gx = index % width
            val gy = index / width
            (gx - 3.5) * (gx - 3.5) + (gy - 3.5) * (gy - 3.5) <= 3.0 * 3.0
        }

        val modes = extractEigenModes(mask, width, height, count = 3)
        for (mode in modes) {
            for (i in mask.indices) {
                if (!mask[i]) {
                    assertEquals(0.0, mode.values[i])
                }
            }
        }
    }

    @Test
    fun bilinearSamplingReproducesGridValuesAtGridPoints() {
        val width = 5
        val height = 5
        val mask = BooleanArray(width * height) { true }
        val mode = extractEigenModes(mask, width, height, count = 1).first()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val sampled = mode.value(x.toDouble(), y.toDouble())
                assertTrue(abs(sampled - mode.values[y * width + x]) < 1e-9)
            }
        }
    }

    @Test
    fun rejectsEmptyMask() {
        assertFailsWith<IllegalArgumentException> {
            extractEigenModes(BooleanArray(16), 4, 4, count = 1)
        }
    }

    @Test
    fun countIsClampedToInteriorCellCount() {
        val width = 2
        val height = 2
        val mask = BooleanArray(width * height) { true }
        val modes = extractEigenModes(mask, width, height, count = 100)
        assertEquals(4, modes.size)
    }
}
