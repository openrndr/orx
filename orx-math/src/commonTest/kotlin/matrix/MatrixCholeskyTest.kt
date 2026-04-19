package org.openrndr.extra.math.matrix

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.abs

class MatrixCholeskyTest {
    @Test
    fun testSolveCholesky() {
        // A symmetric, positive-definite matrix A
        // A = [[4, 12, -16], [12, 37, -43], [-16, -43, 98]]
        val matrix = Matrix(3, 3)
        matrix[0, 0] = 4.0
        matrix[0, 1] = 12.0
        matrix[0, 2] = -16.0
        matrix[1, 0] = 12.0
        matrix[1, 1] = 37.0
        matrix[1, 2] = -43.0
        matrix[2, 0] = -16.0
        matrix[2, 1] = -43.0
        matrix[2, 2] = 98.0

        // Right-hand side vector b
        val b = doubleArrayOf(1.0, 2.0, 3.0)

        // Solve Ax = b
        val x = solveCholesky(matrix, b)

        // Expected solution x approx [32.125, -9.375, 1.125]
        // Let's verify by Ax = b
        val Ax0 = matrix[0, 0] * x[0] + matrix[0, 1] * x[1] + matrix[0, 2] * x[2]
        val Ax1 = matrix[1, 0] * x[0] + matrix[1, 1] * x[1] + matrix[1, 2] * x[2]
        val Ax2 = matrix[2, 0] * x[0] + matrix[2, 1] * x[1] + matrix[2, 2] * x[2]

        assertTrue(abs(Ax0 - b[0]) < 1e-10, "Ax0 = $Ax0, b0 = ${b[0]}")
        assertTrue(abs(Ax1 - b[1]) < 1e-10, "Ax1 = $Ax1, b1 = ${b[1]}")
        assertTrue(abs(Ax2 - b[2]) < 1e-10, "Ax2 = $Ax2, b2 = ${b[2]}")
    }

    @Test
    fun testSolveCholeskyMatrix() {
        // A symmetric, positive-definite matrix A
        val matrix = Matrix(3, 3)
        matrix[0, 0] = 4.0
        matrix[0, 1] = 12.0
        matrix[0, 2] = -16.0
        matrix[1, 0] = 12.0
        matrix[1, 1] = 37.0
        matrix[1, 2] = -43.0
        matrix[2, 0] = -16.0
        matrix[2, 1] = -43.0
        matrix[2, 2] = 98.0

        // Right-hand side matrix B (2 columns)
        val b = Matrix(3, 2)
        b[0, 0] = 1.0; b[0, 1] = 4.0
        b[1, 0] = 2.0; b[1, 1] = 5.0
        b[2, 0] = 3.0; b[2, 1] = 6.0

        // Solve AX = B
        val x = solveCholesky(matrix, b)

        // Verify by AX = B
        val res = matrix * x
        for (row in 0 until 3) {
            for (col in 0 until 2) {
                assertTrue(abs(res[row, col] - b[row, col]) < 1e-10, "res[$row, $col] = ${res[row, col]}, b = ${b[row, col]}")
            }
        }
    }
}
