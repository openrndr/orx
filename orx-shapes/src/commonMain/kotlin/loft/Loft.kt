package org.openrndr.extra.shapes.loft

import org.openrndr.extra.shapes.functions.poseFunction
import org.openrndr.extra.shapes.functions.positionFunction
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment2D
import org.openrndr.shape.triangulate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import org.openrndr.extra.math.differential.diff
import org.openrndr.extra.math.differential.diffdx
import org.openrndr.extra.math.differential.diffdy

/** a vertex of a [Loft]: its [position], its [normal] of unit length and its [texCoord] */
data class LoftVertex(val position: Vector3, val normal: Vector3, val texCoord: Vector2)

/**
 * A surface swept by a [crossSection] in the plane of x and y of a [pose] along v from 0 to 1.
 *
 * @param closedCrossSection true when the cross-section comes round to its start at u = 1, which a solid needs
 * @param closedPath true when the pose comes round to its start at v = 1, like that of a closed path; such a loft
 * has no ends to cap
 * @param corners the u at which the cross-section has sharp corners, where its direction jumps; the [side] gives
 * those the average of the normals on either side, or with `sharpCorners` a vertex for each side
 * @param constantCrossSection true when the cross-section is the same at every v, so that the [side] evaluates it
 * once for every u; [dcrossSectiondv] is then taken to be zero
 */
