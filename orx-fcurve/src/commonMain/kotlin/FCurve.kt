package org.openrndr.extra.fcurve

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.bounds
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Find the (first) t value for a given [x] value
 */
private fun Segment2D.tForX(x: Double): Double {
    if (x == start.x) return 0.0
    if (x == end.x) return 1.0

    if (linear) {
        return (x - start.x) / (end.x - start.x)
    } else {
        val cb = this.cubic
        val a = cb.start.x - x
        val b = cb.control[0].x - x
        val c = cb.control[1].x - x
        val d = cb.end.x - x

        val t = getCubicRoots(a, b, c, d).firstOrNull() ?: 0.0

        return t
    }
}

/**
 * Find the y value for a given [x] value
 */
private fun Segment2D.yForX(x: Double): Double {
    val t = tForX(x)
    return position(t).y
}

/**
 * Scales the tangents of a segment such that the segment is function shaped along [axis]: the position along
 * [axis] does not decrease from start to end, so there is a single point of the segment for every position along
 * [axis] between start and end.
 *
 * Handles that point backwards along [axis] are made perpendicular to [axis]. When the handles of a cubic segment
 * reach so far along [axis] that the position along [axis] would decrease, both handles are shortened by the same
 * factor, which keeps the directions of the tangents. The handles of a cubic with handle lengths h0 and h1 along
 * [axis] on a segment of length w along [axis] do not need shortening when h0 + h1 - w <= sqrt(h0 * h1), segments
 * that meet this condition are not changed. A quadratic segment is function shaped when its control point lies
 * between start and end along [axis], a quadratic that is not is converted to a cubic and scaled.
 */
fun Segment2D.scaleTangents(axis: Vector2 = Vector2.UNIT_X): Segment2D {
    if (linear) {
        return this
    }
    val a = axis.normalized
    val width = (end - start).dot(a)

    if (control.size == 1) {
        val h = (control[0] - start).dot(a)
        if (h in 0.0..width) {
            return this
        }
    }

    val c = this.cubic
    // handles that point backwards along the axis become perpendicular to it
    val backward0 = (c.control[0] - c.start).dot(a) < 0.0
    val backward1 = (c.control[1] - c.end).dot(a) > 0.0
    val d0 = (c.control[0] - c.start).let { d -> if (backward0) d - a * d.dot(a) else d }
    val d1 = (c.control[1] - c.end).let { d -> if (backward1) d - a * d.dot(a) else d }

    // the derivative along the axis is 3 times the Bernstein quadratic h0 (1-t)^2 + 2 (w - h0 - h1) t (1-t) + h1 t^2,
    // which is non-negative on [0, 1] if and only if h0 + h1 - w <= sqrt(h0 h1)
    val h0 = d0.dot(a)
    val h1 = -d1.dot(a)
    val reach = h0 + h1 - sqrt(h0 * h1)
    val r = if (reach > width) (width / reach).coerceAtLeast(0.0) else 1.0

    if (r == 1.0 && !backward0 && !backward1) {
        return c
    }
    // handles that do not change are kept exactly
    val c0 = if (r == 1.0 && !backward0) c.control[0] else c.start + d0 * r
    val c1 = if (r == 1.0 && !backward1) c.control[1] else c.end + d1 * r
    return copy(control = listOf(c0, c1))
}


/**
 * Represents a functional curve composed of multiple 2D segments. It provides utilities to
 * manipulate and query the curve, such as reversing its direction, changing its speed,
 * sampling values, and visualizing it as contours.
 *
 * @param segments a list of 2D segments that define the curve
 */
@Serializable
data class FCurve(val segments: List<Segment2D>) {

    /**
     * Reverse the fcurve
     */
    fun reverse(): FCurve {
        val d = duration
        val t = buildTransform {
            translate(d, 0.0)
            scale(-1.0, 1.0)
        }
        return FCurve(segments.map { it.reverse.transform(t) })
    }

    val bounds by lazy {
        segments.map { it.bounds }.bounds
    }

    /**
     * Represents the minimum value of the curve.
     *
     * This property evaluates to the minimum vertical position (y-coordinate)
     * of the FCurve based on its `segments` and `bounds`. If the `segments`
     * list is empty, the value defaults to `0.0`. Otherwise, it calculates
     * the minimum as the y-coordinate at the starting position of the bounds.
     */
    val min: Double
        get() {
            if (segments.isEmpty()) return 0.0 else return bounds.position(0.0, 0.0).y
        }

