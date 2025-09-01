package matrix

import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.SparseMatrix
import org.openrndr.extra.math.matrix.checkIntegrity
import org.openrndr.extra.math.matrix.toSparseMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SparseMatrixTest {

    @Test
    fun testCreateSparseMatrix() {
        // Create a sparse matrix directly
        val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val columnIndices = intArrayOf(0, 2, 1, 2)
        val rowPointers = intArrayOf(0, 2, 3, 4)
        val sparseMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)

        // Verify dimensions
        assertEquals(3, sparseMatrix.rows)
        assertEquals(3, sparseMatrix.cols)

        // Verify values
        assertEquals(1.0, sparseMatrix[0, 0])
        assertEquals(0.0, sparseMatrix[0, 1])
        assertEquals(2.0, sparseMatrix[0, 2])
        assertEquals(0.0, sparseMatrix[1, 0])
        assertEquals(3.0, sparseMatrix[1, 1])
        assertEquals(0.0, sparseMatrix[1, 2])
        assertEquals(0.0, sparseMatrix[2, 0])
        assertEquals(0.0, sparseMatrix[2, 1])
        assertEquals(4.0, sparseMatrix[2, 2])
    }

    @Test
    fun testFromDenseMatrix() {
        // Create a dense matrix
        val denseMatrix = Matrix(3, 3)
        denseMatrix[0, 0] = 1.0
        denseMatrix[0, 2] = 2.0
        denseMatrix[1, 1] = 3.0
        denseMatrix[2, 2] = 4.0

        // Convert to sparse matrix
        val sparseMatrix = denseMatrix.toSparseMatrix()

        // Verify dimensions
        assertEquals(3, sparseMatrix.rows)
        assertEquals(3, sparseMatrix.cols)

        // Verify values
        assertEquals(1.0, sparseMatrix[0, 0])
        assertEquals(0.0, sparseMatrix[0, 1])
        assertEquals(2.0, sparseMatrix[0, 2])
        assertEquals(0.0, sparseMatrix[1, 0])
        assertEquals(3.0, sparseMatrix[1, 1])
        assertEquals(0.0, sparseMatrix[1, 2])
        assertEquals(0.0, sparseMatrix[2, 0])
        assertEquals(0.0, sparseMatrix[2, 1])
        assertEquals(4.0, sparseMatrix[2, 2])

        // Verify non-zero count
        assertEquals(4, sparseMatrix.nonZeroCount())
    }

    @Test
    fun testToDenseMatrix() {
        // Create a sparse matrix
        val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val columnIndices = intArrayOf(0, 2, 1, 2)
        val rowPointers = intArrayOf(0, 2, 3, 4)
        val sparseMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)

        // Convert to dense matrix
        val denseMatrix = sparseMatrix.toDenseMatrix()

        // Verify dimensions
        assertEquals(3, denseMatrix.rows)
        assertEquals(3, denseMatrix.cols)

        // Verify values
        assertEquals(1.0, denseMatrix[0, 0])
        assertEquals(0.0, denseMatrix[0, 1])
        assertEquals(2.0, denseMatrix[0, 2])
        assertEquals(0.0, denseMatrix[1, 0])
        assertEquals(3.0, denseMatrix[1, 1])
        assertEquals(0.0, denseMatrix[1, 2])
        assertEquals(0.0, denseMatrix[2, 0])
        assertEquals(0.0, denseMatrix[2, 1])
        assertEquals(4.0, denseMatrix[2, 2])
    }

    @Test
    fun testMatrixMultiplication() {
        // Create a sparse matrix
        val values1 = doubleArrayOf(1.0, 2.0, 3.0)
        val columnIndices1 = intArrayOf(0, 1, 0)
        val rowPointers1 = intArrayOf(0, 2, 3)
        val sparseMatrix1 = SparseMatrix(2, 2, values1, columnIndices1, rowPointers1)

        // Create another sparse matrix
        val values2 = doubleArrayOf(4.0, 5.0, 6.0)
        val columnIndices2 = intArrayOf(0, 1, 1)
        val rowPointers2 = intArrayOf(0, 2, 3)
        val sparseMatrix2 = SparseMatrix(2, 2, values2, columnIndices2, rowPointers2)

        // Multiply sparse matrices
        val result = sparseMatrix1.times(sparseMatrix2)

        result.checkIntegrity()

        // Verify dimensions
        assertEquals(2, result.rows)
        assertEquals(2, result.cols)

        // Verify values (1*4 + 2*0 = 4, 1*5 + 2*6 = 17, 3*4 + 0*0 = 12, 3*5 + 0*6 = 15)
        assertEquals(4.0, result[0, 0])
        assertEquals(17.0, result[0, 1])
        assertEquals(12.0, result[1, 0])
        assertEquals(15.0, result[1, 1])
    }

    @Test
    fun testScalarMultiplication() {
        // Create a sparse matrix
        val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val columnIndices = intArrayOf(0, 2, 1, 2)
        val rowPointers = intArrayOf(0, 2, 3, 4)
        val sparseMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)

        // Multiply by scalar
        val result = sparseMatrix * 2.0

        // Verify dimensions
        assertEquals(3, result.rows)
        assertEquals(3, result.cols)

        // Verify values
        assertEquals(2.0, result[0, 0])
        assertEquals(0.0, result[0, 1])
        assertEquals(4.0, result[0, 2])
        assertEquals(0.0, result[1, 0])
        assertEquals(6.0, result[1, 1])
        assertEquals(0.0, result[1, 2])
        assertEquals(0.0, result[2, 0])
        assertEquals(0.0, result[2, 1])
        assertEquals(8.0, result[2, 2])
    }

    @Test
    fun testAddition() {
        // Create a sparse matrix
        val values1 = doubleArrayOf(1.0, 2.0, 3.0)
        val columnIndices1 = intArrayOf(0, 2, 1)
        val rowPointers1 = intArrayOf(0, 2, 3)
        val sparseMatrix1 = SparseMatrix(2, 3, values1, columnIndices1, rowPointers1)

        // Create another sparse matrix
        val values2 = doubleArrayOf(4.0, 5.0, 6.0)
        val columnIndices2 = intArrayOf(0, 1, 2)
        val rowPointers2 = intArrayOf(0, 2, 3)
        val sparseMatrix2 = SparseMatrix(2, 3, values2, columnIndices2, rowPointers2)

        // Add sparse matrices
        val result = sparseMatrix1 + sparseMatrix2
        result.checkIntegrity()

        // Verify dimensions
        assertEquals(2, result.rows)
        assertEquals(3, result.cols)

        // Verify values
        assertEquals(5.0, result[0, 0])
        assertEquals(5.0, result[0, 1])
        assertEquals(2.0, result[0, 2])
        assertEquals(0.0, result[1, 0])
        assertEquals(3.0, result[1, 1])
        assertEquals(6.0, result[1, 2])
    }

    @Test
    fun testSubtraction() {
        // Create a sparse matrix
        val values1 = doubleArrayOf(5.0, 7.0, 9.0)
        val columnIndices1 = intArrayOf(0, 2, 1)
        val rowPointers1 = intArrayOf(0, 2, 3)
        val sparseMatrix1 = SparseMatrix(2, 3, values1, columnIndices1, rowPointers1)

        // Create another sparse matrix
        val values2 = doubleArrayOf(1.0, 2.0, 3.0)
        val columnIndices2 = intArrayOf(0, 1, 2)
        val rowPointers2 = intArrayOf(0, 2, 3)
        val sparseMatrix2 = SparseMatrix(2, 3, values2, columnIndices2, rowPointers2)

        // Subtract sparse matrices
        val result = sparseMatrix1 - sparseMatrix2
        result.checkIntegrity()

        // Verify dimensions
        assertEquals(2, result.rows)
        assertEquals(3, result.cols)

        // Verify values
        assertEquals(4.0, result[0, 0])
        assertEquals(-2.0, result[0, 1])
        assertEquals(7.0, result[0, 2])
        assertEquals(0.0, result[1, 0])
        assertEquals(9.0, result[1, 1])
        assertEquals(-3.0, result[1, 2])
    }

    @Test
    fun testTranspose() {
        // Create a sparse matrix
        val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val columnIndices = intArrayOf(0, 2, 1, 2)
        val rowPointers = intArrayOf(0, 2, 3, 4)
        val sparseMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)
        sparseMatrix.checkIntegrity()

        // Transpose the matrix
        val transposed = sparseMatrix.transpose()

        // Verify dimensions
        assertEquals(3, transposed.rows)
        assertEquals(3, transposed.cols)

        // Verify values
        assertEquals(1.0, transposed[0, 0])
        assertEquals(0.0, transposed[0, 1])
        assertEquals(0.0, transposed[0, 2])
        assertEquals(0.0, transposed[1, 0])
        assertEquals(3.0, transposed[1, 1])
        assertEquals(0.0, transposed[1, 2])
        assertEquals(2.0, transposed[2, 0])
        assertEquals(0.0, transposed[2, 1])
        assertEquals(4.0, transposed[2, 2])
    }

    @Test
    fun testIsSymmetric() {
        // Create a symmetric sparse matrix
        // For a symmetric matrix, if matrix[i,j] = value, then matrix[j,i] = value
        // In CSR format, we need to ensure both positions have the same value

        // This will create the following symmetric matrix:
        // 1.0, 2.0, 3.0
        // 2.0, 4.0, 5.0
        // 3.0, 5.0, 6.0
        val values = doubleArrayOf(1.0, 2.0, 3.0, 2.0, 4.0, 5.0, 3.0, 5.0, 6.0)
        val columnIndices = intArrayOf(0, 1, 2, 0, 1, 2, 0, 1, 2)
        val rowPointers = intArrayOf(0, 3, 6)
        val symmetricMatrix = SparseMatrix(3, 3, values, columnIndices, rowPointers)

        // Verify it's symmetric
        assertTrue(symmetricMatrix.isSymmetric())

        // Create a non-symmetric sparse matrix
        val values2 = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        val columnIndices2 = intArrayOf(0, 2, 1, 2)
        val rowPointers2 = intArrayOf(0, 2, 3, 4)
        val nonSymmetricMatrix = SparseMatrix(3, 3, values2, columnIndices2, rowPointers2)

        // Verify it's not symmetric
        assertFalse(nonSymmetricMatrix.isSymmetric())
    }
}
