package org.openrndr.extra.math.eigenmodes

import org.openrndr.extra.math.matrix.SparseMatrix
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/** Relative offsets of the four grid neighbors used by the 5-point Laplacian stencil. */
private val neighborOffsets = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

/**
 * Builds the discrete Dirichlet Laplacian for the interior grid points marked `true` in [mask],
 * a `width` x `height` grid stored row-major (index = y * width + x).
 *
 * Grid points outside [mask] act as a zero (Dirichlet) boundary condition: a stencil neighbor
 * that falls outside the grid, or lands on a `false` cell, is simply omitted rather than wrapped
 * or reflected, which is what pins the field to zero there. The resulting matrix is indexed by a
 * compact "degree of freedom" numbering over only the interior points, in row-major order among
 * `true` cells, and is always symmetric positive-definite for a non-empty, bounded domain.
 *
 * @param mask Interior/exterior flag per grid cell, size `width * height`.
 * @return The Laplacian as a sparse matrix, and the compact per-cell degree-of-freedom index
 *         (-1 for cells outside the mask) used to interpret its rows/columns.
 */
fun buildDirichletLaplacian(mask: BooleanArray, width: Int, height: Int): Pair<SparseMatrix, IntArray> {
    require(mask.size == width * height) { "mask size (${mask.size}) must equal width * height (${width * height})" }

    val dofIndex = IntArray(width * height) { -1 }
    var dofCount = 0
    for (i in mask.indices) {
        if (mask[i]) {
            dofIndex[i] = dofCount
            dofCount++
        }
    }
    require(dofCount > 0) { "mask has no interior (true) cells" }

    val rowIndices = ArrayList<Pair<Int, Int>>(dofCount * 5)
    val entryValues = ArrayList<Double>(dofCount * 5)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            if (!mask[index]) continue
            val row = dofIndex[index]

            rowIndices.add(row to row)
            entryValues.add(4.0)

            for ((dx, dy) in neighborOffsets) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && mask[ny * width + nx]) {
                    rowIndices.add(row to dofIndex[ny * width + nx])
                    entryValues.add(-1.0)
                }
            }
        }
    }

    val laplacian = SparseMatrix(dofCount, dofCount, rowIndices, entryValues, presorted = false)
    return laplacian to dofIndex
}

/**
 * Builds the discrete Neumann ("free edge") Laplacian for the interior grid points marked `true`
 * in [mask], a `width` x `height` grid stored row-major (index = y * width + x).
 *
 * Unlike [buildDirichletLaplacian], a stencil neighbor that falls outside the grid or lands on a
 * `false` cell isn't treated as pinned to zero -- it's simply dropped from the stencil, which is
 * the standard way to encode a zero-derivative (reflecting) boundary. Each interior point's
 * diagonal is therefore its number of in-domain neighbors rather than a fixed `4`. This makes the
 * matrix exactly the combinatorial graph Laplacian of the mask's grid-adjacency graph: symmetric
 * positive *semi*-definite, with the constant vector as its null (zero-eigenvalue) eigenvector
 * for a connected domain -- see [extractNeumannEigenModes].
 *
 * @param mask Interior/exterior flag per grid cell, size `width * height`.
 * @return The Laplacian as a sparse matrix, and the compact per-cell degree-of-freedom index
 *         (-1 for cells outside the mask) used to interpret its rows/columns.
 */
fun buildNeumannLaplacian(mask: BooleanArray, width: Int, height: Int): Pair<SparseMatrix, IntArray> {
    require(mask.size == width * height) { "mask size (${mask.size}) must equal width * height (${width * height})" }

    val dofIndex = IntArray(width * height) { -1 }
    var dofCount = 0
    for (i in mask.indices) {
        if (mask[i]) {
            dofIndex[i] = dofCount
            dofCount++
        }
    }
    require(dofCount > 0) { "mask has no interior (true) cells" }

    val rowIndices = ArrayList<Pair<Int, Int>>(dofCount * 5)
    val entryValues = ArrayList<Double>(dofCount * 5)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val index = y * width + x
            if (!mask[index]) continue
            val row = dofIndex[index]

            var degree = 0.0
            for ((dx, dy) in neighborOffsets) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && mask[ny * width + nx]) {
                    degree += 1.0
                    rowIndices.add(row to dofIndex[ny * width + nx])
                    entryValues.add(-1.0)
                }
            }
            rowIndices.add(row to row)
            entryValues.add(degree)
        }
    }

    val laplacian = SparseMatrix(dofCount, dofCount, rowIndices, entryValues, presorted = false)
    return laplacian to dofIndex
}

/**
 * One vibrational eigenmode of a domain rasterized to a `width` x `height` grid, as produced by
 * [extractEigenModes]. Values are zero outside the domain that was passed in.
 *
 * @property eigenvalue The Laplacian eigenvalue of this mode; larger means higher spatial frequency.
 * @property frequencyRatio This mode's frequency relative to the fundamental mode, i.e.
 *           `sqrt(eigenvalue / fundamentalEigenvalue)`, mirroring the harmonic ratios (2nd
 *           partial, 3rd partial, ...) familiar from vibrating strings/membranes/plates. For a
 *           Dirichlet domain the fundamental is mode 0 itself (`frequencyRatio == 1.0`). For a
 *           Neumann domain mode 0 is instead the trivial, patternless zero-eigenvalue constant
 *           mode (`frequencyRatio == 0.0`); the fundamental is mode 1.
 * @property width Grid width.
 * @property height Grid height.
 * @property values The mode shape, row-major over the `width` x `height` grid.
 */