    /**
     * Represents the maximum y-coordinate value of the FCurve at its bounds.
     *
     * If the `segments` list of the FCurve is empty, the returned value is 0.0.
     * Otherwise, it calculates the y-coordinate of the furthest extent of the curve
     * by evaluating the position of the `bounds` at the upper-right corner (1.0, 1.0).
     */
    val max: Double
        get() {
            if (segments.isEmpty()) return 0.0 else return bounds.position(1.0, 1.0).y
        }

    /**
     * Change the duration of the Fcurve
     */
    fun changeSpeed(speed: Double): FCurve {
        val c = if (speed < 0.0) reverse() else this
        return if (speed == 1.0) c else {
            val t = buildTransform {
                scale(1.0 / speed, 1.0)
            }
            FCurve(c.segments.map { it.transform(t) })
        }
    }

    /**
     * Shifts the FCurve along the x-axis by a specified amount.
     *
     * @param x The amount to shift the FCurve along the x-axis.
     * @return A new FCurve instance with all segments shifted by the specified amount.
     */
    fun shift(x: Double): FCurve {
        val t = buildTransform {
            translate(x, 0.0)
        }
        return FCurve(segments.map { it.transform(t) })
    }

    /**
     * Offsets the FCurve by a specified amount along the y-axis.
     *
     * @param y The amount to offset the FCurve along the y-axis.
     * @return A new FCurve instance with all segments transformed by the offset.
     */
    fun offset(y: Double): FCurve {
        val t = buildTransform {
            translate(0.0, y)
        }
        return FCurve(segments.map { it.transform(t) })
    }

    /**
     * Scales the FCurve by the specified factors along the x-axis and y-axis.
     *
     * @param sx The scaling factor along the x-axis.
     * @param sy The scaling factor along the y-axis.
     * @return A new FCurve instance with all segments scaled by the specified factors.
     */
    fun scale(sx: Double, sy: Double): FCurve {
        val t = buildTransform {
            scale(sx, sy)
        }
        return FCurve(segments.map { it.transform(t) })
    }

    /**
     * Creates a function to sample the FCurve at a specific time.
     *
     * @param normalized Specifies whether the sampling is normalized to the range [0, 1].
     *                   If `true`, the time parameter will be scaled to the duration of the curve.
     *                   If `false`, the raw time parameter is used directly.
     * @return A lambda function that takes a `Double` representing the time and returns a `Double`
     *         corresponding to the sampled value from the FCurve at that specific time.
     */
    fun sampler(normalized: Boolean = false): (Double) -> Double {
        var cachedSegment: Segment2D? = null
        if (!normalized) {
            return { t ->
                val r = valueWithSegment(t, cachedSegment)
                cachedSegment = r.second
                r.first
            }
        } else {
            val d = duration
            return { t ->
                val r = valueWithSegment(t * d, cachedSegment)
                cachedSegment = r.second
                r.first
            }
        }
    }

    /**
     * Creates a function to sample the FCurve in a looping manner across its defined duration.
     *
     * @param normalized Specifies whether the input time values should be normalized to the range [0, 1]. If `true`,
     *                   the provided time will be scaled to the duration of the FCurve. If `false`, the input time is taken
     *                   as-is but wrapped within the FCurve's duration.
     * @return A lambda function that takes a `Double` representing the time and returns a `Double` evaluated from the FCurve,
     *         ensuring the sampling respects the looping behavior.
     */
    fun loopSampler(normalized: Boolean = false): (Double) -> Double {
        var cachedSegment: Segment2D? = null
        if (!normalized) {
            val d = duration
            return { t ->
                val r = valueWithSegment(t.mod(d), cachedSegment)
                cachedSegment = r.second
                r.first
            }
        } else {
            val d = duration
            return { t ->
                val r = valueWithSegment(t.mod(1.0) * d, cachedSegment)
                cachedSegment = r.second
                r.first
            }
        }
    }

    /**
     * The duration of the FCurve, calculated as the difference between its start and end points.
     * Returns 0.0 if the FCurve has no segments.
     */
    val duration: Double
        get() {
            return if (segments.isEmpty()) {
                0.0
            } else {
                end - start
            }
        }


