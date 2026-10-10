package org.openrndr.extra.math.eigenmodes

import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.SparseMatrix
import org.openrndr.extra.math.matrix.svd
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * An eigenvalue paired with its eigenvector.
 */
data class Eigenpair(val eigenvalue: Double, val eigenvector: DoubleArray)

/**
 * Finds the [count] smallest eigenpairs of this symmetric positive-semidefinite sparse matrix,
 * using shift-invert Lanczos: builds one Krylov subspace for the operator `v -> solve(this, v)`
 * (via [SparseMatrix.solveCG]) -- which maps this matrix's smallest eigenvalues to its largest,
 * the ones Lanczos converges to fastest -- and extracts Ritz pairs from the resulting small,
 * dense tridiagonal eigenproblem.
 *
 * Unlike a per-mode inverse power iteration, this amortizes work across every requested mode: it
 * needs roughly `count` matrix solves *in total*, not `count` independent from-scratch searches,
 * and it also handles closely-spaced eigenvalues far better than plain power iteration (whose
 * convergence rate for a mode degrades as its eigenvalue gets closer to the next one). This is
 * intended for matrices where only a modest number of the smallest eigenpairs are needed, such as
 * the discrete Laplacians produced by [buildDirichletLaplacian] -- it is not a general-purpose
 * eigensolver.
 *
 * Results are returned in ascending order of eigenvalue, each eigenvector sign-normalized (see
 * [normalizeSign]) so that repeated calls -- and calls across nearby random sources or
 * resolutions -- agree on orientation instead of flipping arbitrarily.
 *
 * @param count The total number of smallest eigenpairs to return, including any [knownEigenpairs].
 *        Clamped to the matrix size.
 * @param knownEigenpairs Eigenpairs already known analytically -- typically the zero eigenvalue
 *        of a singular (Neumann/graph) Laplacian, whose eigenvector is the constant vector. These
 *        are placed first in the result and deflated against throughout the Lanczos process,
 *        which both seeds and skips the (otherwise ill-posed, since this matrix is then singular)
 *        search for them.
 * @param random Source of the random initial vectors. Defaults to a fixed seed (`Random(0)`) for
 *        reproducible results; pass e.g. a fresh `Random(System.nanoTime())` for a different
 *        realization of any degenerate modes on each call.
 * @param cgTolerance Relative residual tolerance passed to the inner [SparseMatrix.solveCG] solves.
 */
fun SparseMatrix.smallestEigenpairs(
    count: Int,
    knownEigenpairs: List<Eigenpair> = emptyList(),
    random: Random = Random(0),
    cgTolerance: Double = 1e-10
): List<Eigenpair> {
    require(rows == cols) { "matrix must be square" }
    require(count >= 1) { "count must be at least 1" }

    val n = rows
    val target = minOf(count, n)
    val found = mutableListOf<Eigenpair>()

    for (known in knownEigenpairs) {
        if (found.size >= target) break
        val vector = known.eigenvector.copyOf()
        normalizeSign(vector)
        found.add(Eigenpair(known.eigenvalue, vector))
    }

    val remaining = target - found.size
    if (remaining > 0) {
        found.addAll(lanczosSmallestEigenpairs(remaining, found, random, cgTolerance))
    }

    return found.sortedBy { it.eigenvalue }
}

/**
 * Finds [count] additional smallest eigenpairs of this matrix, beyond the ones in [deflateAgainst]
 * (which are treated as already-known and projected out throughout). See [smallestEigenpairs].
 *
 * A single Krylov sequence is mathematically blind to repeated eigenvalues: for a degenerate
 * eigenspace of dimension r, applying this matrix repeatedly to one starting vector only ever
 * explores a single direction within it (A acts as a scalar there, so it cannot rotate the
 * sequence towards the other r-1 directions), no matter how many Lanczos steps are taken. This is
 * not a rare edge case for domains with any symmetry -- rectangles, circles, regular stars all
 * have exactly repeated eigenvalue pairs. A single run can come up exactly [count] short of
 * requested modes, in which case a retry is the obvious signal -- but it can just as easily land
 * on exactly [count] Ritz values anyway, silently substituting some larger, non-degenerate
 * eigenvalue it *could* see in place of the smaller one it couldn't, which "missing count" alone
 * would never catch. So this always runs at least two independent passes (fresh random vector,
 * deflated against everything found in every previous pass of this call) and merges their Ritz
 * pairs before taking the smallest [count] overall -- two independent random vectors both missing
 * the same hidden direction of a degenerate eigenspace is essentially impossible, so the second
 * pass recovers whatever the first one couldn't see, and re-sorting the union discards whatever
 * too-large substitute the first pass returned in its place.
 */
private fun SparseMatrix.lanczosSmallestEigenpairs(
    count: Int,
    deflateAgainst: List<Eigenpair>,
    random: Random,
    cgTolerance: Double
): List<Eigenpair> {
    val n = rows
    val results = mutableListOf<Eigenpair>()
    val minPasses = 2

    var pass = 0
    while (pass < minPasses || results.size < count) {
        val available = n - deflateAgainst.size - results.size
        if (available <= 0) break

        val runResults = lanczosRun(count, deflateAgainst + results, random, cgTolerance)
        pass++
        if (runResults.isEmpty()) break

        results.addAll(runResults)
    }

    return results.sortedBy { it.eigenvalue }.take(count)
}

