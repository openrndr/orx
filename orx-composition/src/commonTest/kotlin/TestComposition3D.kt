import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.extra.composition.fillTriangles
import org.openrndr.extra.composition.strokeTriangles
import org.openrndr.math.Vector2
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class TestComposition3D {
    /** the area that the triangles cover, the triangles do not overlap */
    private fun area(triangles: List<Vector2>) = triangles.chunked(3).sumOf { (a, b, c) -> abs((b - a).x * (c - a).y - (b - a).y * (c - a).x) / 2.0 }

    private val line = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0)), closed = false)

    @Test
    fun `strokes cover their segments and caps`() {
        assertEquals(1000.0, area(strokeTriangles(listOf(line), 10.0, LineJoin.MITER, LineCap.BUTT, 4.0, 0.01)), 1E-6)
        assertEquals(1100.0, area(strokeTriangles(listOf(line), 10.0, LineJoin.MITER, LineCap.SQUARE, 4.0, 0.01)), 1E-6)
        // two half discs make a disc, of polygons a little smaller
        assertEquals(1000.0 + PI * 25.0, area(strokeTriangles(listOf(line), 10.0, LineJoin.MITER, LineCap.ROUND, 4.0, 0.001)), 0.2)
    }

    @Test
    fun `joins fill the outside of corners, miters up to their limit`() {
        val square = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        // a frame from 95 to 105 around the square, mitered
        assertEquals(110.0 * 110.0 - 90.0 * 90.0, area(strokeTriangles(listOf(square), 10.0, LineJoin.MITER, LineCap.BUTT, 4.0, 0.01)), 1E-3)
        // bevelled corners cut a triangle of 5 by 5 off each corner
        assertEquals(110.0 * 110.0 - 90.0 * 90.0 - 4 * 12.5, area(strokeTriangles(listOf(square), 10.0, LineJoin.BEVEL, LineCap.BUTT, 4.0, 0.01)), 1E-3)
        // a miter longer than the limit is bevelled: the miter of a right angle is sqrt(2) times the width
        assertEquals(110.0 * 110.0 - 90.0 * 90.0 - 4 * 12.5, area(strokeTriangles(listOf(square), 10.0, LineJoin.MITER, LineCap.BUTT, 1.4, 0.01)), 1E-3)
        // round corners are quarter discs
        assertEquals(110.0 * 110.0 - 90.0 * 90.0 - 4 * (25.0 - PI * 25.0 / 4.0), area(strokeTriangles(listOf(square), 10.0, LineJoin.ROUND, LineCap.BUTT, 4.0, 0.001)), 0.2)
    }

    @Test
    fun `a stroke that overlaps itself covers its area once`() {
        val back = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(50.0, 0.0)), closed = false)
        assertEquals(1000.0, area(strokeTriangles(listOf(back), 10.0, LineJoin.BEVEL, LineCap.BUTT, 4.0, 0.01)), 1E-3)
    }

    @Test
    fun `fills follow their rule, open contours are closed`() {
        val outer = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val inner = Rectangle(25.0, 25.0, 50.0, 50.0).contour
        assertEquals(7500.0, area(fillTriangles(Shape(listOf(outer, inner)), 0.1, FillRule.EVEN_ODD)), 1E-6)
        assertEquals(10000.0, area(fillTriangles(Shape(listOf(outer, inner)), 0.1, FillRule.NON_ZERO)), 1E-6)
        val open = ShapeContour.fromPoints(listOf(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0)), closed = false)
        assertEquals(5000.0, area(fillTriangles(Shape(listOf(open)), 0.1, FillRule.NON_ZERO)), 1E-6)
    }
}
