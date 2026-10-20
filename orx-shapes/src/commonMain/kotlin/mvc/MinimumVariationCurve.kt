package org.openrndr.extra.shapes.mvc

import org.openrndr.extra.shapes.biarc.catmullRomTangents
import org.openrndr.extra.shapes.ph.cubicApproximation
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts this polyline into a minimum-variation curve (MVC) through all points: the G2 curve that minimizes the
 * variation of its curvature, ∫(dκ/ds)² ds, after Moreton and Séquin, "Functional optimization for fair surface
 * design" (SIGGRAPH 1992). The variation shrinks as a curve grows, so where the curve loops it has no minimum: the loop
 * would inflate without end. The scale-invariant form, the variation times the cube of the curve length, is minimized
 * instead; it has the same minimizers up to how the length is weighed in, and a circle still scores zero. Where clothoid splines let the curvature change linearly along every segment, the MVC lets
 * it change as little as possible over the whole curve, so it is fairer still. Points on a circle give that circle, the
 * curve with zero variation, and collinear points a straight line.
 *
 * Every edge is a quintic Bézier given by Hermite data at its ends. Neighbouring edges share the tangent direction and
 * the curvature at their common point, which makes the curve G2 by construction. The tangent directions and
 * curvatures are optimized together with Levenberg-Marquardt on the quadrature residuals of the functional, starting
 * from Catmull-Rom tangents and the curvature of the circle through every point and its neighbours. The functional is
 * evaluated by Gauss-Legendre quadrature. At the ends of an open polyline the tangent and curvature are free variables
 * like everywhere else, so the optimization chooses them too.
 *
 * The optimization is local and the functional is not convex, so for erratic points the result is the nearest local
 * minimum, which is still G2 and passes through all points.
 *
 * @param closed whether the polyline is closed, the last point should then not repeat the first
 * @param tolerance the largest distance between the cubic approximation and the quintic segments
 * @param polarity the polarity of the resulting contour
 */
fun List<Vector2>.minimumVariationCurve(
    closed: Boolean,
    tolerance: Double = 0.01,
    polarity: YPolarity = YPolarity.CW_NEGATIVE_Y
): ShapeContour {
    val segments = minimumVariationSegments(closed)
    if (segments.isEmpty()) {
        return ShapeContour.fromPoints(distinctPoints(closed), closed, polarity)
    }
    val cubics = segments.flatMap { segment -> cubicApproximation(segment::position, segment::derivative, tolerance) }
    return ShapeContour.fromSegments(cubics, closed, polarity)
}

private fun List<Vector2>.distinctPoints(closed: Boolean) = filterIndexed { i, p ->
    val next = if (i + 1 < size) this[i + 1] else if (closed) this[0] else null
    next == null || p.squaredDistanceTo(next) > 1E-18
}

/** A quintic Bézier segment with six [controlPoints]. */
internal class QuinticSegment(val controlPoints: List<Vector2>) {
    companion object {
        /**
         * The quintic from [p0] to [p1] with first derivatives [d0] and [d1] and second derivatives [a0] and [a1] at its
         * ends.
         */
        fun hermite(p0: Vector2, d0: Vector2, a0: Vector2, p1: Vector2, d1: Vector2, a1: Vector2) = QuinticSegment(
            listOf(
                p0, p0 + d0 / 5.0, p0 + d0 * (2.0 / 5.0) + a0 / 20.0,
                p1 - d1 * (2.0 / 5.0) + a1 / 20.0, p1 - d1 / 5.0, p1
            )
        )
    }

    fun map(f: (Vector2) -> Vector2) = QuinticSegment(controlPoints.map(f))

    /** The absolute rotation of the tangent, sampled. */
    fun absoluteRotation(samples: Int = 32): Double {
        var total = 0.0
        var previous = derivative(0.0)
        for (k in 1..samples) {
            val d = derivative(k / samples.toDouble())
            total += abs(atan2(cross(previous, d), previous.dot(d)))
            previous = d
        }
        return total
    }
    private val first = differences(controlPoints, 5.0)
    private val second = differences(first, 4.0)

    fun position(t: Double) = casteljau(controlPoints, t)
    fun derivative(t: Double) = casteljau(first, t)
    fun secondDerivative(t: Double) = casteljau(second, t)

    fun curvature(t: Double): Double {
        val r1 = derivative(t)
        val r2 = secondDerivative(t)
        val s = r1.length
        return cross(r1, r2) / (s * s * s)
    }

