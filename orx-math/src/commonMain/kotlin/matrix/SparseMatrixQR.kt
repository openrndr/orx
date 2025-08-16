package org.openrndr.extra.math.matrix

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt


/**
 * Performs QR decomposition on a sparse matrix.
 *
 * QR decomposition factors a matrix A into the product of an orthogonal matrix Q
 * and an upper triangular matrix R, such that A = QR.
 *
 * This implementation uses the Gram-Schmidt process adapted for sparse matrices.
 * It's more straightforward than Householder reflections and easier to debug.
 *
 * This implementation is optimized for sparse matrices by:
 * 1. Efficiently extracting columns from the sparse matrix
 * 2. Avoiding unnecessary operations on zero elements
 * 3. Returning sparse matrices as the result
 *
 * @param matrix The sparse matrix to decompose.
 * @return A Pair of matrices (Q, R) representing the orthogonal and upper triangular matrices.
 */
fun qrDecomposition(matrix: SparseMatrix): Pair<SparseMatrix, SparseMatrix> {
    val m = matrix.rows
    val n = matrix.cols

    // Initialize Q and R matrices
    val q = Matrix(m, n)
    val r = Matrix(n, n)

    // Implement the Gram-Schmidt process
    for (j in 0 until n) {
        // Extract the j-th column of the sparse matrix efficiently
        for (i in 0 until m) {
            q[i, j] = matrix[i, j]
        }

        // Orthogonalize against previous columns
        for (k in 0 until j) {
            // Compute dot product of q_j and q_k
            var dotProduct = 0.0
            for (i in 0 until m) {
                // Only multiply non-zero elements
                if (q[i, j] != 0.0 && q[i, k] != 0.0) {
                    dotProduct += q[i, j] * q[i, k]
                }
            }

            // Store in R
            r[k, j] = dotProduct

            // Subtract projection (only for non-zero elements in q_k)
            for (i in 0 until m) {
                if (q[i, k] != 0.0) {
                    q[i, j] -= dotProduct * q[i, k]
                }
            }
        }

        // Compute the norm of the column (only for non-zero elements)
        var norm = 0.0
        for (i in 0 until m) {
            if (q[i, j] != 0.0) {
                norm += q[i, j] * q[i, j]
            }
        }
        norm = sqrt(norm)

        // Store the norm in R
        r[j, j] = norm

        // Normalize the column if it's not too small
        if (norm > 1e-10) {
            for (i in 0 until m) {
                if (q[i, j] != 0.0) {
                    q[i, j] /= norm
                }
            }
        } else {
            // If the column is too small, set it to zero
            for (i in 0 until m) {
                q[i, j] = 0.0
            }
        }
    }

    // Convert matrices back to sparse format
    val qSparse = q.toSparseMatrix()
    val rSparse = r.toSparseMatrix()

    return Pair(qSparse, rSparse)
}

/**
 * Solves a linear system Ax = b using QR decomposition.
 *
 * Given the QR decomposition of matrix A (where A = QR), this function solves
 * the system Ax = b by first computing y = Q^T * b, and then solving Rx = y
 * using backward substitution.
 *
 * @param qr A Pair of sparse matrices (Q, R) representing the QR decomposition of matrix A.
 * @param b The right-hand side vector/matrix of the system.
 * @return The solution vector/matrix x.
 * @throws IllegalArgumentException If the dimensions of the matrices are incompatible.
 */
fun solveQR(qr: Pair<SparseMatrix, SparseMatrix>, b: SparseMatrix): SparseMatrix {
    val (q, r) = qr

    // Check dimensions
    if (q.rows != b.rows) {
        throw IllegalArgumentException("Incompatible dimensions: Q has ${q.rows} rows, b has ${b.rows} rows")
    }

    val n = r.rows  // Number of columns in the original matrix
    val m = b.cols  // Number of right-hand sides

    // Compute y = Q^T * b
    val qTranspose = q.transpose()
    val y = qTranspose * b

    // Create a dense matrix to store the solution
    val x = Matrix.zeros(n, m)

    // Backward substitution: Solve Rx = y for x
    for (col in 0 until m) {  // For each right-hand side
        for (i in n - 1 downTo 0) {  // Start from the last row
            var sum = y[i, col]

            // Subtract the known terms
            for (k in i + 1 until n) {
                sum -= r[i, k] * x[k, col]
            }

            // Divide by the diagonal element of R
            val rii = r[i, i]
            if (abs(rii) < 1e-10) {
                throw IllegalArgumentException("R matrix is singular or nearly singular")
            }

            x[i, col] = sum / rii
        }
    }

    // Convert to sparse matrix
    return x.toSparseMatrix()
}
