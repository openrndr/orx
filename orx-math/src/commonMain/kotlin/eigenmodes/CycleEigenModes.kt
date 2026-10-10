package org.openrndr.extra.math.eigenmodes

import org.openrndr.extra.math.matrix.SparseMatrix
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Builds the graph Laplacian of an `n`-node cycle graph (node `i` connected to `i - 1` and
 * `i + 1`, indices modulo `n`) -- the discrete analogue of the periodic 1D Laplacian, and the
 * natural graph to put on `n` points sampled around a closed curve. Symmetric positive
 * semi-definite, with the constant vector as its null (zero-eigenvalue) eigenvector -- see
 * [extractCycleEigenModes].
 *
 * Uses unit edge weights throughout (equivalently, unit spacing between nodes): since only
 * eigenvalue *ratios* are exposed via [GridEigenMode.frequencyRatio]-style results, the physical
 * spacing of the original samples doesn't need to be carried through, it only rescales every
 * eigenvalue by the same constant factor.
 *
 * @param n Number of nodes. Must be at least 1.
 */
fun buildCycleLaplacian(n: Int): SparseMatrix {
    require(n >= 1) { "n must be at least 1" }

    return when (n) {
        1 -> SparseMatrix(1, 1, listOf(0 to 0), listOf(0.0), presorted = false)
        2 -> SparseMatrix(
            2, 2,
            listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
            listOf(2.0, -2.0, -2.0, 2.0),
            presorted = false
        )
        else -> {
            val rowIndices = ArrayList<Pair<Int, Int>>(n * 3)
            val entryValues = ArrayList<Double>(n * 3)
            for (i in 0 until n) {
                val prev = (i - 1 + n) % n
                val next = (i + 1) % n
                rowIndices.add(i to i); entryValues.add(2.0)
                rowIndices.add(i to prev); entryValues.add(-1.0)
                rowIndices.add(i to next); entryValues.add(-1.0)
            }
            SparseMatrix(n, n, rowIndices, entryValues, presorted = false)
        }
    }
}

/**
 * One eigenmode of `n` points arranged around a closed curve, as produced by
 * [extractCycleEigenModes] -- the discrete, boundary-only analogue of [GridEigenMode]. These are
 * the eigenvectors of the cycle graph Laplacian, i.e. a generalization of the sine/cosine basis
 * used by classical Fourier shape descriptors to a curve sampled at non-uniform detail.
 *
 * @property eigenvalue The Laplacian eigenvalue of this mode; larger means higher spatial frequency.
 * @property frequencyRatio This mode's frequency relative to the fundamental mode, i.e.
 *           `sqrt(eigenvalue / fundamentalEigenvalue)`. Mode 0 is always the trivial,
 *           patternless zero-eigenvalue constant mode (`frequencyRatio == 0.0`); the fundamental
 *           is mode 1.
 * @property n Number of points/nodes around the curve.
 * @property values The mode shape, one value per node in order around the curve.
 */
data class CycleEigenMode(
    val eigenvalue: Double,
    val frequencyRatio: Double,
    val n: Int,
    val values: DoubleArray
) {
    /**
     * Circularly, linearly samples this mode at node coordinate [t], which wraps around modulo
     * [n] (so both `-0.5` and `n - 0.5` land halfway between the last and first node).
     */
    fun value(t: Double): Double {
        val wrapped = t - floor(t / n) * n
        val i0 = floor(wrapped).toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1) % n
        val f = wrapped - i0
        return values[i0] + (values[i1] - values[i0]) * f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CycleEigenMode) return false
        return eigenvalue == other.eigenvalue &&
                frequencyRatio == other.frequencyRatio &&
                n == other.n &&
                values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = eigenvalue.hashCode()
        result = 31 * result + frequencyRatio.hashCode()
        result = 31 * result + n
        result = 31 * result + values.contentHashCode()
        return result
    }
}

/**
 * Extracts the [count] lowest-frequency eigenmodes of `n` points arranged around a closed curve,
 * by solving the eigenproblem of the [buildCycleLaplacian] graph Laplacian -- the boundary-only
 * counterpart to [extractEigenModes]/[extractNeumannEigenModes], which instead act on the
 * curve's interior.
 *
 * As with [extractNeumannEigenModes], the underlying Laplacian is only positive semi-definite:
 * its smallest eigenvalue is exactly `0` with a constant eigenvector, always included as mode 0
 * when `count >= 1`; [CycleEigenMode.frequencyRatio] is computed relative to the first genuinely
 * non-trivial mode (mode 1) rather than mode 0.
 *
 * @param n Number of points/nodes around the curve. Must be at least 1.
 * @param count Number of modes to extract, ordered from lowest to highest frequency (including
 *        the trivial constant mode). Clamped to `n`.
 * @param random Source of the eigensolver's random initial vectors. Defaults to a fixed seed
 *        (`Random(0)`) for reproducible results.
 * @return The requested modes, ascending by [CycleEigenMode.eigenvalue].
 */
fun extractCycleEigenModes(n: Int, count: Int, random: Random = Random(0)): List<CycleEigenMode> {
    val laplacian = buildCycleLaplacian(n)
    val constant = DoubleArray(n) { 1.0 / sqrt(n.toDouble()) }
    val eigenpairs = laplacian.smallestEigenpairs(
        count,
        knownEigenpairs = listOf(Eigenpair(0.0, constant)),
        random = random
    )
    val reference = referenceEigenvalue(eigenpairs.map { it.eigenvalue })

    return eigenpairs.map { (eigenvalue, vector) ->
        CycleEigenMode(
            eigenvalue = eigenvalue,
            frequencyRatio = if (reference <= 0.0) 0.0 else sqrt(eigenvalue / reference),
            n = n,
            values = vector.copyOf()
        )
    }
}
