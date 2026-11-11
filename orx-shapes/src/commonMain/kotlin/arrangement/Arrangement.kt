    package org.openrndr.extra.shapes.arrangement

import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.kdtree.vector2Mapper
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

// === Helpers ===
private val ShapeContour.start get() = segments.first().start

private val ShapeContour.startContourPoint get() =
    ContourPoint(
        this,
        0.0,
        segments.first(),
        0.0,
        segments.first().start,
    )

private val ShapeContour.end get() = segments.last().end

private val ShapeContour.endContourPoint get() =
    ContourPoint(
        this,
        1.0,
        segments.last(),
        1.0,
        segments.last().start,
    )


private fun <K, V> MutableMap<K, MutableList<V>>.add(key: K, value: V) {
    val ml = get(key)
    if (ml != null)
        ml.add(value)
    else set(key, mutableListOf(value))
}

private fun <K, V> MutableMap<K, MutableList<V>>.addAll(key: K, values: Collection<V>) {
    val ml = get(key)
    if (ml != null)
        ml.addAll(values)
    else set(key, values.toMutableList())
}

/**
 * Polishes an intersection of [a] at [s] and [b] at [t] with Newton steps on A(s) - B(t) = 0. Kartifex' tolerances
 * are in parameter space, so its results can be off by up to about 1e-5 times the segment length. Returns null
 * when refinement does not improve the result, e.g. because the curves are (nearly) tangent there.
 */
private fun refineIntersection(a: Segment2D, b: Segment2D, s: Double, t: Double): Pair<Double, Double>? {
    var u = s
    var v = t
    val r0 = a.position(u).distanceTo(b.position(v))
    var r = r0
    for (i in 0 until 4) {
        val f = a.position(u) - b.position(v)
        val da = a.derivative(u)
        val db = b.derivative(v)
        // Solve [da, -db] (du, dv) = f
        val det = db.x * da.y - da.x * db.y
        if (abs(det) <= 1E-9 * da.length * db.length) break
        val du = (db.x * f.y - db.y * f.x) / det
        val dv = (da.x * f.y - da.y * f.x) / det
        val nu = u - du
        val nv = v - dv
        if (nu < -1E-9 || nu > 1.0 + 1E-9 || nv < -1E-9 || nv > 1.0 + 1E-9) break
        val nr = a.position(nu.coerceIn(0.0, 1.0)).distanceTo(b.position(nv.coerceIn(0.0, 1.0)))
        if (nr >= r) break
        u = nu.coerceIn(0.0, 1.0)
        v = nv.coerceIn(0.0, 1.0)
        r = nr
    }
    return if (r < r0) u to v else null
}

/**
 * Vertex of an arrangement, which represents an intersection (X) between two shapes.
 */
data class XVertex(val pos: Vector2) {
    /** The half-edges leaving this vertex */
    val outgoing = mutableListOf<XHalfEdge>()

    /** The half-edges entering this vertex */
    val incoming = mutableListOf<XHalfEdge>()

    /**
     * The half-edges leaving this vertex sorted by increasing angle. Computed once, when faces are created.
     *
     * Edges are ordered by their tangent direction. Edges whose tangents coincide (tangencies) are ordered among
     * each other by where they first cross a small circle around the vertex: edges do not cross each other, so
     * that is their true order, and tangent curves bend apart before they reach the circle.
     */
    internal val rotation: List<XHalfEdge> by lazy {
        val tangent = outgoing.map { it to it.contour.startAngle() }.sortedBy { it.second }
        // group cyclically consecutive edges whose tangent angles (nearly) coincide
        val groups = mutableListOf(mutableListOf(tangent[0]))
        for (i in 1 until tangent.size) {
            if (tangent[i].second - groups.last().last().second < TANGENT_TIE) groups.last().add(tangent[i])
            else groups.add(mutableListOf(tangent[i]))
        }
        if (groups.size > 1 && tangent.first().second + 2.0 * PI - tangent.last().second < TANGENT_TIE) {
            groups[0].addAll(0, groups.removeLast())
        }
        groups.flatMap { group ->
            if (group.size == 1) listOf(group[0].first) else {
                val r = 0.5 * group.minOf { it.first.contour.farthestDistance(pos) }
                val reference = group[0].second
                group.map { (he, _) ->
                    // angle relative to the group's tangent direction, in (-pi, pi]
                    val a = (he.contour.probeAngle(pos, r) - reference).mod(2.0 * PI)
                    he to if (a > PI) a - 2.0 * PI else a
                }.sortedBy { it.second }.map { it.first }
            }
        }
    }
}

