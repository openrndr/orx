package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.shape.FillRule
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.triangulateIndexed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A loop of the cross-section of an extrusion: its [points], without the start repeated when it is [closed], and the
 * [normals] at them in the plane of the cross-section, of unit length, pointing out of the solid: away from the inside
 * of an outer loop and into a hole
 */
internal class ExtrusionLoop(val points: List<Vector2>, val normals: List<Vector2>, val closed: Boolean)

/**
 * The loops of a cross-section made of [contours], sampled to [distanceTolerance]. A loop inside an odd number of
 * other loops is a hole. The normals are those of the contours, the average of both sides at a corner, which shades
 * corners round; an open contour has its normals on its right.
 */
internal fun crossSectionLoops(contours: List<ShapeContour>, distanceTolerance: Double): List<ExtrusionLoop> {
    val sampled = contours.filter { !it.empty }.map { contour ->
        var samples = contour.adaptivePositionsWithT(distanceTolerance)
        if (contour.closed && samples.size > 1 &&
            samples.first().first.squaredDistanceTo(samples.last().first) < 1E-18
        ) samples = samples.dropLast(1)
        contour to samples
    }
    val polygons = sampled.map { (_, samples) -> samples.map { it.first } }
    return sampled.mapIndexed { index, (contour, samples) ->
        val points = polygons[index]
        val hole = contour.closed &&
                polygons.indices.count { it != index && sampled[it].first.closed && inside(points[0], polygons[it]) } % 2 == 1
        loopOf(contour, samples, hole)
    }
}

/**
 * The loop of [contour] through its [samples], points with their parameters, without the start repeated; a [hole]
 * has its normals into it
 */
internal fun loopOf(contour: ShapeContour, samples: List<Pair<Vector2, Double>>, hole: Boolean = false): ExtrusionLoop {
    val points = samples.map { it.first }
    // which side is out: the right of a counterclockwise loop, the left of a clockwise one, the other for a hole
    val area = points.indices.sumOf { i ->
        val a = points[i]
        val b = points[(i + 1) % points.size]
        a.x * b.y - b.x * a.y
    }
    val side = (if (!contour.closed || area >= 0.0) 1.0 else -1.0) * (if (hole) -1.0 else 1.0)
    val normals = samples.map { (_, t) ->
        val d = tangent(contour, t)
        Vector2(d.y, -d.x) * side
    }
    return ExtrusionLoop(points, normals, contour.closed)
}

/**
 * [count] samples of [contour] at equal distances along it, with their parameters, without the start repeated on a
 * closed contour
 */
internal fun evenSamples(contour: ShapeContour, count: Int): List<Pair<Vector2, Double>> {
    val rectified = contour.rectified()
    val n = count.coerceAtLeast(2)
    return List(n) { k ->
        val u = if (contour.closed) k.toDouble() / n else k.toDouble() / (n - 1)
        val t = rectified.rectify(u)
        contour.position(t) to t
    }
}

/** the direction of [contour] at [t], the average of both sides at a corner */
private fun tangent(contour: ShapeContour, t: Double): Vector2 {
    val h = 1E-7
    fun at(u: Double): Vector2 =
        contour.direction(if (contour.closed) u - kotlin.math.floor(u) else u.coerceIn(0.0, 1.0))
    val before = at(t - h)
    val after = at(t + h)
    val sum = before + after
    return if (sum.squaredLength > 1E-12) sum.normalized else after
}

/** true when [p] is inside [polygon], by the even-odd rule */
private fun inside(p: Vector2, polygon: List<Vector2>): Boolean {
    var result = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) result = !result
        j = i
    }
    return result
}

/**
 * A step of an extrusion: the cross-section placed at [origin] with [x] and [y] as its axes, which may be scaled and
 * turned, [z] the direction of the path. Without [dOrigin], [dx] and [dy], the derivatives of those by the parameter
 * along the path, normals are the normals of the cross-section turned by [normalX] and [normalY]
 */