    /**
     * Represents the starting x-coordinate of the first segment in the FCurve.
     * If the `segments` list is empty, it defaults to `0.0`.
     */
    val start: Double
        get() {
            return if (segments.isEmpty()) {
                0.0
            } else {
                segments.first().start.x
            }
        }


    /**
     * Represents the x-coordinate of the endpoint of the last segment in the FCurve.
     *
     * If the `segments` list is empty, the value defaults to `0.0`.
     * Otherwise, it returns the x-coordinate of the endpoint (`end.x`) of the last segment.
     */
    val end: Double
        get() {
            return if (segments.isEmpty()) {
                0.0
            } else {
                segments.last().end.x
            }
        }


    /**
     * Evaluate the Fcurve at [t]
     * @param segment an optional segment that can be used to speed up scanning for the relevant segment
     * @see valueWithSegment
     */
    fun value(t: Double, segment: Segment2D? = null): Double = valueWithSegment(t, segment).first

    /**
     * Evaluate the Fcurve at [t]
     * @param cachedSegment an optional segment that can be used to speed up scanning for the relevant segment
     */
    fun valueWithSegment(t: Double, cachedSegment: Segment2D? = null): Pair<Double, Segment2D?> {
        if (cachedSegment != null) {
            if (t >= cachedSegment.start.x && t < cachedSegment.end.x) {
                return Pair(cachedSegment.yForX(t), cachedSegment)
            }
        }

        if (segments.isEmpty()) {
            return Pair(0.0, null)
        }
        if (t <= segments.first().start.x) {
            val segment = segments.first()
            return Pair(segment.start.y, segment)
        } else if (t > segments.last().end.x) {
            val segment = segments.last()
            return Pair(segment.end.y, segment)
        } else {
            val segmentIndex = segments.binarySearch {
                if (t < it.start.x) {
                    1
                } else if (t > it.end.x) {
                    -1
                } else {
                    0
                }
            }
            val segment = segments.getOrNull(segmentIndex)
            return Pair(segment?.yForX(t) ?: 0.0, segment)
        }
    }

    /**
     * Return a list of contours that can be used to visualize the Fcurve
     */
    fun contours(scale: Vector2 = Vector2(1.0, -1.0), offset: Vector2 = Vector2.ZERO): List<ShapeContour> {
        var active = mutableListOf<Segment2D>()
        val result = mutableListOf<ShapeContour>()

        for (segment in segments) {

            val tsegment = segment.transform(
                buildTransform {
                    translate(offset)
                    scale(scale.x, scale.y)
                }
            )

            if (active.isEmpty()) {
                active.add(tsegment)
            } else {
                val dy = abs(active.last().end.y - tsegment.start.y)
                if (dy > 1E-3) {
                    result.add(ShapeContour.fromSegments(active, false))
                    active = mutableListOf()
                }
                active.add(tsegment)
            }
        }
        if (active.isNotEmpty()) {
            result.add(ShapeContour.fromSegments(active, false))
        }
        return result
    }
}

/**
 * Fcurve builder
 */
class FCurveBuilder {
    val segments = mutableListOf<Segment2D>()
    var cursor = Vector2(0.0, 0.0)

    var path = ""

    fun moveTo(y: Double, relative: Boolean = false) {
        cursor = if (!relative) cursor.copy(y = y) else cursor.copy(y = cursor.y + y)
        path += "${if (relative) "m" else "M"}$y"
    }

    fun lineTo(x: Double, y: Double, relative: Boolean = false) {
        val r = if (relative) 1.0 else 0.0
        segments.add(Segment2D(cursor, Vector2(x + cursor.x, y + cursor.y * r)))
        cursor = Vector2(cursor.x + x, cursor.y * r + y)
        path += "${if (relative) "l" else "L"}$x,$y"
    }

    fun curveTo(
        x0: Double, y0: Double,
        x: Double, y: Double,
        relative: Boolean = false
    ) {
        val r = if (relative) 1.0 else 0.0
        segments.add(
            Segment2D(
                cursor,
                Vector2(cursor.x + x0, cursor.y * r + y0),
                Vector2(cursor.x + x, cursor.y * r + y)
            ).scaleTangents()
        )
        cursor = Vector2(cursor.x + x, cursor.y * r + y)
        path += "${if (relative) "q" else "Q"}$x0,$y0,$x,$y"
    }