/**
 * The signed area enclosed by this contour, closed by a line from its end to its start if it is open: positive when
 * it turns from +x to +y. Exact for the curves of the contour, by Green's theorem with 3-point Gauss-Legendre
 * quadrature, which integrates the polynomials of quadratic and cubic Bezier segments exactly. openrndr's
 * [ShapeContour.winding] sums over the control polygons instead, which can have the opposite sign for strongly
 * curved segments.
 */
private fun ShapeContour.signedArea(): Double {
    val gx = doubleArrayOf(0.5 - 0.5 * kotlin.math.sqrt(0.6), 0.5, 0.5 + 0.5 * kotlin.math.sqrt(0.6))
    val gw = doubleArrayOf(5.0 / 18.0, 8.0 / 18.0, 5.0 / 18.0)
    var sum = 0.0
    for (s in segments) {
        for (k in 0 until 3) {
            val p = s.position(gx[k])
            val d = s.derivative(gx[k])
            sum += gw[k] * (p.x * d.y - p.y * d.x)
        }
    }
    val a = segments.last().end
    val b = segments.first().start
    sum += a.x * b.y - a.y * b.x
    return sum / 2.0
}

/** The winding of this contour by its exact [signedArea], with the convention of [ShapeContour.winding]. */
private fun ShapeContour.exactWinding(): Winding {
    val area = signedArea()
    return when (polarity) {
        YPolarity.CW_NEGATIVE_Y -> if (area > 0.0) Winding.CLOCKWISE else Winding.COUNTER_CLOCKWISE
        YPolarity.CCW_POSITIVE_Y -> if (area > 0.0) Winding.COUNTER_CLOCKWISE else Winding.CLOCKWISE
    }
}

/**
 * The pieces of [halfEdges] joined into one contour in a single pass, like `+=` on contours but without copying the
 * segments again for every piece: a line joins pieces whose ends do not meet
 */
private fun joined(halfEdges: List<XHalfEdge>): ShapeContour {
    val segments = ArrayList<Segment2D>()
    for (he in halfEdges) {
        val piece = he.contour.segments
        if (piece.isEmpty()) continue
        if (segments.isNotEmpty() && segments.last().end != piece.first().start) {
            segments.add(Segment2D(segments.last().end, piece.first().start))
        }
        segments.addAll(piece)
    }
    return ShapeContour(segments, false, halfEdges.first().contour.polarity)
}

/** Tangent angles closer than this are treated as equal, see [XVertex.rotation]. */
private const val TANGENT_TIE = 1E-4

/** The angle of the direction in which this contour leaves its start point. */
private fun ShapeContour.startAngle(): Double {
    val s = segments.first()
    var d = s.derivative(0.0)
    // a control point on the start point gives a zero derivative
    if (d.squaredLength < 1E-24 * (s.end - s.start).squaredLength) d = s.position(1E-3) - s.start
    return atan2(d.y, d.x)
}

/**
 * Edge of an arrangement.
 * @property contour Geometric representation of the edge, for drawing purposes.
 * @property origin The shape that the edge originates from.
 */
data class XEdge(val source: XVertex, val target: XVertex, val contour: ShapeContour, val origin: ShapeProvider) {
    val start get() = contour.start
    val end get() = contour.end

    /** The two half-edges that correspond to this edge. */
    lateinit var halfEdges: Pair<XHalfEdge, XHalfEdge>

    /** The index of the input contour this edge is a piece of, see [Arrangement]. */
    internal var inputContour = -1

    /** The indices of the other input contours that have a piece on this edge, like a side two shapes share. */
    internal var alsoOn = emptyList<Int>()

    /** True when this edge was merged into another edge that lies on it, see [Arrangement]. */
    internal var merged = false

    /** Takes the half-edges of this edge out of its vertices, when it is merged into another edge. */
    internal fun unlink() {
        val (a, b) = halfEdges
        source.outgoing.removeAll { it === a }
        target.outgoing.removeAll { it === b }
        source.incoming.removeAll { it === b }
        target.incoming.removeAll { it === a }
    }

    init {
        // Once an edge is created, also create half-edges and do the necessary bookkeeping.
        splitAndAdd()
    }

    private fun split(): Pair<XHalfEdge, XHalfEdge> {
        val hes = XHalfEdge(source, target, contour, this) to XHalfEdge(target, source, contour.reversed, this)
        hes.first.twin = hes.second
        hes.second.twin = hes.first
        halfEdges = hes
        return hes
    }

