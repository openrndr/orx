package org.openrndr.extra.math.matrix

import org.openrndr.math.Vector2

/**
 * Constructs a Hankel-like matrix from the invoking list of `Vector2` objects.
 *
 * Each row in the resulting matrix represents sequential values from the list,
 * organized into a window of a specified size. If the `closed` parameter is 
 * set to `true`, the indexing wraps around circularly to use elements 
 * beyond the boundaries of the list.
 * 
 * A Hankel matrix is a matrix where each ascending anti-diagonal from left to right
 * is constant. In other words, elements along each anti-diagonal share the same value.
 * In the classic form, M[i,j] = M[i+1,j-1] for a traditional Hankel matrix.
 * This function creates a "Hankel-like" structure by placing sequential elements from
 * the input list along rows with a sliding window, where each row is shifted by one
 * position relative to the previous row, creating the characteristic anti-diagonal pattern.
 * 
 * An anti-diagonal (also called a skew diagonal or counter diagonal) is a line of elements
 * that runs from the top-right to bottom-left of a matrix, perpendicular to the main diagonal.
 * For example, in a 3x3 matrix, the main diagonal contains elements at positions (0,0), (1,1), (2,2),
 * while anti-diagonals run in the opposite direction: one anti-diagonal might contain (0,2), (1,1), (2,0).
 * In general, elements on the same anti-diagonal satisfy i+j = constant.
 * 
 *
 * @param window The window size determining the number of consecutive elements 
 *               to include in each row of the resulting matrix. Must be greater than zero.
 * @param closed A flag indicating whether the list should be treated as circular. 
 *               If `true`, indexing wraps around at the boundaries; otherwise, it doesn't.
 * @return A `Matrix` containing the Hankel-like arrangement of the elements 
 *         from the list. The matrix is of size `2n x window` 
 *         where `n` refers to the effective number of rows 
 *         (original list size for circular, otherwise depends on window size).
 * @throws IllegalArgumentException If the list is empty or if the window size is not greater than zero.
 */
fun List<Vector2>.hankel(window: Int, closed: Boolean): Matrix {
    require(isNotEmpty()) {
        "List must not be empty"
    }
    require(window > 0) {
        "Window must be greater than zero (=$window)"
    }


    if (closed) {
        val n = size
        val hankelMatrix = Matrix(2 * n, window)
        for (i in 0 until n) {
            for (j in 0 until window) {
                hankelMatrix[i, j] = this[(i + j).mod(size)].x
                hankelMatrix[i + n, j] = this[(i + j).mod(size)].y
            }
        }
        return hankelMatrix
    } else {
        val n = size - (window - 1)
        val hankelMatrix = Matrix(2 * n, window)
        for (i in 0 until n) {
            for (j in 0 until window) {
                hankelMatrix[i, j] = this[i + j].x
                hankelMatrix[i + n, j] = this[i + j].y
            }
        }
        return hankelMatrix
    }
}

/**
 * Computes a list of 2D vectors from the given matrix by averaging values in paired rows.
 * The matrix must have an even number of rows, positive dimensions, and follows a Hankel-like
 * structure for computation.
 *
 * @param closed Determines whether the computation should wrap around circularly. If set to `true`,
 *               the computation treats the ends of the row pairs as connected for averaging.
 *               Defaults to `false`.
 * @return A list of [Vector2] instances, where each vector is computed from the averaged values
 *         of paired rows in the matrix, considering the specified wrapping behavior.
 * @throws IllegalArgumentException If the number of rows is not even, or if the matrix dimensions are not positive.
 */

fun Matrix.dehankelVector2(closed: Boolean = false): List<Vector2> {
    require(rows % 2 == 0) {
        "Matrix rows must be even (=$rows)"
    }
    require(rows > 0 && cols > 0) {
        "Matrix dimensions must be positive (rows=$rows, cols=$cols)"
    }
    val n = rows / 2
    val window = cols
    val size = if (closed) n else n + window - 1
    val sumX = DoubleArray(size)
    val sumY = DoubleArray(size)
    val count = IntArray(size)

    if (closed) {
        for (i in 0 until n) {
            for (j in 0 until window) {
                val k = (i + j).mod(n)
                sumX[k] += this[i, j]
                sumY[k] += this[i + n, j]
                count[k] += 1
            }
        }
    } else {
        for (i in 0 until n) {
            for (j in 0 until window) {
                val k = i + j
                sumX[k] += this[i, j]
                sumY[k] += this[i + n, j]
                count[k] += 1
            }
        }
    }

    return List(size) { i ->
        Vector2(sumX[i] / count[i], sumY[i] / count[i])
    }
}