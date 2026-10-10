package offset

import org.openrndr.extra.shapes.distort.FoldMode
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.times
import org.openrndr.shape.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt


private fun Segment2D.splitOnExtrema(): List<Segment2D> {
    var extrema = extrema().toMutableList()

    if (isStraight(0.05)) {
        return listOf(this)
    }

    if (simple && extrema.isEmpty()) {
        return listOf(this)
    }

    if (extrema.isEmpty()) {
        return listOf(this)
    }
    if (extrema[0] <= 0.01) {
        extrema[0] = 0.0
    } else {
        extrema = (mutableListOf(0.0) + extrema).toMutableList()
    }

    if (extrema.last() < 0.99) {
        extrema = (extrema + listOf(1.0)).toMutableList()
    } else if (extrema.last() >= 0.99) {
        extrema[extrema.lastIndex] = 1.0
    }

    return extrema.zipWithNext().map {
        sub(it.first, it.second)
    }
}

private fun Segment2D.splitToSimple(step: Double): List<Segment2D> {
    var t1 = 0.0
    var t2 = 0.0
    val result = mutableListOf<Segment2D>()
    while (t2 <= 1.0) {
        t2 = t1 + step
        while (t2 <= 1.0 + step) {
            val segment = sub(t1, t2)
            if (!segment.simple) {
                t2 -= step
                if (abs(t1 - t2) < step) {
                    return listOf(this)
                }
                val segment2 = sub(t1, t2)
                result.add(segment2)
                t1 = t2
                break
            }
            t2 += step
        }

    }
    if (t1 < 1.0) {
        result.add(sub(t1, 1.0))
    }
    if (result.isEmpty()) {
        result.add(this)
    }
    return result
}


fun Segment2D.reduced(stepSize: Double = 0.01): List<Segment2D> {
    val pass1 = splitOnExtrema()
    //return pass1
    return pass1.flatMap { it.splitToSimple(stepSize) }
}

fun Segment2D.scale(scale: Double, polarity: YPolarity) = scale(polarity) { scale }

fun Segment2D.scale(polarity: YPolarity, scale: (Double) -> Double): Segment2D {
    if (control.size == 1) {
        return cubic.scale(polarity, scale)
    }

    val newStart = start + normal(0.0, polarity) * scale(0.0)
    val newEnd = end + normal(1.0, polarity) * scale(1.0)

    val a = LineSegment(newStart, start)
    val b = LineSegment(newEnd, end)

    val o = intersection(a, b, 1E7)

    if (o != Vector2.INFINITY) {
        val newControls = control.mapIndexed { index, it ->
            val d = it - o
            val rc = scale((index + 1.0) / 3.0)
            val s = normal(0.0, polarity).dot(d).sign
            val nd = d.normalized * s
            it + rc * nd
        }
        return copy(newStart, newControls, newEnd)
    } else {
        val newControls = control.mapIndexed { index, it ->
            val rc = scale((index + 1.0) / 3.0)
            it + rc * normal((index + 1.0), polarity)
        }
        return copy(newStart, newControls, newEnd)
    }
}

fun Segment2D.offset(
    distance: Double,
    stepSize: Double = 0.01,
    yPolarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): List<Segment2D> {
    return if (linear) {
        val n = normal(0.0, yPolarity)
        if (distance > 0.0) {
            listOf(Segment2D(start + distance * n, end + distance * n))
        } else {
            val d = direction()
            val s = distance.coerceAtMost(length / 2.0)
            val candidate = Segment2D(
                start - s * d + distance * n,
                end + s * d + distance * n
            )
            if (candidate.length > 0.0) {
                listOf(candidate)
            } else {
                emptyList()
            }
        }
    } else {
        reduced(stepSize).map { it.scale(distance, yPolarity) }
    }
}


/**
 * Offsets a [ShapeContour] by given [distance].
 *
 * For a closed contour the result is moved outwards if [distance] is > 0 or inwards if [distance] is < 0, for an open
 * contour it is moved along the contour's normal. The contour is split at its sharp corners into smooth runs. Every run
 * is offset along its normal with [RectifiedContour.distortUniform], with [FoldMode.TRIM] cutting out the stretches
 * where the offset would fold over itself. The runs are then joined: where they pull apart the gap is closed with a
 * join of type [joinType], where they overlap both runs are cut back to their crossing. Remaining self-intersections
 * are resolved with [ShapeContour.removeSelfIntersections], and pieces that are not at [distance] from the contour
 * (such as the inverted remains of an inward offset larger than the shape) are dropped.
 *
 * @param joinType Specifies how to join together the moved runs at sharp corners.
 * @param errorTolerance maximum fit error of the offset curves
 * @param sampleDistance distance along the contour between samples used for fitting the offset curves
 */
