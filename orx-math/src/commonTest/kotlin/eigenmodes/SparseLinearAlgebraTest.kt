package org.openrndr.extra.math.eigenmodes

import org.openrndr.extra.math.matrix.SparseMatrix
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparseLinearAlgebraTest {

    private fun assertArrayClose(expected: DoubleArray, actual: DoubleArray, tolerance: Double = 1e-6) {
        assertEquals(expected.size, actual.size, "array size mismatch")
        for (i in expected.indices) {
            assertTrue(
                abs(expected[i] - actual[i]) < tolerance,
                "mismatch at $i: expected ${expected[i]}, got ${actual[i]}"
            )
        }
    }

    @Test
    fun matVecMatchesDenseMultiplication() {
        val matrix = SparseMatrix(
            3, 3,
            listOf(0 to 0, 0 to 2, 1 to 1, 2 to 0, 2 to 2),
            listOf(2.0, -1.0, 3.0, -1.0, 4.0),
            presorted = false
        )
        val v = doubleArrayOf(1.0, 2.0, 3.0)
        val result = matrix * v

        assertArrayClose(doubleArrayOf(2.0 * 1.0 - 1.0 * 3.0, 3.0 * 2.0, -1.0 * 1.0 + 4.0 * 3.0), result)
    }

    @Test
    fun diagonalExtractsMainDiagonal() {
        val matrix = SparseMatrix(
            2, 2,
            listOf(0 to 0, 0 to 1, 1 to 1),
            listOf(5.0, 1.0, 7.0),
            presorted = false
        )
        assertArrayClose(doubleArrayOf(5.0, 7.0), matrix.diagonal())
    }

    @Test
    fun solveCGSolvesSymmetricPositiveDefiniteSystem() {
        // [[4, 1], [1, 3]] x = [1, 2] -> x = [1/11, 7/11]
        val matrix = SparseMatrix(
            2, 2,
            listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
            listOf(4.0, 1.0, 1.0, 3.0),
            presorted = false
        )
        val x = matrix.solveCG(doubleArrayOf(1.0, 2.0), tolerance = 1e-12, maxIterations = 50)
        assertArrayClose(doubleArrayOf(1.0 / 11.0, 7.0 / 11.0), x, tolerance = 1e-8)
    }
}
