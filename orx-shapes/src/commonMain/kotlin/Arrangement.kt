package org.openrndr.extra.shapes

import org.openrndr.extra.kdtree.buildKDTree
import org.openrndr.extra.kdtree.vector2Mapper
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

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

private fun ShapeContour.direction(ut: Double): Vector2 = normal(ut).perpendicular(polarity.opposite)

private val YPolarity.opposite get() =
    when(this) {
        YPolarity.CCW_POSITIVE_Y -> YPolarity.CW_NEGATIVE_Y
        YPolarity.CW_NEGATIVE_Y -> YPolarity.CCW_POSITIVE_Y
    }

private fun angleBetween(v: Vector2, w: Vector2) = atan2(w.y*v.x - w.x*v.y, w.x*v.x + w.y*v.y)

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
 * Vertex of an arrangement, which represents an intersection (X) between two shapes.
 */
data class XVertex(val pos: Vector2) {
    /** The half-edges leaving this vertex */
    val outgoing = mutableListOf<XHalfEdge>()

    /** The half-edges entering this vertex */
    val incoming = mutableListOf<XHalfEdge>()
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
        if (target.outgoing.size == 2 && this in target.outgoing) return@lazy this
        val y = target.pos
        val x = y - contour.direction(1.0)
        val candidates = target.outgoing.filterNot { it == twin }
            .map {
                val z = y + it.contour.direction(0.0)
                it to angleBetween(z - y, x - y)
            }.filter { it.second > -1E-6 || it.second < -PI + 1E-6 }

        if (candidates.size == 1) {
            candidates[0].first
        } else if (candidates.isEmpty()) {
            twin
        } else {
            val cand = candidates.minBy { abs(it.second) }
            if (cand.second > 1E-6 && candidates.all { it == cand || abs(it.second) - 1E-6 > abs(cand.second) }) return@lazy cand.first
            val maxR = min(candidates.minOf { it.first.end.distanceTo(target.pos) }, start.distanceTo(target.pos))
            val c = Circle(target.pos, maxR / 2.0)
            // if more than one intersection with c then we make a guess
            val x_ = c.contour.intersections(contour)[0]

            val newCandidates = candidates.map { (e, _) ->
                val inters = e.contour.intersections(c.contour)
                // if (inters.size != 1) then we make a guess
                e to angleBetween(inters[0].position - y, x_.position - y)
            }
            newCandidates.filter { it.second > -1E-6 || it.second < -PI + 1E-6 }.minBy { abs(it.second) }.first
        }
    }
}

/**
 * Face of an arrangement.
 * @property edge An arbitrary half-edge incident to this face.
 * @property origins The shapes of which this face is a subset.
 */
open class XFace(val edge: XHalfEdge, val origins: List<ShapeProvider>)

/**
 * A bounded face of an arrangement.
 * @property edge An arbitrary half-edge incident to this face.
 * @property origins The shapes of which this face is a subset.
 * @property contour The geometric representation of this face.
 */
class BoundedFace(edge: XHalfEdge, origins: List<ShapeProvider>, val contour: ShapeContour): XFace(edge, origins)

/**
 * Create an arrangement of a list of [ShapeProvider] objects, like [Shape]s or [ShapeContour]s.
 * @property maxIters The maximum number of edges incident to a face, used to detect infinite loops so that an error is thrown instead.
 */
data class Arrangement(val shapes: List<ShapeProvider>, val maxIters: Int = 1000) {
    constructor(vararg shapes: ShapeProvider): this(shapes.toList())

    /** Maps a contour to the vertices incident to it. */
    val cVertsMap = mutableMapOf<ShapeContour, MutableList<Pair<XVertex, Double>>>()

    /** Maps a shape to the edges incident to it. */
    val hEdgesMap = mutableMapOf<ShapeProvider, MutableList<XEdge>>()

    /** Maps a shape to the faces that it is a superset of. */
    val hFacesMap = mutableMapOf<ShapeProvider, MutableList<XFace>>()

    /** All vertices of the arrangement. */
    val vertices by lazy { cVertsMap.flatMap { it.value.map { it.first } }.toSet().toList() }