fun ShapeContour.offset(
    distance: Double,
    joinType: SegmentJoin = SegmentJoin.ROUND,
    errorTolerance: Double = 0.05,
    sampleDistance: Double = 1.0
): Shape {
    if (empty) {
        return Shape(emptyList())
    }
    if (distance == 0.0) {
        return shape
    }
    if (closed && winding != Winding.CLOCKWISE) {
        // the inside tests below rely on the contour's own shape, which is empty for a counter-clockwise contour
        return clockwise.offset(distance, joinType, errorTolerance, sampleDistance)
    }
    if (closed && distance < 0.0 && -distance >= 0.5 * min(bounds.width, bounds.height)) {
        // no disk of radius -distance fits inside the bounds, so nothing is left of the eroded shape
        return Shape(emptyList())
    }

    // positive distances move a closed contour outwards, whichever way its normals point
    val cleaned = offsetCurve(distance * normalSign(), joinType, errorTolerance, sampleDistance)
    if (cleaned.empty) {
        return Shape(emptyList())
    }

    val resolved = cleaned.removeSelfIntersections()
    // Every point of a correct offset is at distance |distance| from the contour, and for a closed contour on the
    // side of the contour given by the sign of distance. That rules out remains of collapsed parts, which end up too
    // close to the contour or on the wrong side of it.
    val tolerance = max(0.05 * abs(distance), 10.0 * errorTolerance)
    return Shape(resolved.contours.filter { candidate ->
        val samples = candidate.equidistantPositions(32)
        val valid = samples.count { p ->
            val atDistance = abs(p.distanceTo(nearest(p).position) - abs(distance)) <= tolerance
            val onSide = !closed || (p in shape) == (distance < 0.0)
            atDistance && onSide
        }
        valid > samples.size / 2
    })
}

/**
 * The offset curve of this contour at [signedDistance] along its normal, before self-intersections are resolved. The
 * contour is split at its sharp corners into smooth runs that are offset with [RectifiedContour.distortUniform] and
 * [FoldMode.TRIM]. Neighbouring runs that overlap are cut back to their crossing, gaps are closed with a join of type
 * [joinType]. Segments too short for removeSelfIntersections are dropped.
 */
internal fun ShapeContour.offsetCurve(
    signedDistance: Double,
    joinType: SegmentJoin,
    errorTolerance: Double,
    sampleDistance: Double
): ShapeContour {
    fun offsetRun(run: ShapeContour): ShapeContour {
        val rectified = run.rectified()
        return rectified.distortUniform(errorTolerance, maxDepth = 12, sampleDistance, FoldMode.TRIM) { t, position ->
            position + rectified.normal(t) * signedDistance
        }
    }

    val runs = cornerRuns()
    val result = if (runs == null) {
        // a closed contour without sharp corners is offset in one piece
        offsetRun(this)
    } else {
        joinRuns(runs, runs.map { offsetRun(it) }, signedDistance, joinType)
    }
    return result.withoutTinySegments()
}

/**
 * Offsets a [Shape] by given [distance].
 *
 * The filled region of the shape grows if [distance] is > 0 and shrinks if [distance] is < 0: outlines move outwards
 * while holes shrink, and the other way around. Which closed contours are outlines and which are holes follows from
 * their nesting (a contour inside an odd number of other contours is a hole), not from their winding, so islands
 * inside holes are handled too. Every outline is offset with [ShapeContour.offset], its holes are offset by
 * -[distance] and subtracted, and the results for all outlines are combined. Open contours are offset individually and
 * added to the result.
 *
 * @param joinType Specifies how to join together the moved runs at sharp corners.
 * @param errorTolerance maximum fit error of the offset curves
 * @param sampleDistance distance along the contours between samples used for fitting the offset curves
 */
