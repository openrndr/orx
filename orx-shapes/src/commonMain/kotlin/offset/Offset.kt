package offset

import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.math.times
import org.openrndr.shape.*
import kotlin.math.abs
import kotlin.math.sign
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
 * Offsets a [ShapeContour]'s [Segment]s by given [distance].
 *
 * [Segment]s are moved outwards if [distance] is > 0 or inwards if [distance] is < 0.
 *
 * @param joinType Specifies how to join together the moved [Segment]s.
 */
fun ShapeContour.offset(distance: Double, joinType: SegmentJoin = SegmentJoin.ROUND): ShapeContour {
    val offsets =
        segments.map { it.offset(distance, yPolarity = polarity) }
            .filter { it.isNotEmpty() }
    val tempContours = offsets.map {
        ShapeContour.fromSegments(it, closed = false, distanceTolerance = 0.01)
    }
    val offsetContours = tempContours.map { it }.filter { it.length > 0.0 }.toMutableList()

    for (i in 0 until offsetContours.size) {
        offsetContours[i] = offsetContours[i].removeLoops()
    }

    for (i0 in 0 until if (this.closed) offsetContours.size else offsetContours.size - 1) {
        val i1 = (i0 + 1) % (offsetContours.size)
        val its = intersections(offsetContours[i0], offsetContours[i1])
        if (its.size == 1) {
            offsetContours[i0] = offsetContours[i0].sub(0.0, its[0].a.contourT)
            offsetContours[i1] = offsetContours[i1].sub(its[0].b.contourT, 1.0)
        }
    }

    if (offsets.isEmpty()) {
        return ShapeContour(emptyList(), false)
    }


    val startPoint = if (closed) offsets.last().last().end else offsets.first().first().start

    val candidateContour = contour {
        moveTo(startPoint)
        for (offsetContour in offsetContours) {
            val delta = (offsetContour.position(0.0) - cursor)
            val joinDistance = delta.length
            if (joinDistance > 10e-6) {
                when (joinType) {
                    SegmentJoin.BEVEL -> lineTo(offsetContour.position(0.0))
                    SegmentJoin.ROUND -> arcTo(
                        crx = joinDistance * 0.5 * sqrt(2.0),
                        cry = joinDistance * 0.5 * sqrt(2.0),
                        angle = 90.0,
                        largeArcFlag = false,
                        sweepFlag = true,
                        end = offsetContour.position(0.0)
                    )
                    SegmentJoin.MITER -> {
                        val ls = lastSegment ?: offsetContours.last().segments.last()
                        val fs = offsetContour.segments.first()
                        val i = intersection(
                            ls.end,
                            ls.end + ls.direction(1.0),
                            fs.start,
                            fs.start - fs.direction(0.0),
                            eps = 10E8
                        )
                        if (i !== Vector2.INFINITY) {
                            lineTo(i)
                            lineTo(fs.start)
                        } else {
                            lineTo(fs.start)
                        }
                    }
                }
            }
            for (offsetSegment in offsetContour.segments) {
                segment(offsetSegment)
            }

        }
        if (this@offset.closed) {
            close()
        }
    }

    val postProc = false

    var final = candidateContour.removeLoops()

    if (postProc && !final.empty) {
        val head = Segment2D(
            segments[0].start + segments[0].normal(0.0)
                .perpendicular(polarity) * 1000.0, segments[0].start
        ).offset(distance).firstOrNull()?.copy(end = final.segments[0].start)?.contour

        val tail = Segment2D(
            segments.last().end,
            segments.last().end - segments.last().normal(1.0)
                .perpendicular(polarity) * 1000.0
        ).offset(distance).firstOrNull()?.copy(start = final.segments.last().end)?.contour

        if (head != null) {
            val headInts = intersections(final, head)
            if (headInts.size == 1) {
                final = final.sub(headInts[0].a.contourT, 1.0)
            }
            if (headInts.size > 1) {
                val sInts = headInts.sortedByDescending { it.a.contourT }
                final = final.sub(sInts[0].a.contourT, 1.0)
            }
        }
//            final = head + final
//
        if (tail != null) {
            val tailInts = intersections(final, tail)
            if (tailInts.size == 1) {
                final = final.sub(0.0, tailInts[0].a.contourT)
            }
            if (tailInts.size > 1) {
                val sInts = tailInts.sortedBy { it.a.contourT }
                final = final.sub(0.0, sInts[0].a.contourT)
            }
        }

//            final = final + tail

    }

    return final
}