    private fun splitAndAdd(): Pair<XHalfEdge, XHalfEdge> {
        val (a, b) = split()
        source.outgoing.add(a)
        target.outgoing.add(b)
        source.incoming.add(b)
        target.incoming.add(a)
        return halfEdges
    }
}

/**
 * Half-edge of an arrangement.
 * Each edge is split length-wise into two half-edges of opposite orientation.
 * Half-edges can be used to traverse an arrangement.
 * @property contour Geometric representation of the edge, for drawing purposes.
 * @property original The edge that was split into this half-edge and its [twin].
 */
data class XHalfEdge(val source: XVertex, val target: XVertex, val contour: ShapeContour, val original: XEdge) {
    val start get() = contour.start
    val end get() = contour.end

    /** The shape that the half-edge originates from. */
    val origin get() = original.origin

    /** The half-edge of opposite direction originating from [original].
     * This is useful for traversing to a different face. */
    lateinit var twin: XHalfEdge

    /** The face to the right of this half-edge. */
    lateinit var face: XFace

    /** The next half-edge of the [face]. */
    val next by lazy {
        // a loop edge on a vertex that has nothing else (an untouched closed contour)
        if (target.outgoing.size == 2 && target.outgoing.any { it === this }) return@lazy this
        // The next half-edge is the one that comes right before the twin in the order around the target.
        val order = target.rotation
        val i = order.indexOfFirst { it === twin }
        order[(i - 1 + order.size) % order.size]
    }

    /** Index of the face walk that visited this half-edge, used while creating faces. */
    internal var walk = -1
}

/** Lower bound on the largest distance from [p] to a point on this contour. */
private fun ShapeContour.farthestDistance(p: Vector2): Double =
    segments.maxOf { s -> (0..8).maxOf { s.position(it / 8.0).distanceTo(p) } }

/**
 * The angle, seen from [p], of the point where this contour (which starts at [p]) first crosses the circle of radius
 * [r] around [p].
 */
private fun ShapeContour.probeAngle(p: Vector2, r: Double): Double {
    val steps = 32
    for (s in segments) {
        var t0 = 0.0
        for (i in 1..steps) {
            val t1 = i.toDouble() / steps
            if (s.position(t1).distanceTo(p) >= r) {
                // bisect for the crossing within [t0, t1]
                var lo = t0
                var hi = t1
                repeat(50) {
                    val mid = (lo + hi) / 2.0
                    if (s.position(mid).distanceTo(p) >= r) hi = mid else lo = mid
                }
                val q = s.position(hi) - p
                return atan2(q.y, q.x)
            }
            t0 = t1
        }
    }
    val q = segments.last().end - p
    return atan2(q.y, q.x)
}

/**
 * Face of an arrangement.
 * @property edge An arbitrary half-edge incident to this face.
 * @property origins The shapes of which this face is a subset.
 */
open class XFace(val edge: XHalfEdge, val origins: List<ShapeProvider>) {
    /**
     * For the outer face of a connected component: the bounded face of another component that it lies in,
     * or null when the component is not nested inside another one.
     */
    var parent: BoundedFace? = null
        internal set
}

/**
 * A bounded face of an arrangement.
 * @property edge An arbitrary half-edge incident to this face.
 * @property origins The shapes of which this face is a subset.
 * @property contour The outer boundary of this face. Connected components that lie inside the face are not cut
 * out of it; use [shape] for that.
 */
class BoundedFace(edge: XHalfEdge, origins: List<ShapeProvider>, val contour: ShapeContour): XFace(edge, origins) {
    /** The outer boundaries of the connected components that lie directly inside this face, counterclockwise. */
    val innerBoundaries = mutableListOf<ShapeContour>()

    /** The geometric representation of this face: [contour] with [innerBoundaries] as holes. */
    val shape: Shape by lazy { Shape(listOf(contour) + innerBoundaries) }
}

/**
 * Create an arrangement of a list of [ShapeProvider] objects, like [Shape]s or [ShapeContour]s.
 * @property maxIters The maximum number of edges incident to a face, used to detect infinite loops so that an error is thrown instead.
 * @property epsilon Distance below which intersection points are considered to be the same vertex. When null it
 * is derived from the size of the input, see [vertexEpsilon].
 */
data class Arrangement(val shapes: List<ShapeProvider>, val maxIters: Int = 1000, val epsilon: Double? = null) {
    constructor(vararg shapes: ShapeProvider): this(shapes.toList())

    /**
     * The [Shape] of each input. Computed once: for primitives like [Circle], `shape` builds a new object on every
     * call, which would also discard the region that is cached for inside tests.
     */
    private val inputShapes = shapes.map { it.shape }

