import org.openrndr.extra.shapes.cusps.findCusps
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [findCusps] looks for points where a segment's own parametric velocity hits
 * (near) zero - a true fold, not merely a sharp but well-defined corner
 * between two segments. These tests construct exact cusps algebraically (by
 * choosing hodograph coefficients directly, see [cubicCuspSegment]) rather
 * than approximating one visually, so the expected t is known exactly.
 */
class TestFindCusps {

    @Test
    fun quadraticCuspIsFoundAtItsExactT() {
        // P0, P1, P2 colinear with P1 beyond P2 (not between P0 and P2): the
        // textbook construction for an exact quadratic-Bezier cusp, at
        // t = (P1-P0) / ((P1-P0) - (P2-P1)) = 20 / 30 = 2/3.
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(20.0, 0.0), Vector2(10.0, 0.0))
        val contour = ShapeContour(listOf(segment), false)

        val cusps = contour.findCusps()
        assertEquals(1, cusps.size)
        assertTrue(abs(cusps[0] - 2.0 / 3.0) < 1e-6)
    }

    @Test
    fun cubicCuspIsFoundAtItsExactT() {
        // Built by picking hodograph coefficients A=(2,2), B=(-2,-2), C=(0.5,0.5)
        // so that B'(0.5) = A*0.25 + B*0.5 + C = (0,0) exactly, then
        // integrating back to control points (P1 = P0 + C/3, etc).
        val segment = Segment2D(
            Vector2(0.0, 0.0),
            Vector2(1.0 / 6.0, 1.0 / 6.0),
            Vector2(0.0, 0.0),
            Vector2(1.0 / 6.0, 1.0 / 6.0)
        )
        val contour = ShapeContour(listOf(segment), false)

        val cusps = contour.findCusps()
        assertEquals(1, cusps.size)
        assertTrue(abs(cusps[0] - 0.5) < 1e-6)
    }

    @Test
    fun plainCurveHasNoCusps() {
        val segment = Segment2D(Vector2(0.0, 0.0), Vector2(50.0, 100.0), Vector2(100.0, 0.0))
        val contour = ShapeContour(listOf(segment), false)
        assertEquals(emptyList(), contour.findCusps())
    }

    @Test
    fun linearSegmentsAreNeverReportedAsCusps() {
        val contour = ShapeContour.fromPoints(
            listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0)),
            closed = false
        )
        assertEquals(emptyList(), contour.findCusps())
    }

    @Test
    fun cuspTIsReportedInTheContoursGlobalParameterSpace() {
        // Put the cuspy segment second of two, so its local t=2/3 must come
        // back scaled into [0.5, 1.0] (segment 1 of 2) rather than [0.0, 1.0].
        val plain = Segment2D(Vector2(-10.0, 0.0), Vector2(0.0, 0.0))
        val cuspy = Segment2D(Vector2(0.0, 0.0), Vector2(20.0, 0.0), Vector2(10.0, 0.0))
        val contour = ShapeContour(listOf(plain, cuspy), false)

        val cusps = contour.findCusps()
        assertEquals(1, cusps.size)
        val expected = (1 + 2.0 / 3.0) / 2
        assertTrue(abs(cusps[0] - expected) < 1e-6)
    }
}
