package org.openrndr.extra.shapes.eigenmodes

import org.openrndr.extra.math.eigenmodes.bilinearSample
import org.openrndr.extra.math.eigenmodes.extractCycleEigenModes
import org.openrndr.extra.math.eigenmodes.extractEigenModes as extractDirichletGridEigenModes
import org.openrndr.extra.math.eigenmodes.extractNeumannEigenModes as extractNeumannGridEigenModes
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.contains
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Which condition the vibrational eigenmode problem imposes at the domain's boundary. See
 * [Shape.extractEigenModes].
 */
enum class EigenModeBoundaryCondition {
    /** The boundary is pinned to zero -- a clamped drum head. Every eigenvalue is positive. */
    DIRICHLET,

    /**
     * The boundary is free (zero derivative) -- an unclamped plate. The smallest eigenvalue is
     * exactly `0`, with a constant (patternless) eigenvector, always returned as mode 0.
     */
    NEUMANN
}

/**
 * One vibrational eigenmode of a [Shape]'s interior, as produced by [Shape.extractEigenModes].
 *
 * This is the discrete analogue of solving `-Δu = λu` on the shape's interior with either a
 * pinned or a free boundary -- see [EigenModeBoundaryCondition].
 *
 * @property eigenvalue The Laplacian eigenvalue of this mode; larger means higher spatial frequency.
 * @property frequencyRatio This mode's frequency relative to the fundamental mode, i.e.
 *           `sqrt(eigenvalue / fundamentalEigenvalue)`, mirroring the harmonic ratios of a
 *           vibrating string/membrane/plate. For [EigenModeBoundaryCondition.DIRICHLET] the
 *           fundamental is mode 0 itself (`frequencyRatio == 1.0`). For
 *           [EigenModeBoundaryCondition.NEUMANN], mode 0 is instead the trivial, patternless
 *           zero-eigenvalue constant mode (`frequencyRatio == 0.0`); the fundamental is mode 1.
 * @property bounds The shape's bounding box that the underlying grid was rasterized into.
 * @property width Grid width used for rasterization.
 * @property height Grid height used for rasterization.
 * @property values The mode shape, row-major over the `width` x `height` grid; zero outside the shape.
 */
data class EigenMode(
    val eigenvalue: Double,
    val frequencyRatio: Double,
    val bounds: Rectangle,
    val width: Int,
    val height: Int,
    val values: DoubleArray
) {
    /**
     * Samples this mode at a world-space [position] using bilinear interpolation. Positions
     * outside [bounds] are clamped to its edge.
     */
    fun value(position: Vector2): Double {
        val gx = (position.x - bounds.x) / bounds.width * (width - 1)
        val gy = (position.y - bounds.y) / bounds.height * (height - 1)
        return bilinearSample(values, width, height, gx, gy)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EigenMode) return false
        return eigenvalue == other.eigenvalue &&
                frequencyRatio == other.frequencyRatio &&
                bounds == other.bounds &&
                width == other.width &&
                height == other.height &&
                values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = eigenvalue.hashCode()
        result = 31 * result + frequencyRatio.hashCode()
        result = 31 * result + bounds.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + values.contentHashCode()
        return result
    }
}

/**
 * Extracts the [count] lowest-frequency vibrational eigenmodes of this shape's interior, by
 * rasterizing it to a grid and solving the discrete clamped- or free-membrane eigenproblem on it.
 *
 * @param count Number of modes to extract, ordered from lowest to highest frequency.
 * @param resolution Grid cells along the longer side of the shape's bounding box; the shorter
 *        side is scaled to match the shape's aspect ratio. Higher values resolve finer boundary
 *        detail and higher-frequency modes at the cost of a larger eigenproblem to solve.
 * @param random Source of the eigensolver's random initial vectors. Defaults to a fixed seed
 *        (`Random(0)`) for reproducible results.
 * @param boundaryCondition Whether the shape's boundary is pinned ([EigenModeBoundaryCondition.DIRICHLET],
 *        the default) or free ([EigenModeBoundaryCondition.NEUMANN]).
 * @return The requested modes, ascending by [EigenMode.eigenvalue].
 */
fun Shape.extractEigenModes(
    count: Int,
    resolution: Int = 64,
    random: Random = Random(0),
    boundaryCondition: EigenModeBoundaryCondition = EigenModeBoundaryCondition.DIRICHLET
): List<EigenMode> {
    val bounds = bounds
    val (mask, width, height) = rasterize(this, bounds, resolution)

    val gridModes = when (boundaryCondition) {
        EigenModeBoundaryCondition.DIRICHLET -> extractDirichletGridEigenModes(mask, width, height, count, random)
        EigenModeBoundaryCondition.NEUMANN -> extractNeumannGridEigenModes(mask, width, height, count, random)
    }
    return gridModes.map { mode ->
        EigenMode(mode.eigenvalue, mode.frequencyRatio, bounds, width, height, mode.values)
    }
}

private data class Raster(val mask: BooleanArray, val width: Int, val height: Int)

