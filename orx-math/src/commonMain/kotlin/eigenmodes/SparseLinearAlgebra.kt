package org.openrndr.extra.math.eigenmodes

import org.openrndr.extra.math.matrix.SparseMatrix
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Multiplies this sparse matrix with a dense vector.
 *
 * @param v A vector of size [SparseMatrix.cols].
 * @return A vector of size [SparseMatrix.rows] holding the product.
 */
operator fun SparseMatrix.times(v: DoubleArray): DoubleArray {
    require(v.size == cols) { "vector size (${v.size}) must match matrix column count ($cols)" }
    val result = DoubleArray(rows)
    for (i in 0 until rows) {
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size
        var sum = 0.0
        for (k in start until end) {
            sum += values[k] * v[columnIndices[k]]
        }
        result[i] = sum
    }
    return result
}

/**
 * Extracts the main diagonal of this sparse matrix.
 */
fun SparseMatrix.diagonal(): DoubleArray {
    val result = DoubleArray(minOf(rows, cols))
    for (i in 0 until rows) {
        val start = rowPointers[i]
        val end = if (i < rows - 1) rowPointers[i + 1] else values.size
        for (k in start until end) {
            if (columnIndices[k] == i) {
                result[i] = values[k]
                break
            }
        }
    }
    return result
}

internal fun dot(a: DoubleArray, b: DoubleArray): Double {
    var sum = 0.0
    for (i in a.indices) sum += a[i] * b[i]
    return sum
}

/**
 * Solves the symmetric positive-definite system `this * x = b` using the
 * Jacobi-preconditioned conjugate gradient method.
 *
 * This matrix is never formed densely, only matrix-vector products are used, which makes this
 * suitable for the large sparse systems produced by discretized Laplacians.
 *
 * @param b The right-hand side vector, of size [SparseMatrix.rows].
 * @param tolerance Target relative residual norm at which iteration stops.
 * @param maxIterations Maximum number of conjugate gradient iterations.
 * @return The solution vector `x`.
 */
fun SparseMatrix.solveCG(
    b: DoubleArray,
    tolerance: Double = 1e-10,
    maxIterations: Int = max(50, rows * 2)
): DoubleArray {
    require(rows == cols) { "matrix must be square" }
    require(b.size == rows) { "right-hand side size (${b.size}) must match matrix size ($rows)" }

    val n = rows
    val x = DoubleArray(n)
    val diag = diagonal()
    val precond = DoubleArray(n) { if (diag[it] != 0.0) 1.0 / diag[it] else 1.0 }

    val bNorm = sqrt(dot(b, b))
    if (bNorm < tolerance) return x

    val r = b.copyOf()
    val z = DoubleArray(n) { precond[it] * r[it] }
    val p = z.copyOf()
    var rz = dot(r, z)

    for (iteration in 0 until maxIterations) {
        val ap = this * p
        val alpha = rz / dot(p, ap)
        for (i in 0 until n) {
            x[i] += alpha * p[i]
            r[i] -= alpha * ap[i]
        }
        if (sqrt(dot(r, r)) < tolerance * bNorm) break
        for (i in 0 until n) z[i] = precond[i] * r[i]
        val rzNew = dot(r, z)
        val beta = rzNew / rz
        for (i in 0 until n) p[i] = z[i] + beta * p[i]
        rz = rzNew
        if (iteration == maxIterations - 1) break
    }
    return x
}