    private val xs = DoubleArray(6) { controlPoints[it].x }
    private val ys = DoubleArray(6) { controlPoints[it].y }

    /** The arc length ∫|r'(t)| dt by Gauss-Legendre quadrature. */
    fun length(): Double = quinticResiduals(xs, ys, Fairness.ENERGY, DoubleArray(RESIDUALS))

    /** ∫(dκ/ds)² ds = ∫ κ'(t)² / |r'(t)| dt by Gauss-Legendre quadrature, the sum of the squared [residuals]. */
    fun variation(): Double = variationResiduals().sumOf { it * it }

    /** The bending energy ∫κ² ds = ∫ κ(t)²·|r'(t)| dt by Gauss-Legendre quadrature. */
    fun energy(): Double = energyResiduals().sumOf { it * it }

    /** The quadrature residuals of [fairness], whose squares sum to its functional. */
    fun residuals(fairness: Fairness) = DoubleArray(RESIDUALS).also { quinticResiduals(xs, ys, fairness, it) }

    /**
     * √(w·|r'(t)|)·κ(t) at the [RESIDUALS] quadrature nodes t with weights w, whose squares sum to the [energy].
     */
    fun energyResiduals(): DoubleArray = residuals(Fairness.ENERGY)

    /**
     * √(w / |r'(t)|)·κ'(t) at the [RESIDUALS] quadrature nodes t with weights w, whose squares sum to the [variation].
     */
    fun variationResiduals(): DoubleArray = residuals(Fairness.VARIATION)
}

/**
 * A fairness functional and the power of the curve length it is multiplied by to make it scale-invariant: the
 * functional shrinks as a curve grows, by that power, so without it loops inflate without end.
 */
internal enum class Fairness(val lengthExponent: Int) {
    /** The variation of curvature ∫(dκ/ds)² ds of a minimum-variation curve. */
    VARIATION(3),

    /** The bending energy ∫κ² ds of an elastica, a minimum-energy curve. */
    ENERGY(1)
}

private const val QUADRATURE_PIECES = 2
private const val RESIDUALS = QUADRATURE_PIECES * 8

/** Nodes and weights of 8-point Gauss-Legendre quadrature on [-1, 1]. */
private val glNodes = doubleArrayOf(
    -0.9602898564975363, -0.7966664774136267, -0.5255324099163290, -0.1834346424956498,
    0.1834346424956498, 0.5255324099163290, 0.7966664774136267, 0.9602898564975363
)
private val glWeights = doubleArrayOf(
    0.1012285362903763, 0.2223810344533745, 0.3137066458778873, 0.3626837833783620,
    0.3626837833783620, 0.3137066458778873, 0.2223810344533745, 0.1012285362903763
)

/** The [RESIDUALS] quadrature nodes on [0, 1] and their weights. */
private val quadratureNodes = DoubleArray(RESIDUALS) {
    (it / glNodes.size + (glNodes[it % glNodes.size] + 1.0) / 2.0) / QUADRATURE_PIECES
}
private val quadratureWeights = DoubleArray(RESIDUALS) { glWeights[it % glNodes.size] / (2.0 * QUADRATURE_PIECES) }

/**
 * The weights of the six control points of a quintic in its [order]-th derivative at every quadrature node, six per
 * node, so that evaluating a derivative there is a dot product.
 */
private fun derivativeWeights(order: Int) = DoubleArray(RESIDUALS * 6).also { table ->
    for (i in 0 until 6) {
        var coefficients = DoubleArray(6) { if (it == i) 1.0 else 0.0 }
        for (r in 0 until order) {
            val c = coefficients
            coefficients = DoubleArray(c.size - 1) { (c[it + 1] - c[it]) * (5 - r) }
        }
        for (node in 0 until RESIDUALS) {
            val p = coefficients.copyOf()
            val t = quadratureNodes[node]
            for (r in 1 until p.size) for (k in 0 until p.size - r) p[k] = p[k] * (1.0 - t) + p[k + 1] * t
            table[node * 6 + i] = p[0]
        }
    }
}

private val firstWeights = derivativeWeights(1)
private val secondWeights = derivativeWeights(2)
private val thirdWeights = derivativeWeights(3)

/**
 * Writes the quadrature residuals of [fairness] for the quintic with control point coordinates [xs] and [ys] to
 * [residuals] and returns its arc length, without allocating: the optimization evaluates this for every finite
 * difference.
 */
