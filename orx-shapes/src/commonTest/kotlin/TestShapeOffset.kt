import offset.offset
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.SegmentJoin
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contains
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestShapeOffset {
    private val outer = Rectangle(0.0, 0.0, 200.0, 200.0).contour
    private val hole = Circle(100.0, 100.0, 40.0).contour.reversed

    private fun Shape.area(): Double {
        // filled area: outlines minus holes, independent of how the contours wind
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

    @Test
    fun dilationShrinksHoles() {
        val shape = Shape(listOf(outer, hole)).offset(10.0, SegmentJoin.ROUND)
        assertEquals(2, shape.contours.size)
        assertArea(200.0 * 200.0 + 4 * 200.0 * 10.0 + PI * 10.0 * 10.0 - PI * 30.0 * 30.0, shape.area())
        assertTrue(Vector2(100.0, 100.0) !in shape, "the center of the hole should stay empty")
        assertTrue(Vector2(100.0, 135.0) in shape, "the rim of the hole should be filled")
    }

    @Test
    fun erosionGrowsHoles() {
        val shape = Shape(listOf(outer, hole)).offset(-10.0, SegmentJoin.ROUND)
        assertEquals(2, shape.contours.size)
        assertArea(180.0 * 180.0 - PI * 50.0 * 50.0, shape.area())
        assertTrue(Vector2(100.0, 145.0) !in shape, "the grown hole should be empty")
    }

    @Test
    fun holesCloseUnderLargeDilation() {
        val shape = Shape(listOf(outer, hole)).offset(45.0, SegmentJoin.MITER)
        assertEquals(1, shape.contours.size)
        assertArea(290.0 * 290.0, shape.area())
        assertTrue(Vector2(100.0, 100.0) in shape)
    }

    @Test
    fun holeRoleFollowsNestingNotWinding() {
        // the hole wound the same way as the outline
        val sameWinding = Shape(listOf(outer, Circle(100.0, 100.0, 40.0).contour))
        val shape = sameWinding.offset(10.0, SegmentJoin.ROUND)
        assertTrue(Vector2(100.0, 100.0) !in shape, "the hole should stay a hole")
        assertArea(200.0 * 200.0 + 4 * 200.0 * 10.0 + PI * 10.0 * 10.0 - PI * 30.0 * 30.0, shape.area())
    }

    @Test
    fun islandInsideHole() {
        val bigHole = Circle(100.0, 100.0, 80.0).contour.reversed
        val island = Circle(100.0, 100.0, 20.0).contour
        val shape = Shape(listOf(outer, bigHole, island)).offset(5.0, SegmentJoin.ROUND)
        assertEquals(3, shape.contours.size)
        assertTrue(Vector2(100.0, 100.0) in shape, "the island should be kept")
        assertTrue(Vector2(100.0, 124.0) in shape, "the island should grow")
        assertTrue(Vector2(100.0, 150.0) !in shape, "the hole between island and outline should stay empty")
    }

    @Test
    fun separateOutlinesMergeWhenTheyMeet() {
        val a = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val b = Rectangle(110.0, 0.0, 100.0, 100.0).contour
        val apart = Shape(listOf(a, b)).offset(4.0, SegmentJoin.MITER)
        assertEquals(2, apart.contours.size)
        val merged = Shape(listOf(a, b)).offset(6.0, SegmentJoin.MITER)
        assertEquals(1, merged.contours.size)
        assertArea(222.0 * 112.0, merged.area())
    }

    @Test
    fun openContoursAreOffsetToo() {
        val line = ShapeContour.fromPoints(listOf(Vector2(300.0, 0.0), Vector2(400.0, 0.0)), closed = false)
        val shape = Shape(listOf(outer, line)).offset(10.0, SegmentJoin.ROUND)
        assertEquals(1, shape.closedContours.size)
        assertEquals(1, shape.openContours.size)
    }
}