    /** The bounds of each input shape, to skip the inside tests of shapes that are far away. */
    private val inputBounds = inputShapes.map { it.bounds }

    /** Length of the diagonal of the bounding box of all input shapes. */
    private val diagonal = inputShapes.map { it.bounds }.bounds.let { b ->
        Vector2(b.width, b.height).length
    }

    /**
     * The distance below which points are merged into a single vertex. Kartifex finds intersections with an
     * absolute tolerance of 1e-5, so this never goes below that; otherwise it scales with the input.
     */
    val vertexEpsilon = epsilon ?: (1E-7 * diagonal + 2E-5)

    /** Maps a contour to the vertices incident to it. */
    val cVertsMap = mutableMapOf<ShapeContour, MutableList<Pair<XVertex, Double>>>()

    /** Maps a shape to the edges incident to it. */
    val hEdgesMap = mutableMapOf<ShapeProvider, MutableList<XEdge>>()

    /** Maps a shape to the faces that it is a superset of. */
    val hFacesMap = mutableMapOf<ShapeProvider, MutableList<XFace>>()

    /** All vertices of the arrangement. */
    val vertices by lazy { cVertsMap.flatMap { it.value.map { it.first } }.toSet().toList() }

    /** All edges of the arrangement, once each, also those that lie on several input contours. */
    val edges by lazy { allEdges.toList() }

    /** The edges in the order they were created, without those that were merged into others. */
    private val allEdges = mutableListOf<XEdge>()

    /** All half-edges of the arrangement. */
    val halfEdges by lazy {
        edges.flatMap {
            it.halfEdges.toList()
        }
    }

    /** All faces of the arrangement. */
    val faces = mutableListOf<XFace>()

    val boundedFaces by lazy { faces.filterIsInstance<BoundedFace>() }
    val unboundedFaces by lazy { faces.filter{ it !is BoundedFace } }

    /** The faces that are a subset of some input shape. */
    val originFaces by lazy { boundedFaces.filter { it.origins.isNotEmpty() } }

    /** The bounded faces that are not a subset of any input shape. */
    val holes by lazy { boundedFaces.filter { it.origins.isEmpty() } }

    /** The outer boundary contours of each connected component. */
    val boundaries: List<ShapeContour> by lazy {
        unboundedFaces.map { f ->
            val start = f.edge
            val walk = mutableListOf(start)
            var current = start.next
            while (current !== start) {
                walk.add(current)
                current = current.next
            }
            joined(walk)
        }
    }

    /** A list containing an arbitrary half-edge for each connected component. */
    val components by lazy {
        unboundedFaces.map { it.edge }
    }

    /** A point on segment [segment] of a contour at parameter [t], belonging to vertex-candidate [node]. */
    private class ContourEvent(val segment: Int, val t: Double, val node: Int)

    /** The contours of the input, each paired with the shape it came from and whether to self-intersect it. */
    private class InputContour(val contour: ShapeContour, val shape: ShapeProvider, val selfIntersect: Boolean) {
        val events = mutableListOf<ContourEvent>()
    }

    /** The indices into [inputContours] of the contours of each distinct input shape, in the order of its contours. */
    private val shapeContours = mutableMapOf<ShapeProvider, MutableList<Int>>()

    private val inputContours: List<InputContour> = buildList {
        val seen = mutableSetOf<ShapeProvider>()
        // A shape that is passed in twice is intersected with itself; its contours are only added once.
        val repeated = shapes.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        for ((s, shape) in shapes.zip(inputShapes)) {
            if (!seen.add(s)) continue
            val indices = shapeContours.getOrPut(s) { mutableListOf() }
            for (c in shape.contours) {
                if (!c.empty) {
                    indices.add(size)
                    add(InputContour(c, s, s in repeated))
                }
            }
        }
    }

    /** Union-find over vertex candidates ("nodes"), in the spirit of kartifex' VertexUnion. */
    private val nodePositions = mutableListOf<Vector2>()
    private val nodeParents = mutableListOf<Int>()

    private fun newNode(position: Vector2): Int {
        nodePositions.add(position)
        nodeParents.add(nodeParents.size)
        return nodeParents.size - 1
    }

    private fun root(node: Int): Int {
        var r = node
        while (nodeParents[r] != r) r = nodeParents[r]
        var n = node
        while (nodeParents[n] != r) {
            val next = nodeParents[n]
            nodeParents[n] = r
            n = next
        }
        return r
    }

    private fun join(a: Int, b: Int) {
        val ra = root(a)
        val rb = root(b)
        if (ra < rb) nodeParents[rb] = ra else if (rb < ra) nodeParents[ra] = rb
    }

