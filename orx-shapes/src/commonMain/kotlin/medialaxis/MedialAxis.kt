package org.openrndr.extra.shapes.medialaxis

import org.openrndr.extra.kdtree.kdTree
import org.openrndr.extra.shapes.fit.fitCubicBeziers
import org.openrndr.extra.triangulation.ConstrainedVoronoiDiagram
import org.openrndr.extra.triangulation.constrainedDelaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A single run of the medial axis between two nodes.
 *
 * @property contour the branch, fitted with cubic Bézier segments
 * @property start index into [MedialAxis.nodes] of the node at the start of [contour], -1 for an isolated loop
 * @property end index into [MedialAxis.nodes] of the node at the end of [contour], -1 for an isolated loop
 */
class MedialAxisBranch(val contour: ShapeContour, val start: Int, val end: Int)

/**
 * The medial axis of [shape] represented as a graph of [branches] connecting [nodes]. Nodes are branch end points
 * (leaves, including convex corners of [shape]) and junctions. Branches that meet at a node share the node position
 * exactly.
 */
class MedialAxis(val shape: Shape, val nodes: List<Vector2>, val branches: List<MedialAxisBranch>) {
    /** The branch contours, one for every run of the medial axis. */
    val contours: List<ShapeContour> get() = branches.map { it.contour }

    /** The radius of the maximal inscribed disk at [point], or the distance from [point] to the boundary of [shape]. */
    fun radius(point: Vector2): Double =
        shape.contours.minOfOrNull { it.nearest(point).position.distanceTo(point) } ?: 0.0
}

/**
 * Computes the medial axis of this [Shape].
 *
 * The shape is linearized using [ShapeContour.sampleLinear] (which keeps corners) after which every linear segment is
 * subdivided to a spacing of at most [samplingDistance], then a [ConstrainedVoronoiDiagram] of the linearized shape is used to find the topology of the medial axis, see
 * [medialAxis].
 *
 * @param samplingDistance the spacing of the boundary samples, smaller values capture finer detail
 * @param fitTolerance the maximum distance between the refined medial axis points and the fitted branches
 * @param pruneLength leaf branches attached to a junction whose significance is less than this are removed, see
 * [medialAxis]
 */
fun Shape.medialAxis(
    samplingDistance: Double = 2.0,
    fitTolerance: Double = samplingDistance * 0.1,
    pruneLength: Double = samplingDistance * 2.0
): MedialAxis {
    val linear = Shape(contours.filter { it.closed && !it.empty }.mapNotNull { contour ->
        val points = mutableListOf<Vector2>()
        // the refinement works on the original shape, so a coarse flattening suffices
        for (segment in contour.sampleLinear(samplingDistance * 0.25).segments) {
            val n = max(1, ceil(segment.length / samplingDistance).toInt())
            for (i in 0 until n) points.add(segment.start.mix(segment.end, i.toDouble() / n))
        }
        val deduplicated = points.filterIndexed { i, p -> p.distanceTo(points[(i + 1) % points.size]) > 1E-9 }
        if (deduplicated.size >= 3) ShapeContour.fromPoints(deduplicated, closed = true) else null
    })
    if (linear.contours.isEmpty()) return MedialAxis(this, emptyList(), emptyList())

    val diagram = linear.constrainedDelaunayTriangulation().voronoiDiagram(
        bounds.offsetEdges(max(bounds.width, bounds.height) * 0.1 + 1.0)
    )
    return medialAxis(this, diagram, fitTolerance, pruneLength)
}

/**
 * Computes the medial axis of [shape] from [diagram], a [ConstrainedVoronoiDiagram] of a linear simplification of
 * [shape] whose vertices lie on the boundary of [shape] (and no interior points).
 *
 * [ConstrainedVoronoiDiagram.internalEdges] approximates the medial axis; it determines the topology of the result.
 * Every vertex of the approximation is moved onto the medial axis of the original [shape] by finding the boundary
 * pieces it is (nearly) equidistant to and solving for equal distance. Junctions are solved for equal distance to
 * three boundary pieces. Leaves nearly equidistant to the two segments of a convex corner are extended to that corner.
 * Finally every branch is fitted with cubic Béziers, keeping the shared node positions exact.
 *
 * @param fitTolerance the maximum distance between the refined medial axis points and the fitted branches,
 * defaults to a tenth of the mean edge length of the simplification
 * @param pruneLength leaf branches attached to a junction whose significance is less than this are removed, defaults
 * to two times the mean edge length of the simplification. The significance of a branch is its length minus the
 * decrease of the inscribed radius along it; it is close to zero for the spurious branches that run straight towards
 * the boundary.
 */
