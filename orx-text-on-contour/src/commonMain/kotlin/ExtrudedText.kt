package org.openrndr.extra.textoncontour

import org.openrndr.draw.font.Face
import org.openrndr.extra.meshgenerators.TriangleMeshBuilder
import org.openrndr.extra.textwriter.ContourTextLayout
import org.openrndr.extra.textwriter.TypesetGlyph
import org.openrndr.extra.textwriter.TypesetLine
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.FillRule
import org.openrndr.shape.triangulate
import kotlin.math.abs

/**
 * Writes the glyphs of [layout] as solid letters: every glyph filled, with the holes of letters like o and e cut out,
 * and extruded along the z-axis of its own placement, so that text along a contour or a 3D path, see
 * [layoutTextOnPath3D], becomes solid. Caps face out on both sides, sides face out from the outlines, and triangles are
 * wound counterclockwise seen from outside, also where a placement mirrors.
 *
 * @param depth the length of the extrusion, in the units of the placed text, along the z-axis of the placement of every
 * glyph, from 0 to [depth] (negative goes the other way), or from -depth / 2 to depth / 2 when [center] is set
 * @param distanceTolerance the tolerance of the outlines, in the units of the placed text, or null for 1/200 of the em
 * of every glyph as it is placed
 * @param center true to center the extrusion on the plane the glyph is placed in
 */
fun TriangleMeshBuilder.extrudeText(
    layout: ContourTextLayout,
    depth: Double,
    distanceTolerance: Double? = null,
    center: Boolean = true
) = extrudeGlyphs(layout.glyphs.map { it.glyph to it.transform }, depth, distanceTolerance, center)

/**
 * Writes the glyphs of [lines], as typeset by [org.openrndr.extra.textwriter.typesetText], as solid letters: see the
 * [ContourTextLayout] overload. The z-axis of their placement is the z-axis of the text.
 */
fun TriangleMeshBuilder.extrudeText(
    lines: List<TypesetLine>,
    depth: Double,
    distanceTolerance: Double? = null,
    center: Boolean = true
) = extrudeGlyphs(lines.flatMap { line -> line.glyphs.map { it to it.transform() } }, depth, distanceTolerance, center)

/** the filled outline of a glyph in its own coordinates: counterclockwise triangles, and its rim with the inside left */
private class GlyphFill(val triangles: List<Vector2>, val rim: List<Pair<Vector2, Vector2>>)

private val fills = mutableMapOf<Triple<Face, Int, Double>, GlyphFill>()

/** the fill of the glyph [index] of [face] at [tolerance], made once */
private fun fill(face: Face, index: Int, tolerance: Double): GlyphFill =
    fills.getOrPut(Triple(face, index, tolerance)) {
        val rings = face.glyphForIndex(index).shape().closedContours.filter { it.segments.isNotEmpty() }
            .map { it.adaptivePositions(tolerance).dropLast(1) }.filter { it.size >= 3 }
        val points = rings.flatten()
        val indices = triangulate(rings, FillRule.NON_ZERO)
        val triangles = ArrayList<Vector2>()
        val corners = ArrayList<IntArray>()
        for (t in 0 until indices.size / 3) {
            var a = indices[3 * t]
            var b = indices[3 * t + 1]
            val c = indices[3 * t + 2]
            if (a !in points.indices || b !in points.indices || c !in points.indices) continue
            val area = (points[b] - points[a]).let { e -> e.x * (points[c] - points[a]).y - e.y * (points[c] - points[a]).x }
            if (area == 0.0) continue
            if (area < 0.0) a = b.also { b = a }
            corners.add(intArrayOf(a, b, c))
            triangles.addAll(listOf(points[a], points[b], points[c]))
        }
        // the edges that only one triangle uses are the rim; in a counterclockwise triangle the inside is on their left
        val uses = HashMap<Pair<Int, Int>, Int>()
        for (c in corners) for (k in 0..2) {
            val e = minOf(c[k], c[(k + 1) % 3]) to maxOf(c[k], c[(k + 1) % 3])
            uses[e] = (uses[e] ?: 0) + 1
        }
        val rim = corners.flatMap { c ->
            (0..2).mapNotNull { k ->
                val a = c[k]
                val b = c[(k + 1) % 3]
                if (uses[minOf(a, b) to maxOf(a, b)] == 1) points[a] to points[b] else null
            }
        }
        GlyphFill(triangles, rim)
    }

private fun TriangleMeshBuilder.extrudeGlyphs(
    glyphs: List<Pair<TypesetGlyph, Matrix44>>,
    depth: Double,
    distanceTolerance: Double?,
    center: Boolean
) {
    for ((glyph, placement) in glyphs) {
        val x = Vector3(placement.c0r0, placement.c0r1, placement.c0r2)
        val y = Vector3(placement.c1r0, placement.c1r1, placement.c1r2)
        val z = Vector3(placement.c2r0, placement.c2r1, placement.c2r2)
        val scale = maxOf(x.length, y.length)
        if (scale <= 0.0 || z.length <= 0.0) continue
        // the tolerance and the depth in the units of the outline of the glyph
        val em = glyph.face.sizeInPoints * scale
        val tolerance = (distanceTolerance ?: (em / 200.0)) / scale
        val d = depth / z.length
        val (z0, z1) = if (center) (-d / 2.0 to d / 2.0) else (0.0 to d)
        val lo = minOf(z0, z1)
        val hi = maxOf(z0, z1)
        if (hi - lo <= 0.0) continue
        val fill = fill(glyph.face, glyph.glyphIndex, tolerance)

        isolated {
            transform *= placement
            // a placement that mirrors turns the winding of its triangles around
            val m = transform
            val mirrored = Vector3(m.c0r0, m.c0r1, m.c0r2).cross(Vector3(m.c1r0, m.c1r1, m.c1r2))
                .dot(Vector3(m.c2r0, m.c2r1, m.c2r2)) < 0.0
            fun triangle(a: Vector3, b: Vector3, c: Vector3, n: Vector3, ta: Vector2, tb: Vector2, tc: Vector2) {
                if (mirrored) {
                    write(a, n, ta); write(c, n, tc); write(b, n, tb)
                } else {
                    write(a, n, ta); write(b, n, tb); write(c, n, tc)
                }
            }
            // the caps: the one at hi faces along z, the one at lo against it
            val t = fill.triangles
            for (i in 0 until t.size / 3) {
                val a = t[3 * i]
                val b = t[3 * i + 1]
                val c = t[3 * i + 2]
                triangle(Vector3(a.x, a.y, hi), Vector3(b.x, b.y, hi), Vector3(c.x, c.y, hi), Vector3.UNIT_Z, a, b, c)
                triangle(Vector3(a.x, a.y, lo), Vector3(c.x, c.y, lo), Vector3(b.x, b.y, lo), -Vector3.UNIT_Z, a, c, b)
            }
            // the sides, facing out: to the right of the rim, which has the inside on its left
            for ((a, b) in fill.rim) {
                val e = b - a
                if (e.length <= 0.0) continue
                val n = Vector3(e.y, -e.x, 0.0).normalized
                val a0 = Vector3(a.x, a.y, lo)
                val b0 = Vector3(b.x, b.y, lo)
                val a1 = Vector3(a.x, a.y, hi)
                val b1 = Vector3(b.x, b.y, hi)
                triangle(a0, b0, b1, n, Vector2(0.0, 0.0), Vector2(1.0, 0.0), Vector2(1.0, 1.0))
                triangle(a0, b1, a1, n, Vector2(0.0, 0.0), Vector2(1.0, 1.0), Vector2(0.0, 1.0))
            }
        }
    }
}