private fun quinticResiduals(xs: DoubleArray, ys: DoubleArray, fairness: Fairness, residuals: DoubleArray): Double {
    var length = 0.0
    for (node in 0 until RESIDUALS) {
        val o = node * 6
        var x1 = 0.0; var y1 = 0.0; var x2 = 0.0; var y2 = 0.0
        for (i in 0 until 6) {
            x1 += firstWeights[o + i] * xs[i]; y1 += firstWeights[o + i] * ys[i]
            x2 += secondWeights[o + i] * xs[i]; y2 += secondWeights[o + i] * ys[i]
        }
        val w = quadratureWeights[node]
        val s2 = x1 * x1 + y1 * y1
        val s = sqrt(s2)
        val s3 = s2 * s
        val cross12 = x1 * y2 - y1 * x2
        length += w * s
        residuals[node] = when (fairness) {
            Fairness.ENERGY -> sqrt(w * s) * (cross12 / s3)
            Fairness.VARIATION -> {
                var x3 = 0.0; var y3 = 0.0
                for (i in 0 until 6) {
                    x3 += thirdWeights[o + i] * xs[i]; y3 += thirdWeights[o + i] * ys[i]
                }
                val dk = (x1 * y3 - y1 * x3) / s3 - 3.0 * cross12 * (x1 * x2 + y1 * y2) / (s3 * s2)
                sqrt(w / s) * dk
            }
        }
    }
    return length
}

private fun cross(a: Vector2, b: Vector2) = a.x * b.y - a.y * b.x

private fun differences(points: List<Vector2>, factor: Double) = List(points.size - 1) {
    (points[it + 1] - points[it]) * factor
}

private fun casteljau(points: List<Vector2>, t: Double): Vector2 {
    val p = points.toMutableList()
    for (r in 1 until p.size) for (i in 0 until p.size - r) p[i] = p[i] * (1.0 - t) + p[i + 1] * t
    return p[0]
}

/**
 * The quintic segments of [minimumVariationCurve], between the points that don't repeat the next point and the knots
 * inserted between them; empty when there are too few points.
 *
 * The optimization runs on the points translated and scaled to a mean edge length of 1, in two stages. The first has
 * one quintic per edge. A quintic can't keep its curvature constant over a large turn, so the second splits every edge
 * that turns by more than [MAX_TURN] in the first stage into several quintics, joined at free knots whose tangents and
 * curvatures are optimized too, starting from the first-stage curve. A free knot moves only along the normal of the
 * first-stage curve: sliding along the curve barely changes it, and leaving that direction free makes the optimization
 * crawl.
 */
internal fun List<Vector2>.minimumVariationSegments(closed: Boolean): List<QuinticSegment> =
    fairSegments(closed, Fairness.VARIATION)

/**
 * The quintic segments that minimize [fairness] times the power of the length given by it, see
 * [minimumVariationSegments], which describes the optimization for either functional.
 */
internal fun List<Vector2>.fairSegments(closed: Boolean, fairness: Fairness): List<QuinticSegment> {
    val original = distinctPoints(closed)
    if (original.size < 2 || (closed && original.size < 3)) return emptyList()
    val n = original.size
    val edges = if (closed) n else n - 1
    val center = original.reduce { a, b -> a + b } / n.toDouble()
    val scale = (0 until edges).sumOf { original[it].distanceTo(original[(it + 1) % n]) } / edges
    val points = original.map { (it - center) / scale }

    val (angles, curvatures) = initialGuess(points, closed)
    val first = FairCurveSolver(points, points, BooleanArray(n) { true }, closed, fairness, angles, curvatures)
    val firstSegments = first.solve()

    val knots = mutableListOf<Vector2>()
    val normals = mutableListOf<Vector2>()
    val fixed = mutableListOf<Boolean>()
    val knotAngles = mutableListOf<Double>()
    val knotCurvatures = mutableListOf<Double>()
    fun add(position: Vector2, isFixed: Boolean, angle: Double, curvature: Double) {
        knots.add(position); fixed.add(isFixed); knotAngles.add(angle); knotCurvatures.add(curvature)
        normals.add(Vector2(-sin(angle), cos(angle)))
    }
    for (e in 0 until edges) {
        val segment = firstSegments[e]
        val start = segment.derivative(0.0)
        add(points[e], true, atan2(start.y, start.x), segment.curvature(0.0))
        val pieces = ceil(segment.absoluteRotation() / MAX_TURN).toInt().coerceIn(1, MAX_PIECES)
        for (k in 1 until pieces) {
            val t = k / pieces.toDouble()
            val d = segment.derivative(t)
            add(segment.position(t), false, atan2(d.y, d.x), segment.curvature(t))
        }
    }
    if (!closed) {
        val last = firstSegments.last()
        val end = last.derivative(1.0)
        add(points.last(), true, atan2(end.y, end.x), last.curvature(1.0))
    }
    val segments = if (knots.size == n) firstSegments else {
        FairCurveSolver(
            knots, normals, fixed.toBooleanArray(), closed, fairness,
            knotAngles.toDoubleArray(), knotCurvatures.toDoubleArray()
        ).solve()
    }
    return segments.map { segment -> segment.map { it * scale + center } }
}