fun medialAxis(
    shape: Shape,
    diagram: ConstrainedVoronoiDiagram,
    fitTolerance: Double? = null,
    pruneLength: Double? = null
): MedialAxis {
    val linearShape = diagram.constrainedDelaunayTriangulation.shape
    val spacing = linearShape.contours.flatMap { c -> c.segments.map { it.length } }.average()
        .takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    val scale = max(shape.bounds.width, shape.bounds.height).takeIf { it > 0.0 } ?: 1.0

    val (vertices, edges) = diagram.internalEdges()
    val sitePositions = diagram.triangulation.points
    val siteTree = sitePositions.kdTree()

    val graph = Graph(vertices.size)
    for ((a, b) in edges) graph.addEdge(a, b)
    val radii = vertices.map { v -> siteTree.findNearest(v)?.distanceTo(v) ?: 0.0 }
    graph.prune(vertices, radii, pruneLength ?: (2.0 * spacing))
    val runs = graph.branches()
    if (runs.isEmpty()) return MedialAxis(shape, emptyList(), emptyList())

    val boundary = Boundary(shape)

    // sites of the voronoi diagram, projected onto the original boundary; sites away from it are ignored
    val siteFeet = sitePositions.map { site ->
        boundary.project(site)?.takeIf { it.position.distanceTo(site) <= spacing }
    }
    val siteIndex = sitePositions.withIndex().associate { (i, p) -> p to i }

    val refiner = Refiner(shape, boundary, spacing, scale)
    val refined = mutableMapOf<Int, Refined>()
    fun refine(vertex: Int): Refined = refined.getOrPut(vertex) {
        val v = vertices[vertex]
        val seeds = siteTree.findKNearest(v, 8).mapNotNull { siteIndex[it] }
            .mapNotNull { siteFeet[it] }
        refiner.refine(v, seeds, min(graph.degree(vertex), 3))
    }

    val nodeIndex = mutableMapOf<Int, Int>()
    val nodes = mutableListOf<Vector2>()
    fun node(vertex: Int, position: Vector2): Int = nodeIndex.getOrPut(vertex) {
        nodes.add(position)
        nodes.size - 1
    }

    val branches = runs.map { run ->
        var runVertices = run.vertices
        val leafAtStart = !run.closed && graph.degree(runVertices.first()) == 1
        val leafAtEnd = !run.closed && graph.degree(runVertices.last()) == 1
        // trim leaf ends that don't touch the boundary at two separate places, those are left-overs of pruning
        fun Refined.trimmable() = corner == null && !twoSided
        if (leafAtEnd) while (runVertices.size > 2 && refine(runVertices.last()).trimmable()) {
            runVertices = runVertices.dropLast(1)
        }
        if (leafAtStart) while (runVertices.size > 2 && refine(runVertices.first()).trimmable()) {
            runVertices = runVertices.drop(1)
        }

        val points = runVertices.map { refine(it).position }.toMutableList()
        var start = -1
        var end = -1
        if (!run.closed) {
            // extend leaves to the convex corner or the center of curvature they point at. A tip that doesn't continue
            // the branch belongs to a pruned fork, as found at flattened ends with two curvature maxima.
            fun extension(fromLeaf: List<Refined>): Vector2? {
                val leaf = fromLeaf.first()
                return leaf.corner ?: leaf.tip?.takeIf { tip ->
                    val direction = branchDirection(fromLeaf)
                    direction != null && within30Degrees(direction, tip - leaf.position)
                }
            }
            if (leafAtStart) extension(runVertices.map { refine(it) })?.let { points.add(0, it) }
            if (leafAtEnd) extension(runVertices.map { refine(it) }.asReversed())?.let { points.add(it) }
            start = node(runVertices.first(), points.first())
            end = node(runVertices.last(), points.last())
        }
        val cleaned = removeBacktracks(points, 1E-9 * scale)
        val segments = if (cleaned.size >= 2) {
            fitCubicBeziers(cleaned, errorTolerance = fitTolerance ?: (0.1 * spacing))
        } else {
            listOf(Segment2D(cleaned.first(), cleaned.first()))
        }
        MedialAxisBranch(ShapeContour(segments, closed = run.closed), start, end)
    }
    return MedialAxis(shape, nodes, branches)
}

