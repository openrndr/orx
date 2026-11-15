package expand

import org.openrndr.draw.LineCap
import org.openrndr.extra.shapes.expand.expand
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExpandStroke {
    /** the distance to [curve] by brute force over [samples] dense samples */
    private fun distances(curve: ShapeContour, samples: Int = 20000): (Vector2) -> Double {
        val dense = List(samples) { curve.position(it / samples.toDouble()) }
        return { p -> sqrt(dense.minOf { it.squaredDistanceTo(p) }) }
    }

    private fun area(c: ShapeContour): Double {
        val p = c.adaptivePositions(0.1)
        var a = 0.0
        for (i in p.indices) {
            val u = p[i]
            val v = p[(i + 1) % p.size]
            a += u.x * v.y - v.x * u.y
        }
        return abs(a) / 2
    }

    @Test
    fun overlappingStrokesCoverTheirCurves() {
        val random = Random(11)
        repeat(12) { k ->
            val curve = hobbyCurve(List(random.nextInt(4, 10)) { Vector2(random.nextDouble(0.0, 400.0), random.nextDouble(0.0, 400.0)) })
            val distance = distances(curve)
            for (weight in listOf(4.0, 30.0, 90.0)) {
                for ((join, cap) in listOf(SegmentJoin.ROUND to LineCap.ROUND, SegmentJoin.MITER to LineCap.SQUARE, SegmentJoin.BEVEL to LineCap.BUTT)) {
                    val stroke = curve.expand(weight, join, cap)
                    val inside = curve.equidistantPositions(60).drop(1).dropLast(1).count { it in stroke }
                    assertEquals(58, inside, "curve $k, weight $weight, $join/$cap: the stroke leaves parts of the curve out")
                    for (c in stroke.contours) {
                        assertTrue(2 * area(c) / c.length > 0.05, "curve $k, weight $weight, $join/$cap: a stray sliver")
                    }
                }
                // round joins and caps: the outlines of the stroke are at half the weight from the curve
                val round = curve.expand(weight, SegmentJoin.ROUND, LineCap.ROUND)
                val worst = round.contours.maxOf { c -> c.equidistantPositions(80).maxOf { abs(distance(it) - weight / 2) } }
                assertTrue(worst < 0.5, "curve $k, weight $weight: an outline is $worst off half the weight")
            }
        }
    }

    @Test
    fun aStrokeIsThePointsWithinHalfItsWeight() {
        val center = Vector2(300.0, 300.0)
        val curves = listOf(
            hobbyCurve(List(9) { k -> Vector2(100.0 + k * 50.0, 300.0 + (if (k % 2 == 0) -60.0 else 60.0)) }),
            hobbyCurve(List(9) { k -> val a = k * 2 * PI / 8 + 0.2; center + Vector2(sin(a) * 160.0, sin(a) * cos(a) * 110.0) }),
            hobbyCurve(List(30) { k -> val a = k * 0.5; center + Vector2(cos(a), sin(a)) * (20.0 + k * 6.0) })
        )
        // a coarse grid and curve samples less than half a pixel apart keep the test fast enough for the JS tests,
        // the points closer to the edge of the stroke than two steps of the grid are not tested anyway
        val n = 50
        for ((index, curve) in curves.withIndex()) {
            val distance = distances(curve, 4000)
            val b = curve.bounds.offsetEdges(60.0)
            val step = maxOf(b.width, b.height) / n
            for (weight in listOf(10.0, 80.0)) {
                val stroke = curve.expand(weight, SegmentJoin.ROUND, LineCap.ROUND)
                for (k in 0 until n * n) {
                    val p = Vector2(b.x + (k % n + 0.5) * step, b.y + (k / n + 0.5) * step)
                    val margin = distance(p) - weight / 2
                    if (abs(margin) < 2 * step) continue
                    assertEquals(margin < 0.0, p in stroke, "curve $index, weight $weight at $p")
                }
            }
        }
    }
}