fun Shape.offset(
    distance: Double,
    joinType: SegmentJoin = SegmentJoin.ROUND,
    errorTolerance: Double = 0.05,
    sampleDistance: Double = 1.0
): Shape {
    if (empty) {
        return Shape(emptyList())
    }
    if (distance == 0.0) {
        return this
    }

    // clockwise copies, such that every contour's own shape is its inside regardless of how it was wound
    val closed = contours.filter { it.closed && !it.empty }.map { it.clockwise }
    val open = contours.filter { !it.closed && !it.empty }

    // nesting depth of every closed contour: the number of other closed contours it lies inside
    val depths = closed.map { contour ->
        val p = contour.segments.first().position(0.5)
        closed.count { other -> other !== contour && p in other.shape }
    }

    var filled: Shape? = null
    for ((index, outline) in closed.withIndex()) {
        if (depths[index] % 2 != 0) continue
        var component = outline.offset(distance, joinType, errorTolerance, sampleDistance)
        // the holes of this outline are the contours directly inside it
        for ((holeIndex, hole) in closed.withIndex()) {
            if (depths[holeIndex] != depths[index] + 1 || component.empty) continue
            if (hole.segments.first().position(0.5) !in outline.shape) continue
            val holeOffset = hole.offset(-distance, joinType, errorTolerance, sampleDistance)
            if (!holeOffset.empty) {
                component = component.difference(holeOffset)
            }
        }
        if (!component.empty) {
            filled = filled?.union(component) ?: component
        }
    }

    val openOffsets = open.flatMap { it.offset(distance, joinType, errorTolerance, sampleDistance).contours }
    return Shape((filled?.contours ?: emptyList()) + openOffsets)
}

/**
 * Drops segments shorter than [minLength] and reconnects their neighbours. Fitting towards a near-stationary point of
 * a folded offset produces cascades of ever shorter segments, and segments around the intersection precision of
 * removeSelfIntersections (about 1e-5) make it fail.
 */
internal fun ShapeContour.withoutTinySegments(minLength: Double = 1E-4): ShapeContour {
    val kept = mutableListOf<Segment2D>()
    for (segment in segments) {
        if (segment.length < minLength) continue
        val last = kept.lastOrNull()
        kept.add(if (last != null && last.end != segment.start) segment.copy(start = last.end) else segment)
    }
    if (kept.isEmpty()) {
        return ShapeContour.EMPTY
    }
    if (closed && kept.last().end != kept.first().start) {
        kept[kept.size - 1] = kept.last().copy(end = kept.first().start)
    }
    return ShapeContour(kept, closed, polarity)
}

/** 1.0 when the normals of this closed contour point outwards (or the contour is open), -1.0 otherwise */
private fun ShapeContour.normalSign(): Double {
    if (!closed) {
        return 1.0
    }
    val longest = segments.maxBy { it.length }
    val p = longest.position(0.5)
    val n = longest.normal(0.5, polarity)
    val eps = 1E-4 * (bounds.width + bounds.height)
    return if (shape.contains(p + n * eps)) -1.0 else 1.0
}

private fun isCorner(incoming: Vector2, outgoing: Vector2): Boolean {
    val cross = incoming.x * outgoing.y - incoming.y * outgoing.x
    return abs(kotlin.math.atan2(cross, incoming.dot(outgoing))) > 1E-3
}

/**
 * Splits this contour at its sharp corners into smooth open runs, the first run starting at a corner.
 * @return the runs, or null for a closed contour without sharp corners
 */
private fun ShapeContour.cornerRuns(): List<ShapeContour>? {
    val n = segments.size
    val cornerAfter = (0 until n).map { i ->
        if (i == n - 1 && !closed) false else isCorner(segments[i].direction(1.0), segments[(i + 1) % n].direction(0.0))
    }
    if (closed && cornerAfter.none { it }) {
        return null
    }
    // for a closed contour start right after a corner
    val first = if (closed) (cornerAfter.indexOfFirst { it } + 1) % n else 0
    val runs = mutableListOf<ShapeContour>()
    var current = mutableListOf<Segment2D>()
    for (k in 0 until n) {
        val i = (first + k) % n
        current.add(segments[i])
        if (cornerAfter[i] || k == n - 1) {
            runs.add(ShapeContour(current, false, polarity))
            current = mutableListOf()
        }
    }
    return runs
}

/**
 * Joins the offset [offsets] of [runs]: overlapping neighbours are cut back to their crossing, gaps are closed with
 * a join of type [joinType] around the corner between the runs.
 */