/**
 * The direction in which the branch given by [fromLeaf] (ordered from its leaf inwards) arrives at its leaf, measured
 * over a stretch of about one inscribed radius.
 */
private fun branchDirection(fromLeaf: List<Refined>): Vector2? {
    val end = fromLeaf[0]
    var length = 0.0
    for (i in 1 until fromLeaf.size) {
        length += fromLeaf[i].position.distanceTo(fromLeaf[i - 1].position)
        if (length >= end.radius || i == fromLeaf.lastIndex) {
            val direction = end.position - fromLeaf[i].position
            return if (direction.length > 0.0) direction else null
        }
    }
    return null
}

private fun within30Degrees(a: Vector2, b: Vector2): Boolean =
    a.length > 0.0 && b.length > 0.0 && a.normalized.dot(b.normalized) >= cos(PI / 6.0)

/**
 * Removes interior points that (nearly) coincide with their predecessor or that make the polyline fold back on itself.
 * The end points are always kept.
 */
private fun removeBacktracks(points: List<Vector2>, epsilon: Double): List<Vector2> {
    if (points.size <= 2) return points
    val result = mutableListOf(points.first())
    for (i in 1 until points.size - 1) {
        val p = points[i]
        val previous = result.last()
        val next = points[i + 1]
        if (p.distanceTo(previous) <= epsilon) continue
        if ((p - previous).dot(next - p) < 0.0 && (next - previous).dot(p - previous) < 0.0) continue
        result.add(p)
    }
    val last = points.last()
    if (result.size > 1 && result.last().distanceTo(last) <= epsilon) result.removeAt(result.lastIndex)
    result.add(last)
    return result
}

// --- graph ---------------------------------------------------------------------------------------

private class Run(val vertices: List<Int>, val closed: Boolean)

private class Graph(size: Int) {
    val adjacency = List(size) { mutableSetOf<Int>() }

    fun addEdge(a: Int, b: Int) {
        if (a == b) return
        adjacency[a].add(b)
        adjacency[b].add(a)
    }

    fun removeEdge(a: Int, b: Int) {
        adjacency[a].remove(b)
        adjacency[b].remove(a)
    }

    fun degree(vertex: Int) = adjacency[vertex].size

    /**
     * Repeatedly removes the leaf runs attached to a junction whose significance (length minus the decrease in
     * [radii] from junction to leaf) is less than [threshold], least significant first, never reducing a junction to
     * a leaf.
     */
    fun prune(vertices: List<Vector2>, radii: List<Double>, threshold: Double) {
        if (threshold <= 0.0) return
        while (true) {
            val candidates = branches()
                .filter { !it.closed }
                .mapNotNull { run ->
                    val v = run.vertices
                    when {
                        degree(v.first()) == 1 && degree(v.last()) >= 3 -> v.reversed()
                        degree(v.last()) == 1 && degree(v.first()) >= 3 -> v
                        else -> null
                    }
                }
                .map { v ->
                    val length = v.zipWithNext { a, b -> vertices[a].distanceTo(vertices[b]) }.sum()
                    v to length - (radii[v.first()] - radii[v.last()])
                }
                .filter { it.second < threshold }
                .sortedBy { it.second }
            var removed = false
            for ((v, _) in candidates) {
                if (degree(v.first()) < 3) continue
                for (i in 0 until v.size - 1) removeEdge(v[i], v[i + 1])
                removed = true
            }
            if (!removed) return
        }
    }