class Loft(
    val crossSection: (u: Double, v: Double) -> Vector2,
    val dcrossSectiondu: (u: Double, v: Double) -> Vector2 = crossSection.diffdx(),
    val dcrossSectiondv: (u: Double, v: Double) -> Vector2 = crossSection.diffdy(),
    val pose: (v: Double) -> Matrix44,
    val dposedv: (v: Double) -> Matrix44 = pose.diff(),
    val closedCrossSection: Boolean = true,
    val closedPath: Boolean = false,
    val corners: List<Double> = emptyList(),
    val constantCrossSection: Boolean = false,
) {
    fun position(u: Double, v: Double): Vector3 = position(pose(v), crossSection(u, v))

    /** the point [p] of the cross-section placed by [pose] */
    private fun position(pose: Matrix44, p: Vector2): Vector3 = pose[3].xyz + pose[0].xyz * p.x + pose[1].xyz * p.y

    fun normal(u: Double, v: Double): Vector3 =
        normal(pose(v), dposedv(v), crossSection(u, v), dcrossSectiondu(u, v), dcrossSectiondv(u, v))

    /** the normal, of any length, at the point [p] of the cross-section with its derivatives, placed by [pose] */
    private fun normal(pose: Matrix44, dposedv: Matrix44, p: Vector2, dpdu: Vector2, dpdv: Vector2): Vector3 {
        val n = pose[0].xyz
        val b = pose[1].xyz
        val dsdu = n * dpdu.x + b * dpdu.y
        val dsdv = dposedv[3].xyz + n * dpdv.x + dposedv[0].xyz * p.x + b * dpdv.y + dposedv[1].xyz * p.y
        return dsdu.cross(dsdv)
    }

    /** a column of the grid of the [side]: its [u], and which derivative of the cross-section by u its normals take */
    private class Column(val u: Double, val side: Int)

    /** the derivative of the cross-section by u at a corner: from before it, after it, or the average of both */
    private fun cornerDerivative(u: Double, v: Double, side: Int): Vector2 {
        val h = 1E-6
        fun at(w: Double) = crossSection(if (closedCrossSection) w - floor(w) else w.coerceIn(0.0, 1.0), v)
        val p = crossSection(u, v)
        val before = (p - at(u - h)) / h
        val after = (at(u + h) - p) / h
        return when (side) {
            BEFORE -> before
            AFTER -> after
            else -> (before.normalized + after.normalized) * ((before.length + after.length) / 2)
        }
    }

    /** whether [u] is at one of the [corners], u = 0 and u = 1 being the same on a closed cross-section */
    private fun isCorner(u: Double): Boolean = corners.any { c ->
        abs(c - u) < CORNER_EPSILON || (closedCrossSection && abs(abs(c - u) - 1.0) < CORNER_EPSILON)
    }

    /**
     * the columns of the grid, from u = 0 to 1: u = k / [around], and with [sharpCorners] the [corners] too, each
     * twice, before and after it
     */
    private fun columns(around: Int, sharpCorners: Boolean): List<Column> {
        val grid = List(around + 1) { it.toDouble() / around }
        if (!sharpCorners || corners.isEmpty()) {
            return grid.map { Column(it, if (isCorner(it)) AVERAGE else SMOOTH) }
        }
        val us = (grid + corners.filter { it > 0.0 && it < 1.0 }).sorted()
            .fold(mutableListOf<Double>()) { acc, u -> if (acc.isEmpty() || u - acc.last() > CORNER_EPSILON) acc.add(u); acc }
        // the ends at the start and end of the cross-section have only one side
        return us.flatMap { u ->
            when {
                !isCorner(u) -> listOf(Column(u, SMOOTH))
                u < CORNER_EPSILON -> listOf(Column(u, AFTER))
                u > 1.0 - CORNER_EPSILON -> listOf(Column(u, BEFORE))
                else -> listOf(Column(u, BEFORE), Column(u, AFTER))
            }
        }
    }

    /**
     * the points of the cross-section at [v] through which the [side] and the caps go, without the repeated start of a
     * closed one
     */
    private fun rim(v: Double, around: Int, sharpCorners: Boolean): List<Vector2> {
        val us = columns(around, sharpCorners).map { it.u }.distinct()
        return (if (closedCrossSection) us.dropLast(1) else us).map { crossSection(it, v) }
    }

    /** +1 when the cross-section at [v] goes counterclockwise, -1 when it goes clockwise */
    private fun winding(v: Double, around: Int): Double {
        val points = rim(v, around, false)
        var area = 0.0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            area += a.x * b.y - b.x * a.y
        }
        return if (area < 0.0) -1.0 else 1.0
    }

    /** true when the loft has ends to cap: a closed cross-section along a path that is not closed */
    val hasCaps: Boolean get() = closedCrossSection && !closedPath

    /**
     * The triangles of the side of the loft, three vertices each, on a grid of [along] steps of v and [around] steps
     * of u. Normals point out of the loft and triangles are wound counterclockwise seen from outside, whichever way
     * the cross-section goes round; for a cross-section that is not closed, which makes a ribbon, they follow the
     * cross-section. Texture coordinates are (u, v).
     *
     * At the [corners] of the cross-section the normals are the average of those on either side, which shades the
     * corners round. With [sharpCorners] the grid has a column at every corner too, with a vertex for either side and
     * its normal, so that the faces meet at a sharp edge.
     *
     * The pose is evaluated once for every step of v, and a [constantCrossSection] once for every column.
     */
    fun side(along: Int, around: Int, sharpCorners: Boolean = false): List<LoftVertex> {
        val sign = if (closedCrossSection) winding(0.0, around) else 1.0
        val columns = columns(around, sharpCorners)
        fun derivative(column: Column, v: Double) =
            if (column.side == SMOOTH) dcrossSectiondu(column.u, v) else cornerDerivative(column.u, v, column.side)
        val fixed = if (constantCrossSection) columns.map { crossSection(it.u, 0.0) to derivative(it, 0.0) } else null
        val grid = List(along + 1) { j ->
            val v = j.toDouble() / along
            val pose = pose(v)
            val dpose = dposedv(v)
            List(columns.size) { k ->
                val column = columns[k]
                val p: Vector2
                val dpdu: Vector2
                val dpdv: Vector2
                if (fixed != null) {
                    p = fixed[k].first
                    dpdu = fixed[k].second
                    dpdv = Vector2.ZERO
                } else {
                    p = crossSection(column.u, v)
                    dpdu = derivative(column, v)
                    dpdv = dcrossSectiondv(column.u, v)
                }
                LoftVertex(
                    position(pose, p),
                    (normal(pose, dpose, p, dpdu, dpdv) * sign).normalized,
                    Vector2(column.u, v)
                )
            }
        }
        val result = ArrayList<LoftVertex>(along * columns.size * 6)
        for (j in 0 until along) for (k in 0 until columns.size - 1) {
            // no faces between the two vertices of a corner
            if (columns[k + 1].u - columns[k].u < CORNER_EPSILON) continue
            val a = grid[j][k]
            val b = grid[j][k + 1]
            val c = grid[j + 1][k + 1]
            val d = grid[j + 1][k]
            // counterclockwise seen from where the normal points
            if (sign > 0.0) {
                result.add(a); result.add(b); result.add(c); result.add(a); result.add(c); result.add(d)
            } else {
                result.add(a); result.add(c); result.add(b); result.add(a); result.add(d); result.add(c)
            }
        }
        return result
    }

    /**
     * The triangles of the cap at the start, at v = 0, or at the [end], at v = 1: the cross-section filled, through
     * the same points as the [side] with the same [around] and [sharpCorners], so that its rim meets the side. Concave
     * cross-sections are filled too. The normal points back along the path at the start and along it at the end, and
     * triangles are wound counterclockwise seen from there. Texture coordinates map the bounds of the cross-section to
     * 0..1. Empty when the loft [hasCaps] not.
     */
    fun cap(end: Boolean, around: Int, sharpCorners: Boolean = false): List<LoftVertex> {
        if (!hasCaps) return emptyList()
        val v = if (end) 1.0 else 0.0
        val points = rim(v, around, sharpCorners)
        val indices = triangulate(listOf(points))
        val pose = pose(v)
        val x = pose[0].xyz
        val y = pose[1].xyz
        val origin = pose[3].xyz
        val normal = (x.cross(y) * if (end) 1.0 else -1.0).normalized
        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val w = (points.maxOf { it.x } - minX).takeIf { it > 0.0 } ?: 1.0
        val h = (points.maxOf { it.y } - minY).takeIf { it > 0.0 } ?: 1.0
        fun vertex(i: Int): LoftVertex {
            val p = points[i]
            return LoftVertex(origin + x * p.x + y * p.y, normal, Vector2((p.x - minX) / w, (p.y - minY) / h))
        }
        val result = ArrayList<LoftVertex>(indices.size)
        for (t in 0 until indices.size / 3) {
            val i0 = indices[3 * t]
            val i1 = indices[3 * t + 1]
            val i2 = indices[3 * t + 2]
            if (i0 !in points.indices || i1 !in points.indices || i2 !in points.indices) continue
            val a = points[i0]
            val b = points[i1]
            val c = points[i2]
            // counterclockwise in the plane of the pose faces along the path: the start cap is wound the other way
            val ccw = (b - a).let { e -> e.x * (c - a).y - e.y * (c - a).x } > 0.0
            if (ccw == end) result.addAll(listOf(vertex(i0), vertex(i1), vertex(i2)))
            else result.addAll(listOf(vertex(i0), vertex(i2), vertex(i1)))
        }
        return result
    }
}

