package org.openrndr.extra.shapes.metro

import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Schematizes this [Shape] in the style of a metro map: every contour is replaced by a polyline whose segments are
 * all parallel to one of [directions] evenly spaced directions. See [ShapeContour.metro].
 */
fun Shape.metro(
    directions: Int = 4,
    angleOffset: Double = 0.0,
    segmentPenalty: Double = 0.0,
    boundsWeight: Double = 0.0,
    distanceTolerance: Double = 0.5,
    samplingDistance: Double = Double.POSITIVE_INFINITY
): Shape = Shape(contours.map {
    it.metro(directions, angleOffset, segmentPenalty, boundsWeight, distanceTolerance, samplingDistance)
})

/**
 * Schematizes this [ShapeContour] in the style of a metro map: the contour is replaced by a polyline whose segments
 * are all parallel to one of [directions] evenly spaced directions (2: rectilinear, 3: hexilinear, 4: octilinear).
 *
 * The contour is linearized using [ShapeContour.sampleLinear], optionally subdivided to a spacing of at most
 * [samplingDistance], after which the vertices are fitted using [metro].
 *
 * @param directions the number of allowed segment directions, evenly spread over 180 degrees
 * @param angleOffset rotation of the allowed directions in degrees
 * @param segmentPenalty cost per output segment, in squared distance units, see [metro]
 * @param boundsWeight weight of the segment length error, see [metro]
 * @param distanceTolerance the distance tolerance used to linearize curved segments
 * @param samplingDistance the maximum spacing of the fitted points, the default only uses the linearized vertices
 */
fun ShapeContour.metro(
    directions: Int = 4,
    angleOffset: Double = 0.0,
    segmentPenalty: Double = 0.0,
    boundsWeight: Double = 0.0,
    distanceTolerance: Double = 0.5,
    samplingDistance: Double = Double.POSITIVE_INFINITY
): ShapeContour {
    if (empty) return this
    val points = mutableListOf<Vector2>()
    val linear = sampleLinear(distanceTolerance)
    for (segment in linear.segments) {
        val n = if (samplingDistance.isFinite()) max(1, ceil(segment.length / samplingDistance).toInt()) else 1
        for (i in 0 until n) points.add(segment.start.mix(segment.end, i.toDouble() / n))
    }
    if (!closed) points.add(linear.segments.last().end)
    val result = points.metro(closed, directions, angleOffset, segmentPenalty, boundsWeight)
    return ShapeContour.fromPoints(result, closed, polarity)
}

/**
 * Fits a polyline with segments in a limited set of directions to this sequence of points, using the path
 * schematisation heuristic described in Dwyer, Hurst and Merrick, "A Fast and Simple Heuristic for Metro Map Path
 * Simplification" (ISVC 2008).
 *
 * The points are partitioned into blocks of consecutive points, starting with blocks of two points. Each block is
 * fitted with the least-squares line through its barycenter in the best of the allowed directions. Adjacent blocks
 * share their end point. Adjacent blocks with parallel lines are merged until no parallel neighbours remain. The
 * vertices of the output are the intersections of the lines of adjacent blocks; the first and last vertex of an
 * open path are the projections of the first and last point onto their block lines.
 *
 * When [segmentPenalty] or [boundsWeight] is positive, adjacent blocks are then greedily merged as long as that
 * lowers the cost function
 *
 * `cost = segmentPenalty * m + sum(regression(B) + boundsWeight * bounds(B))`
 *
 * where `m` is the number of blocks, `regression(B)` the sum of squared distances between the points of block `B`
 * and its line and `bounds(B)` the squared distance between the ends of the segment of `B` and the extent of its
 * points along the line. This is equation 2 of the paper with `alpha = segmentPenalty * n` and `beta = 1`.
 *
 * @param closed whether the points describe a closed loop
 * @param directions the number of allowed segment directions, evenly spread over 180 degrees
 * @param angleOffset rotation of the allowed directions in degrees
 * @param segmentPenalty cost per output segment, in squared distance units; larger values give simpler paths
 * @param boundsWeight weight of the segment length error; positive values remove short segments that fit poorly
 * @return the vertices of the schematized polyline, for closed input the last vertex is not repeated
 */
fun List<Vector2>.metro(
    closed: Boolean,
    directions: Int = 4,
    angleOffset: Double = 0.0,
    segmentPenalty: Double = 0.0,
    boundsWeight: Double = 0.0
): List<Vector2> {
    require(directions >= 1) { "directions should be at least 1, got $directions" }
    val points = filterIndexed { i, p ->
        val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
        next == null || p.squaredDistanceTo(next) > 1E-18
    }
    if (points.size < 2 || (closed && points.size < 3)) return points
    val origin = points.reduce { a, b -> a + b } / points.size.toDouble()
    val directionVectors = List(directions) {
        val angle = angleOffset.asRadians + PI * it / directions
        Vector2(cos(angle), sin(angle))
    }
    val fit = MetroFit(points.map { it - origin }, closed, directionVectors, segmentPenalty, boundsWeight)
    return fit.solve().map { it + origin }
}