    /** Map (segment, t) on [c] to its canonical form, with t = 1 moved to the start of the next segment. */
    private fun canonical(c: ShapeContour, segment: Int, t: Double): Pair<Int, Double> = when {
        t < 1.0 -> segment to t.coerceAtLeast(0.0)
        segment < c.segments.lastIndex -> (segment + 1) to 0.0
        c.closed -> 0 to 0.0
        else -> segment to 1.0
    }

    private fun createVertices() {
        val eps = vertexEpsilon

        // Open contours: their end points are vertices.
        for (ic in inputContours) {
            val c = ic.contour
            if (!c.closed) {
                ic.events.add(ContourEvent(0, 0.0, newNode(c.segments.first().start)))
                ic.events.add(ContourEvent(c.segments.lastIndex, 1.0, newNode(c.segments.last().end)))
            }
        }

        // Find segment-segment intersections with a sweep over x. Both contours of an intersection share a
        // single node, so they agree on the vertex identity.
        class SweepItem(
            val contour: Int, val segment: Int, val minX: Double, val maxX: Double, val minY: Double, val maxY: Double
        )

        val items = inputContours.flatMapIndexed { ci, ic ->
            ic.contour.segments.mapIndexed { si, s ->
                // the control polygon contains the segment, and is cheaper than its exact bounds
                val minX = minOf(s.start.x, s.end.x, s.control.minOfOrNull { it.x } ?: s.start.x)
                val maxX = maxOf(s.start.x, s.end.x, s.control.maxOfOrNull { it.x } ?: s.start.x)
                val minY = minOf(s.start.y, s.end.y, s.control.minOfOrNull { it.y } ?: s.start.y)
                val maxY = maxOf(s.start.y, s.end.y, s.control.maxOfOrNull { it.y } ?: s.start.y)
                SweepItem(ci, si, minX - eps, maxX + eps, minY - eps, maxY + eps)
            }
        }.sortedBy { it.minX }

        val active = mutableListOf<SweepItem>()
        for (item in items) {
            active.removeAll { it.maxX < item.minX }
            for (other in active) {
                // the sweep finds pairs that overlap along x; those that do not overlap along y cannot meet
                if (other.maxY < item.minY || other.minY > item.maxY) continue
                val sameContour = other.contour == item.contour
                if (sameContour && (!inputContours[item.contour].selfIntersect || other.segment == item.segment)) {
                    continue
                }
                val ica = inputContours[item.contour]
                val icb = inputContours[other.contour]
                val sa = ica.contour.segments[item.segment]
                val sb = icb.contour.segments[other.segment]
                for (inter in intersections(sa, sb)) {
                    // Intersections that were snapped onto a segment end are kept as they are, so that they
                    // match exactly at the joints between segments.
                    val atEnd = inter.a.segmentT.let { it == 0.0 || it == 1.0 } ||
                            inter.b.segmentT.let { it == 0.0 || it == 1.0 }
                    val refined = if (atEnd) null else refineIntersection(sa, sb, inter.a.segmentT, inter.b.segmentT)
                    val sta = refined?.first ?: inter.a.segmentT
                    val stb = refined?.second ?: inter.b.segmentT
                    val position = if (refined == null) inter.position else (sa.position(sta) + sb.position(stb)) / 2.0
                    val (ia, ta) = canonical(ica.contour, item.segment, sta)
                    val (ib, tb) = canonical(icb.contour, other.segment, stb)
                    // Neighbouring segments of a self-intersected contour meet at their shared end point.
                    if (sameContour && ia == ib && ta == tb) continue
                    val node = newNode(position)
                    ica.events.add(ContourEvent(ia, ta, node))
                    icb.events.add(ContourEvent(ib, tb, node))
                }
            }
            active.add(item)
        }

        // Along each contour, merge consecutive candidates that are within eps of each other (kartifex' dedupe).
        for (ic in inputContours) {
            ic.events.sortWith(compareBy<ContourEvent> { it.segment }.thenBy { it.t })
            val ev = ic.events
            val pos = ev.map { ic.contour.segments[it.segment].position(it.t) }
            for (i in 1 until ev.size) {
                if (pos[i - 1].distanceTo(pos[i]) < eps) join(ev[i - 1].node, ev[i].node)
            }
            if (ic.contour.closed && ev.size > 1 && pos.last().distanceTo(pos.first()) < eps) {
                join(ev.last().node, ev.first().node)
            }
        }

        // Points where three or more curves meet are found once per pair of curves, as slightly different
        // points that share no curve pairwise. Merge any candidates that lie within eps of each other.
        if (nodePositions.isNotEmpty()) {
            val indices = nodePositions.indices.toMutableList()
            val tree = buildKDTree(indices, 2) { i, d -> vector2Mapper(nodePositions[i], d) }
            for (i in nodePositions.indices) {
                for (j in tree.findAllInRadius(i, eps, includeQuery = false)) join(i, j)
            }
        }

        // Each union-find root becomes a vertex, placed at the mean of its members.
        val members = nodePositions.indices.groupBy { root(it) }
        val vertexOf = members.mapValues { (_, ms) ->
            XVertex(ms.fold(Vector2.ZERO) { acc, m -> acc + nodePositions[m] } / ms.size.toDouble())
        }
        nodeVertices = nodePositions.indices.map { vertexOf.getValue(root(it)) }

        for (ic in inputContours) {
            val n = ic.contour.segments.size
            // one lookup per contour: hashing a contour hashes all its segments
            if (ic.events.isNotEmpty()) {
                cVertsMap.addAll(ic.contour, ic.events.map { e -> nodeVertices[e.node] to (e.segment + e.t) / n })
            }
        }
    }

