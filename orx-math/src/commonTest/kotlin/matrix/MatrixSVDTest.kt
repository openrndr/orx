package org.openrndr.extra.math.matrix

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatrixSVDTest {

    private fun assertMatrixEquals(expected: Matrix, actual: Matrix, tolerance: Double = 1e-6) {
        assertEquals(expected.rows, actual.rows, "Row count mismatch")
        assertEquals(expected.cols, actual.cols, "Col count mismatch")
        for (i in 0 until expected.rows) {
            for (j in 0 until expected.cols) {
                val diff = abs(expected[i, j] - actual[i, j])
                assertTrue(
                    diff < tolerance,
                    "Mismatch at ($i, $j): expected ${expected[i, j]}, got ${actual[i, j]} (diff: $diff)"
                )
            }
        }
    }

    private fun assertOrthogonal(matrix: Matrix, tolerance: Double = 1e-6) {
        val prod = matrix * matrix.transposed()
        val identity = Matrix.identity(matrix.rows)
        assertMatrixEquals(identity, prod, tolerance)
    }

    private fun verifySVD(a: Matrix, tolerance: Double = 1e-6) {
        val (u, s, v) = a.svd(fullMatrix = true)

        // Check dimensions
        assertEquals(a.rows, u.rows, "U rows")
        assertEquals(a.rows, u.cols, "U cols")
        assertEquals(a.rows, s.rows, "S rows")
        assertEquals(a.cols, s.cols, "S cols")
        assertEquals(a.cols, v.rows, "V rows")
        assertEquals(a.cols, v.cols, "V cols")

        // Check orthogonality
        assertOrthogonal(u, tolerance)
        assertOrthogonal(v, tolerance)

        // Check S is diagonal and sorted descending and non-negative
        val minDim = minOf(s.rows, s.cols)
        var prev = Double.POSITIVE_INFINITY
        for (i in 0 until s.rows) {
            for (j in 0 until s.cols) {
                if (i == j) {
                    val sVal = s[i, j]
                    assertTrue(sVal >= -tolerance, "Singular value must be non-negative: $sVal")
                    assertTrue(sVal <= prev + tolerance, "Singular values must be in descending order: $sVal > $prev")
                    prev = sVal
                } else {
                    assertTrue(abs(s[i, j]) < tolerance, "Non-diagonal element in S at ($i, $j) must be zero: ${s[i, j]}")
                }
            }
        }

        // Check reconstruction A = U * S * V^T
        val reconstructed = u * s * v.transposed()
        assertMatrixEquals(a, reconstructed, tolerance)

        verifyReducedSVD(a, tolerance)
    }

    private fun verifyReducedSVD(a: Matrix, tolerance: Double = 1e-6) {
        val (u, s, v) = a.svd(fullMatrix = false)
        val k = minOf(a.rows, a.cols)

        // Check dimensions
        assertEquals(a.rows, u.rows, "Reduced U rows")
        assertEquals(k, u.cols, "Reduced U cols")
        assertEquals(1, s.rows, "Reduced S rows")
        assertEquals(k, s.cols, "Reduced S cols")
        assertEquals(a.cols, v.rows, "Reduced V rows")
        assertEquals(k, v.cols, "Reduced V cols")

        // Check orthogonality of columns: U^T * U = I_k, V^T * V = I_k
        if (k > 0) {
            val uProd = u.transposed() * u
            val vProd = v.transposed() * v
            val identityK = Matrix.identity(k)
            assertMatrixEquals(identityK, uProd, tolerance)
            assertMatrixEquals(identityK, vProd, tolerance)
        }

        // Check S is sorted descending and non-negative
        var prev = Double.POSITIVE_INFINITY
        for (i in 0 until k) {
            val sVal = s[0, i]
            assertTrue(sVal >= -tolerance, "Singular value must be non-negative: $sVal")
            assertTrue(sVal <= prev + tolerance, "Singular values must be in descending order: $sVal > $prev")
            prev = sVal
        }

        // Check reconstruction A = U * diag(S) * V^T
        val sDiag = Matrix.zeros(k, k)
        for (i in 0 until k) {
            sDiag[i, i] = s[0, i]
        }
        val reconstructed = u * sDiag * v.transposed()
        assertMatrixEquals(a, reconstructed, tolerance)
    }

    @Test
    fun testIdentityMatrix() {
        val identity = Matrix.identity(3)
        verifySVD(identity)
    }

    @Test
    fun testSquareMatrix2x2() {
        val m = Matrix(2, 2)
        m[0, 0] = 3.0
        m[0, 1] = 2.0
        m[1, 0] = 2.0
        m[1, 1] = 3.0
        verifySVD(m)
    }

    @Test
    fun testSquareMatrix3x3() {
        val m = Matrix(3, 3)
        m[0, 0] = 1.0; m[0, 1] = 2.0; m[0, 2] = 3.0
        m[1, 0] = 4.0; m[1, 1] = 5.0; m[1, 2] = 6.0
        m[2, 0] = 7.0; m[2, 1] = 8.0; m[2, 2] = 9.0
        verifySVD(m)
    }

    @Test
    fun testRectangularMoreRows() {
        // 4 x 2 matrix
        val m = Matrix(4, 2)
        m[0, 0] = 1.0; m[0, 1] = 2.0
        m[1, 0] = 3.0; m[1, 1] = 4.0
        m[2, 0] = 5.0; m[2, 1] = 6.0
        m[3, 0] = 7.0; m[3, 1] = 8.0
        verifySVD(m)
    }

    @Test
    fun testRectangularMoreCols() {
        // 2 x 4 matrix
        val m = Matrix(2, 4)
        m[0, 0] = 1.0; m[0, 1] = 2.0; m[0, 2] = 3.0; m[0, 3] = 4.0
        m[1, 0] = 5.0; m[1, 1] = 6.0; m[1, 2] = 7.0; m[1, 3] = 8.0
        verifySVD(m)
    }

    @Test
    fun testZeroMatrix() {
        val m = Matrix.zeros(3, 3)
        verifySVD(m)
    }

    @Test
    fun testSingularValuesKnown() {
        // Known matrix with exact singular values
        // A = [[1, 0], [0, -3]] -> Singular values are 3, 1
        val m = Matrix(2, 2)
        m[0, 0] = 1.0
        m[1, 1] = -3.0
        val (u, s, v) = m.svd()
        assertTrue(abs(s[0, 0] - 3.0) < 1e-6)
        assertTrue(abs(s[1, 1] - 1.0) < 1e-6)
        verifySVD(m)
    }

    @Test
    fun test1x1Matrix() {
        val m = Matrix(1, 1)
        m[0, 0] = -5.0
        val (u, s, v) = m.svd()
        assertTrue(abs(s[0, 0] - 5.0) < 1e-6)
        verifySVD(m)
    }

    @Test
    fun testRankDeficientMatrix() {
        // Rank 1 matrix of size 3x3
        val m = Matrix(3, 3)
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                m[i, j] = (i + 1).toDouble() * (j + 1).toDouble()
            }
        }
        val (u, s, v) = m.svd()
        assertTrue(s[0, 0] > 1e-6)
        assertTrue(abs(s[1, 1]) < 1e-6)
        assertTrue(abs(s[2, 2]) < 1e-6)
        verifySVD(m)
    }

    @Test
    fun test5x3Matrix() {
        val m = Matrix(5, 3)
        var count = 1.0
        for (i in 0 until 5) {
            for (j in 0 until 3) {
                m[i, j] = count++
            }
        }
        verifySVD(m)
    }

    @Test
    fun test3x5Matrix() {
        val m = Matrix(3, 5)
        var count = 1.0
        for (i in 0 until 3) {
            for (j in 0 until 5) {
                m[i, j] = count++
            }
        }
        verifySVD(m)
    }

    @Test
    fun testEmptyMatrices() {
        val m0x0 = Matrix(0, 0)
        val (u0, s0, v0) = m0x0.svd(fullMatrix = true)
        assertEquals(0, u0.rows); assertEquals(0, u0.cols)
        assertEquals(0, s0.rows); assertEquals(0, s0.cols)
        assertEquals(0, v0.rows); assertEquals(0, v0.cols)

        val (ur0, sr0, vr0) = m0x0.svd(fullMatrix = false)
        assertEquals(0, ur0.rows); assertEquals(0, ur0.cols)
        assertEquals(1, sr0.rows); assertEquals(0, sr0.cols)
        assertEquals(0, vr0.rows); assertEquals(0, vr0.cols)

        val m0x3 = Matrix(0, 3)
        val (u03, s03, v03) = m0x3.svd(fullMatrix = true)
        assertEquals(0, u03.rows); assertEquals(0, u03.cols)
        assertEquals(0, s03.rows); assertEquals(3, s03.cols)
        assertEquals(3, v03.rows); assertEquals(3, v03.cols)

        val (ur03, sr03, vr03) = m0x3.svd(fullMatrix = false)
        assertEquals(0, ur03.rows); assertEquals(0, ur03.cols)
        assertEquals(1, sr03.rows); assertEquals(0, sr03.cols)
        assertEquals(3, vr03.rows); assertEquals(0, vr03.cols)

        val m3x0 = Matrix(3, 0)
        val (u30, s30, v30) = m3x0.svd(fullMatrix = true)
        assertEquals(3, u30.rows); assertEquals(3, u30.cols)
        assertEquals(3, s30.rows); assertEquals(0, s30.cols)
        assertEquals(0, v30.rows); assertEquals(0, v30.cols)

        val (ur30, sr30, vr30) = m3x0.svd(fullMatrix = false)
        assertEquals(3, ur30.rows); assertEquals(0, ur30.cols)
        assertEquals(1, sr30.rows); assertEquals(0, sr30.cols)
        assertEquals(0, vr30.rows); assertEquals(0, vr30.cols)
    }
}