private fun SparseMatrix.lanczosRun(
    count: Int,
    deflateAgainst: List<Eigenpair>,
    random: Random,
    cgTolerance: Double
): List<Eigenpair> {
    val n = rows
    val available = n - deflateAgainst.size
    if (available <= 0) return emptyList()

    // A handful of extra Lanczos steps beyond the target count reliably improves accuracy of the
    // requested Ritz pairs, at a modest, roughly linear extra cost -- standard practice for
    // Krylov-subspace eigensolvers.
    val m = min(available, count + max(10, count / 2))

    val basis = ArrayList<DoubleArray>(m)
    val alpha = DoubleArray(m)
    val beta = DoubleArray(m)

    var v = DoubleArray(n) { random.nextDouble(-1.0, 1.0) }
    deflateAndNormalize(v, deflateAgainst)

    var steps = 0
    for (j in 0 until m) {
        basis.add(v)

        val w = solveCG(v, cgTolerance)
        deflate(w, deflateAgainst)

        // Full reorthogonalization against every basis vector so far (including v itself, which
        // yields alpha_j) -- more robust in finite precision than the classical three-term
        // recurrence, and cheap since m stays modest.
        var alphaJ = 0.0
        for (b in basis) {
            val projection = dot(w, b)
            for (i in w.indices) w[i] -= projection * b[i]
            if (b === v) alphaJ = projection
        }
        alpha[j] = alphaJ
        steps = j + 1

        val betaJ = sqrt(dot(w, w))
        if (betaJ < 1e-10 || j == m - 1) break
        beta[j] = betaJ
        v = DoubleArray(n) { w[it] / betaJ }
    }

    val t = Matrix.zeros(steps, steps)
    for (i in 0 until steps) {
        t[i, i] = alpha[i]
        if (i < steps - 1) {
            t[i, i + 1] = beta[i]
            t[i + 1, i] = beta[i]
        }
    }

    // T is symmetric positive-definite (this matrix, deflated against deflateAgainst, is
    // invertible and positive-definite, and so is its inverse): SVD's singular values are exactly
    // its eigenvalues, descending -- the largest correspond to this matrix's smallest eigenvalues.
    val (u, s, _) = t.svd(fullMatrix = false)

    val results = mutableListOf<Eigenpair>()
    val take = min(count, steps)
    for (k in 0 until take) {
        val theta = s[0, k]
        if (theta <= 0.0) continue

        val ritzVector = DoubleArray(n)
        for (l in 0 until steps) {
            val coeff = u[l, k]
            if (coeff == 0.0) continue
            val basisVector = basis[l]
            for (i in 0 until n) ritzVector[i] += coeff * basisVector[i]
        }
        val norm = sqrt(dot(ritzVector, ritzVector))
        if (norm < 1e-12) continue
        for (i in ritzVector.indices) ritzVector[i] /= norm
        deflateAndNormalize(ritzVector, deflateAgainst)

        // Refine via the Rayleigh quotient against the true matrix, rather than trusting 1/theta
        // directly -- cheap (one matrix-vector product) and more accurate.
        val refinedEigenvalue = dot(ritzVector, this * ritzVector)
        normalizeSign(ritzVector)
        results.add(Eigenpair(refinedEigenvalue, ritzVector))
    }

    return results
}

/**
 * Flips the sign of [v] in place, if needed, so that its largest-magnitude component is
 * positive. Eigenvectors are only defined up to sign; this picks a canonical one so that
 * repeated extraction (across seeds, resolutions, or re-runs) doesn't flip a mode's polarity
 * arbitrarily.
 *
 * Domains with a reflection symmetry (a rectangle, a circle, a symmetric star, ...) produce
 * antisymmetric modes whose two largest-magnitude components are an exact mirrored pair of
 * *opposite* sign -- an inherent tie, not just a numerical coincidence. Picking the single
 * largest component in that case would let tiny solver noise decide the sign at random, so
 * instead this breaks such (near-)ties deterministically by lowest index.
 */
fun normalizeSign(v: DoubleArray) {
    var maxAbs = 0.0
    for (x in v) maxAbs = max(maxAbs, abs(x))
    if (maxAbs == 0.0) return

    var chosenIndex = 0
    for (i in v.indices) {
        if (abs(v[i]) >= maxAbs * (1.0 - 1e-3)) {
            chosenIndex = i
            break
        }
    }
    if (v[chosenIndex] < 0.0) {
        for (i in v.indices) v[i] = -v[i]
    }
}

/**
 * Projects out the components of [v] along each of the [against] eigenvectors (Gram-Schmidt), in
 * place.
 */
private fun deflate(v: DoubleArray, against: List<Eigenpair>) {
    for (mode in against) {
        val u = mode.eigenvector
        val projection = dot(v, u)
        for (i in v.indices) v[i] -= projection * u[i]
    }
}

/**
 * [deflate]s [v] against [against], then normalizes it to unit length in place.
 */
private fun deflateAndNormalize(v: DoubleArray, against: List<Eigenpair>) {
    deflate(v, against)
    val norm = sqrt(dot(v, v))
    if (norm > 1e-12) {
        for (i in v.indices) v[i] /= norm
    } else {
        // degenerate case: fall back to a fresh unit vector orthogonal to nothing in particular
        v[0] = 1.0
        for (i in 1 until v.size) v[i] = 0.0
    }
}