private fun rasterize(shape: Shape, bounds: Rectangle, resolution: Int): Raster {
    require(bounds.width > 0.0 && bounds.height > 0.0) { "shape has empty bounds" }

    val width: Int
    val height: Int
    if (bounds.width >= bounds.height) {
        width = resolution
        height = max(1, (resolution * bounds.height / bounds.width).roundToInt())
    } else {
        height = resolution
        width = max(1, (resolution * bounds.width / bounds.height).roundToInt())
    }

    val cellWidth = bounds.width / width
    val cellHeight = bounds.height / height
    val mask = BooleanArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val position = Vector2(
                bounds.x + (x + 0.5) * cellWidth,
                bounds.y + (y + 0.5) * cellHeight
            )
            mask[y * width + x] = position in shape
        }
    }
    require(mask.any { it }) { "shape's bounding box rasterized to an empty grid at resolution $resolution" }

    return Raster(mask, width, height)
}

/**
 * One eigenmode of a [RectifiedContour]'s outline, as produced by
 * [RectifiedContour.extractBoundaryEigenModes].
 *
 * Unlike [EigenMode], this only considers the boundary curve itself, not the shape's interior --
 * it's the eigenbasis of the contour sampled as a closed graph, a generalization of the
 * sine/cosine basis used by classical Fourier shape descriptors to non-uniformly detailed curves.
 *
 * @property eigenvalue The Laplacian eigenvalue of this mode; larger means higher spatial frequency.
 * @property frequencyRatio This mode's frequency relative to the fundamental mode. Mode 0 is
 *           always the trivial, patternless zero-eigenvalue constant mode (`frequencyRatio ==
 *           0.0`); the fundamental is mode 1.
 * @property contour The rectified contour that was sampled. Rectification is what makes equal
 *           steps in [ts] equal steps in arc length, which is what the underlying graph
 *           Laplacian assumes of its nodes.
 * @property ts The (rectified) contour parameters sampled around the contour, in order; `ts[i]`
 *           is where [values]`[i]` was taken, and `contour.position(ts[i])` recovers the
 *           world-space point.
 * @property values The mode shape, one value per parameter in [ts].
 */
data class BoundaryEigenMode(
    val eigenvalue: Double,
    val frequencyRatio: Double,
    val contour: RectifiedContour,
    val ts: List<Double>,
    val values: DoubleArray
) {
    /** Number of points/nodes around the curve. */
    val n: Int get() = ts.size

    /**
     * Samples this mode at point-index coordinate [t], which wraps around modulo [n] (so both
     * `-0.5` and `n - 0.5` land halfway between the last and first point), using linear
     * interpolation between the nearest sample points.
     */
    fun value(t: Double): Double {
        val wrapped = t - floor(t / n) * n
        val i0 = floor(wrapped).toInt().coerceIn(0, n - 1)
        val i1 = (i0 + 1) % n
        val f = wrapped - i0
        return values[i0] + (values[i1] - values[i0]) * f
    }

    /** The world-space position that sample point [index] (`0 until n`) was taken at. */
    fun position(index: Int): Vector2 = contour.position(ts[index])

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoundaryEigenMode) return false
        return eigenvalue == other.eigenvalue &&
                frequencyRatio == other.frequencyRatio &&
                contour.contour == other.contour.contour &&
                ts == other.ts &&
                values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        var result = eigenvalue.hashCode()
        result = 31 * result + frequencyRatio.hashCode()
        result = 31 * result + contour.contour.hashCode()
        result = 31 * result + ts.hashCode()
        result = 31 * result + values.contentHashCode()
        return result
    }
}

/**
 * Extracts the [count] lowest-frequency eigenmodes of this rectified contour's outline, ignoring
 * its interior entirely -- the boundary-only counterpart to [Shape.extractEigenModes]. `resolution`
 * parameters equidistant in rectified (arc-length) space are treated as a cycle graph, and its
 * graph-Laplacian eigenvectors extracted; see [org.openrndr.extra.math.eigenmodes.extractCycleEigenModes].
 * A [RectifiedContour] is required rather than a plain `ShapeContour` specifically so that equal
 * steps in `t` are equal steps in arc length -- true by construction here, since `ts[i] = i /
 * resolution` in rectified space -- which is what the cycle graph's uniform edge weights assume.
 *
 * As with [EigenModeBoundaryCondition.NEUMANN], the underlying Laplacian is only positive
 * semi-definite: mode 0 is always the trivial, patternless zero-eigenvalue constant mode.
 *
 * @param count Number of modes to extract, ordered from lowest to highest frequency (including
 *        the trivial constant mode).
 * @param resolution Number of points sampled equidistantly (by arc length) around the contour.
 * @param random Source of the eigensolver's random initial vectors. Defaults to a fixed seed
 *        (`Random(0)`) for reproducible results.
 * @return The requested modes, ascending by [BoundaryEigenMode.eigenvalue].
 */
fun RectifiedContour.extractBoundaryEigenModes(
    count: Int,
    resolution: Int = 128,
    random: Random = Random(0)
): List<BoundaryEigenMode> {
    require(contour.closed) { "contour must be closed" }
    require(resolution >= 3) { "resolution must be at least 3" }

    val ts = List(resolution) { it.toDouble() / resolution }
    val cycleModes = extractCycleEigenModes(resolution, count, random)
    return cycleModes.map { mode ->
        BoundaryEigenMode(mode.eigenvalue, mode.frequencyRatio, this, ts, mode.values)
    }
}