    /** The vertex that each node was merged into. */
    private var nodeVertices = emptyList<XVertex>()

    /**
     * The part of [c] between ([s0], [t0]) and ([s1], [t1]), going forward and wrapping around if [c] is closed.
     * Its end points are snapped exactly onto [v0] and [v1], so all edges that meet at a vertex share its position.
     */
    private fun piece(
        c: ShapeContour, s0: Int, t0: Double, s1: Int, t1: Double, v0: XVertex, v1: XVertex, wholeLoop: Boolean
    ): ShapeContour? {
        val n = c.segments.size
        val segments = mutableListOf<Segment2D>()
        val forward = s0 < s1 || (s0 == s1 && t0 < t1)
        // Going backward means wrapping around, which only makes sense for closed contours. Coinciding
        // parameters denote an empty piece, unless the piece is the whole contour (a single vertex on it).
        if (!forward && (!c.closed || (s0 == s1 && t0 == t1 && !wholeLoop))) return null
        if (s0 == s1 && t0 < t1) {
            segments.add(c.segments[s0].sub(t0, t1))
        } else {
            if (t0 < 1.0) segments.add(c.segments[s0].sub(t0, 1.0))
            var s = (s0 + 1) % n
            while (s != s1) {
                segments.add(c.segments[s])
                s = (s + 1) % n
            }
            if (t1 > 0.0) segments.add(c.segments[s1].sub(0.0, t1))
        }
        if (segments.isEmpty()) return null
        segments[0] = segments[0].copy(start = v0.pos)
        segments[segments.lastIndex] = segments.last().copy(end = v1.pos)
        return ShapeContour(segments, false, c.polarity)
    }

    private fun createEdges() {
        val eps = vertexEpsilon
        for ((index, ic) in inputContours.withIndex()) {
            val c = ic.contour
            val s = ic.shape
            val ev = ic.events
            // added to hEdgesMap at once: hashing a contour as a key hashes all its segments
            val created = mutableListOf<XEdge>()

            // A closed contour that is not intersected by anything has no vertices. We add a dummy one;
            // it is not added to the vertices list.
            if (ev.isEmpty()) {
                val dummy = XVertex(c.segments.first().start)
                allEdges.add(XEdge(dummy, dummy, c, s).also { it.inputContour = index })
                continue
            }

            // Pieces between consecutive events; for closed contours the last piece wraps around to the first
            // event. Pieces that start and end at the same vertex are kept only when they are not degenerate:
            // a contour may leave a vertex and come back to it.
            val pairs = ev.zipWithNext().toMutableList()
            if (c.closed) pairs.add(ev.last() to ev.first())
            for ((a, b) in pairs) {
                val va = nodeVertices[a.node]
                val vb = nodeVertices[b.node]
                val p = piece(c, a.segment, a.t, b.segment, b.t, va, vb, wholeLoop = a === b) ?: continue
                if (va === vb && p.length < 10.0 * eps) continue
                created.add(XEdge(va, vb, p, s).also { it.inputContour = index })
            }
            allEdges.addAll(created)
        }
        mergeCoincidentEdges()
        // the edges of each input contour, also those it shares with others; added to hEdgesMap at once per shape:
        // hashing a contour as a key hashes all its segments
        val byContour = Array(inputContours.size) { mutableListOf<XEdge>() }
        for (e in allEdges) {
            byContour[e.inputContour].add(e)
            for (k in e.alsoOn) byContour[k].add(e)
        }
        for ((shape, indices) in shapeContours) {
            val list = mutableListOf<XEdge>()
            for (k in indices) for (e in byContour[k]) if (list.none { it === e }) list.add(e)
            if (list.isNotEmpty()) hEdgesMap.addAll(shape, list)
        }
    }

