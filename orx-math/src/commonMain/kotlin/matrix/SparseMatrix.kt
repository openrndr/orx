package org.openrndr.extra.math.matrix

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Counts the number of non-zero elements in a matrix.
 *
 * @param matrix The matrix to count non-zero elements in.
 * @return The number of non-zero elements.
 */
private fun countNonZeroElements(matrix: Matrix): Int {
    var count = 0
    for (i in 0 until matrix.rows) {
        for (j in 0 until matrix.cols) {
            if (matrix[i, j] != 0.0) {
                count++
            }
        }
    }
    return count
}

/**
 * Represents a sparse matrix using the Compressed Sparse Row (CSR) format.
 *
 * The CSR format stores a sparse matrix using three arrays:
 * - values: stores the non-zero values of the matrix
 * - columnIndices: stores the column indices of the non-zero values
 * - rowPointers: stores the starting position of each row in the values array
 *
 * This format is memory-efficient for sparse matrices where most elements are zero.
 *
 * @property rows The number of rows in the matrix.
 * @property cols The number of columns in the matrix.
 * @property values Array containing the non-zero values of the matrix.
 * @property columnIndices Array containing the column indices of the non-zero values.
 * @property rowPointers Array containing the starting position of each row in the values array.
 */
class SparseMatrix(
    val rows: Int,
    val cols: Int,
    val values: DoubleArray,
    val columnIndices: IntArray,
    val rowPointers: IntArray
) {


    fun row(i: Int): DoubleArray {
        val ret = DoubleArray(cols)
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size
        for (k in start until end) {
            ret[columnIndices[k]] = values[k]
        }

        return ret
    }


    /**
     * Gets the value at the specified position in the matrix.
     *
     * @param i The row index.
     * @param j The column index.
     * @return The value at position (i, j).
     */
    operator fun get(i: Int, j: Int): Double {
        if (i < 0 || i >= rows || j < 0 || j >= cols) {
            throw IndexOutOfBoundsException("Index out of bounds: ($i, $j)")
        }

        // Search for the value in the CSR format
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size

        for (k in start until end) {
            if (columnIndices[k] == j) {
                return values[k]
            }
        }

        // If no value is found, return 0 (sparse matrix default)
        return 0.0
    }


    /**
     * Creates a dense Matrix representation of this sparse matrix.
     *
     * @return A dense Matrix with the same values as this sparse matrix.
     */
    fun toDenseMatrix(): Matrix {
        val result = Matrix.zeros(rows, cols)

        for (i in 0 until rows) {
            val start = rowPointers[i]
            val end = if (i < rows - 1) rowPointers[i + 1] else values.size

            for (k in start until end) {
                val j = columnIndices[k]
                result[i, j] = values[k]
            }
        }

        return result
    }


    /**
     * Multiplies this sparse matrix with another sparse matrix and returns the resulting sparse matrix.
     *
     * @param other The sparse matrix to multiply with this sparse matrix.
     * @return The resulting sparse matrix after multiplication.
     * @throws IllegalArgumentException If the number of columns in this matrix does not match the number of rows in the other matrix.
     */
    operator fun times(other: SparseMatrix): SparseMatrix {
        if (cols != other.rows) {
            throw IllegalArgumentException("Matrix dimensions don't match for multiplication")
        }

        // Create a temporary dense matrix to store the result
        // We'll convert it to sparse at the end
        val tempResult = Matrix.zeros(rows, other.cols)

        // Perform the multiplication
        for (i in 0 until rows) {
            val rowStart = rowPointers[i]
            val rowEnd = if (i < rows - 1) rowPointers[i + 1] else values.size

            for (k in rowStart until rowEnd) {
                val col = columnIndices[k]
                val value = values[k]

                // For each non-zero element in the other matrix's row 'col'
                val otherRowStart = other.rowPointers[col]
                val otherRowEnd = if (col < other.rows - 1) other.rowPointers[col + 1] else other.values.size

                for (l in otherRowStart until otherRowEnd) {
                    val j = other.columnIndices[l]
                    val otherValue = other.values[l]

                    // Accumulate the product
                    tempResult[i, j] += value * otherValue
                }
            }
        }

        // Convert the result to a sparse matrix
        return fromMatrix(tempResult)
    }


    /**
     * Multiplies this sparse matrix by a scalar value.
     *
     * @param scalar The scalar value to multiply by.
     * @return A new sparse matrix with all elements multiplied by the scalar.
     */
    operator fun times(scalar: Double): SparseMatrix {
        val newValues = DoubleArray(values.size) { i -> values[i] * scalar }
        return SparseMatrix(rows, cols, newValues, columnIndices.copyOf(), rowPointers.copyOf())
    }


    /**
     * Extension function to add two SparseMatrices.
     *
     * @param other The SparseMatrix to add.
     * @return A new Matrix that is the sum of the two matrices.
     * @throws IllegalArgumentException If the dimensions of the matrices don't match.
     */
    operator fun plus(other: SparseMatrix): SparseMatrix {
        if (rows != other.rows || cols != other.cols) {
            throw IllegalArgumentException("Matrix dimensions don't match for addition")
        }

        // Count non-zero elements in result
        var nonZeroCount = 0
        val countMap = mutableMapOf<Int, MutableMap<Int, Double>>()

        // Process this matrix's non-zeros
        for (i in 0 until rows) {
            val start = rowPointers[i]
            val end = if (i < rows - 1) rowPointers[i + 1] else values.size

            for (k in start until end) {
                val j = columnIndices[k]
                val value = values[k]
                if (!countMap.containsKey(i)) {
                    countMap[i] = mutableMapOf()
                }
                countMap[i]?.put(j, value)
                nonZeroCount++
            }
        }

        // Add other matrix's non-zeros 
        for (i in 0 until other.rows) {
            val start = other.rowPointers[i]
            val end = if (i < other.rows - 1) other.rowPointers[i + 1] else other.values.size

            for (k in start until end) {
                val j = other.columnIndices[k]
                val value = other.values[k]

                if (countMap.containsKey(i) && countMap[i]?.containsKey(j) == true) {
                    // Update existing value
                    val newValue = countMap[i]?.get(j)!! + value
                    countMap[i]?.put(j, newValue)
                } else {
                    // Add new value
                    if (!countMap.containsKey(i)) {
                        countMap[i] = mutableMapOf()
                    }
                    countMap[i]?.put(j, value)
                    nonZeroCount++
                }
            }
        }

        // Create result arrays
        val resultValues = DoubleArray(nonZeroCount)
        val resultColIndices = IntArray(nonZeroCount)
        val resultRowPointers = IntArray(rows + 1)

        var valueIndex = 0
        resultRowPointers[0] = 0

        for (i in 0 until rows) {
            val rowMap = countMap[i]
            if (rowMap != null) {
                for ((j, value) in rowMap.entries.sortedBy { it.key }) {
                    resultValues[valueIndex] = value
                    resultColIndices[valueIndex] = j
                    valueIndex++
                }
            }
            resultRowPointers[i + 1] = valueIndex
        }

        return SparseMatrix(rows, cols, resultValues, resultColIndices, resultRowPointers)
    }

    /**
     * Extension function to subtract a SparseMatrix from another SparseMatrix.
     *
     * @param other The SparseMatrix to subtract.
     * @return A new Matrix that is the difference of the two matrices.
     * @throws IllegalArgumentException If the dimensions of the matrices don't match.
     */
    operator fun minus(other: SparseMatrix): SparseMatrix {
        if (rows != other.rows || cols != other.cols) {
            throw IllegalArgumentException("Matrix dimensions don't match for subtraction")
        }


        // Count non-zero elements in result
        var nonZeroCount = 0
        val countMap = mutableMapOf<Int, MutableMap<Int, Double>>()

        // Process this matrix's non-zeros
        for (i in 0 until rows) {
            val start = rowPointers[i]
            val end = if (i < rows - 1) rowPointers[i + 1] else values.size

            for (k in start until end) {
                val j = columnIndices[k]
                val value = values[k]
                if (!countMap.containsKey(i)) {
                    countMap[i] = mutableMapOf()
                }
                countMap[i]?.put(j, value)
                nonZeroCount++
            }
        }

        // Subtract other matrix's non-zeros
        for (i in 0 until other.rows) {
            val start = other.rowPointers[i]
            val end = if (i < other.rows - 1) other.rowPointers[i + 1] else other.values.size

            for (k in start until end) {
                val j = other.columnIndices[k]
                val value = other.values[k]

                if (countMap.containsKey(i) && countMap[i]?.containsKey(j) == true) {
                    // Update existing value
                    val newValue = countMap[i]?.get(j)!! - value
                    if (newValue != 0.0) {
                        countMap[i]?.put(j, newValue)
                    } else {
                        countMap[i]?.remove(j)
                        if (countMap[i]?.isEmpty() == true) {
                            countMap.remove(i)
                        }
                        nonZeroCount--
                    }
                } else {
                    // Add new negative value
                    if (!countMap.containsKey(i)) {
                        countMap[i] = mutableMapOf()
                    }
                    countMap[i]?.put(j, -value)
                    nonZeroCount++
                }
            }
        }

        // Create result arrays
        val resultValues = DoubleArray(nonZeroCount)
        val resultColIndices = IntArray(nonZeroCount)
        val resultRowPointers = IntArray(rows + 1)

        var valueIndex = 0
        resultRowPointers[0] = 0

        for (i in 0 until rows) {
            val rowMap = countMap[i]
            if (rowMap != null) {
                for ((j, value) in rowMap.entries.sortedBy { it.key }) {
                    resultValues[valueIndex] = value
                    resultColIndices[valueIndex] = j
                    valueIndex++
                }
            }
            resultRowPointers[i + 1] = valueIndex
        }

        return SparseMatrix(rows, cols, resultValues, resultColIndices, resultRowPointers)
    }

    /**
     * Creates a copy of this sparse matrix.
     *
     * @return A new sparse matrix with the same values as this matrix.
     */
    fun copy(): SparseMatrix {
        return SparseMatrix(
            rows,
            cols,
            values.copyOf(),
            columnIndices.copyOf(),
            rowPointers.copyOf()
        )
    }

    /**
     * Checks if this sparse matrix is symmetric within a specified tolerance.
     *
     * A matrix is considered symmetric if it is square (same number of rows and columns)
     * and satisfies the condition matrix[i, j] == matrix[j, i] for all i and j, within
     * the given tolerance.
     *
     * @param tolerance The maximum allowable difference between matrix[i, j] and matrix[j, i]
     *                  to still consider the matrix symmetric. Default is 1e-10.
     * @return `true` if the matrix is symmetric within the specified tolerance, `false` otherwise.
     */
    fun isSymmetric(tolerance: Double = 1e-10): Boolean {
        if (rows != cols) return false

        // Convert to dense matrix for simplicity
        // A more optimized implementation would check directly in the sparse format
        val dense = toDenseMatrix()

        for (i in 0 until rows) {
            for (j in 0 until i) { // Only check lower triangle
                if (abs(dense[i, j] - dense[j, i]) > tolerance) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Returns the number of non-zero elements in this sparse matrix.
     *
     * @return The number of non-zero elements.
     */
    fun nonZeroCount(): Int {
        return values.size
    }

    /**
     * Returns the density of this sparse matrix (ratio of non-zero elements to total elements).
     *
     * @return The density as a value between 0 and 1.
     */
    fun density(): Double {
        return values.size.toDouble() / (rows * cols)
    }

    /**
     * Creates a transposed version of this sparse matrix.
     *
     * @return A new SparseMatrix that is the transpose of this matrix.
     */
    fun transpose(): SparseMatrix {
        // Count non-zero elements per column for creating row pointers in transposed matrix
        val colCounts = IntArray(cols)
        for (k in columnIndices.indices) {
            colCounts[columnIndices[k]]++
        }

        // Create row pointers for transposed matrix
        val newRowPointers = IntArray(cols + 1)
        newRowPointers[0] = 0
        for (j in 0 until cols) {
            newRowPointers[j + 1] = newRowPointers[j] + colCounts[j]
        }

        val newValues = DoubleArray(values.size)
        val newColumnIndices = IntArray(values.size)

        // Keep track of current position in each column
        val colPositions = newRowPointers.copyOf()

        // Fill in values and column indices for transposed matrix
        for (i in 0 until rows) {
            val start = rowPointers[i]
            val end = if (i < rows - 1) rowPointers[i + 1] else values.size

            for (k in start until end) {
                val j = columnIndices[k]
                val pos = colPositions[j]
                newValues[pos] = values[k]
                newColumnIndices[pos] = i
                colPositions[j]++
            }
        }

        return SparseMatrix(cols, rows, newValues, newColumnIndices, newRowPointers)
    }

    companion object {
        /**
         * Creates a sparse matrix from a dense matrix.
         *
         * @param matrix The dense matrix to convert.
         * @return A sparse matrix representation of the input matrix.
         */
        fun fromMatrix(matrix: Matrix): SparseMatrix {
            val nonZeroCount = countNonZeroElements(matrix)

            val values = DoubleArray(nonZeroCount)
            val columnIndices = IntArray(nonZeroCount)
            val rowPointers = IntArray(matrix.rows + 1)

            var valueIndex = 0
            rowPointers[0] = 0

            for (i in 0 until matrix.rows) {
                for (j in 0 until matrix.cols) {
                    val value = matrix[i, j]
                    if (value != 0.0) {
                        values[valueIndex] = value
                        columnIndices[valueIndex] = j
                        valueIndex++
                    }
                }
                rowPointers[i + 1] = valueIndex
            }

            return SparseMatrix(matrix.rows, matrix.cols, values, columnIndices, rowPointers)
        }

        /**
         * Creates a sparse identity matrix of the specified size.
         *
         * @param n The size of the identity matrix (number of rows and columns).
         * @return A sparse identity matrix of size n x n.
         */
        fun identity(n: Int): SparseMatrix {
            val values = DoubleArray(n) { 1.0 }
            val columnIndices = IntArray(n) { it }
            val rowPointers = IntArray(n + 1) { it }

            return SparseMatrix(n, n, values, columnIndices, rowPointers)
        }

        /**
         * Creates a sparse zero matrix with the specified dimensions.
         *
         * @param rows The number of rows in the matrix.
         * @param cols The number of columns in the matrix.
         * @return A sparse zero matrix with dimensions [rows] x [cols].
         */
        fun zeros(rows: Int, cols: Int): SparseMatrix {
            // For a zero matrix, there are no non-zero elements
            val values = DoubleArray(0)
            val columnIndices = IntArray(0)
            val rowPointers = IntArray(rows + 1) { 0 }

            return SparseMatrix(rows, cols, values, columnIndices, rowPointers)
        }
    }
}

/**
 * Extension function to convert a dense Matrix to a SparseMatrix.
 *
 * @return A sparse matrix representation of this dense matrix.
 */
fun Matrix.toSparseMatrix(): SparseMatrix {
    return SparseMatrix.fromMatrix(this)
}


/**
 * Extension function to multiply a scalar by a SparseMatrix.
 *
 * @param matrix The SparseMatrix to multiply.
 * @return A new SparseMatrix with all elements multiplied by the scalar.
 */
operator fun Double.times(matrix: SparseMatrix): SparseMatrix {
    return matrix * this
}


/**
 * Extension function to add a Matrix to a SparseMatrix.
 *
 * @param other The SparseMatrix to add.
 * @return A new Matrix that is the sum of the two matrices.
 * @throws IllegalArgumentException If the dimensions of the matrices don't match.
 */
operator fun Matrix.plus(other: SparseMatrix): Matrix {
    if (rows != other.rows || cols != other.cols) {
        throw IllegalArgumentException("Matrix dimensions don't match for addition")
    }

    val result = this.copy()
    val otherDense = other.toDenseMatrix()

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i, j] += otherDense[i, j]
        }
    }

    return result
}