private fun ShapeContour.joinRuns(
    runs: List<ShapeContour>,
    offsets: List<ShapeContour>,
    signedDistance: Double,
    joinType: SegmentJoin
): ShapeContour {
    val count = runs.size
    val cornerCount = if (closed) count else count - 1

    // per corner (after run i) either the crossing parameters of the overlapping offsets, or null for a gap
    val crossings = (0 until cornerCount).map { i ->
        val a = offsets[i]
        val b = offsets[(i + 1) % count]
        if (a.empty || b.empty) {
            null
        } else {
            intersections(a, b).minByOrNull { (1.0 - it.a.contourT) * a.length + it.b.contourT * b.length }
                ?.let { it.a.contourT to it.b.contourT }
        }
    }

    // trim every offset run to the crossings at both of its ends
    val trimmed = offsets.mapIndexed { i, offset ->
        if (offset.empty) {
            offset
        } else {
            val previous = if (i > 0 || closed) crossings.getOrNull((i - 1 + count) % count) else null
            val next = crossings.getOrNull(i)
            val t0 = previous?.second ?: 0.0
            val t1 = next?.first ?: 1.0
            if (t1 - t0 > 1E-9) offset.sub(t0, t1) else offset
        }
    }

    val segments = mutableListOf<Segment2D>()
    fun append(segment: Segment2D) {
        // degenerate segments can't be handled by removeSelfIntersections
        if (segment.length < 1E-9) {
            return
        }
        val last = segments.lastOrNull()
        if (last != null && last.end != segment.start) {
            // snap tiny gaps left by trimming at a crossing, bridge real ones
            if (last.end.distanceTo(segment.start) < 1E-6) {
                segments.add(segment.copy(start = last.end))
                return
            }
            segments.add(Segment2D(last.end, segment.start))
        }
        segments.add(segment)
    }

    for (i in 0 until count) {
        trimmed[i].segments.forEach { append(it) }
        if (i < cornerCount && crossings[i] == null) {
            val nextOffset = trimmed[(i + 1) % count]
            val last = segments.lastOrNull()
            if (last != null && !nextOffset.empty) {
                val vertex = runs[i].segments.last().end
                joinSegments(vertex, last.end, last.direction(1.0), nextOffset.segments.first().start,
                    nextOffset.segments.first().direction(0.0), abs(signedDistance), joinType).forEach { append(it) }
            }
        }
    }
    if (segments.isEmpty()) {
        return ShapeContour.EMPTY
    }
    if (closed) {
        val first = segments.first()
        val last = segments.last()
        if (last.end != first.start) {
            if (last.end.distanceTo(first.start) < 1E-6) {
                segments[segments.size - 1] = last.copy(end = first.start)
            } else {
                segments.add(Segment2D(last.end, first.start))
            }
        }
    }
    return ShapeContour(segments, closed, polarity)
}

/**
 * The segments of a join from [from] (arriving in direction [fromDirection]) to [to] (leaving in direction
 * [toDirection]) around [vertex].
 */
private fun joinSegments(
    vertex: Vector2,
    from: Vector2,
    fromDirection: Vector2,
    to: Vector2,
    toDirection: Vector2,
    radius: Double,
    joinType: SegmentJoin
): List<Segment2D> {
    if (from.distanceTo(to) < 1E-9) {
        return emptyList()
    }
    return when (joinType) {
        SegmentJoin.BEVEL -> listOf(Segment2D(from, to))
        SegmentJoin.MITER -> {
            val miter = intersection(from, from + fromDirection, to, to - toDirection, eps = 10E8)
            val inFront = miter != Vector2.INFINITY &&
                    (miter - from).dot(fromDirection) > 0.0 && (to - miter).dot(toDirection) > 0.0
            if (inFront) listOf(Segment2D(from, miter), Segment2D(miter, to)) else listOf(Segment2D(from, to))
        }
        SegmentJoin.ROUND -> arcSegments(vertex, from, to, radius)
    }
}

/** A circular arc around [center] from [from] to [to] the short way round, as cubic segments of at most 90 degrees */
internal fun arcSegments(center: Vector2, from: Vector2, to: Vector2, radius: Double): List<Segment2D> {
    val a0 = kotlin.math.atan2(from.y - center.y, from.x - center.x)
    val a1 = kotlin.math.atan2(to.y - center.y, to.x - center.x)
    var delta = a1 - a0
    while (delta > PI) delta -= 2.0 * PI
    while (delta <= -PI) delta += 2.0 * PI
    val pieces = ceil(abs(delta) / (PI / 2.0)).toInt().coerceAtLeast(1)
    val theta = delta / pieces
    val k = 4.0 / 3.0 * kotlin.math.tan(theta / 4.0)
    return (0 until pieces).map { i ->
        val s = a0 + theta * i
        val e = s + theta
        val p0 = if (i == 0) from else center + Vector2(cos(s), sin(s)) * radius
        val p3 = if (i == pieces - 1) to else center + Vector2(cos(e), sin(e)) * radius
        val c0 = p0 + Vector2(-sin(s), cos(s)) * (k * radius)
        val c1 = p3 - Vector2(-sin(e), cos(e)) * (k * radius)
        Segment2D(p0, c0, c1, p3)
    }
}

