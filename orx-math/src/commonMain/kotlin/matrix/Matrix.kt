package org.openrndr.extra.math.matrix

import kotlinx.serialization.Serializable

/**
 * Represents a two-dimensional matrix with support for basic operations such as indexing,
 * copying, and mathematical computations.
 *
 * @property rows The number of rows in the matrix.
 * @property cols The number of columns in the matrix.
 */
@Serializable
class Matrix(val rows: Int, val cols: Int) {
    val data = Array(rows) { DoubleArray(cols) }

    val allRows: IntRange get() = 0..<rows
    val allColumns: IntRange get() = 0..<cols

    operator fun get(row: Int, columnRange: IntRange): Matrix {
        return get(row..row, columnRange)
    }

    operator fun get(rowRange: IntRange, column: Int): Matrix {
        return get(rowRange, column..column)
    }

    operator fun get(rowRange: IntRange, columnRange: IntRange): Matrix {
        val result = Matrix(rowRange.count(), columnRange.count())
        for (i in rowRange) {
            for (j in columnRange) {
                result[i - rowRange.first, j - columnRange.first] = this[i, j]
            }
        }
        return result
    }

    operator fun get(row: Int, column: Int) = data[row][column]


    /**
     * Set value
     *
     * Usage example:
     * ```
     * m[row, column] = value
     * ```
     */
    operator fun set(row: Int, column: Int, value: Double) {
        data[row][column] = value
    }

