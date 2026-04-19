package org.openrndr.extra.math.matrix

import kotlin.math.abs

/**
 * Solves a system of linear equations Ax = B using Gaussian elimination with partial pivoting.
 *
 * @param a The coefficient matrix A.
 * @param b The right-hand side matrix B.
 * @return The solution matrix X such that Ax = B.
 * @throws IllegalArgumentException if the matrix is singular or not square.
 */
fun solveLinearSystem(a: Matrix, b: Matrix): Matrix {
    if (a.rows != a.cols) {
        throw IllegalArgumentException("Matrix A must be square")
    }
    if (a.rows != b.rows) {
        throw IllegalArgumentException("Matrix A and B must have the same number of rows")
    }

    val n = a.rows
    val m = b.cols
    val ac = a.copy()
    val xc = b.copy()

    for (i in 0 until n) {
        // Pivot selection
        var pivot = i
        for (j in i + 1 until n) {
            if (abs(ac[j, i]) > abs(ac[pivot, i])) {
                pivot = j
            }
        }

        // Swap rows in ac and xc
        for (k in 0 until n) {
            val temp = ac[i, k]
            ac[i, k] = ac[pivot, k]
            ac[pivot, k] = temp
        }
        for (k in 0 until m) {
            val temp = xc[i, k]
            xc[i, k] = xc[pivot, k]
            xc[pivot, k] = temp
        }

        if (abs(ac[i, i]) < 1e-15) {
            throw IllegalArgumentException("Matrix is singular")
        }

        // Elimination
        for (j in i + 1 until n) {
            val factor = ac[j, i] / ac[i, i]
            for (k in i until n) {
                ac[j, k] -= factor * ac[i, k]
            }
            for (k in 0 until m) {
                xc[j, k] -= factor * xc[i, k]
            }
        }
    }

    // Back substitution
    val x = Matrix.zeros(n, m)
    for (k in 0 until m) {
        for (i in n - 1 downTo 0) {
            var sum = 0.0
            for (j in i + 1 until n) {
                sum += ac[i, j] * x[j, k]
            }
            x[i, k] = (xc[i, k] - sum) / ac[i, i]
        }
    }

    return x
}

/**
 * Computes the inverse of a given square matrix.
 *
 * The method uses a linear system solver to determine the inverse by solving the equation
 * `AX = I`, where `A` is the input matrix, `I` is the identity matrix, and `X` is the resulting
 * inverse matrix.
 *
 * @param m The square matrix to be inverted. Must have equal number of rows and columns.
 * @return A new matrix representing the inverse of the input matrix.
 * @throws IllegalArgumentException If the input matrix is not square or if it is singular.
 */
fun invertMatrix(m: Matrix): Matrix {
    if (m.rows != m.cols) {
        throw IllegalArgumentException("Matrix must be square")
    }
    return solveLinearSystem(m, Matrix.identity(m.rows))
}