/**
 * Extension function to subtract a Matrix from a SparseMatrix.
 *
 * @param other The Matrix to subtract.
 * @return A new Matrix that is the difference of the two matrices.
 * @throws IllegalArgumentException If the dimensions of the matrices don't match.
 */
operator fun SparseMatrix.minus(other: Matrix): Matrix {
    if (rows != other.rows || cols != other.cols) {
        throw IllegalArgumentException("Matrix dimensions don't match for subtraction")
    }

    val result = toDenseMatrix()

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i, j] -= other[i, j]
        }
    }

    return result
}

/**
 * Adds this sparse matrix to another matrix and returns the resulting matrix.
 *
 * @param other The matrix to add to this sparse matrix.
 * @return The resulting matrix after addition.
 * @throws IllegalArgumentException If the dimensions of the matrices don't match.
 */
operator fun SparseMatrix.plus(other: Matrix): Matrix {
    if (rows != other.rows || cols != other.cols) {
        throw IllegalArgumentException("Matrix dimensions don't match for addition")
    }

    // Convert to dense matrix for simplicity
    val result = toDenseMatrix()

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i, j] += other[i, j]
        }
    }

    return result
}

/**
 * Extension function to subtract a SparseMatrix from a Matrix.
 *
 * @param other The SparseMatrix to subtract.
 * @return A new Matrix that is the difference of the two matrices.
 * @throws IllegalArgumentException If the dimensions of the matrices don't match.
 */