    /** Splits the graph into maximal runs whose interior vertices have degree 2, plus isolated cycles. */
    fun branches(): List<Run> {
        val visited = mutableSetOf<Long>()
        fun key(a: Int, b: Int) = min(a, b).toLong() * adjacency.size + max(a, b)
        val runs = mutableListOf<Run>()

        fun walk(from: Int, to: Int): List<Int> {
            val path = mutableListOf(from, to)
            visited.add(key(from, to))
            var previous = from
            var current = to
            while (degree(current) == 2 && current != from) {
                val next = adjacency[current].first { it != previous }
                if (!visited.add(key(current, next))) break
                path.add(next)
                previous = current
                current = next
            }
            return path
        }

        for (vertex in adjacency.indices) {
            if (degree(vertex) == 0 || degree(vertex) == 2) continue
            for (neighbor in adjacency[vertex]) {
                if (key(vertex, neighbor) in visited) continue
                runs.add(Run(walk(vertex, neighbor), closed = false))
            }
        }
        // what remains are cycles of degree 2 vertices
        for (vertex in adjacency.indices) {
            if (degree(vertex) != 2) continue
            val neighbor = adjacency[vertex].firstOrNull { key(vertex, it) !in visited } ?: continue
            val path = walk(vertex, neighbor)
            runs.add(Run(if (path.last() == path.first()) path else path + path.first(), closed = true))
        }
        return runs
    }
}

// --- boundary ------------------------------------------------------------------------------------

/** A point on the boundary of the original shape. */
private class Foot(val contour: Int, val segment: Int, val t: Double, val position: Vector2)

private fun Rectangle.distanceTo(p: Vector2): Double {
    val dx = max(max(x - p.x, 0.0), p.x - (x + width))
    val dy = max(max(y - p.y, 0.0), p.y - (y + height))
    return sqrt(dx * dx + dy * dy)
}

private class Boundary(shape: Shape) {
    val contours = shape.contours.filter { !it.empty }
    private val segmentBounds = contours.map { c -> c.segments.map { it.bounds } }

    /** The point on the boundary nearest to [p]. */
    fun project(p: Vector2): Foot? {
        var best: Foot? = null
        var bestDistance = Double.POSITIVE_INFINITY
        val candidates = contours.indices.flatMap { c -> contours[c].segments.indices.map { c to it } }
            .map { it to segmentBounds[it.first][it.second].distanceTo(p) }
            .sortedBy { it.second }
        for ((index, boundsDistance) in candidates) {
            if (boundsDistance > bestDistance) break
            val (c, s) = index
            val nearest = contours[c].segments[s].nearest(p)
            val d = nearest.position.distanceTo(p)
            if (d < bestDistance) {
                bestDistance = d
                best = Foot(c, s, nearest.segmentT, nearest.position)
            }
        }
        return best
    }

    private fun previous(contour: Int, segment: Int): Int? {
        val c = contours[contour]
        return if (segment > 0) segment - 1 else if (c.closed) c.segments.lastIndex else null
    }

    private fun next(contour: Int, segment: Int): Int? {
        val c = contours[contour]
        return if (segment < c.segments.lastIndex) segment + 1 else if (c.closed) 0 else null
    }

    /**
     * The local minimum of the distance between [c] and the boundary nearest to [start], found by Newton iteration
     * along the contour of [start], moving into neighboring segments where needed.
     */
    fun localFoot(start: Foot, c: Vector2): Foot {
        val contour = start.contour
        val segments = contours[contour].segments
        var j = start.segment
        var t = start.t
        var hops = 0
        val maxHops = segments.size

        for (iteration in 0 until 64) {
            val s = segments[j]
            val p = s.position(t)
            val d1 = s.derivative(t)
            val g = (p - c).dot(d1)
            val h = d1.squaredLength + (p - c).dot(s.derivative2(t))
            val step = if (h > 1E-12) -g / h else -g / max(d1.squaredLength, 1E-12)
            var tn = t + step
            if (tn < 0.0) {
                if (t <= 0.0) {
                    val jp = previous(contour, j) ?: return Foot(contour, j, 0.0, s.start)
                    val sp = segments[jp]
                    // the distance also decreases towards this segment from the previous one: a concave corner
                    if ((sp.end - c).dot(sp.derivative(1.0)) <= 0.0 || hops >= maxHops) {
                        return Foot(contour, j, 0.0, s.start)
                    }
                    j = jp; t = 1.0; hops++
                    continue
                }
                tn = 0.0
            } else if (tn > 1.0) {
                if (t >= 1.0) {
                    val jn = next(contour, j) ?: return Foot(contour, j, 1.0, s.end)
                    val sn = segments[jn]
                    if ((sn.start - c).dot(sn.derivative(0.0)) >= 0.0 || hops >= maxHops) {
                        return Foot(contour, j, 1.0, s.end)
                    }
                    j = jn; t = 0.0; hops++
                    continue
                }
                tn = 1.0
            }
            val converged = abs(tn - t) < 1E-12
            t = tn
            if (converged) break
        }
        return Foot(contour, j, t, segments[j].position(t))
    }