/** The largest turn of a single quintic in the second stage, see [minimumVariationSegments]. */
private const val MAX_TURN = PI / 4
private const val MAX_PIECES = 8

/** Catmull-Rom tangent angles and the curvatures of the circles through every point and its neighbours. */
private fun initialGuess(points: List<Vector2>, closed: Boolean): Pair<DoubleArray, DoubleArray> {
    val n = points.size
    val tangents = catmullRomTangents(points, closed, 0.5)
    val angles = DoubleArray(n) { atan2(tangents[it].y, tangents[it].x) }
    val curvatures = DoubleArray(n) { i ->
        val (a, b, c) = when {
            closed -> Triple(points[(i - 1 + n) % n], points[i], points[(i + 1) % n])
            n < 3 -> Triple(points[0], points[0], points[1])
            i == 0 -> Triple(points[0], points[1], points[2])
            i == n - 1 -> Triple(points[n - 3], points[n - 2], points[n - 1])
            else -> Triple(points[i - 1], points[i], points[i + 1])
        }
        val denominator = (b - a).length * (c - b).length * (c - a).length
        if (denominator > 1E-12) 2.0 * cross(b - a, c - b) / denominator else 0.0
    }
    return Pair(angles, curvatures)
}

/**
 * One optimization of [fairSegments] over [knots], of which the [fixed] ones keep their position. The
 * variables are the tangent angle and curvature at every knot and, for every free knot, u in its displacement u·D
 * along its entry in [normals], with D the mean distance to its neighbouring knots, which keeps u well scaled.
 *
 * The speed and tangential acceleration at the ends of a quintic only reparametrize it. Left free, the optimization
 * drives speeds towards zero, where the curvature can change between the quadrature nodes unseen. They are therefore
 * those of a circular arc at uniform speed: the speed is the length of the arc between the end points that turns by the
 * angle between the end tangents, chord·(φ/2)/sin(φ/2), and the tangential acceleration is zero. Quintics that are
 * circular arcs come out nearly exact that way.
 */