    fun copy(): Matrix {
        val result = Matrix(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i, j] = this[i, j]
            }
        }
        return result
    }

    /**
     * Returns a new matrix that is the transpose of this matrix.
     *
     * The transpose of a matrix is obtained by flipping the matrix over its diagonal,
     * effectively switching the row and column indices of each element.
     *
     * @return A new matrix representing the transpose of the current matrix.
     */
    fun transposed(): Matrix {
        val result = Matrix(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j, i] = this[i, j]
            }
        }
        return result
    }

    /**
     * Checks if the given matrix is symmetric within a specified tolerance.
     *
     * A matrix is considered symmetric if it is square (same number of rows and columns)
     * and satisfies the condition matrix[i, j] == matrix[j, i] for all i and j, within
     * the given tolerance.
     *
     * @param matrix The matrix to be checked for symmetry.
     * @param tolerance The maximum allowable difference between matrix[i, j] and matrix[j, i]
     *                  to still consider the matrix symmetric. Default is 1e-10.
     * @return `true` if the matrix is symmetric within the specified tolerance, `false` otherwise.
     */
    fun isSymmetric(matrix: Matrix, tolerance: Double = 1e-10): Boolean {
        if (matrix.rows != matrix.cols) return false

        for (i in 0 until matrix.rows) {
            for (j in 0 until matrix.cols) {
                if (kotlin.math.abs(matrix[i, j] - matrix[j, i]) > tolerance) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        /**
         * Generates an identity matrix of the specified size.
         *
         * An identity matrix is a square matrix with ones on the diagonal
         * and zeros elsewhere.
         *
         * @param n The size of the identity matrix (number of rows and columns).
         *          Must be a positive integer.
         * @return A square matrix of size n x n, where all diagonal elements are 1
         *         and all non-diagonal elements are 0.
         */
        fun identity(n: Int): Matrix {
            val result = Matrix(n, n)
            for (i in 0 until n) {
                result[i, i] = 1.0
            }
            return result
        }

        /**
         * Creates a new matrix with the specified number of rows and columns, filled with zeros.
         *
         * @param rows The number of rows in the matrix.
         * @param cols The number of columns in the matrix.
         * @return A matrix with dimensions [rows] x [cols], initialized to zero.
         */
        fun zeros(rows: Int, cols: Int): Matrix = Matrix(rows, cols)

        fun zerosLike(matrix: Matrix): Matrix = zeros(matrix.rows, matrix.cols)
    }


    /**
     * Multiplies two matrices and returns the resulting matrix.
     *
     * @param a The first matrix to be multiplied.
     * @param b The second matrix to be multiplied.
     * @return The resulting matrix after multiplying A and B.
     * @throws IllegalArgumentException If the number of columns in A does not match the number of rows in B.
     */
    fun multiply(a: Matrix, b: Matrix): Matrix {

        if (a.rows == b.cols && b.rows == 1 && a.cols != 1) {
            println("a: ${a.rows}x${a.cols}, b: ${b.rows}x${b.cols}")
            return Matrix(a.rows, b.cols) { i, j ->
                a[i, j] * b[0, i]

            }
        } else if (a.cols == b.cols && b.rows == 1) {
            return Matrix(a.rows, b.cols) { i, j ->
                a[i, j] * b[0, j]

            }
        }

        else {

            if (a.cols != b.rows) {
                throw IllegalArgumentException("Matrix dimensions don't match for multiplication")
            }

            val result = Matrix.zeros(a.rows, b.cols)
            for (i in 0 until a.rows) {
                for (j in 0 until b.cols) {
                    for (k in 0 until a.cols) {
                        result[i, j] += a[i, k] * b[k, j]
                    }
                }
            }
            return result
        }
    }

    /**
     * Multiplies this matrix with another matrix and returns the resulting matrix.
     *
     * @param other The matrix to multiply with this matrix.
     * @return The resulting matrix after multiplication.
     * @throws IllegalArgumentException If the number of columns in this matrix does not match the number of rows in the other matrix.
     */
    operator fun times(other: Matrix): Matrix {
        return multiply(this, other)
    }

    operator fun times(scale: Double): Matrix {
        return Matrix(rows, cols) { j, i -> this[j, i] * scale }
    }

}

/**
 * Calculates the mean of each column in the matrix and returns the result as a new single-row matrix.
 *
 * The mean is calculated by summing all elements in each column and dividing by the number of rows.
 *
 * @return A row matrix containing the mean of each column of the input matrix.
 */
fun Matrix.columnMean(): Matrix {
    val means = DoubleArray(cols)

    for (j in 0 until rows) {
        for (i in 0 until cols) {
            means[i] += this[j, i]
        }
    }

    for (i in 0 until cols) {
        means[i] /= rows.toDouble()
    }

    val result = Matrix.zeros(1, cols)
    for (i in 0 until cols) {
        result[0, i] = means[i]
    }
    return result
}

fun Matrix(rows: Int, cols: Int, init: (Int, Int) -> Double): Matrix {
    val m = Matrix(rows, cols)
    for (j in 0 until rows) {
        for (i in 0 until cols) {
            m[j, i] = init(j, i)
        }
    }
    return m
}


operator fun Matrix.minus(other: Matrix): Matrix {
    val result = Matrix.zeros(rows, cols)
    if (cols == other.cols && other.rows == 1) {
        for (j in 0 until rows) {
            for (i in 0 until cols) {
                result[j, i] = this[j, i] - other[0, i]
            }
        }
    } else if (cols == other.cols && rows == other.rows) {
        for (j in 0 until rows) {
            for (i in 0 until cols) {
                result[j, i] = this[j, i] - other[j, i]
            }
        }
    } else {
        error("Cannot subtract matrices of different dimensions")
    }
    return result
}

operator fun Matrix.plus(other: Matrix): Matrix {
    val result = Matrix.zeros(rows, cols)
    if (cols == other.cols && other.rows == 1) {
        for (j in 0 until rows) {
            for (i in 0 until cols) {
                result[j, i] = this[j, i] + other[0, i]
            }
        }
    } else if (cols == other.cols && rows == other.rows) {
        for (j in 0 until rows) {
            for (i in 0 until cols) {
                result[j, i] = this[j, i] + other[j, i]
            }
        }
    } else {
        error("Cannot subtract matrices of different dimensions")
    }
    return result
}

/**
 * Horizontally stacks a list of matrices into a single matrix.
 *
 * This function combines multiple matrices in the list by aligning them side by side,
 * effectively creating a single matrix that includes all input matrices as column blocks.
 *
 * The resulting matrix will have the same number of rows as the matrices in the list,
 * and the total number of columns will be the sum of the columns of all matrices in the list.
 *
 * Preconditions:
 * - The list must not be empty.
 * - All matrices in the list must have the same number of rows.
 *
 * @throws IllegalArgumentException If the list is empty.
 * @throws IllegalArgumentException If the matrices in the list have different row dimensions.
 */
fun List<Matrix>.hstack() {
    require(isNotEmpty()) {
        "Cannot hstack empty list"
    }

    require(all { it.rows == first().rows }) {
        "Cannot hstack matrices of different row dimensions"
    }

    val result = Matrix.zeros(first().rows, sumOf { it.cols })
    var col = 0
    for (m in this) {
        for (j in 0 until m.rows) {
            for (i in 0 until m.cols) {
                result[j, col + i] = m[j, i]
            }
        }
        col += m.cols
    }
}

/**
 * Vertically stacks a list of matrices into a single matrix.
 *
 * This function combines all matrices in the list by aligning them vertically.
 * Each matrix is appended below the previous one in the stack, forming a new
 * matrix where the number of rows is the sum of all the rows in the input matrices,
 * and the number of columns matches the column count of the first matrix in the list.
 *
 * Throws an exception in the following cases:
 * - If the list is empty.
 * - If the matrices in the list have different column dimensions.
 *
 * @receiver A list of matrices to be vertically stacked.
 * @throws IllegalArgumentException If the list is empty.
 * @throws IllegalArgumentException If the matrices have mismatched column dimensions.
 */
fun List<Matrix>.vstack() {
    require(isNotEmpty()) {
        "Cannot vstack empty list"
    }
    require(all { it.cols == first().cols }) {
        "Cannot vstack matrices of different column dimensions"
    }
    val result = Matrix.zeros(sumOf { it.rows }, first().cols)
    var row = 0
    for (m in this) {
        for (j in 0 until m.rows) {
            for (i in 0 until m.cols) {
                result[row + j, i] = m[j, i]
            }
        }
        row += m.rows
    }
}