internal class ExtrusionRow(
    val origin: Vector3,
    val x: Vector3,
    val y: Vector3,
    val z: Vector3,
    val normalX: Vector3,
    val normalY: Vector3,
    val dOrigin: Vector3? = null,
    val dx: Vector3? = null,
    val dy: Vector3? = null
) {
    fun position(p: Vector2): Vector3 = origin + x * p.x + y * p.y

    /** the normal at the point [p] of the cross-section, whose normal in its plane is [n] and whose direction is [d] */
    fun normal(p: Vector2, n: Vector2, d: Vector2): Vector3 {
        val estimate = (normalX * n.x + normalY * n.y).normalized
        if (dOrigin == null || dx == null || dy == null) return estimate
        val dsdu = x * d.x + y * d.y
        val dsdv = dOrigin + dx * p.x + dy * p.y
        val exact = dsdu cross dsdv
        if (exact.squaredLength < 1E-24 || !exact.x.isFinite()) return estimate
        return (if ((exact dot estimate) < 0.0) -exact else exact).normalized
    }
}

/** the steps of [frames], the first again at the end of a [closed] path unless the frames come round already */
internal fun frameRows(frames: List<Matrix44>, closed: Boolean): List<ExtrusionRow> {
    val rows = frames.map { frame ->
        val n = normalMatrix(frame)
        ExtrusionRow(frame[3].xyz, frame[0].xyz, frame[1].xyz, frame[2].xyz.normalized, n[0].xyz, n[1].xyz)
    }
    if (!closed || rows.size < 2) return rows
    return if (rows.last().origin.squaredDistanceTo(rows.first().origin) < 1E-18) rows else rows + rows.first()
}

/**
 * [count] positions at equal distances along this path, from its start to its end; a closed path does not repeat its
 * start at the end
 */
internal fun Path3D.evenSteps(count: Int): List<Vector3> {
    if (empty || count < 1) return emptyList()
    val rectified = rectified()
    return if (closed) List(count) { rectified.position(it.toDouble() / count) }
    else List(count) { rectified.position(if (count == 1) 0.0 else it.toDouble() / (count - 1)) }
}

/**
 * The poses of a [PosePath3D] of this path, starting with [up0], at the [steps] along it: positions of the path in
 * order, each found after the one before it
 */
internal fun Path3D.posesAt(steps: List<Vector3>, up0: Vector3): List<Matrix44> {
    if (steps.isEmpty() || empty) return emptyList()
    val rectified = rectified()
    val poses = PosePath3D(rectified, up0)
    val length = this.length.takeIf { it > 0.0 } ?: return steps.map { Matrix44.IDENTITY }
    var previous = 0.0
    return steps.mapIndexed { i, step ->
        val v = when {
            i == 0 -> 0.0
            i == steps.lastIndex && !closed -> 1.0
            else -> {
                // the parameter nearest to the step, a little way after that of the step before it
                val reach = 3.0 * step.distanceTo(steps[i - 1]) / length + 1E-9
                var lo = previous
                var hi = (previous + reach).coerceAtMost(1.0)
                fun d(v: Double) = rectified.position(v).squaredDistanceTo(step)
                val g = (kotlin.math.sqrt(5.0) - 1) / 2
                var a = hi - g * (hi - lo)
                var b = lo + g * (hi - lo)
                var da = d(a)
                var db = d(b)
                repeat(60) {
                    if (da < db) {
                        hi = b; b = a; db = da; a = hi - g * (hi - lo); da = d(a)
                    } else {
                        lo = a; a = b; da = db; b = lo + g * (hi - lo); db = d(b)
                    }
                }
                (lo + hi) / 2
            }
        }
        previous = v
        val pose = poses.pose(v)
        // at the step itself
        Matrix44.fromColumnVectors(pose[0], pose[1], pose[2], step.xyz1)
    }
}

