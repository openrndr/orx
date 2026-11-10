package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.triangulate
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sign

/** a vertex of a surface on a Bezier patch: its [position], its [normal] of unit length and its [texCoord] */
data class PatchVertex(val position: Vector3, val normal: Vector3, val texCoord: Vector2)

/**
 * The part of a Bezier patch inside a [shape], tessellated: the counterpart of
 * [org.openrndr.extra.shapes.distort.envelope] for the area of a shape instead of its outline. The x and y of the shape
 * in the [basis] rectangle give the u and v on the patch, as they do for the envelope, so the rim of the surface follows
 * the enveloped outline.
 *
 * The tessellation is made in the u, v square, once: the shape is triangulated and its triangles are clipped to a grid
 * of [gridSize] by [gridSize] cells, so that no triangle spans more than a cell and the surface bends with the patch
 * inside the shape too. Neighbouring triangles share their corners, and the edges of the shape are split where they
 * cross the grid, so the surface has no cracks. Since the tessellation does not depend on the control points of the
 * patch, [vertices] can map it onto a patch that changes, frame by frame.
 *
 * @param shape the shape, in the coordinates of [basis]; holes, like that of an O, are cut out by [fillRule], and
 * contours that overlap or cross themselves fill the shape once
 * @param basis the rectangle of the shape that maps to the whole patch
 * @param gridSize the number of grid cells along u and along v
 * @param distanceTolerance the tolerance of the outline of the shape as it is sampled, in the coordinates of [basis]
 * @param fillRule how the contours of the shape fill it
 * @param clipToPatch true to keep only the part of the shape on the patch, where u and v are from 0 to 1; false to
 * keep all of it, where the patch is extrapolated beyond its edges
 */
