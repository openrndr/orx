package org.openrndr.extra.math.eigenmodes

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NeumannEigenModesTest {

    /**
     * Analytic Neumann eigenvalues of the 5-point Laplacian on a full n1 x n2 grid of interior
     * points, i.e. lambda_pq = (2 - 2*cos(p*pi/n1)) + (2 - 2*cos(q*pi/n2)) for p in 0 until n1,
     * q in 0 until n2 -- the 2D discrete-cosine-transform spectrum.
     */
    private fun analyticSpectrum(n1: Int, n2: Int): List<Double> {
        val values = ArrayList<Double>(n1 * n2)
        for (p in 0 until n1) {
            for (q in 0 until n2) {
                values.add((2.0 - 2.0 * cos(p * PI / n1)) + (2.0 - 2.0 * cos(q * PI / n2)))
            }
        }
        return values.sorted()
    }

    @Test
    fun laplacianOfFullRectangleMatchesAnalyticSpectrum() {
        val width = 4
        val height = 5
        val mask = BooleanArray(width * height) { true }

        val modes = extractNeumannEigenModes(mask, width, height, count = 6)
        val expected = analyticSpectrum(width, height)

        // A looser tolerance than the equivalent Dirichlet spectrum test: this grid happens to
        // have a couple of closely-spaced eigenvalues among the requested modes (~1.6% apart),
        // which slows inverse power iteration's convergence rate for them.
        for (i in modes.indices) {
            assertTrue(
                abs(modes[i].eigenvalue - expected[i]) < 5e-3,
                "mode $i: expected eigenvalue ${expected[i]}, got ${modes[i].eigenvalue}"
            )
        }
    }

    @Test
    fun modeZeroIsTheTrivialConstantMode() {
        val width = 6
        val height = 6
        val mask = BooleanArray(width * height) { true }

        val modes = extractNeumannEigenModes(mask, width, height, count = 4)

        assertTrue(abs(modes[0].eigenvalue) < 1e-6)
        assertEquals(0.0, modes[0].frequencyRatio, 1e-9)

        val interior = modes[0].values.filterIndexed { i, _ -> mask[i] }
        val first = interior.first()
        for (v in interior) assertTrue(abs(v - first) < 1e-6, "mode 0 must be constant over the domain")

        assertEquals(1.0, modes[1].frequencyRatio, 1e-6)
        for (i in 2 until modes.size) {
            assertTrue(modes[i].frequencyRatio >= modes[i - 1].frequencyRatio - 1e-6)
        }
    }

    @Test
    fun diagonalIsNeighborDegreeNotAFixedFour() {
        val width = 3
        val height = 3
        val mask = BooleanArray(width * height) { true }
        val (laplacian, dofIndex) = buildNeumannLaplacian(mask, width, height)

        // corner (0,0) has 2 in-domain neighbors, edge (1,0) has 3, center (1,1) has 4
        assertEquals(2.0, laplacian.diagonal()[dofIndex[0]], 1e-9)
        assertEquals(3.0, laplacian.diagonal()[dofIndex[1]], 1e-9)
        assertEquals(4.0, laplacian.diagonal()[dofIndex[1 * width + 1]], 1e-9)
    }
}