/**
 * The steps of an extrusion along [path] with the cross-section scaled by [scale] and turned by [torsion] degrees,
 * both functions of the parameter of the path: the poses of a [PosePath3D] starting with [up0], with their derivatives,
 * at steps of the rectified parameter that are halved until the path is within [distanceTolerance] of their chords and
 * turns at most [MAX_STEP_TURN] in each
 */
internal fun scaledRows(
    path: Path3D,
    scale: (Double) -> Double,
    torsion: (Double) -> Double,
    up0: Vector3,
    distanceTolerance: Double
): List<ExtrusionRow> {
    if (path.empty) return emptyList()
    val rectified = path.rectified()
    val poses = PosePath3D(rectified, up0)
    val closed = path.closed

    // the steps
    val vs = mutableListOf(0.0)
    val cosMaxTurn = cos(MAX_STEP_TURN)
    fun add(v0: Double, v1: Double, depth: Int) {
        val p0 = rectified.position(v0)
        val p1 = rectified.position(v1)
        val pm = rectified.position((v0 + v1) / 2)
        val sagitta = pm.distanceTo((p0 + p1) / 2.0)
        val turn = rectified.direction(v0).normalized dot rectified.direction(v1).normalized
        if (depth < 16 && (sagitta > distanceTolerance || turn < cosMaxTurn)) {
            add(v0, (v0 + v1) / 2, depth + 1)
            add((v0 + v1) / 2, v1, depth + 1)
        } else {
            vs.add(v1)
        }
    }
    val start = path.segments.size.coerceAtLeast(1)
    for (k in 0 until start) add(k.toDouble() / start, (k + 1.0) / start, 0)

    val h = 1E-5
    return vs.map { v ->
        val pose = poses.pose(v)
        val dpose = poses.poseDerivative(v)
        // scale and torsion, by the parameter of the path at v, and their derivatives by v
        fun t(w: Double) = rectified.rectify(if (closed) w - kotlin.math.floor(w) else w.coerceIn(0.0, 1.0))
        val v0 = if (closed) v - h else (v - h).coerceAtLeast(0.0)
        val v1 = if (closed) v + h else (v + h).coerceAtMost(1.0)
        val t = t(v)
        val s = scale(t)
        val ds = (scale(t(v1)) - scale(t(v0))) / (v1 - v0)
        val theta = torsion(t) * PI / 180.0
        val dtheta = (torsion(t(v1)) - torsion(t(v0))) / (v1 - v0) * PI / 180.0
        val c = cos(theta)
        val sn = sin(theta)
        val x = pose[0].xyz
        val y = pose[1].xyz
        val dx = dpose[0].xyz
        val dy = dpose[1].xyz
        // the axes of the cross-section turned by theta and scaled by s, and their derivatives
        val tx = x * c + y * sn
        val ty = y * c - x * sn
        ExtrusionRow(
            origin = pose[3].xyz,
            x = tx * s,
            y = ty * s,
            z = pose[2].xyz,
            normalX = tx,
            normalY = ty,
            dOrigin = dpose[3].xyz,
            dx = tx * ds + ty * (s * dtheta) + (dx * c + dy * sn) * s,
            dy = ty * ds - tx * (s * dtheta) + (dy * c - dx * sn) * s
        )
    }
}

/** the largest turn of the path within a step of [scaledRows], in radians */
private const val MAX_STEP_TURN = 5.0 * PI / 180.0

/**
 * Writes the side of an extrusion of [loops] through [rows], and, along a path that is not [closedPath], the caps at
 * its ends through the same points. Vertex normals point out of the solid, and every triangle is wound
 * counterclockwise seen from where they point. With [useFaceNormals] every quad has the normal of its face. Texture
 * coordinates on the side are the part of the loop and the part of the steps.
 */