    fun curveTo(
        x0: Double, y0: Double,
        x1: Double, y1: Double,
        x: Double, y: Double, relative: Boolean = false
    ) {
        val r = if (relative) 1.0 else 0.0
        segments.add(
            Segment2D(
                cursor,
                Vector2(cursor.x + x0, cursor.y * r + y0),
                Vector2(cursor.x + x1, cursor.y * r + y1),
                Vector2(cursor.x + x, cursor.y * r + y)
            ).scaleTangents()
        )
        cursor = Vector2(cursor.x + x, cursor.y * r + y)
        path += "${if (relative) "c" else "C"}$x0,$y0,$x,$y"
    }

    fun continueTo(x: Double, y: Double, relative: Boolean = false) {
        val r = if (relative) 1.0 else 0.0

        if (segments.isNotEmpty()) {
            val lastSegment = segments.last()
            val outTangent = if (segments.last().linear) lastSegment.end else segments.last().control.last()
            val outPos = lastSegment.end
            val d = outPos - outTangent
            val ts = 1.0// x / lastDuration
            segments.add(
                Segment2D(
                    cursor,
                    cursor + d * ts,
                    Vector2(cursor.x + x, cursor.y * r + y)
                ).scaleTangents()
            )
        } else {
            segments.add(
                Segment2D(cursor,
                    Vector2(cursor.x + x, cursor.y * r + y)).quadratic
            )
        }
        cursor = Vector2(cursor.x + x, cursor.y * r + y)
        path += "${if (relative) "t" else "T"}$x,$y"
    }

    fun continueTo(x1: Double, y1: Double, x: Double, y: Double, relative: Boolean = false) {
        val r = if (relative) 1.0 else 0.0
        val lastSegment = segments.last()
        val outTangent = if (lastSegment.linear) lastSegment.position(0.5) else segments.last().control.last()
        val dx = cursor.x - outTangent.x
        val dy = cursor.y - outTangent.y
        segments.add(
            Segment2D(
                cursor,
                Vector2(cursor.x + dx, cursor.y + dy),
                Vector2(cursor.x + x1, cursor.y * r + y1),
                Vector2(cursor.x + x, cursor.y * r + y)
            ).scaleTangents()
        )
        cursor = Vector2(cursor.x + x, cursor.y * r + y)
        path += "${if (relative) "s" else "S"}$x1,$y1,$x,$y"
    }

    fun hold(x: Double, relative: Boolean = true) {
        if (relative) {
            lineTo(x, cursor.y)
        } else {
            require(segments.isEmpty()) { "absolute hold (H $x) is only allowed when used as first command" }
            cursor = cursor.copy(x = x)
        }
        path += "h$x"
    }

    /**
     * build the Fcurve
     */
    fun build(): FCurve {
        return FCurve(segments)
    }
}

/**
 * build an Fcurve
 * @see FCurveBuilder
 */
fun fcurve(builder: FCurveBuilder.() -> Unit): FCurve {
    val fb = FCurveBuilder()
    fb.builder()
    return fb.build()
}


/**
 * Splits an input string containing fcurve path commands and numbers into individual components,
 * preserving the order of commands and associated numbers.
 * The splitting considers the relations between commands and numbers, ensuring proper separation.
 *
 * @param d The input string representing fcurve path commands and numbers.
 * @return A list of strings where each element is either an fcurve path command or a related numerical value.
 */
fun fCurveCommands(d: String): List<String> {
    val fcurveCommands = "mMlLqQsStTcChH"
    val number = "0-9.\\-E%"

    return d.split(Regex("(?:[\t ,]|\r?\n)+|(?<=[$fcurveCommands])(?=[$number])|(?<=[$number])(?=[$fcurveCommands])"))
        .filter { it.isNotBlank() }
}

