package matrix

import org.openrndr.extra.math.matrix.SparseMatrix
import org.openrndr.extra.math.matrix.qrDecomposition
import org.openrndr.extra.math.matrix.solveQR
import kotlin.test.assertEquals
import kotlin.math.abs
import kotlin.test.Test

class SparseMatrixQRTest {

    @Test
    fun testQRDecomposition() {
        // Create a sparse matrix
        // This will create the following matrix:
        // 4.0, 3.0, 0.0
        // 6.0, 3.0, 0.0
        // 0.0, 1.0, 5.0
        val values = doubleArrayOf(4.0, 3.0, 6.0, 3.0, 1.0, 5.0)
        val columnIndices = intArrayOf(0, 1, 0, 1, 1, 2)
        val rowPointers = intArrayOf(0, 2, 4, 6)
        val sparseMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)

        // Perform QR decomposition
        val (q, r) = qrDecomposition(sparseMatrix)

        val originalDense = sparseMatrix.toDenseMatrix()
        for (i in 0 until originalDense.rows) {
            val row = (0 until originalDense.cols).map { j -> originalDense[i, j] }.joinToString()
        }

        val qDense = q.toDenseMatrix()
        for (i in 0 until qDense.rows) {
            val row = (0 until qDense.cols).map { j -> qDense[i, j] }.joinToString()
        }

        val rDense = r.toDenseMatrix()
        for (i in 0 until rDense.rows) {
            val row = (0 until rDense.cols).map { j -> rDense[i, j] }.joinToString()
        }

        // Verify Q is orthogonal (Q^T * Q = I)
        val qTranspose = q.transpose()
        val qTq = qTranspose * q
        val identity = SparseMatrix.identity(q.rows)

        val qTqDense = qTq.toDenseMatrix()
        for (i in 0 until qTqDense.rows) {
            val row = (0 until qTqDense.cols).map { j -> qTqDense[i, j] }.joinToString()
        }

        // Check that Q^T * Q is approximately identity
        for (i in 0 until q.rows) {
            for (j in 0 until q.cols) {
                assertEquals(identity[i, j], qTq[i, j], 1e-10)
            }
        }

        // Verify R is upper triangular
        for (i in 0 until r.rows) {
            for (j in 0 until r.cols) {
                if (i > j) {
                    assertEquals(0.0, abs(r[i, j]), 1e-10)
                }
            }
        }

        // Verify A = Q * R
        val qr = q * r
        
        val qrDense = qr.toDenseMatrix()
        for (i in 0 until qrDense.rows) {
            val row = (0 until qrDense.cols).map { j -> qrDense[i, j] }.joinToString()
        }

        // Check that A = Q * R
        for (i in 0 until sparseMatrix.rows) {
            for (j in 0 until sparseMatrix.cols) {
                assertEquals(sparseMatrix[i, j], qr[i, j], 1e-10)
            }
        }
    }

    @Test
    fun testSolveWithQRDecomposition() {
        // Create a sparse matrix A
        // This will create the following matrix:
        // 4.0, 3.0, 0.0
        // 6.0, 3.0, 0.0
        // 0.0, 1.0, 5.0
        val valuesA = doubleArrayOf(4.0, 3.0, 6.0, 3.0, 1.0, 5.0)
        val columnIndicesA = intArrayOf(0, 1, 0, 1, 1, 2)
        val rowPointersA = intArrayOf(0, 2, 4, 6)
        val matrixA = SparseMatrix(3, 3, valuesA, columnIndicesA, rowPointersA)

        // Create a sparse matrix b (right-hand side)
        // This will create the following vector:
        // 1.0
        // 2.0
        // 3.0
        val valuesB = doubleArrayOf(1.0, 2.0, 3.0)
        val columnIndicesB = intArrayOf(0, 0, 0)
        val rowPointersB = intArrayOf(0, 1, 2, 3)
        val matrixB = SparseMatrix(3, 1, valuesB, columnIndicesB, rowPointersB)

        // Perform QR decomposition
        val qr = qrDecomposition(matrixA)

        // Solve the system Ax = b
        val x = solveQR(qr, matrixB)


        // Verify A * x = b
        val product = matrixA * x

        // Check that A * x = b
        for (i in 0 until matrixB.rows) {
            for (j in 0 until matrixB.cols) {
                assertEquals(matrixB[i, j], product[i, j], 1e-10)
            }
        }
    }
}