    /**
     * Pieces of input contours that lie on each other, like a side that two shapes share, are the same edge: of each
     * set of edges between the same two vertices that coincide, the first is kept, and remembers the input contours
     * of the others; the others are taken out of their vertices.
     */
    private fun mergeCoincidentEdges() {
        val tolerance = 10.0 * vertexEpsilon
        fun coincide(a: ShapeContour, b: ShapeContour) = listOf(0.25, 0.5, 0.75).all { t ->
            val p = a.position(t)
            b.nearest(p).position.distanceTo(p) < tolerance
        }
        for (e in allEdges) {
            if (e.merged || e.source === e.target) continue
            for (h in e.source.outgoing.toList()) {
                val f = h.original
                if (f === e || f.merged || h.target !== e.target) continue
                if (!coincide(e.contour, h.contour) || !coincide(h.contour, e.contour)) continue
                f.merged = true
                f.unlink()
                e.alsoOn = e.alsoOn + f.inputContour + f.alsoOn
            }
        }
        allEdges.removeAll { it.merged }
    }

    private fun createFaces() {
        for ((walk, heStart) in halfEdges.withIndex()) {
            // Start from a half-edge that has not been handled yet
            if (heStart.walk >= 0) continue
            heStart.walk = walk
            val visited = mutableListOf(heStart)
            var current = heStart

            // Repeatedly go to the next half-edge, until we arrive back where we started.
            var iters = 0
            while (current.next !== heStart && iters < maxIters) {
                current = current.next
                current.walk = walk
                visited.add(current)
                iters++
            }
            if (iters >= maxIters) {
                error("Arrangement: A face seems to consist of more than maxIters ($maxIters) edges. This is likely " +
                        "a robustness issue arising from using input shapes that would result in small or thin faces.")
            }

            // filled in by assignOrigins, once all faces are known
            val origins = mutableListOf<ShapeProvider>()
            val faceContour = joined(visited)
            val closed = if (faceContour.closed) faceContour else faceContour.close()

            // A walk that traverses each of its edges in both directions (e.g. around an open curve) encloses
            // nothing; its winding would be decided by round-off, so treat it as unbounded.
            val encloses = !visited.all { it.twin.walk == walk }

            val f = if (encloses && closed.exactWinding() == Winding.CLOCKWISE) BoundedFace(heStart, origins, closed)
            else XFace(heStart, emptyList()).also { if (encloses) outerBoundaries[it] = closed }
            faces.add(f)
            faceWalks[f] = visited

            visited.forEach { e ->
                e.face = f
            }
        }
        nestComponents()
        assignOrigins()
    }

    /** The half-edges around each face, in order. */
    private val faceWalks = mutableMapOf<XFace, List<XHalfEdge>>()