private fun evaluateFCurveCommands(parts: List<String>): FCurve {
    val mparts = parts.reversed().toMutableList()

    fun popToken(): String = mparts.removeLast()

    fun popNumber(): Double = mparts.removeLast().toDoubleOrNull() ?: error("not a number")

    fun String.numberOrFactorOf(percentageOf: (Double) -> Double): Double {
        return if (endsWith("%")) {
            val f = (dropLast(1).toDoubleOrNull() ?: error("'$this' is not a percentage")) / 100.0
            percentageOf(f)
        } else {
            toDoubleOrNull() ?: error("'$this' is not a number")
        }
    }

    fun String.numberOrPercentageOf(percentageOf: () -> Double): Double {
        return numberOrFactorOf { f -> f * percentageOf() }
    }

    fun popNumberOrPercentageOf(percentageOf: () -> Double): Double {
        return mparts.removeLast().numberOrPercentageOf(percentageOf)
    }

    /**
     * Use the [fcurve] builder to construct the FCurve
     */
    return fcurve {
        fun dx(): Double {
            val lastSegment = segments.lastOrNull() ?: Segment2D(Vector2.ZERO, Vector2.ZERO)
            return lastSegment.end.x - lastSegment.start.x
        }

        while (mparts.isNotEmpty()) {
            val command = mparts.removeLast()
            when (command) {

                /**
                 * Handle move cursor command
                 */
                "m", "M" -> {
                    val isRelative = command.first().isLowerCase()
                    moveTo(popNumberOrPercentageOf { cursor.y }, isRelative)
                }

                /**
                 * Handle line command
                 */
                "l", "L" -> {
                    val isRelative = command.first().isLowerCase()
                    val x = popNumber()
                    val y = popNumber()
                    lineTo(x, y, isRelative)
                }

                /**
                 * Handle cubic bezier command
                 */
                "c", "C" -> {
                    val relative = command.first().isLowerCase()
                    val tcx0 = popToken()
                    val tcy0 = popToken()
                    val tcx1 = popToken()
                    val tcy1 = popToken()
                    val x = popNumber()
                    val y = popNumber()
                    val x0 = tcx0.numberOrPercentageOf { x }
                    val y0 = tcy0.numberOrFactorOf { factor ->
                        if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                    }
                    val x1 = tcx1.numberOrPercentageOf { x }
                    val y1 = tcy1.numberOrFactorOf { factor ->
                        if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                    }
                    curveTo(x0, y0, x1, y1, x, y, relative)
                }

                /**
                 * Handle quadratic bezier command
                 */
                "q", "Q" -> {
                    val relative = command.first().isLowerCase()
                    val tcx0 = popToken()
                    val tcy0 = popToken()
                    val x = popNumberOrPercentageOf { dx() }
                    val y = popNumberOrPercentageOf { cursor.y }
                    val x0 = tcx0.numberOrPercentageOf { x }
                    val y0 = tcy0.numberOrFactorOf { factor ->
                        if (relative) y * factor else cursor.y * (1.0 - factor).coerceAtLeast(0.0) + y * factor
                    }
                    curveTo(x0, y0, x, y, relative)
                }

                /**
                 * Handle horizontal line (or hold) command
                 */
                "h", "H" -> {
                    val isRelative = command.first().isLowerCase()
                    hold(popNumberOrPercentageOf { dx() }, isRelative)
                }

                /**
                 * Handle cubic smooth to command
                 */
                "s", "S" -> {
                    val relative = command.first().isLowerCase()
                    val tcx0 = popToken()
                    val tcy0 = popToken()
                    val x = popNumber()
                    val y = popNumber()
                    val x1 = tcx0.numberOrPercentageOf { x }
                    val y1 = tcy0.numberOrPercentageOf { y }
                    continueTo(x1, y1, x, y, relative)
                }

                /**
                 * Handle quadratic smooth to command
                 */
                "t", "T" -> {
                    val isRelative = command.first().isLowerCase()
                    val x = popNumber()
                    val y = popNumber()
                    continueTo(x, y, isRelative)
                }

                else -> error("unknown command: $command in ${parts}")
            }
        }
    }
}

/**
 * Parses the provided string to create an FCurve. This function attempts to either interpret the
 * input as a constant value or evaluate it as a series of functional curve commands.
 *
 * @param d A string representing either a constant value or functional curve commands.
 *          If the string can be converted to a double, it is treated as a constant value for the FCurve.
 *          Otherwise, it is parsed as functional curve commands.
 * @return An FCurve constructed based on the input string.
 */
fun fcurve(d: String): FCurve {
    val constantExpression = d.toDoubleOrNull()
    if (constantExpression != null) {
        return FCurve(listOf(Segment2D(Vector2(0.0, constantExpression), Vector2(0.0, constantExpression))))
    }
    return evaluateFCurveCommands(fCurveCommands(d))
}