data class GridEigenMode(
    val eigenvalue: Double,
    val frequencyRatio: Double,
    val width: Int,
    val height: Int,
    val values: DoubleArray
) {
    /**
     * Bilinearly samples this mode at grid coordinates ([x], [y]), where `x` ranges over
     * `0 until width` and `y` over `0 until height`. Positions outside that range are clamped.
     */
    fun value(x: Double, y: Double): Double = bilinearSample(values, width, height, x, y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GridEigenMode) return false
        return eigenvalue == other.eigenvalue &&
                frequencyRatio == other.frequencyRatio &&
                width == other.width &&
                height == other.height &&
                values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = eigenvalue.hashCode()
        result = 31 * result + frequencyRatio.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + values.contentHashCode()
        return result
    }
}

/**
 * Bilinearly samples a row-major `width` x `height` grid of [values] at grid coordinates
 * ([x], [y]), where `x` ranges over `0 until width` and `y` over `0 until height`. Positions
 * outside that range are clamped.
 */
fun bilinearSample(values: DoubleArray, width: Int, height: Int, x: Double, y: Double): Double {
    val cx = x.coerceIn(0.0, (width - 1).toDouble())
    val cy = y.coerceIn(0.0, (height - 1).toDouble())

    val x0 = floor(cx).toInt().coerceIn(0, width - 1)
    val y0 = floor(cy).toInt().coerceIn(0, height - 1)
    val x1 = (x0 + 1).coerceAtMost(width - 1)
    val y1 = (y0 + 1).coerceAtMost(height - 1)

    val tx = cx - x0
    val ty = cy - y0

    val v00 = values[y0 * width + x0]
    val v10 = values[y0 * width + x1]
    val v01 = values[y1 * width + x0]
    val v11 = values[y1 * width + x1]

    val top = v00 + (v10 - v00) * tx
    val bottom = v01 + (v11 - v01) * tx
    return top + (bottom - top) * ty
}

/**
 * Extracts the [count] lowest-frequency Dirichlet ("fixed edge") vibrational eigenmodes of the
 * domain marked `true` in [mask], a `width` x `height` grid. This is the discrete analogue of
 * solving `-Δu = λu` on the domain with `u = 0` on its boundary -- the same problem that
 * produces Chladni figures on a clamped drum head.
 *
 * @param mask Interior/exterior flag per grid cell, size `width * height`. Must contain at least
 *        one `true` cell.
 * @param count Number of modes to extract, ordered from lowest to highest frequency. Clamped to
 *        the number of interior grid cells.
 * @param random Source of the eigensolver's random initial vectors. Defaults to a fixed seed
 *        (`Random(0)`) for reproducible results.
 * @return The requested modes, ascending by [GridEigenMode.eigenvalue].
 */
fun extractEigenModes(
    mask: BooleanArray,
    width: Int,
    height: Int,
    count: Int,
    random: Random = Random(0)
): List<GridEigenMode> {
    val (laplacian, dofIndex) = buildDirichletLaplacian(mask, width, height)
    val eigenpairs = laplacian.smallestEigenpairs(count, random = random)
    return eigenpairsToGridModes(eigenpairs, mask, dofIndex, width, height)
}

/**
 * Extracts the [count] lowest-frequency Neumann ("free edge") vibrational eigenmodes of the
 * domain marked `true` in [mask], a `width` x `height` grid. This is the discrete analogue of
 * solving `-Δu = λu` on the domain with a zero-derivative (reflecting) boundary -- the free-plate
 * counterpart to [extractEigenModes]'s clamped drum head.
 *
 * The underlying Laplacian is only positive semi-definite: its smallest eigenvalue is exactly
 * `0`, with a constant field as eigenvector -- a uniform, patternless "mode" representing the
 * domain free to sit at any level, rather than an actual vibration. That trivial mode is always
 * included as mode 0 when `count >= 1`; [GridEigenMode.frequencyRatio] is computed relative to
 * the first genuinely non-trivial mode (mode 1) rather than mode 0.
 *
 * @param mask Interior/exterior flag per grid cell, size `width * height`. Must contain at least
 *        one `true` cell.
 * @param count Number of modes to extract, ordered from lowest to highest frequency (including
 *        the trivial constant mode). Clamped to the number of interior grid cells.
 * @param random Source of the eigensolver's random initial vectors. Defaults to a fixed seed
 *        (`Random(0)`) for reproducible results.
 * @return The requested modes, ascending by [GridEigenMode.eigenvalue].
 */
fun extractNeumannEigenModes(
    mask: BooleanArray,
    width: Int,
    height: Int,
    count: Int,
    random: Random = Random(0)
): List<GridEigenMode> {
    val (laplacian, dofIndex) = buildNeumannLaplacian(mask, width, height)
    val dofCount = dofIndex.count { it >= 0 }
    val constant = DoubleArray(dofCount) { 1.0 / sqrt(dofCount.toDouble()) }
    val eigenpairs = laplacian.smallestEigenpairs(
        count,
        knownEigenpairs = listOf(Eigenpair(0.0, constant)),
        random = random
    )
    return eigenpairsToGridModes(eigenpairs, mask, dofIndex, width, height)
}

private fun eigenpairsToGridModes(
    eigenpairs: List<Eigenpair>,
    mask: BooleanArray,
    dofIndex: IntArray,
    width: Int,
    height: Int
): List<GridEigenMode> {
    val reference = referenceEigenvalue(eigenpairs.map { it.eigenvalue })

    return eigenpairs.map { (eigenvalue, vector) ->
        val field = DoubleArray(width * height)
        for (i in mask.indices) {
            val dof = dofIndex[i]
            if (dof >= 0) field[i] = vector[dof]
        }
        GridEigenMode(
            eigenvalue = eigenvalue,
            frequencyRatio = if (reference <= 0.0) 0.0 else sqrt(eigenvalue / reference),
            width = width,
            height = height,
            values = field
        )
    }
}