    /** All edges of the arrangement. */
    val edges by lazy { hEdgesMap.flatMap { it.value } }

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
            var current = start.next
            var contour = start.contour
            while (current != start) {
                contour += current.contour
                current = current.next
            }
            contour
        }
    }

    /** A list containing an arbitrary half-edge for each connected component. */
    val components by lazy {
        unboundedFaces.map { it.edge }
    }

    init {
        createVertices()
        createEdges()
        createFaces()
    }

    private fun createVertices() {
        data class CandidateVertex(val position: Vector2, val contourPoints: List<ContourPoint>)

        val candidates = buildList {
            // For open contours, add the start and end points as (candidate) vertices.
            for (s in shapes) {
                for (c in s.shape.contours) {
                    if (!c.closed) {
                        add(CandidateVertex(c.start, listOf(c.startContourPoint)))
                        add(CandidateVertex(c.end, listOf(c.endContourPoint)))
                    }
                }
            }
            // Compute pairwise intersections between contours.
            for (i in shapes.indices) {
                val s1 = shapes[i]
                for (j in i + 1 until shapes.size) {
                    val s2 = shapes[j]
                    val inters = s1.shape.contours.flatMap {
                            c1 -> s2.shape.contours.flatMap { c2 ->
                        c1.intersections(c2)
                    }
                    }
                    for (inter in inters) {
                        add(CandidateVertex(inter.position, listOf(inter.a, inter.b)))
                    }
                }
            }
        }

        // We will merge vertices that lie close together. For this compute a kd-tree of all candidate vertices.
        val tree = buildKDTree(candidates.toMutableList(), 2) { v, d ->
            vector2Mapper(v.position, d)
        }

        val unvisited = candidates.toMutableSet()
        val new = mutableListOf<XVertex>()

        while (unvisited.isNotEmpty()) {
            val inter = unvisited.first()
            val inters = tree.findAllInRadius(inter, 1E-1, includeQuery = true).filter { it in unvisited }
            unvisited.removeAll(inters)
            if (inters.size == 1) {
                val v = XVertex(inters[0].position)
                new.add(v)
                for (cp in inters[0].contourPoints)
                    cVertsMap.add(cp.contour, v to cp.contourT)
            } else if (inters.size > 1) {
                val center = inters.fold(Vector2.ZERO) { acc, x -> acc + x.position } / inters.size.toDouble()
                val v = XVertex(center)
                val cps = mutableSetOf<ContourPoint>()
                for (cp in inters.flatMap { it.contourPoints }) {
                    if (cps.any { it.contour == cp.contour && abs(it.contourT - cp.contourT) < 1E-2 }) continue
                    cps.add(cp)
                }
                for (cp in cps) {
                    cVertsMap.add(cp.contour, v to cp.contourT)
                }
                new.add(v)
            } else {
                error("Impossible")
            }
        }
    }

    private fun createEdges() {
        for (s in shapes) {
            // If a shape is passed in twice to compute self-intersections skip edge construction for the second one.
            if (s in hEdgesMap.keys) continue

            for (c in s.shape.contours) {
                // Determine whether a contour is closed and not intersected by anything.
                // There are no vertices on such a contour. We add a dummy one; it is not added to the vertices list.
                if (cVertsMap[c]?.isEmpty() != false) {
                    val dummy = XVertex(c.start)
                    val e = XEdge(dummy, dummy, c, s)
                    hEdgesMap.add(s, e)
                    continue
                }

                // Create edges between consecutive vertices of this contour.
                val tValues = cVertsMap[c]!!.sortedBy { it.second }
                val middleEdges = tValues.zipWithNext { (v1, t1), (v2, t2) ->
                    val piece = c.sub(t1, t2)
                    if (piece.empty) {
                        null
                    } else {
                        val e = XEdge(v1, v2, piece, s)
                        e
                    }
                }.filterNotNull()
                hEdgesMap.addAll(s, middleEdges)

                // If the contour is closed, make the last edge connecting the ends.
                if (c.closed) {
                    val (lastV, lastT) = tValues.last()
                    val (firstV, firstT) = tValues.first()
                    val lastPiece = c.sub(lastT, 1.0) + c.sub(0.0, firstT)
                    if (!lastPiece.empty) {
                        val lastEdge = XEdge(lastV, firstV, lastPiece, s)
                        hEdgesMap.add(s, lastEdge)
                    }
                }
            }
        }
    }

    private fun createFaces() {
        val remainingHalfEdges = halfEdges.toMutableList()

        while(remainingHalfEdges.isNotEmpty()) {
            // Pick an arbitrary half-edge that has not been handled yet
            val heStart = remainingHalfEdges.first()
            val visited = mutableListOf(heStart)
            var current = heStart
            var faceContour = heStart.contour

            // Repeatedly go to the next half-edge, until we arrive back where we started.
            var iters = 0
            while (current.next != heStart && iters < maxIters) {
                current = current.next
                visited.add(current)
                faceContour += current.contour
                iters++
            }
            if (iters >= maxIters) {
                error("Arrangement: A face seems to consist of more than maxIters ($maxIters) edges. This is likely " +
                        "a robustness issue arising from using input shapes that would result in small or thin faces.")
            }

            remainingHalfEdges.removeAll(visited)

            val facePt = heStart.contour.position(0.5) + heStart.contour.normal(0.5) * -0.01
            val origins = shapes.filter { s -> s.shape.closedContours.isNotEmpty() && facePt in s.shape }
            val closed = if (faceContour.closed) faceContour else faceContour.close()

            val f = if (closed.winding == Winding.CLOCKWISE) BoundedFace(heStart, origins, closed)
            else XFace(heStart, emptyList())
            faces.add(f)
            for (s in origins) {
                hFacesMap.add(s, f)
            }

            visited.forEach { e ->
                e.face = f
            }
        }
    }
}