    /**
     * How far the boundary between [a] and [b] (along the shorter way around their contour) bulges away from the
     * disk centered at [c] that touches [a] and [b]: the maximum distance from [c] to that part of the boundary,
     * sampled with [step], minus the distance from [c] to [a]. Infinite when [a] and [b] lie on different contours.
     */
    fun gap(a: Foot, b: Foot, c: Vector2, step: Double): Double {
        val arc = shorterArc(a, b) ?: return Double.POSITIVE_INFINITY
        val r = a.position.distanceTo(c)
        var maxDistance = r
        sampleArc(a.contour, arc, step) { segment, t -> maxDistance = max(maxDistance, segment.position(t).distanceTo(c)) }
        return maxDistance - r
    }

    /**
     * The center of curvature at the point of maximum curvature on the boundary between [a] and [b] (along the
     * shorter way around their contour), considering only points whose center of curvature lies on the side of
     * [towards]. Null when [a] and [b] lie on different contours or no such point exists.
     */
    fun maximumCurvatureCenter(a: Foot, b: Foot, towards: Vector2, step: Double): Vector2? {
        val arc = shorterArc(a, b) ?: return null
        var best: Vector2? = null
        var bestCurvature = 0.0
        sampleArc(a.contour, arc, step) { segment, t ->
            val p = segment.position(t)
            val d1 = segment.derivative(t)
            val d2 = segment.derivative2(t)
            val cross = d1.x * d2.y - d1.y * d2.x
            val speed = d1.length
            if (abs(cross) > 1E-12 && speed > 1E-12) {
                val curvature = abs(cross) / (speed * speed * speed)
                val center = p + Vector2(-d1.y, d1.x) * (d1.squaredLength / cross)
                if (curvature > bestCurvature && (center - p).dot(towards - p) > 0.0) {
                    bestCurvature = curvature
                    best = center
                }
            }
        }
        return best
    }

    /**
     * The pieces (segment, t0, t1) of the shorter way from [a] to [b] along their contour. [swapped] is set when
     * this is already the reverse direction, so that a near tie (both ways rounding to more than half the contour
     * length) doesn't swap back and forth forever.
     */
    private fun shorterArc(a: Foot, b: Foot, swapped: Boolean = false): List<Triple<Int, Double, Double>>? {
        if (a.contour != b.contour) return null
        val contour = contours[a.contour]
        val segments = contour.segments

        val pieces = mutableListOf<Triple<Int, Double, Double>>()
        if (a.segment == b.segment && a.t <= b.t) {
            pieces.add(Triple(a.segment, a.t, b.t))
        } else {
            if (!contour.closed && b.segment < a.segment) return shorterArc(b, a)
            pieces.add(Triple(a.segment, a.t, 1.0))
            var j = next(a.contour, a.segment)
            while (j != null && j != b.segment) {
                pieces.add(Triple(j, 0.0, 1.0))
                j = next(a.contour, j)
            }
            pieces.add(Triple(b.segment, 0.0, b.t))
        }
        val forwardLength = pieces.sumOf { (j, t0, t1) -> segments[j].length * (t1 - t0) }
        if (contour.closed && !swapped && forwardLength > contour.length - forwardLength) return shorterArc(b, a, true)
        return pieces
    }

    private inline fun sampleArc(
        contour: Int,
        pieces: List<Triple<Int, Double, Double>>,
        step: Double,
        sample: (Segment2D, Double) -> Unit
    ) {
        val segments = contours[contour].segments
        for ((j, t0, t1) in pieces) {
            val n = max(1, ceil(segments[j].length * (t1 - t0) / step).toInt())
            for (i in 0..n) sample(segments[j], t0 + (t1 - t0) * i / n)
        }
    }

