import org.openrndr.draw.LineCap
import org.openrndr.extra.shapes.expand.expand
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestExpand {
    private fun Shape.area(): Double {
        val signed = contours.map { c ->
            val points = c.adaptivePositions(0.05)
            points.indices.sumOf { i ->
                val a = points[i]
                val b = points[(i + 1) % points.size]
                a.x * b.y - b.x * a.y
            } * 0.5
        }
        val outerSign = signed.maxBy { abs(it) }.let { if (it < 0) -1.0 else 1.0 }
        return signed.sumOf { it * outerSign }
    }

    private fun assertArea(expected: Double, actual: Double) {
        assertTrue(abs(expected - actual) / expected < 0.005, "expected area $expected, got $actual")
    }

    private val line = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0)), closed = false)

    @Test
    fun caps() {
        val butt = line.expand(10.0, cap = LineCap.BUTT)
        assertEquals(1, butt.contours.size)
        assertArea(100.0 * 10.0, butt.area())

        val square = line.expand(10.0, cap = LineCap.SQUARE)
        assertArea(110.0 * 10.0, square.area())
        assertTrue(Vector2(-4.0, 4.0) in square)

        val round = line.expand(10.0, cap = LineCap.ROUND)
        assertArea(100.0 * 10.0 + PI * 5.0 * 5.0, round.area())
        assertTrue(Vector2(-4.0, 0.0) in round)
        assertTrue(Vector2(-4.0, 4.0) !in round)
    }

    @Test
    fun joins() {
        val corner = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0)), closed = false)
        // two 10 wide legs overlapping in a 5x5 square, plus the join at the outer corner
        val legs = 2 * 100.0 * 10.0 - 25.0
        assertArea(legs + 25.0, corner.expand(10.0, SegmentJoin.MITER, LineCap.BUTT).area())
        assertArea(legs + 25.0 / 2.0, corner.expand(10.0, SegmentJoin.BEVEL, LineCap.BUTT).area())
        assertArea(legs + PI * 25.0 / 4.0, corner.expand(10.0, SegmentJoin.ROUND, LineCap.BUTT).area())
    }

    @Test
    fun selfCrossingStrokeIsMerged() {
        // the stroke crosses itself at (50, 0)
        val crossing = ShapeContour.fromPoints(
            listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 50.0), Vector2(50.0, 50.0), Vector2(50.0, -50.0)),
            closed = false
        )
        val shape = crossing.expand(10.0, SegmentJoin.MITER, LineCap.BUTT)
        assertTrue(Vector2(50.0, 0.0) in shape, "the crossing should be filled")
        assertTrue(Vector2(75.0, 25.0) !in shape, "the loop's inside should stay empty")
    }

    @Test
    fun curvedContour() {
        val arc = Circle(0.0, 0.0, 50.0).contour.sub(0.0, 0.25)
        val shape = arc.expand(10.0, cap = LineCap.BUTT)
        assertEquals(1, shape.contours.size)
        assertArea(PI * (55.0 * 55.0 - 45.0 * 45.0) / 4.0, shape.area())
    }

    @Test
    fun closedContourExpandsToRing() {
        val shape = Circle(0.0, 0.0, 50.0).contour.expand(10.0)
        assertEquals(2, shape.contours.size)
        assertArea(PI * (55.0 * 55.0 - 45.0 * 45.0), shape.area())
        assertTrue(Vector2.ZERO !in shape)
        assertTrue(Vector2(50.0, 0.0) in shape)
    }
}
