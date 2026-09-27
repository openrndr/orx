package org.openrndr.extra.marchingsquares

import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.sqrt


/**
 * Finds contours for [shapeFunction] using an adaptive quadtree and dual contouring, as described
 * in https://www.mattkeeter.com/projects/contours/. Unlike plain marching squares, cells
 * are only subdivided down to [maxDepth] where the boundary actually needs the extra
 * resolution, and dual vertices are placed on sharp features (corners, thin edges)
 * instead of naive edge midpoints.
 *
 * @param shapeFunction the scalar field to contour; negative is "inside"
 * @param area the rectangular area in which [shapeFunction] is evaluated
 * @param maxDepth the maximum quadtree depth, i.e. the area is split into up to `2^maxDepth`
 * cells along each axis
 * @return a list of [ShapeContour] instances
 */
fun findContoursAdaptive(
    shapeFunction: ShapeFunction,
    area: Rectangle,
    maxDepth: Int
): List<ShapeContour> {
    val clamped = clampToBorder(shapeFunction, area)

    // buildQuadTreeNode halves width and height independently at every level, so a non-square
    // area would keep that same aspect ratio all the way down instead of ever reaching a
    // square cell -- building over the square, centered superset of area instead keeps cells
    // square at every depth. clamped already reads 0.0 (outside) beyond area's own bounds, so
    // the margin this can add on the shorter axis just collapses straight to empty.
    val side = maxOf(area.width, area.height)
    val squareArea = Rectangle.fromCenter(area.center, side, side)

    val tree = buildQuadTreeNode(clamped, squareArea, maxDepth)
    val segments = mutableListOf<LineSegment>()
    faceProc(clamped, tree, segments)
    return assembleContours(segments)
}



/**
 * Places the dual vertex for [cell]: the point that best lies on the feature (edge or
 * corner) of [shapeFunction] passing through the cell, found by a least-squares fit of the
 * tangent planes at every zero-crossing point on the cell's boundary. Returns `null` if
 * [cell] has fewer than two crossing points, i.e. it does not straddle the boundary.
 *
 * This solves the same normal-equation system as the referenced article's SVD-based
 * solve, but does so directly: since the system is only 2x2, its (symmetric) normal
 * matrix is eigendecomposed analytically and near-zero eigenvalues (directions the data
 * doesn't constrain) are dropped, which is equivalent to a truncated-SVD least-squares
 * solve and keeps the result well-behaved when the crossing normals are near-parallel.
 *
 * Two guards keep this well-behaved when the crossing normals are *almost* but not quite
 * parallel (a nearly-flat feature -- e.g. a curve grazing a straight domain border that
 * [clampToBorder] introduces): the eigenvalue cutoff is a much less aggressive fraction of
 * the dominant eigenvalue than a strict "is this basically zero" test would use, since
 * dividing by a merely-small (rather than truly negligible) eigenvalue amplifies ordinary
 * floating-point noise into an enormous, wrong offset; and the result is clamped back into
 * [cell] afterward as a hard backstop, since a dual vertex describing a small cell has no
 * business landing hundreds of units away from it.
 */
internal fun feature(shapeFunction: ShapeFunction, cell: Rectangle): Vector2? {
    val points = crossingSegments(shapeFunction, cell).flatMap { listOf(it.start, it.end) }
    if (points.size < 2) {
        return null
    }
    val normals = points.map { normal(shapeFunction, it) }
    val center = points.fold(Vector2.ZERO) { acc, p -> acc + p } * (1.0 / points.size)

    var m00 = 0.0
    var m01 = 0.0
    var m11 = 0.0
    var r0 = 0.0
    var r1 = 0.0
    for (i in points.indices) {
        val n = normals[i]
        val b = (points[i] - center) dot n
        m00 += n.x * n.x
        m01 += n.x * n.y
        m11 += n.y * n.y
        r0 += n.x * b
        r1 += n.y * b
    }

    // Eigendecompose the symmetric 2x2 normal matrix [[m00, m01], [m01, m11]].
    val trace = m00 + m11
    val det = m00 * m11 - m01 * m01
    val discriminant = maxOf(0.0, (trace * trace) / 4.0 - det)
    val s = sqrt(discriminant)
    val lambda0 = trace / 2.0 + s
    val lambda1 = trace / 2.0 - s

    val e0 = if (m01 != 0.0 || m00 - lambda0 != 0.0) {
        val v = Vector2(-m01, m00 - lambda0)
        if (v.squaredLength > 1E-18) v.normalized else Vector2(1.0, 0.0)
    } else {
        Vector2(1.0, 0.0)
    }
    val e1 = Vector2(-e0.y, e0.x)

    val threshold = 1E-2 * maxOf(lambda0, 1.0)
    var offset = Vector2.ZERO
    for ((lambda, e) in listOf(lambda0 to e0, lambda1 to e1)) {
        if (lambda > threshold) {
            offset += e * ((e.x * r0 + e.y * r1) / lambda)
        }
    }
    val result = center + offset
    return Vector2(
        result.x.coerceIn(cell.x, cell.x + cell.width),
        result.y.coerceIn(cell.y, cell.y + cell.height)
    )
}