operator fun Matrix.minus(other: SparseMatrix): Matrix {
    if (rows != other.rows || cols != other.cols) {
        throw IllegalArgumentException("Matrix dimensions don't match for subtraction")
    }

    val result = this.copy()
    val otherDense = other.toDenseMatrix()

    for (i in 0 until rows) {
        for (j in 0 until cols) {
            result[i, j] -= otherDense[i, j]
        }
    }

    return result
}


/**
 * Multiplies this sparse matrix with another matrix and returns the resulting matrix.
 *
 * @param other The matrix to multiply with this sparse matrix.
 * @return The resulting matrix after multiplication.
 * @throws IllegalArgumentException If the number of columns in this matrix does not match the number of rows in the other matrix.
 */
operator fun SparseMatrix.times(other: Matrix): Matrix {
    if (cols != other.rows) {
        throw IllegalArgumentException("Matrix dimensions don't match for multiplication")
    }

    val result = Matrix.zeros(rows, other.cols)

    for (i in 0 until rows) {
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size

        for (j in 0 until other.cols) {
            var sum = 0.0
            for (k in start until end) {
                val col = columnIndices[k]
                sum += values[k] * other[col, j]
            }
            result[i, j] = sum
        }
    }

    return result
}

/**
 * Constructs a SparseMatrix instance with the specified number of rows, columns, and non-zero values.
 *
 * @param rows The number of rows in the matrix.
 * @param cols The number of columns in the matrix.
 * @param values A map containing the non-zero values and their positions, where the key is a pair of row and column indices, and the value is the corresponding matrix value.
 * @return A new SparseMatrix instance with the given dimensions and non-zero values.
 */