internal fun extrudeLoops(
    loops: List<ExtrusionLoop>,
    rows: List<ExtrusionRow>,
    closedPath: Boolean,
    startCap: Boolean,
    endCap: Boolean,
    useFaceNormals: Boolean,
    writer: VertexWriter
) = extrudeRowLoops({ loops }, rows, closedPath, startCap, endCap, useFaceNormals, false, writer)

/**
 * As [extrudeLoops], with the loops of every row from [loopsAt], the same number of them with the same number of
 * points in every row. With [neighbourNormals], rows without derivatives take the change along the path from the rows
 * on either side, so that normals follow a cross-section that changes along the path, like a cone.
 */
internal fun extrudeRowLoops(
    loopsAt: (row: Int) -> List<ExtrusionLoop>,
    rows: List<ExtrusionRow>,
    closedPath: Boolean,
    startCap: Boolean,
    endCap: Boolean,
    useFaceNormals: Boolean,
    neighbourNormals: Boolean,
    writer: VertexWriter
) {
    if (rows.size < 2) return
    val rowLoops = rows.indices.map(loopsAt)
    val loopCount = rowLoops.minOf { it.size }
    // the first row again at the end of a closed path
    val wraps = closedPath && rows.last().origin.squaredDistanceTo(rows.first().origin) < 1E-18
    for (l in 0 until loopCount) {
        val loops = rowLoops.map { it[l] }
        val n = loops.minOf { it.points.size }
        if (n < 2) continue
        val closedLoop = loops[0].closed
        val positions = rows.mapIndexed { j, row -> List(n) { k -> row.position(loops[j].points[k]) } }
        // the side comes round where the first and the last row meet, not where a scale or a morph ends elsewhere
        val meets = wraps && (0 until n).all { k -> positions[0][k].squaredDistanceTo(positions.last()[k]) < 1E-18 }
        val normals = rows.mapIndexed { j, row ->
            val loop = loops[j]
            List(n) { k ->
                val p = loop.points[k]
                val normal = loop.normals[k]
                val tangent = Vector2(-normal.y, normal.x)
                if (!neighbourNormals || row.dOrigin != null) {
                    row.normal(p, normal, tangent)
                } else {
                    val last = rows.size - 1
                    // the change of a value along the path: central between the neighbours, of second order at open
                    // ends
                    fun change(f: (Int) -> Vector3): Vector3 = when {
                        j in 1 until last -> f(j + 1) - f(j - 1)
                        meets -> f(1) - f(last - 1)
                        rows.size < 3 -> (f(last) - f(0)) * 2.0
                        j == 0 -> f(1) * 4.0 - f(0) * 3.0 - f(2)
                        else -> f(last) * 3.0 - f(last - 1) * 4.0 + f(last - 2)
                    }
                    // along the path, and the change of the cross-section in its plane, by scale or by morph, in the
                    // coordinates of every frame, so that the turn of the frames is left out
                    fun local(i: Int): Vector3 {
                        val q = positions[i][k] - rows[i].origin
                        return Vector3(q dot rows[i].normalX.normalized, q dot rows[i].normalY.normalized, 0.0)
                    }
                    val along = change { rows[it].origin }.length
                    val dl = change(::local)
                    val dsdv = row.z * along + row.normalX.normalized * dl.x + row.normalY.normalized * dl.y
                    val estimate = (row.normalX * normal.x + row.normalY * normal.y).normalized
                    val dsdu = row.x * tangent.x + row.y * tangent.y
                    val exact = dsdu cross dsdv
                    if (exact.squaredLength < 1E-24 || !exact.x.isFinite()) estimate
                    else (if ((exact dot estimate) < 0.0) -exact else exact).normalized
                }
            }
        }
        val quads = if (closedLoop) n else n - 1
        for (j in 0 until rows.size - 1) {
            val v0 = j.toDouble() / (rows.size - 1)
            val v1 = (j + 1.0) / (rows.size - 1)
            for (k in 0 until quads) {
                val k1 = (k + 1) % n
                val u0 = k.toDouble() / quads
                val u1 = (k + 1.0) / quads
                val a = positions[j][k]
                val b = positions[j][k1]
                val c = positions[j + 1][k1]
                val d = positions[j + 1][k]
                var na = normals[j][k]
                var nb = normals[j][k1]
                var nc = normals[j + 1][k1]
                var nd = normals[j + 1][k]
                val face = (c - a) cross (d - b)
                // a quad that has collapsed to a line or a point has no face
                if (face.squaredLength < 1E-24) continue
                val out = na + nb + nc + nd
                if (useFaceNormals) {
                    val f = (if ((face dot out) < 0.0) -face else face).normalized
                    if (f.x.isFinite()) {
                        na = f; nb = f; nc = f; nd = f
                    }
                }
                val ta = Vector2(u0, v0)
                val tb = Vector2(u1, v0)
                val tc = Vector2(u1, v1)
                val td = Vector2(u0, v1)
                // counterclockwise seen from where the normals point; a triangle with no area, like where the side
                // meets a cross-section scaled to a point, is left out
                fun triangle(p0: Vector3, n0: Vector3, t0: Vector2, p1: Vector3, n1: Vector3, t1: Vector2, p2: Vector3, n2: Vector3, t2: Vector2) {
                    if (((p1 - p0) cross (p2 - p0)).squaredLength < 1E-24) return
                    writer(p0, n0, t0); writer(p1, n1, t1); writer(p2, n2, t2)
                }
                if ((face dot out) >= 0.0) {
                    triangle(a, na, ta, b, nb, tb, c, nc, tc)
                    triangle(a, na, ta, c, nc, tc, d, nd, td)
                } else {
                    triangle(a, na, ta, c, nc, tc, b, nb, tb)
                    triangle(a, na, ta, d, nd, td, c, nc, tc)
                }
            }
        }
    }
    if (!closedPath) {
        if (startCap) writeCap(rowLoops.first(), rows.first(), -rows.first().z, writer)
        if (endCap) writeCap(rowLoops.last(), rows.last(), rows.last().z, writer)
    }
}

