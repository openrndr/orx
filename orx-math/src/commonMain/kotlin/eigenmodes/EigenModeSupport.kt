package org.openrndr.extra.math.eigenmodes

/**
 * Picks the eigenvalue to report `1.0` frequency ratios against: the smallest eigenvalue that
 * isn't (numerically) zero.
 *
 * For a pinned (Dirichlet) domain every eigenvalue is strictly positive, so this is simply the
 * first one. For a free (Neumann) domain or a closed boundary curve, the Laplacian is only
 * positive *semi*-definite and its smallest eigenvalue is exactly `0`, with a constant
 * eigenvector -- a uniform, patternless "mode" rather than a vibration -- so that one is skipped
 * when picking the reference.
 */
internal fun referenceEigenvalue(eigenvalues: List<Double>): Double {
    val scale = eigenvalues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    return eigenvalues.firstOrNull { it > 1e-9 * scale } ?: eigenvalues.firstOrNull() ?: 1.0
}