/**
 * A loft of this contour along [pose]: the cross-section at the rectified u of the contour, with the derivatives of
 * the cross-section and of the pose exact, and the corners of the contour, where its direction turns more than
 * [cornerAngle] (radians) from one segment to the next, as the [Loft.corners].
 */
fun RectifiedContour.loft(pose: PosePath3D, cornerAngle: Double = 1E-3): Loft {
    val length = contour.length
    return Loft(
        crossSection = { u: Double, _: Double -> this.positionFunction(u) },
        dcrossSectiondu = { u: Double, _: Double -> direction(u).normalized * length },
        dcrossSectiondv = { _: Double, _: Double -> Vector2.ZERO },
        pose = pose.poseFunction,
        dposedv = { v -> pose.poseDerivative(v) },
        closedCrossSection = originalPath.closed,
        closedPath = pose.path.path.closed,
        corners = corners(cornerAngle),
        constantCrossSection = true
    )
}

/** the rectified u of the joins of the segments of this contour where its direction turns more than [angle] */
private fun RectifiedContour.corners(angle: Double): List<Double> {
    val segments = contour.segments
    val m = segments.size
    val joins = if (contour.closed) 0 until m else 1 until m
    val limit = cos(angle)
    return joins.mapNotNull { i ->
        val before = segments[(i - 1 + m) % m]
        val after = segments[i]
        if ((before.endTangent() dot after.startTangent()) < limit) {
            if (i == 0) 0.0 else inverseRectify(i.toDouble() / m)
        } else null
    }
}

/** the direction in which this segment starts, from the first of its points that is not at its start */
private fun Segment2D.startTangent(): Vector2 =
    (control + end).map { it - start }.firstOrNull { it.squaredLength > 1E-24 }?.normalized ?: Vector2.ZERO

/** the direction in which this segment ends, from the last of its points that is not at its end */
private fun Segment2D.endTangent(): Vector2 =
    (listOf(start) + control).reversed().map { end - it }.firstOrNull { it.squaredLength > 1E-24 }?.normalized
        ?: Vector2.ZERO

/** columns of the side of a [Loft]: smooth, before a corner, after a corner, or the average at a corner */
private const val SMOOTH = 0
private const val BEFORE = 1
private const val AFTER = 2
private const val AVERAGE = 3

/** how close u are to be the same, as for corners */
private const val CORNER_EPSILON = 1E-9