/** the cap of the closed [loops] placed by [row], facing [facing], through the points of the loops */
private fun writeCap(loops: List<ExtrusionLoop>, row: ExtrusionRow, facing: Vector3, writer: VertexWriter) {
    val closed = loops.filter { it.closed && it.points.size >= 3 }
    if (closed.isEmpty()) return
    // a cross-section scaled to nothing has no cap
    if ((row.x cross row.y).squaredLength < 1E-24) return
    // the points of the loops, then the points where loops cross
    val triangulation = triangulateIndexed(closed.map { it.points }, FillRule.EVEN_ODD)
    val points = triangulation.vertices
    val indices = triangulation.indices
    val normal = facing.normalized
    val minX = points.minOf { it.x }
    val minY = points.minOf { it.y }
    val w = (points.maxOf { it.x } - minX).takeIf { it > 0.0 } ?: 1.0
    val h = (points.maxOf { it.y } - minY).takeIf { it > 0.0 } ?: 1.0
    fun write(i: Int) {
        val p = points[i]
        writer(row.position(p), normal, Vector2((p.x - minX) / w, (p.y - minY) / h))
    }
    for (t in 0 until indices.size / 3) {
        val i0 = indices[3 * t]
        val i1 = indices[3 * t + 1]
        val i2 = indices[3 * t + 2]
        if (i0 !in points.indices || i1 !in points.indices || i2 !in points.indices) continue
        val a = row.position(points[i0])
        val b = row.position(points[i1])
        val c = row.position(points[i2])
        if ((((b - a) cross (c - a)) dot normal) >= 0.0) {
            write(i0); write(i1); write(i2)
        } else {
            write(i0); write(i2); write(i1)
        }
    }
}
