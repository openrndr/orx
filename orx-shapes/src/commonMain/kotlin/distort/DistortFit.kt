package org.openrndr.extra.shapes.distort

import org.openrndr.math.EuclideanVector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Approximates the curve [displaced] traces for t from [t0] to [t1] by cubic pieces, four control points each: the
 * fitting that the distort functions share.
 *
 * Every piece starts and ends at the displaced points at its ends, and its control points lie along the direction of
 * the displaced curve there; only their distances are fitted, by least squares to [samplesPerPiece] - 1 samples inside
 * the piece. A piece whose RMS error exceeds [errorTolerance] is halved, at most [maxDepth] times. Where two pieces meet
 * they share one direction, so the result is smooth wherever the displaced curve is; at [t0] and [t1] the direction is
 * taken from inside the range, so corners at the ends of a range are kept.
 *
 * Samples lie on a grid of [samplesPerPiece] times 2^[maxDepth] steps, so the halves of a piece reuse its samples and
 * every point of the curve is displaced once.
 */
internal fun <T : EuclideanVector<T>> fitDistorted(
    t0: Double,
    t1: Double,
    errorTolerance: Double,
    maxDepth: Int,
    samplesPerPiece: Int,
    displaced: (Double) -> T
): List<List<T>> {
    val n = max(samplesPerPiece, 2).toLong()
    val depth = maxDepth.coerceIn(0, 40)
    val total = n shl depth
    fun tAt(k: Long): Double = if (k >= total) t1 else t0 + (t1 - t0) * (k.toDouble() / total)

    // the displaced points of the grid, in an array when the grid is small, as it is for the usual depths
    val dense = total <= 65536L
    val pointArray = if (dense) arrayOfNulls<Any>(total.toInt() + 1) else null
    val pointMap = if (dense) null else HashMap<Long, T>()

    @Suppress("UNCHECKED_CAST")
    fun point(k: Long): T = if (pointArray != null) {
        (pointArray[k.toInt()] as T?) ?: displaced(tAt(k)).also { pointArray[k.toInt()] = it }
    } else {
        pointMap!!.getOrPut(k) { displaced(tAt(k)) }
    }

    // the direction of the displaced curve at grid point k, by differences over a small step of the range: central
    // inside, one-sided at the ends of the range
    val h = (t1 - t0) * 1E-5
    val directions = HashMap<Long, T?>()
    fun direction(k: Long): T? = if (k in directions) directions[k] else directions.getOrPut(k) {
        val t = tAt(k)
        val d = when (k) {
            0L -> (displaced(t + h) * 4.0 - displaced(t + 2 * h) - point(k) * 3.0)
            total -> (point(k) * 3.0 - displaced(t - h) * 4.0 + displaced(t - 2 * h))
            else -> displaced(t + h) - displaced(t - h)
        }
        if (d.length > 1E-12 && d.length.isFinite()) d.normalized else null
    }

    fun b1(s: Double) = 3.0 * (1.0 - s) * (1.0 - s) * s
    fun b2(s: Double) = 3.0 * (1.0 - s) * s * s

    /** the piece from grid point [a] to [b] and its RMS error */
    fun fit(a: Long, b: Long): Pair<List<T>, Double> {
        val q0 = point(a)
        val q3 = point(b)
        val chord = q3 - q0
        val chordLength = chord.length
        val fallback = if (chordLength > 0.0) chord / chordLength else chord
        val d0 = direction(a) ?: fallback
        val d1 = direction(b) ?: fallback

        val step = (b - a) / n
        var m11 = 0.0; var m12 = 0.0; var m22 = 0.0
        var r1 = 0.0; var r2 = 0.0
        val samples = ArrayList<Pair<Double, T>>(n.toInt() - 1)
        for (i in 1 until n) {
            val s = i.toDouble() / n
            val q = point(a + step * i)
            samples.add(s to q)
            val b1s = b1(s)
            val b2s = b2(s)
            val residual = q - q0 * ((1.0 - s) * (1.0 - s) * (1.0 - s) + b1s) - q3 * (b2s + s * s * s)
            val u = d0 * b1s
            val v = d1 * -b2s
            m11 += u dot u; m12 += u dot v; m22 += v dot v
            r1 += u dot residual; r2 += v dot residual
        }
        val det = m11 * m22 - m12 * m12
        var alpha = if (abs(det) < 1E-12) -1.0 else (r1 * m22 - r2 * m12) / det
        var beta = if (abs(det) < 1E-12) -1.0 else (m11 * r2 - m12 * r1) / det
        // a control point behind its end lets the piece double back on itself; long handles are fine, they are what
        // makes a looping or strongly curved piece
        if (alpha < 0.0 || beta < 0.0 || !alpha.isFinite() || !beta.isFinite()) {
            alpha = chordLength / 3.0
            beta = chordLength / 3.0
        }
        val piece = listOf(q0, q0 + d0 * alpha, q3 - d1 * beta, q3)

        var sum = 0.0
        for ((s, q) in samples) {
            val r = 1.0 - s
            val p = piece[0] * (r * r * r) + piece[1] * (3 * r * r * s) + piece[2] * (3 * r * s * s) + piece[3] * (s * s * s)
            sum += p.squaredDistanceTo(q)
        }
        return piece to (if (samples.isEmpty()) 0.0 else sqrt(sum / samples.size))
    }

    val result = mutableListOf<List<T>>()
    fun recurse(a: Long, b: Long, level: Int) {
        val (piece, rms) = fit(a, b)
        if (rms <= errorTolerance || level >= depth) {
            result.add(piece)
        } else {
            val m = (a + b) / 2
            recurse(a, m, level + 1)
            recurse(m, b, level + 1)
        }
    }
    recurse(0L, total, 0)
    return result
}