internal fun featureOf(shapeFunction: ShapeFunction, leaf: QuadTree): Vector2 =
    feature(shapeFunction, leaf.cell)
        ?: error("leaf $leaf has no zero crossing, it should have been collapsed to full or empty")


/**
 * Recursively walks [tree], collecting the dual-contouring segments of every pair of
 * adjacent cells (within [tree] and across its four children) into [out].
 */
internal fun faceProc(shapeFunction: ShapeFunction, tree: QuadTree, out: MutableList<LineSegment>) {
    if (tree.children.isEmpty()) {
        return
    }
    val (a, b, c, d) = tree.children
    faceProc(shapeFunction, a, out)
    faceProc(shapeFunction, b, out)
    faceProc(shapeFunction, c, out)
    faceProc(shapeFunction, d, out)
    edgeProcH(shapeFunction, a, b, out)
    edgeProcH(shapeFunction, c, d, out)
    edgeProcV(shapeFunction, a, c, out)
    edgeProcV(shapeFunction, b, d, out)
}

/** Connects horizontally adjacent (left/right) cells, recursing into matching subdivisions. */
internal fun edgeProcH(shapeFunction: ShapeFunction, left: QuadTree, right: QuadTree, out: MutableList<LineSegment>) {
    when {
        left.isLeaf && right.isLeaf -> {
            // left and right are always the same size here: any ambiguous (isLeaf) cell is
            // driven all the way to maxDepth by buildQuadTree, so two of them being directly
            // compared can't differ in depth.
            val x = left.cell.x + left.cell.width
            if (hasSignChange(shapeFunction, Vector2(x, left.cell.y), Vector2(x, left.cell.y + left.cell.height))) {
                out.add(LineSegment(featureOf(shapeFunction, left), featureOf(shapeFunction, right)))
            }
        }

        left.isLeaf && right.children.isNotEmpty() -> {
            val (a, _, c, _) = right.children
            edgeProcH(shapeFunction, left, a, out)
            edgeProcH(shapeFunction, left, c, out)
        }

        left.children.isNotEmpty() && right.isLeaf -> {
            val (_, b, _, d) = left.children
            edgeProcH(shapeFunction, b, right, out)
            edgeProcH(shapeFunction, d, right, out)
        }

        left.children.isNotEmpty() && right.children.isNotEmpty() -> {
            val (_, b, _, d) = left.children
            val (a, _, c, _) = right.children
            edgeProcH(shapeFunction, b, a, out)
            edgeProcH(shapeFunction, d, c, out)
        }
        // Any combination touching a full/empty node contributes no segment: there is no
        // contour feature on a side that borders a cell entirely inside or outside.
    }
}

/** Connects vertically adjacent (lower/upper) cells, recursing into matching subdivisions. */
internal fun edgeProcV(shapeFunction: ShapeFunction, lower: QuadTree, upper: QuadTree, out: MutableList<LineSegment>) {
    when {
        lower.isLeaf && upper.isLeaf -> {
            // Same-size guarantee as in edgeProcH applies here too.
            val y = lower.cell.y + lower.cell.height
            if (hasSignChange(shapeFunction, Vector2(lower.cell.x, y), Vector2(lower.cell.x + lower.cell.width, y))) {
                out.add(LineSegment(featureOf(shapeFunction, lower), featureOf(shapeFunction, upper)))
            }
        }

        lower.children.isNotEmpty() && upper.isLeaf -> {
            val (_, _, c, d) = lower.children
            edgeProcV(shapeFunction, c, upper, out)
            edgeProcV(shapeFunction, d, upper, out)
        }

        lower.isLeaf && upper.children.isNotEmpty() -> {
            val (a, b, _, _) = upper.children
            edgeProcV(shapeFunction, lower, a, out)
            edgeProcV(shapeFunction, lower, b, out)
        }

        lower.children.isNotEmpty() && upper.children.isNotEmpty() -> {
            val (_, _, c, d) = lower.children
            val (a, b, _, _) = upper.children
            edgeProcV(shapeFunction, c, a, out)
            edgeProcV(shapeFunction, d, b, out)
        }
    }
}