class PatchClip(
    shape: Shape,
    val basis: Rectangle,
    val gridSize: Int = 32,
    distanceTolerance: Double = 0.5,
    fillRule: FillRule = FillRule.NON_ZERO,
    val clipToPatch: Boolean = true
) {
    /** the triangles in the u, v square, three corners each, counterclockwise */
    val triangles: List<Vector2>

    /**
     * the edges of the rim of the surface in the u, v square, split where they cross the grid: every edge runs with the
     * inside of the surface on its left
     */
    val boundary: List<Pair<Vector2, Vector2>>

    /** the distinct corners of the [triangles], in the u, v square */
    val uvs: List<Vector2>

    /** the [triangles] as indices into [uvs], three for every triangle */
    val indices: IntArray

    init {
        fun uv(p: Vector2) = Vector2((p.x - basis.x) / basis.width, (p.y - basis.y) / basis.height)
        // the contours sampled like openrndr's triangulation samples them, without the repeated start
        val rings = shape.closedContours.filter { it.segments.isNotEmpty() }
            .map { c -> c.adaptivePositions(distanceTolerance).dropLast(1).map(::uv) }
            .filter { it.size >= 3 }
        // triangulated as positions, so that the points where contours cross are kept and the fill rule holds
        val flat = if (rings.isEmpty()) emptyList() else triangulate(
            Shape(rings.map { ShapeContour.fromPoints(it, true) }), distanceTolerance = 1E-9, fillRule = fillRule
        )

        val result = ArrayList<Vector2>()
        for (t in 0 until flat.size / 3) {
            var a = flat[3 * t]
            var b = flat[3 * t + 1]
            val c = flat[3 * t + 2]
            val area = (b - a).let { e -> e.x * (c - a).y - e.y * (c - a).x }
            if (area == 0.0) continue
            if (area < 0.0) a = b.also { b = a }
            clipToGrid(listOf(a, b, c), result)
        }
        // the distinct corners, and the triangles by them: crossings of the grid that neighbouring triangles compute
        // from different parts of a shared edge can differ in their last bits, so corners within 1E-10 are one
        val index = HashMap<Pair<Long, Long>, Int>()
        val distinct = ArrayList<Vector2>()
        indices = IntArray(result.size) { k ->
            val p = result[k]
            index.getOrPut(round(p.x * 1E10).toLong() to round(p.y * 1E10).toLong()) {
                distinct.add(p); distinct.size - 1
            }
        }
        uvs = distinct
        triangles = indices.map { uvs[it] }
        boundary = boundaryEdges()
    }

    /** clips the counterclockwise triangle [triangle] to the cells of the grid, adding the pieces to [out] */
    private fun clipToGrid(triangle: List<Vector2>, out: MutableList<Vector2>) {
        val n = gridSize.coerceAtLeast(1)
        var i0 = floor(triangle.minOf { it.x } * n).toInt()
        var i1 = floor(triangle.maxOf { it.x } * n).toInt()
        var j0 = floor(triangle.minOf { it.y } * n).toInt()
        var j1 = floor(triangle.maxOf { it.y } * n).toInt()
        if (clipToPatch) {
            i0 = i0.coerceAtLeast(0); i1 = i1.coerceAtMost(n - 1)
            j0 = j0.coerceAtLeast(0); j1 = j1.coerceAtMost(n - 1)
        }
        for (i in i0..i1) for (j in j0..j1) {
            var poly = triangle
            poly = clipHalf(poly, true, i.toDouble() / n, keepGreater = true)
            poly = clipHalf(poly, true, (i + 1).toDouble() / n, keepGreater = false)
            poly = clipHalf(poly, false, j.toDouble() / n, keepGreater = true)
            poly = clipHalf(poly, false, (j + 1).toDouble() / n, keepGreater = false)
            if (poly.size < 3) continue
            // a convex piece, split into a fan of triangles from its first corner
            for (k in 1 until poly.size - 1) {
                val a = poly[0]
                val b = poly[k]
                val c = poly[k + 1]
                val area = (b - a).let { e -> e.x * (c - a).y - e.y * (c - a).x }
                if (area > 1E-18) out.addAll(listOf(a, b, c))
            }
        }
    }

    /**
     * the part of the convex polygon [poly] on one side of the line where x (or y when [alongX] is false) is [value]
     * (Sutherland-Hodgman); a crossing is computed from the ends of its edge in the order of the coordinate, so that
     * neighbouring triangles get the same point
     */
    private fun clipHalf(poly: List<Vector2>, alongX: Boolean, value: Double, keepGreater: Boolean): List<Vector2> {
        if (poly.isEmpty()) return poly
        fun coordinate(p: Vector2) = if (alongX) p.x else p.y
        fun inside(p: Vector2) = if (keepGreater) coordinate(p) >= value else coordinate(p) <= value
        fun crossing(p: Vector2, q: Vector2): Vector2 {
            val (lo, hi) = if (coordinate(p) <= coordinate(q)) p to q else q to p
            val t = (value - coordinate(lo)) / (coordinate(hi) - coordinate(lo))
            val r = lo + (hi - lo) * t
            return if (alongX) Vector2(value, r.y) else Vector2(r.x, value)
        }
        val result = ArrayList<Vector2>(poly.size + 2)
        for (k in poly.indices) {
            val p = poly[k]
            val q = poly[(k + 1) % poly.size]
            val pin = inside(p)
            val qin = inside(q)
            if (pin) result.add(p)
            if (pin != qin) result.add(crossing(p, q))
        }
        return result
    }

    /** the edges of the triangles that no other triangle shares, in the direction of their triangle */
    private fun boundaryEdges(): List<Pair<Vector2, Vector2>> {
        val count = HashMap<Long, Int>()
        fun key(a: Int, b: Int): Long = if (a < b) a.toLong() * 2147483647L + b else b.toLong() * 2147483647L + a
        for (t in 0 until indices.size / 3) for (k in 0..2) {
            val a = indices[3 * t + k]
            val b = indices[3 * t + (k + 1) % 3]
            if (a != b) count[key(a, b)] = (count[key(a, b)] ?: 0) + 1
        }
        val edges = ArrayList<Pair<Vector2, Vector2>>()
        for (t in 0 until indices.size / 3) for (k in 0..2) {
            val a = indices[3 * t + k]
            val b = indices[3 * t + (k + 1) % 3]
            if (a != b && count[key(a, b)] == 1) edges.add(uvs[a] to uvs[b])
        }
        return edges
    }

    /**
     * The triangles of the surface on [patch], three vertices each. With a [thickness] of 0 it is the surface itself,
     * facing along the normal of the patch. Otherwise it is a solid between the surface and its offset by [thickness]
     * along the normal (against it when negative): a face at either side, facing away from the solid, and walls along
     * the rim, facing out. Triangles are wound counterclockwise seen from where their normals point. Texture coordinates
     * are the u and v of the patch.
     */
    fun vertices(patch: BezierPatch3D, thickness: Double = 0.0): List<PatchVertex> {
        // every distinct corner once
        val positions = Array(uvs.size) { patch.position(uvs[it].x, uvs[it].y) }
        val normals = Array(uvs.size) { patch.normal(uvs[it].x, uvs[it].y) }

        val result = ArrayList<PatchVertex>(indices.size * (if (thickness == 0.0) 1 else 2) + boundary.size * 6)
        // a face at the offset h, facing along the normal when [up], against it otherwise
        fun face(h: Double, up: Boolean) {
            val s = if (up) 1.0 else -1.0
            for (t in 0 until indices.size / 3) {
                val a = indices[3 * t]
                val b = indices[3 * t + 1]
                val c = indices[3 * t + 2]
                // counterclockwise in u, v faces along du x dv, the normal
                for (k in if (up) intArrayOf(a, b, c) else intArrayOf(a, c, b)) {
                    result.add(PatchVertex(positions[k] + normals[k] * h, normals[k] * s, uvs[k]))
                }
            }
        }
        if (thickness == 0.0) {
            face(0.0, true)
            return result
        }
        val sign = sign(thickness)
        face(thickness, sign > 0.0)
        face(0.0, sign < 0.0)
        // the walls along the rim, between the surface and its offset, facing away from the inside on the left
        fun lifted(p: Vector2, h: Double) = patch.position(p.x, p.y) + patch.normal(p.x, p.y) * h
        for ((a, b) in boundary) {
            val a0 = lifted(a, 0.0)
            val b0 = lifted(b, 0.0)
            val a1 = lifted(a, thickness)
            val b1 = lifted(b, thickness)
            // outward on the patch: to the right of the edge in u, v, by the derivatives of the patch
            val e = b - a
            val mid = (a + b) * 0.5
            val outward = patch.du(mid.x, mid.y) * e.y - patch.dv(mid.x, mid.y) * e.x
            // the corners of the wall with their u, v, wound to face outward
            var quad = listOf(a0 to a, b0 to b, b1 to b, a1 to a)
            var n = (b0 - a0).cross(b1 - a0).let { if (it.length < 1E-18) (b1 - a0).cross(a1 - a0) else it }
            if (n.dot(outward) < 0.0) {
                quad = quad.reversed()
                n = -n
            }
            val normal = n.normalized
            for (k in listOf(0, 1, 2, 0, 2, 3)) result.add(PatchVertex(quad[k].first, normal, quad[k].second))
        }
        return result
    }
}

/**
 * The part of this patch inside [shape], tessellated, see [PatchClip]: the surface, or with a [thickness] a solid
 * between the surface and its offset along the normal. For a patch that changes from frame to frame, make a
 * [PatchClip] once and map it with [PatchClip.vertices] instead.
 */
fun BezierPatch3D.clip(
    shape: Shape,
    basis: Rectangle,
    gridSize: Int = 32,
    thickness: Double = 0.0,
    distanceTolerance: Double = 0.5
): List<PatchVertex> = PatchClip(shape, basis, gridSize, distanceTolerance).vertices(this, thickness)