private val Double.asRadians get() = this * PI / 180.0

/**
 * A block of consecutive points `start..end` (inclusive, indices modulo the point count for closed paths) fitted with
 * the line through [centroid] in direction [direction].
 */
private class Block(val start: Int, val end: Int, val direction: Int, val centroid: Vector2, val regression: Double) {
    var prev: Block? = null
    var next: Block? = null
    var alive = true
}

private class MetroFit(
    val points: List<Vector2>,
    val closed: Boolean,
    val directions: List<Vector2>,
    val segmentPenalty: Double,
    val boundsWeight: Double
) {
    val n = points.size

    // prefix sums of the sufficient statistics, over the points repeated twice for closed paths
    val count = if (closed) 2 * n else n
    val sx = DoubleArray(count + 1)
    val sy = DoubleArray(count + 1)
    val sxx = DoubleArray(count + 1)
    val syy = DoubleArray(count + 1)
    val sxy = DoubleArray(count + 1)

    init {
        for (i in 0 until count) {
            val p = points[i % n]
            sx[i + 1] = sx[i] + p.x
            sy[i + 1] = sy[i] + p.y
            sxx[i + 1] = sxx[i] + p.x * p.x
            syy[i + 1] = syy[i] + p.y * p.y
            sxy[i + 1] = sxy[i] + p.x * p.y
        }
    }

    val minBlocks = if (closed) 3 else 1
    var blockCount = 0
    lateinit var head: Block

    fun point(i: Int) = points[i % n]

    /**
     * Fits the points `start..end` with the least-squares line through their barycenter, in the direction with the
     * lowest regression cost (Theorem 1 of the paper).
     */
    fun fit(start: Int, end: Int): Block {
        val m = (end - start + 1).toDouble()
        val x = sx[end + 1] - sx[start]
        val y = sy[end + 1] - sy[start]
        // central second moments
        val cxx = sxx[end + 1] - sxx[start] - x * x / m
        val cyy = syy[end + 1] - syy[start] - y * y / m
        val cxy = sxy[end + 1] - sxy[start] - x * y / m
        var best = Double.POSITIVE_INFINITY
        var bestDirection = 0
        for ((i, d) in directions.withIndex()) {
            // the normal of d is (-d.y, d.x)
            val cost = d.y * d.y * cxx + d.x * d.x * cyy - 2.0 * d.x * d.y * cxy
            if (cost < best - 1E-12 * max(1.0, abs(cost))) {
                best = cost
                bestDirection = i
            }
        }
        return Block(start, end, bestDirection, Vector2(x / m, y / m), max(0.0, best))
    }

    fun merged(a: Block, b: Block): Block = fit(a.start, a.start + (a.end - a.start) + (b.end - b.start))

    fun merge(a: Block, b: Block): Block {
        val m = merged(a, b)
        m.prev = a.prev
        m.next = b.next
        a.prev?.next = m
        b.next?.prev = m
        a.alive = false
        b.alive = false
        if (head === a || head === b) head = m
        blockCount--
        return m
    }

    fun blocks(): List<Block> {
        val result = mutableListOf<Block>()
        var b: Block? = head
        while (b != null) {
            result.add(b)
            b = b.next
            if (b === head) break
        }
        return result
    }

    fun project(b: Block, p: Vector2): Vector2 {
        val d = directions[b.direction]
        return b.centroid + d * (p - b.centroid).dot(d)
    }

    /**
     * The vertices connecting block [a] to the next block [b]: the intersection of their lines or, if the lines are
     * parallel, the projections of their shared point onto both lines.
     */
    fun junction(a: Block, b: Block): List<Vector2> {
        val da = directions[a.direction]
        val db = directions[b.direction]
        val denominator = da cross db
        if (a.direction != b.direction && abs(denominator) > 1E-12) {
            val t = ((b.centroid - a.centroid) cross db) / denominator
            return listOf(a.centroid + da * t)
        }
        val shared = point(b.start)
        val pa = project(a, shared)
        val pb = project(b, shared)
        return if (pa.squaredDistanceTo(pb) < 1E-18) listOf(pa) else listOf(pa, pb)
    }

    fun segmentStart(b: Block, prev: Block?) = if (prev == null) project(b, point(b.start)) else junction(prev, b).last()

    fun segmentEnd(b: Block, next: Block?) = if (next == null) project(b, point(b.end)) else junction(b, next).first()

    /**
     * The squared distances between the ends of the segment of [b] and the extent of its points along its line.
     */
    fun boundsCost(b: Block, prev: Block?, next: Block?): Double {
        var d = directions[b.direction]
        if ((point(b.end) - point(b.start)).dot(d) < 0.0) d = -d
        var lo = Double.POSITIVE_INFINITY
        var hi = Double.NEGATIVE_INFINITY
        for (i in b.start..b.end) {
            val t = point(i).dot(d)
            lo = min(lo, t)
            hi = max(hi, t)
        }
        val t0 = segmentStart(b, prev).dot(d)
        val t1 = segmentEnd(b, next).dot(d)
        return (t0 - lo) * (t0 - lo) + (t1 - hi) * (t1 - hi)
    }

    /**
     * Whether the segment of [b], between the junctions with its current neighbours, runs against the order of its
     * points. This happens for short blocks whose neighbours' lines cross beyond them, the output then overshoots and
     * doubles back along the segment of [b], forming a small loop.
     */
    fun reversed(b: Block): Boolean {
        var d = directions[b.direction]
        if ((point(b.end) - point(b.start)).dot(d) < 0.0) d = -d
        return segmentEnd(b, b.next).dot(d) < segmentStart(b, b.prev).dot(d) - 1E-9
    }

    fun cost(b: Block, prev: Block?, next: Block?): Double =
        segmentPenalty + b.regression + if (boundsWeight > 0.0) boundsWeight * boundsCost(b, prev, next) else 0.0

    /**
     * Merges blocks until no adjacent blocks have parallel lines (Section 3 of the paper) and no block has a reversed
     * segment, see [reversed]. A reversed block is merged with the neighbour that increases the regression cost the
     * least.
     */
    fun mergeForced() {
        var changed = true
        while (changed) {
            changed = false
            for (a in blocks()) {
                if (!a.alive || blockCount <= minBlocks) continue
                val b = a.next
                if (b != null && a.direction == b.direction) {
                    merge(a, b)
                    changed = true
                } else if (reversed(a)) {
                    val p = a.prev
                    val withPrev = p?.let { merged(it, a).regression - it.regression } ?: Double.POSITIVE_INFINITY
                    val withNext = b?.let { merged(a, it).regression - it.regression } ?: Double.POSITIVE_INFINITY
                    if (withPrev < withNext) merge(p!!, a) else merge(a, b!!)
                    changed = true
                }
            }
        }
    }

    /**
     * Greedily merges the pair of adjacent blocks that lowers the cost function the most (Section 4 of the paper).
     */
    fun mergeGreedy(): Boolean {
        if (blockCount <= minBlocks) return false
        var bestDelta = 0.0
        var bestPair: Block? = null
        for (a in blocks()) {
            val b = a.next ?: continue
            val m = merged(a, b)
            val pa = a.prev
            val nb = b.next
            val before = cost(a, pa, b) + cost(b, a, nb) +
                    (pa?.let { cost(it, it.prev, a) } ?: 0.0) + (nb?.let { cost(it, b, it.next) } ?: 0.0)
            val after = cost(m, pa, nb) +
                    (pa?.let { cost(it, it.prev, m) } ?: 0.0) + (nb?.let { cost(it, m, it.next) } ?: 0.0)
            val delta = after - before
            if (delta < bestDelta) {
                bestDelta = delta
                bestPair = a
            }
        }
        val a = bestPair ?: return false
        merge(a, a.next!!)
        return true
    }

    fun solve(): List<Vector2> {
        // initial partitioning into blocks of two points
        val initial = List(if (closed) n else n - 1) { fit(it, it + 1) }
        for (i in initial.indices) {
            initial[i].next = initial.getOrNull(i + 1) ?: if (closed) initial[0] else null
            initial[i].prev = initial.getOrNull(i - 1) ?: if (closed) initial.last() else null
        }
        head = initial[0]
        blockCount = initial.size

        mergeForced()
        if (segmentPenalty > 0.0 || boundsWeight > 0.0) {
            while (mergeGreedy()) {
                mergeForced()
            }
        }

        val blocks = blocks()
        val result = mutableListOf<Vector2>()
        if (closed) {
            for (b in blocks) result.addAll(junction(b, b.next!!))
        } else {
            result.add(segmentStart(blocks.first(), null))
            for (b in blocks.dropLast(1)) result.addAll(junction(b, b.next!!))
            result.add(segmentEnd(blocks.last(), null))
        }
        return result
    }
}
