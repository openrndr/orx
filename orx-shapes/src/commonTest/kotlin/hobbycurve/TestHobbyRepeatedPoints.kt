package hobbycurve

import org.openrndr.extra.shapes.hobbycurve.HobbyTension
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.ShapeContour
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestHobbyRepeatedPoints {
    private val points = listOf(Vector2(0.0, 0.0), Vector2(100.0, 20.0), Vector2(150.0, 120.0), Vector2(40.0, 160.0), Vector2(-20.0, 80.0))

    private fun assertSame(a: ShapeContour, b: ShapeContour) {
        assertEquals(b.segments.size, a.segments.size)
        assertEquals(b.closed, a.closed)
        for ((x, y) in a.segments.zip(b.segments)) {
            for ((p, q) in (listOf(x.start) + x.control + x.end).zip(listOf(y.start) + y.control + y.end)) {
                assertTrue(p.distanceTo(q) < 1E-9, "$p != $q")
            }
        }
    }

    private fun assertSame(a: Path3D, b: Path3D) {
        assertEquals(b.segments.size, a.segments.size)
        for ((x, y) in a.segments.zip(b.segments)) {
            for ((p, q) in (listOf(x.start) + x.control + x.end).zip(listOf(y.start) + y.control + y.end)) {
                assertTrue(p.distanceTo(q) < 1E-9, "$p != $q")
            }
        }
    }

    @Test
    fun repeatedPointsArePassedOver() {
        for (closed in listOf(false, true)) {
            val expected = hobbyCurve(points, closed)
            // in the middle, at the start, at the end, several times
            val repeats = listOf(
                listOf(points[0], points[1], points[1], points[2], points[3], points[4]),
                listOf(points[0], points[0], points[1], points[2], points[3], points[4]),
                listOf(points[0], points[1], points[2], points[3], points[4], points[4]),
                listOf(points[0], points[1], points[2], points[2], points[2], points[3], points[4])
            )
            for (input in repeats) assertSame(hobbyCurve(input, closed), expected)
        }
    }

    @Test
    fun aClosedCurveMayEndAtItsStart() {
        assertSame(hobbyCurve(points + points[0], closed = true), hobbyCurve(points, closed = true))
        assertSame(hobbyCurve(points + points[0] + points[0], closed = true), hobbyCurve(points, closed = true))
        assertSame(hobbyCurve(listOf(points[0]) + points + points[0], closed = true), hobbyCurve(points, closed = true))
    }

    @Test
    fun tensionsFollowTheirChords() {
        // tensions per chord of the input: the chord of the repeated point has its own, which is passed over
        val tensions = listOf(1.0, 2.0, 1.0, 3.0, 1.0, 1.5).map { HobbyTension(it, it) }
        val input = listOf(points[0], points[1], points[2], points[2], points[3], points[4])
        val expected = listOf(1.0, 2.0, 3.0, 1.0, 1.5).map { HobbyTension(it, it) }
        assertSame(hobbyCurve(input, true, segmentTensions = tensions), hobbyCurve(points, true, segmentTensions = expected))
        // the chord indices that the factors are asked for are those of the input
        val asked = mutableListOf<Int>()
        hobbyCurve(input, true, tensions = { i, _, _ -> asked.add(i); 1.0 to 1.0 })
        assertEquals(listOf(0, 1, 3, 4, 5), asked)
        // a closed curve ending at its start: the closing chord is the one into the repeated start
        val closing = listOf(1.0, 2.0, 3.0, 1.5, 1.2, 9.0).map { HobbyTension(it, it) }
        assertSame(
            hobbyCurve(points + points[0], true, segmentTensions = closing),
            hobbyCurve(points, true, segmentTensions = closing.take(5))
        )
    }

    @Test
    fun tooFewDistinctPoints() {
        assertTrue(hobbyCurve(listOf(points[0], points[0], points[0])).empty)
        assertEquals(1, hobbyCurve(listOf(points[0], points[0], points[1], points[1])).segments.size)
    }

    @Test
    fun repeatedPointsIn3D() {
        val p3 = points.mapIndexed { i, p -> Vector3(p.x, p.y, i * 10.0) }
        for (closed in listOf(false, true)) {
            val input = listOf(p3[0], p3[1], p3[1], p3[2], p3[3], p3[4]) + (if (closed) listOf(p3[0]) else emptyList())
            assertSame(hobbyCurve(input, closed), hobbyCurve(p3, closed))
        }
    }
}