fun SparseMatrix(rows: Int, cols: Int, values: Map<Pair<Int, Int>, Double>): SparseMatrix {
    val indices = values.entries.map { it.key }
    val values = values.entries.map { it.value }
    return SparseMatrix(rows, cols, indices, values, true)
}

/**
 * Constructs a sparse matrix in Compressed Sparse Row (CSR) format.
 *
 * @param rows The number of rows in the matrix.
 * @param cols The number of columns in the matrix.
 * @param indices A list of pairs representing the row and column indices of non-zero elements.
 * @param values A list of values corresponding to the non-zero elements at the specified indices.
 * @param presorted Indicates whether the indices are already sorted by row and column. If set to `false`, the indices
 *        will be sorted internally. Defaults to `false`.
 * @return The constructed `SparseMatrix` object in CSR format.
 * @throws IllegalArgumentException If the number of indices does not match the number of values, or if the indices list is empty.
 */

fun SparseMatrix(
    rows: Int,
    cols: Int,
    indices: List<Pair<Int, Int>>,
    values: List<Double>,
    presorted: Boolean = true
): SparseMatrix {
    if (indices.size != values.size) {
        throw IllegalArgumentException("Number of indices must match number of values")
    }
    if (indices.isEmpty()) {
        throw IllegalArgumentException("Cannot create sparse matrix from empty data")
    }

    // Sort entries by row, then column
    val sortedEntries = indices.zip(values).let {
        if (presorted) it else it.sortedWith(compareBy({ it.first.first }, { it.first.second }))
    }

    // Create arrays for CSR format
    val resultValues = DoubleArray(values.size)
    val resultColIndices = IntArray(values.size)
    val resultRowPointers = IntArray(rows + 1)

    // Fill in values and column indices
    for (i in sortedEntries.indices) {
        val entry = sortedEntries[i]
        resultValues[i] = entry.second
        resultColIndices[i] = entry.first.second
    }

    // Create row pointers
    var currentRow = 0
    resultRowPointers[0] = 0
    for (i in sortedEntries.indices) {
        val row = sortedEntries[i].first.first
        while (currentRow < row) {
            currentRow++
            resultRowPointers[currentRow] = i
        }
    }
    while (currentRow < rows) {
        currentRow++
        resultRowPointers[currentRow] = sortedEntries.size
    }

    return SparseMatrix(rows, cols, resultValues, resultColIndices, resultRowPointers)
}

fun SparseMatrix.checkIntegrity() : SparseMatrix {
    for (i in 0 until rows) {
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size

        val indices = (start until end).map {
            columnIndices[it]
        }
        require(indices.size == indices.distinct().size) { "Duplicate indices found in row $i" }
    }
    return this
}

/**
 * Calculates the Frobenius norm of the sparse matrix.
 * The Frobenius norm is the square root of the sum of the squares
 * of all the non-zero elements in the matrix.
 *
 * @return The Frobenius norm as a Double value.
 * @see
 */
fun SparseMatrix.frobeniusNorm() : Double {
    var sum = 0.0
    for (i in 0 until rows) {
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size
        for (j in start until end) {
            sum += values[j] * values[j]
        }
    }
    return sqrt(sum)
}