    /**
     * The corner shared by the segments of [a] and [b] if they are adjacent segments of the same contour that meet
     * at an angle.
     */
    fun sharedCorner(a: Foot, b: Foot): Vector2? {
        if (a.contour != b.contour) return null
        val (first, second) = when {
            next(a.contour, a.segment) == b.segment -> a to b
            next(b.contour, b.segment) == a.segment -> b to a
            else -> return null
        }
        val segments = contours[a.contour].segments
        val incoming = segments[first.segment].derivative(1.0)
        val outgoing = segments[second.segment].derivative(0.0)
        if (incoming.squaredLength < 1E-24 || outgoing.squaredLength < 1E-24) return null
        val angle = acos(incoming.normalized.dot(outgoing.normalized).coerceIn(-1.0, 1.0))
        return if (angle > 1E-3) segments[second.segment].start else null
    }
}

// --- refinement ----------------------------------------------------------------------------------

/**
 * A vertex moved onto the medial axis of the original shape.
 * @property corner the convex corner the vertex points at, if any
 * @property twoSided true when the vertex touches the boundary at two separate places
 * @property tip the center of curvature where the medial axis ends when continued past this vertex, if any
 * @property radius the distance from [position] to the boundary
 */
private class Refined(
    val position: Vector2,
    val corner: Vector2?,
    val twoSided: Boolean,
    val tip: Vector2?,
    val radius: Double
)

private class Refiner(val shape: Shape, val boundary: Boundary, val spacing: Double, val scale: Double) {

    /**
     * Moves [v] onto the medial axis by making it equidistant to the [sides] nearest distinct boundary pieces found
     * from [seeds]. Three sides are used for junctions, two otherwise.
     */
    fun refine(v: Vector2, seeds: List<Foot>, sides: Int): Refined {
        val mergeDistance = 1E-6 * scale
        val distinct = mutableListOf<Foot>()
        for (seed in seeds) {
            val foot = boundary.localFoot(seed, v)
            if (distinct.none { it.position.distanceTo(foot.position) <= mergeDistance }) distinct.add(foot)
        }
        distinct.sortBy { it.position.distanceTo(v) }
        if (distinct.size < 2) {
            return Refined(v, null, false, null, distinct.firstOrNull()?.position?.distanceTo(v) ?: 0.0)
        }

        val feet = distinct.take(if (sides >= 3 && distinct.size >= 3) 3 else 2).toMutableList()
        var c = v
        val r0 = feet[0].position.distanceTo(v)
        val maxShift = max(0.5 * r0, 2.0 * spacing)

        for (iteration in 0 until 32) {
            val d = feet.map { it.position.distanceTo(c) }
            if (d.any { it < 1E-12 }) break
            val u = feet.mapIndexed { i, f -> (c - f.position) / d[i] }

            var step: Vector2? = null
            if (feet.size == 3) {
                val r1 = u[0] - u[1]
                val r2 = u[0] - u[2]
                val f1 = d[0] - d[1]
                val f2 = d[0] - d[2]
                val det = r1.x * r2.y - r1.y * r2.x
                if (abs(det) > 1E-6) {
                    step = Vector2(-(r2.y * f1 - r1.y * f2) / det, -(-r2.x * f1 + r1.x * f2) / det)
                }
            }
            if (step == null) {
                val gradient = u[0] - u[1]
                val g2 = gradient.squaredLength
                if (g2 < 1E-4) break
                step = gradient * (-(d[0] - d[1]) / g2)
            }
            val limit = max(spacing, 0.25 * d.min())
            if (step.length > limit) step = step.normalized * limit

            c += step
            for (i in feet.indices) feet[i] = boundary.localFoot(feet[i], c)
            if (step.length < 1E-10 * scale) break
        }

        val accepted = c.distanceTo(v) <= maxShift && c in shape
        val position = if (accepted) c else v
        val corner = boundary.sharedCorner(feet[0], feet[1])
        val twoSided = accepted && feet[0].position.distanceTo(feet[1].position) >= spacing &&
                boundary.gap(feet[0], feet[1], position, 0.5 * spacing) >= 0.1 * spacing
        val tip = if (twoSided && corner == null) {
            boundary.maximumCurvatureCenter(feet[0], feet[1], position, 0.25 * spacing)
                ?.takeIf { it.distanceTo(position) <= 0.5 * feet[0].position.distanceTo(position) && it in shape }
        } else null
        return Refined(position, corner, twoSided, tip, feet[0].position.distanceTo(position))
    }
}
