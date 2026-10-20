package org.openrndr.extra.math.eigenmodes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CycleEigenModesTest {

    /**
     * Analytic spectrum of the n-node cycle graph Laplacian: lambda_k = 2 - 2*cos(2*pi*k/n) for
     * k in 0 until n.
     */
    private fun analyticSpectrum(n: Int): List<Double> {
        return (0 until n).map { k -> 2.0 - 2.0 * cos(2.0 * PI * k / n) }.sorted()
    }

    @Test
    fun matchesAnalyticSpectrum() {
        val n = 12
        val modes = extractCycleEigenModes(n, count = 6)
        val expected = analyticSpectrum(n)

        for (i in modes.indices) {
            assertTrue(
                abs(modes[i].eigenvalue - expected[i]) < 1e-4,
                "mode $i: expected eigenvalue ${expected[i]}, got ${modes[i].eigenvalue}"
            )
        }
    }

    @Test
    fun modeZeroIsTheTrivialConstantMode() {
        val n = 10
        val modes = extractCycleEigenModes(n, count = 4)

        assertTrue(abs(modes[0].eigenvalue) < 1e-9)
        assertEquals(0.0, modes[0].frequencyRatio, 1e-9)
        val first = modes[0].values.first()
        for (v in modes[0].values) assertTrue(abs(v - first) < 1e-6, "mode 0 must be constant")

        assertEquals(1.0, modes[1].frequencyRatio, 1e-6)
    }

    @Test
    fun valueWrapsAroundPeriodically() {
        val n = 8
        val mode = extractCycleEigenModes(n, count = 2).last()

        assertEquals(mode.values[0], mode.value(0.0), 1e-9)
        assertEquals(mode.value(0.0), mode.value(n.toDouble()), 1e-9)
        assertEquals(mode.value(-1.0), mode.value(n - 1.0), 1e-9)

        val midpoint = (mode.values[n - 1] + mode.values[0]) / 2.0
        assertEquals(midpoint, mode.value(n - 0.5), 1e-9)
    }

    @Test
    fun countIsClampedToN() {
        val modes = extractCycleEigenModes(4, count = 100)
        assertEquals(4, modes.size)
    }
}