private class FairCurveSolver(
    val knots: List<Vector2>,
    val normals: List<Vector2>,
    val fixed: BooleanArray,
    val closed: Boolean,
    val fairness: Fairness,
    initialAngles: DoubleArray,
    initialCurvatures: DoubleArray
) {
    val n = knots.size
    val edges = if (closed) n else n - 1

    // variable layout: angles, curvatures, then the displacements of the free knots
    fun angle(k: Int) = k
    fun curvature(k: Int) = n + k
    private val freeIndex = IntArray(n).also {
        var next = 0
        for (k in 0 until n) it[k] = if (fixed[k]) -1 else next++
    }
    val size = 2 * n + freeIndex.count { it >= 0 }
    fun displacement(k: Int) = 2 * n + freeIndex[k]

    private val x = DoubleArray(size).also {
        initialAngles.copyInto(it, 0)
        initialCurvatures.copyInto(it, n)
    }

    /** The scale of the displacement of every free knot, the mean distance to its neighbours. */
    private val reach = DoubleArray(n) { k ->
        if (fixed[k]) 0.0 else (knots[k].distanceTo(knots[(k - 1 + n) % n]) + knots[k].distanceTo(knots[(k + 1) % n])) / 2.0
    }

    fun position(x: DoubleArray, k: Int) =
        if (fixed[k]) knots[k] else knots[k] + normals[k] * (reach[k] * x[displacement(k)])

    fun segment(x: DoubleArray, e: Int): QuinticSegment {
        val xs = DoubleArray(6)
        val ys = DoubleArray(6)
        controlPoints(x, e, xs, ys)
        return QuinticSegment(List(6) { Vector2(xs[it], ys[it]) })
    }

    /** Writes the control point coordinates of the quintic of edge [e] to [xs] and [ys], see [FairCurveSolver]. */
    fun controlPoints(x: DoubleArray, e: Int, xs: DoubleArray, ys: DoubleArray) {
        val i = e
        val j = (e + 1) % n
        val p0x = positionX(x, i)
        val p0y = positionY(x, i)
        val p1x = positionX(x, j)
        val p1y = positionY(x, j)
        val t0x = cos(x[angle(i)])
        val t0y = sin(x[angle(i)])
        val t1x = cos(x[angle(j)])
        val t1y = sin(x[angle(j)])
        val turn = atan2(t0x * t1y - t0y * t1x, t0x * t1x + t0y * t1y) / 2.0
        val chord = sqrt((p1x - p0x) * (p1x - p0x) + (p1y - p0y) * (p1y - p0y))
        val speed = if (abs(turn) < 1E-8) chord else chord * turn / sin(turn)
        val k0 = speed * speed * x[curvature(i)]
        val k1 = speed * speed * x[curvature(j)]
        // the Hermite control points of QuinticSegment.hermite, with d = t·speed and a = normal·speed²·κ
        xs[0] = p0x
        ys[0] = p0y
        xs[1] = p0x + t0x * speed / 5.0
        ys[1] = p0y + t0y * speed / 5.0
        xs[2] = p0x + t0x * speed * (2.0 / 5.0) - t0y * k0 / 20.0
        ys[2] = p0y + t0y * speed * (2.0 / 5.0) + t0x * k0 / 20.0
        xs[3] = p1x - t1x * speed * (2.0 / 5.0) - t1y * k1 / 20.0
        ys[3] = p1y - t1y * speed * (2.0 / 5.0) + t1x * k1 / 20.0
        xs[4] = p1x - t1x * speed / 5.0
        ys[4] = p1y - t1y * speed / 5.0
        xs[5] = p1x
        ys[5] = p1y
    }

    private fun positionX(x: DoubleArray, k: Int) =
        if (fixed[k]) knots[k].x else knots[k].x + normals[k].x * (reach[k] * x[displacement(k)])

    private fun positionY(x: DoubleArray, k: Int) =
        if (fixed[k]) knots[k].y else knots[k].y + normals[k].y * (reach[k] * x[displacement(k)])

    // scratch space of the evaluations in objective and levenbergMarquardt, which run once per finite difference
    private val xs = DoubleArray(6)
    private val ys = DoubleArray(6)
    private val residuals = DoubleArray(RESIDUALS)
    private val plus = DoubleArray(RESIDUALS)
    private val minus = DoubleArray(RESIDUALS)
    private val columns = Array(6) { DoubleArray(RESIDUALS) }

    /** The residuals of [fairness] of the quintic of edge [e] written to [out], and its length. */
    private fun evaluate(x: DoubleArray, e: Int, out: DoubleArray): Double {
        controlPoints(x, e, xs, ys)
        return quinticResiduals(xs, ys, fairness, out)
    }

    fun solve(): List<QuinticSegment> {
        levenbergMarquardt(x)
        return List(edges) { segment(x, it) }
    }

    /**
     * The scale-invariant objective, the [fairness] functional times the total length to its power; infinite where
     * degenerate.
     */
    fun objective(x: DoubleArray): Double {
        var functional = 0.0
        var length = 0.0
        for (e in 0 until edges) {
            length += evaluate(x, e, residuals)
            for (r in residuals) functional += r * r
        }
        val total = functional * length.pow(fairness.lengthExponent)
        return if (total.isFinite()) total else Double.POSITIVE_INFINITY
    }

    /**
     * Minimizes the [objective] over [x] in place with Levenberg-Marquardt. With r the quadrature residuals of the
     * functional E = rᵀr, L the total length and p the length exponent, the residuals of the objective are L^(p/2)·r.
     * Every r and every edge length depends only on the variables of its own edge, so their derivatives J and ∇L are
     * found by central differences one edge at a time, and the Gauss-Newton matrix of the objective is
     * Lᵖ·JᵀJ + p/2·Lᵖ⁻¹·(Jᵀr·∇Lᵀ + ∇L·rᵀJ) + p²/4·Lᵖ⁻²·E·∇L·∇Lᵀ, with gradient Lᵖ·Jᵀr + p/2·Lᵖ⁻¹·E·∇L. Steps solve
     * (A + λ·diag(A))·δ = -g for that matrix A and gradient g; λ shrinks after a step that lowers the objective and grows
     * otherwise.
     */
    fun levenbergMarquardt(x: DoubleArray) {
        var f = objective(x)
        if (!f.isFinite()) return
        var lambda = 1E-3
        for (iteration in 0 until MAX_ITERATIONS) {
            val jtj = Array(size) { DoubleArray(size) }
            val jtr = DoubleArray(size)
            val lengthGradient = DoubleArray(size)
            var functional = 0.0
            var length = 0.0
            val h = 1E-7
            for (e in 0 until edges) {
                length += evaluate(x, e, residuals)
                for (r in residuals) functional += r * r
                val variables = edgeVariables[e]
                for ((a, v) in variables.withIndex()) {
                    val keep = x[v]
                    x[v] = keep + h
                    val plusLength = evaluate(x, e, plus)
                    x[v] = keep - h
                    val minusLength = evaluate(x, e, minus)
                    x[v] = keep
                    lengthGradient[v] += (plusLength - minusLength) / (2 * h)
                    val column = columns[a]
                    for (r in 0 until RESIDUALS) column[r] = (plus[r] - minus[r]) / (2 * h)
                }
                for ((a, va) in variables.withIndex()) {
                    val ca = columns[a]
                    var sum = 0.0
                    for (r in 0 until RESIDUALS) sum += ca[r] * residuals[r]
                    jtr[va] += sum
                    for ((b, vb) in variables.withIndex()) {
                        val cb = columns[b]
                        var product = 0.0
                        for (r in 0 until RESIDUALS) product += ca[r] * cb[r]
                        jtj[va][vb] += product
                    }
                }
            }
            val p = fairness.lengthExponent
            val lp = length.pow(p)
            val lp1 = length.pow(p - 1)
            val lp2 = length.pow(p - 2)
            val matrix = Array(size) { r ->
                DoubleArray(size) { c ->
                    lp * jtj[r][c] + p / 2.0 * lp1 * (jtr[r] * lengthGradient[c] + lengthGradient[r] * jtr[c]) +
                            p * p / 4.0 * lp2 * functional * lengthGradient[r] * lengthGradient[c]
                }
            }
            val gradient = DoubleArray(size) { lp * jtr[it] + p / 2.0 * lp1 * functional * lengthGradient[it] }
            if (gradient.maxOf { abs(it) } < GRADIENT_TOLERANCE) break

            var improved = false
            while (lambda < 1E12) {
                val m = Array(size) { r -> matrix[r].copyOf() }
                for (v in 0 until size) m[v][v] += lambda * max(matrix[v][v], 1E-9)
                val step = solveSymmetric(m, DoubleArray(size) { -gradient[it] })
                if (step != null) {
                    val candidate = DoubleArray(size) { x[it] + step[it] }
                    val candidateF = objective(candidate)
                    if (candidateF < f) {
                        val decrease = f - candidateF
                        candidate.copyInto(x)
                        f = candidateF
                        lambda = max(lambda / 3.0, 1E-12)
                        improved = true
                        if (decrease <= 1E-12 * f) return
                        break
                    }
                }
                lambda *= 4.0
            }
            if (!improved) break
        }
    }

    /** The variables that edge [e] depends on. */
    fun variablesOf(e: Int): List<Int> {
        val j = (e + 1) % n
        return buildList {
            for (k in listOf(e, j).distinct()) {
                add(angle(k)); add(curvature(k))
                if (!fixed[k]) add(displacement(k))
            }
        }
    }

    private val edgeVariables = Array(edges) { variablesOf(it).toIntArray() }
}

/** Solves the symmetric positive definite system [m]·x = [b] by Cholesky decomposition; null when it isn't. */
private fun solveSymmetric(m: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
    val n = b.size
    val l = Array(n) { DoubleArray(n) }
    for (i in 0 until n) {
        for (j in 0..i) {
            var sum = m[i][j]
            for (k in 0 until j) sum -= l[i][k] * l[j][k]
            if (i == j) {
                if (sum <= 0.0 || !sum.isFinite()) return null
                l[i][i] = sqrt(sum)
            } else {
                l[i][j] = sum / l[j][j]
            }
        }
    }
    val y = DoubleArray(n)
    for (i in 0 until n) {
        var sum = b[i]
        for (k in 0 until i) sum -= l[i][k] * y[k]
        y[i] = sum / l[i][i]
    }
    val x = DoubleArray(n)
    for (i in n - 1 downTo 0) {
        var sum = y[i]
        for (k in i + 1 until n) sum -= l[k][i] * x[k]
        x[i] = sum / l[i][i]
    }
    return x
}

private const val MAX_ITERATIONS = 500
private const val GRADIENT_TOLERANCE = 1E-12