    /**
     * Find the input shapes that contain each bounded face. Walking from face to face across an edge flips the parity
     * of the number of crossings of the contour the edge belongs to, starting from no crossings outside everything,
     * and from the face a nested component lies in. A shape contains a face by the rule of its region: of the
     * contours the face is inside of, by the parity, the one with the smallest area decides, and a clockwise one is a
     * hole. That is the
     * point test of [Shape.contains] for every face at once, without sampling a point inside thin faces. Shapes with
     * open contours, whose region closes them with lines that are not edges, and shapes with contours whose parity
     * does not agree around the faces are tested with a point of each face.
     */
    private fun assignOrigins() {
        val n = inputContours.size
        val parity = mutableMapOf<XFace, BooleanArray>()
        val children = faces.filter { it !is BoundedFace && it.parent != null }.groupBy { it.parent!! }
        val queue = ArrayDeque<XFace>()
        // contours whose parity does not agree around the faces, like a contour that crosses itself without being
        // intersected with itself: the faces are not on one side of it, so the shapes of those are tested by point
        val unreliable = BooleanArray(n)
        for (f in faces) if (f !is BoundedFace && f.parent == null) {
            parity[f] = BooleanArray(n)
            queue.addLast(f)
        }
        while (queue.isNotEmpty()) {
            val f = queue.removeFirst()
            val p = parity.getValue(f)
            for (h in faceWalks.getValue(f)) {
                val g = h.twin.face
                val q = p.copyOf()
                val k = h.original.inputContour
                if (k >= 0) q[k] = !q[k]
                for (also in h.original.alsoOn) q[also] = !q[also]
                val known = parity[g]
                if (known != null) {
                    for (c in 0 until n) if (known[c] != q[c]) unreliable[c] = true
                    continue
                }
                parity[g] = q
                queue.addLast(g)
            }
            if (f is BoundedFace) for (child in children[f].orEmpty()) {
                if (child in parity) continue
                parity[child] = p.copyOf()
                queue.addLast(child)
            }
        }

        // whether each input contour is clockwise, as kartifex' rings decide it, and the contours of each shape from
        // the smallest area to the largest, the order in which its region tests them; per index of [shapes], once
        val areas = DoubleArray(n) { inputContours[it].contour.signedArea() }
        val clockwise = BooleanArray(n) { areas[it] < 0.0 }
        val contoursOf = shapes.map { shapeContours[it].orEmpty() }
        val testOrder = contoursOf.map { indices -> indices.sortedBy { kotlin.math.abs(areas[it]) } }
        val closedShape = BooleanArray(shapes.size) { inputShapes[it].closedContours.isNotEmpty() }
        val pointTest = BooleanArray(shapes.size) { i ->
            inputShapes[i].contours.any { !it.closed && !it.empty } || contoursOf[i].any { unreliable[it] }
        }
        val pointTested = shapes.indices.filter { closedShape[it] && pointTest[it] }
        // the shapes each contour belongs to, by index of [shapes]: a shape passed twice has its contours once
        val shapesOf = Array(n) { mutableListOf<Int>() }
        for (i in shapes.indices) if (closedShape[i] && !pointTest[i]) for (k in contoursOf[i]) shapesOf[k].add(i)

        val perShape = shapes.indices.map { mutableListOf<XFace>() }
        for (f in faces) {
            if (f !is BoundedFace) continue
            val p = parity[f] ?: continue
            val inside = mutableSetOf<Int>()
            // only shapes with a contour the face is inside of can contain it
            for (k in 0 until n) if (p[k]) for (i in shapesOf[k]) if (i !in inside) {
                val decisive = testOrder[i].first { p[it] }
                if (!clockwise[decisive]) inside.add(i)
            }
            if (pointTested.isNotEmpty()) {
                val facePt = f.edge.contour.position(0.5) + f.edge.contour.normal(0.5) * -faceProbeOffset
                for (i in pointTested) if (facePt in inputBounds[i] && facePt in inputShapes[i]) inside.add(i)
            }
            val origins = f.origins as MutableList<ShapeProvider>
            // in the order of the input
            for (i in inside.sorted()) {
                origins.add(shapes[i])
                perShape[i].add(f)
            }
        }
        // added to hFacesMap at once per shape: hashing a contour as a key hashes all its segments
        for (i in shapes.indices) if (perShape[i].isNotEmpty()) hFacesMap.addAll(shapes[i], perShape[i])
    }

    /** Outer boundaries of the unbounded faces of components that enclose an area. */
    private val outerBoundaries = mutableMapOf<XFace, ShapeContour>()

    /**
     * Find, for every connected component, the bounded face of another component that it lies in, and add its outer
     * boundary to that face as a hole.
     */
    private fun nestComponents() {
        val unbounded = faces.filter { it !is BoundedFace }
        if (unbounded.size < 2) return

        // label the components by flood fill over the half-edges
        val component = mutableMapOf<XVertex, Int>()
        for ((id, u) in unbounded.withIndex()) {
            val stack = mutableListOf(u.edge.source)
            while (stack.isNotEmpty()) {
                val v = stack.removeLast()
                if (component.put(v, id) != null) continue
                for (he in v.outgoing) if (he.target !in component) stack.add(he.target)
            }
        }

        // Faces of different components never touch, so faces that contain a given point are nested in each
        // other; the innermost one has the smallest bounding box.
        val bounded = boundedFaces.map { it to it.contour.bounds }
        for ((id, u) in unbounded.withIndex()) {
            val p = u.edge.source.pos
            val parent = bounded
                .filter { (f, b) -> component[f.edge.source] != id && p in b && p in f.contour }
                .minByOrNull { (_, b) -> b.width * b.height }?.first ?: continue
            u.parent = parent
            outerBoundaries[u]?.let { parent.innerBoundaries.add(it) }
        }
    }

    /** Distance from an edge at which a point is sampled to determine which input shapes contain a face. */
    private val faceProbeOffset = maxOf(1E-5 * diagonal, 2.0 * vertexEpsilon)

    init {
        createVertices()
        createEdges()
        createFaces()
    }
}
