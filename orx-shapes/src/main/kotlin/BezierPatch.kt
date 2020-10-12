package org.openrndr.extra.shapes

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import kotlin.random.Random

class BezierPatch(val points: List<List<Vector2>>) {
    init {
        require(points.size == 4 && points.all { it.size == 4 })
    }

    /**
     * Return a transposed version of the bezier path by transposing the [points] matrix
     */
    val transposed
        get() = BezierPatch(
                listOf(
                        listOf(points[0][0], points[1][0], points[2][0], points[3][0]),
                        listOf(points[0][1], points[1][1], points[2][1], points[3][1]),
                        listOf(points[0][2], points[1][2], points[2][2], points[3][2]),
                        listOf(points[0][3], points[1][3], points[2][3], points[3][3]),
                )
        )

    private fun coeffs(t: Double): DoubleArray {
        val it = 1.0 - t
        val it2 = it * it
        val it3 = it2 * it
        val t2 = t * t
        val t3 = t2 * t
        return doubleArrayOf(it3, 3 * it2 * t, 3 * it * t2, t3)
    }

    /**
     * Return a point on the patch by using its u,v parameterization
     * @param u a value between 0 and 1
     * @param v a value between 0 and 1
     */
    fun position(u: Double, v: Double): Vector2 {
        val csu = coeffs(u)
        val csv = coeffs(v)
        var result = Vector2.ZERO
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                result += points[j][i] * csu[i] * csv[j]
            }
        }
        return result
    }

    /**
     * Generate a random point on the path
     * @return a point that is uniformly distributed in uv space
     */
    fun randomPoint(random: Random = Random.Default) = position(random.nextDouble(), random.nextDouble())

    fun horizontal(v: Double): ShapeContour {
        val cs = coeffs(v)
        val cps = Array(4) { Vector2.ZERO }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                cps[j] += points[i][j] * cs[i]
            }
        }
        return ShapeContour(listOf(Segment(cps[0], cps[1], cps[2], cps[3])), false)
    }

    fun vertical(u: Double): ShapeContour {
        val cs = coeffs(u)
        val cps = Array(4) { Vector2.ZERO }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                cps[j] += points[j][i] * cs[i]
            }
        }
        return ShapeContour(listOf(Segment(cps[0], cps[1], cps[2], cps[3])), false)
    }

    /**
     * Extract a sub-patch based on uv parameterization
     */
    fun sub(u0: Double, v0: Double, u1: Double, v1: Double): BezierPatch {
        val c0 = Segment(points[0][0], points[0][1], points[0][2], points[0][3]).sub(u0, u1)
        val c1 = Segment(points[1][0], points[1][1], points[1][2], points[1][3]).sub(u0, u1)
        val c2 = Segment(points[2][0], points[2][1], points[2][2], points[2][3]).sub(u0, u1)
        val c3 = Segment(points[3][0], points[3][1], points[3][2], points[3][3]).sub(u0, u1)

        val sub0 = bezierPatch(c0, c1, c2, c3)
        val d0 = Segment(sub0.points[0][0], sub0.points[1][0], sub0.points[2][0], sub0.points[3][0]).sub(v0, v1)
        val d1 = Segment(sub0.points[0][1], sub0.points[1][1], sub0.points[2][1], sub0.points[3][1]).sub(v0, v1)
        val d2 = Segment(sub0.points[0][2], sub0.points[1][2], sub0.points[2][2], sub0.points[3][2]).sub(v0, v1)
        val d3 = Segment(sub0.points[0][3], sub0.points[1][3], sub0.points[2][3], sub0.points[3][3]).sub(v0, v1)

        return bezierPatch(d0, d1, d2, d3).transposed
    }
}

/**
 * Create a cubic bezier patch from 4 segments. The control points of the segments are used in row-wise fashion
 */
fun bezierPatch(c0: Segment, c1: Segment, c2: Segment, c3: Segment): BezierPatch {
    val c0c = c0.cubic
    val c1c = c1.cubic
    val c2c = c2.cubic
    val c3c = c3.cubic

    val c0l = listOf(c0c.start, c0c.control[0], c0c.control[1], c0c.end)
    val c1l = listOf(c1c.start, c1c.control[0], c1c.control[1], c1c.end)
    val c2l = listOf(c2c.start, c2c.control[0], c2c.control[1], c2c.end)
    val c3l = listOf(c3c.start, c3c.control[0], c3c.control[1], c3c.end)

    return BezierPatch(listOf(c0l, c1l, c2l, c3l))
}

/**
 * Create a bezier patch from a closed shape contour (with 4 segments).
 * @param alpha control for linearity, default is `1.0/3.0`
 */
fun bezierPatch(shapeContour: ShapeContour, alpha: Double = 1.0 / 3.0): BezierPatch {
    require(shapeContour.segments.size == 4) {
        """contour needs exactly 4 segments (has ${shapeContour.segments.size})"""
    }
    val c0 = shapeContour.segments[0].cubic
    val c1 = shapeContour.segments[1].cubic
    val c2 = shapeContour.segments[2].cubic
    val c3 = shapeContour.segments[3].cubic

    val fa = 1.0 - alpha
    val fb = alpha

    val x00 = (c0.control[0] * fa + c2.control[1] * fb + c3.control[1] * fa + c1.control[0] * fb) / 2.0
    val x01 = (c0.control[1] * fa + c2.control[0] * fb + c3.control[1] * fb + c1.control[0] * fa) / 2.0
    val x10 = (c0.control[0] * fb + c2.control[1] * fa + c3.control[0] * fa + c1.control[1] * fb) / 2.0
    val x11 = (c0.control[1] * fb + c2.control[0] * fa + c3.control[0] * fb + c1.control[1] * fa) / 2.0
    val cps = listOf(
            listOf(c0.start, c0.control[0], c0.control[1], c0.end),
            listOf(c3.control[1], x00, x01, c1.control[0]),
            listOf(c3.control[0], x10, x11, c1.control[1]),
            listOf(c2.end, c2.control[1], c2.control[0], c2.start),
            )
    return BezierPatch(cps)
}

/**
 * Create a bezier patch from 4 corners
 * @param corners a list of corners from which to create the patch
 * @param alpha control for linearity, default is `1.0/3.0`
 */
fun bezierPatch(corners: List<Vector2>, alpha: Double = 1.0 / 3.0): BezierPatch {
    require(corners.size == 4) {
        """need exactly 4 corners (got ${corners.size}"""
    }
    return bezierPatch(ShapeContour.fromPoints(corners, true), alpha)
}

/**
 * Distort a shape contour
  */
fun BezierPatch.distort(shapeContour: ShapeContour, referenceRectangle: Rectangle = shapeContour.bounds): ShapeContour {
    val distortedSegments = shapeContour.segments.map {
        val c = it.cubic
        val e = c.end.map(referenceRectangle)
        val c0 = c.control[0].map(referenceRectangle)
        val c1 = c.control[1].map(referenceRectangle)
        val s = c.start.map(referenceRectangle)

        val ne = position(e.x, e.y)
        val ns = position(s.x, s.y)
        val nc0 = position(c0.x, c0.y)
        val nc1 = position(c1.x, c1.y)
        Segment(ns, nc0, nc1, ne)
    }
    return ShapeContour(distortedSegments, shapeContour.closed, shapeContour.polarity)
}

private fun Vector2.map(rect: Rectangle): Vector2 {
    val nx = (x - rect.x) / rect.width
    val ny = (y - rect.y) / rect.height
    return Vector2(nx, ny)
}
