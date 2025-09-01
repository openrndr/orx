package org.openrndr.extra.math.matrix

import kotlin.math.sqrt

/**
 * Performs the Cholesky decomposition on a given square, positive-definite matrix.
 * The Cholesky decomposition expresses the matrix as L * L^T, where L is a lower triangular matrix.
 *
 * @param matrix The input matrix to decompose. Must be square (rows == cols) and positive-definite.
 * @return A lower triangular matrix resulting from the Cholesky decomposition of the input matrix.
 * @throws IllegalArgumentException if the input matrix is not square or not positive-definite.
 */
fun choleskyDecomposition(matrix: Matrix): Matrix {
    if (matrix.rows != matrix.cols) {
        throw IllegalArgumentException("Matrix must be square")
    }

    val n = matrix.rows
    val L = Matrix.zeros(n, n)

    for (i in 0 until n) {
        for (j in 0..i) {
            when {
                i == j -> {
                    // Diagonal elements
                    var sum = 0.0
                    for (k in 0 until j) {
                        sum += L[j, k] * L[j, k]
                    }
                    val diagonal = matrix[j, j] - sum
                    if (diagonal <= 0) {
                        throw IllegalArgumentException("Matrix is not positive definite")
                    }
                    L[j, j] = sqrt(diagonal)
                }
                else -> {
                    // Lower triangular elements
                    var sum = 0.0
                    for (k in 0 until j) {
                        sum += L[i, k] * L[j, k]
                    }
                    L[i, j] = (matrix[i, j] - sum) / L[j, j]
                }
            }
        }
    }

    return L
}

/**
 * Solves the system of equations Ly = b using forward substitution, where L is a lower triangular matrix.
 *
 * @param L A lower triangular matrix of dimensions n x n.
 * @param b A vector of size n representing the constant terms.
 * @return A vector of size n representing the solution y to the system Ly = b.
 */
private fun forwardSubstitution(L: Matrix, b: DoubleArray): DoubleArray {
    val n = L.rows
    val y = DoubleArray(n)

    for (i in 0 until n) {
        var sum = 0.0
        for (j in 0 until i) {
            sum += L[i, j] * y[j]
        }
        y[i] = (b[i] - sum) / L[i, i]
    }

    return y
}

/**
 * Performs backward substitution to solve a system of linear equations represented as L^T * x = y,
 * where L is a lower triangular matrix and y is a known vector.
 *
 * This method assumes that the input matrix `L` is square and its diagonal contains no zeros.
 *
 * @param L A lower triangular matrix of type `Matrix` representing the system's coefficients.
 *          It is accessed in a transposed fashion during the algorithm, where L[j, i] represents L^T[i, j].
 * @param y A `DoubleArray` of size n representing the right-hand side vector of the linear system.
 * @return A `DoubleArray` of size n representing the solution vector x such that L^T * x = y.
 */
private fun backwardSubstitution(L: Matrix, y: DoubleArray): DoubleArray {
    val n = L.rows
    val x = DoubleArray(n)

    for (i in n - 1 downTo 0) {
        var sum = 0.0
        for (j in i + 1 until n) {
            sum += L[j, i] * x[j]  // L[j, i] represents L^T[i, j]
        }
        x[i] = (y[i] - sum) / L[i, i]
    }

    return x
}

/**
 * Computes the inverse of a given symmetric, positive-definite matrix using the Cholesky decomposition.
 *
 * This method assumes that the input matrix is square, symmetric, and positive-definite.
 *
 * @param matrix The input matrix to invert. Must be a square, symmetric, and positive-definite matrix.
 * @return The inverse of the input matrix as a new `Matrix` object.
 * @throws IllegalArgumentException if the input matrix is not square, symmetric, or positive-definite.
 */
fun invertMatrixCholesky(matrix: Matrix): Matrix {
    val n = matrix.rows
    val L = choleskyDecomposition(matrix)
    val inverse = Matrix.zeros(n, n)

    // For each column of the identity matrix
    for (col in 0 until n) {
        // Create unit vector for this column
        val unitVector = DoubleArray(n)
        unitVector[col] = 1.0

        // Solve L * y = e_col
        val y = forwardSubstitution(L, unitVector)

        // Solve L^T * x = y
        val x = backwardSubstitution(L, y)

        // Store result in inverse matrix
        for (row in 0 until n) {
            inverse[row, col] = x[row]
        }
    }

    return inverse
}