import org.openrndr.extra.processing.PShape
import org.openrndr.extra.processing.toShape
import org.openrndr.extra.processing.toShapeContours
import org.openrndr.extra.processing.toVector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import processing.core.PShape
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPShapeExtensions {
    val eps = 1e-5

    private fun assertContourEqualsPShape(a: ShapeContour, b: PShape, name: String) {
        assertEquals(
            a.segments.size,
            b.vertexCount,
            "$name ShapeContour segment count should match PShape vertex count"
        )

        val aVerts = a.segments.map { it.start }
        val bVerts = List(b.vertexCount) {
            b.getVertex(it).toVector2()
        }
        aVerts.zip(bVerts).forEachIndexed { index, (a, b) ->
            assertEquals(a.x, b.x, eps, "$name vertex $index x should match")
            assertEquals(a.y, b.y, eps, "$name vertex $index y should match")
        }
    }

    private fun assertContourEqualsContour(a: ShapeContour, b: ShapeContour, name: String) {
        assertEquals(
            a.segments.size,
            b.segments.size,
            "$name ShapeContour segment counts should match"
        )

        val aSegs = a.segments
        val bSegs = b.segments
        aSegs.zip(bSegs).forEachIndexed { index, (sa, sb) ->
            assertEquals(sa.start.x, sb.start.x, eps, "$name seg $index start vertex x should match")
            assertEquals(sa.start.y, sb.start.y, eps, "$name seg $index start vertex y should match")
            assertEquals(sa.end.x, sb.end.x, eps, "$name seg $index end vertex x should match")
            assertEquals(sa.end.y, sb.end.y, eps, "$name seg $index end vertex y should match")
            assertEquals(sa.control.size, sb.control.size, "$name seg $index control point counts should match")
            sa.control.zip(sb.control).forEachIndexed { ci, (c0, c1) ->
                assertEquals(c0.x, c1.x, eps, "$name seg $index control $ci x should match")
                assertEquals(c0.y, c1.y, eps, "$name seg $index control $ci y should match")
            }
        }
    }

    @Test
    fun `rectangular ShapeContour should match PShape version`() {
        val a = Rectangle(50.0, 40.0, 100.0, 200.0).contour
        val b = PShape(a)
        assertContourEqualsPShape(a, b, "rectangle")
    }

    @Test
    fun `circular polygonal ShapeContour should match PShape version`() {
        val a = Circle(123.11, 33.83, 59.13).contour.sampleEquidistant(37)
        val b = PShape(a)
        assertContourEqualsPShape(a, b, "circle polygon")
    }

    @Test
    fun `circle ShapeContour converted to PShape and back to ShapeContour should match`() {
        val a = Circle(1.11, 3.33, 77.19).contour
        val b = PShape(a).toShapeContours().first()

        assertContourEqualsContour(a, b, "circle")
    }

    @Test
    fun `Shape converted to PShape and back to Shape should match`() {
        val a = Shape(listOf(
            Circle(0.0, 0.0, 80.0).contour,
            Circle(0.0, 0.0, 40.0).contour.reversed
        ))
        val b = PShape(a).toShape()

        a.contours.zip(b.contours).forEachIndexed { index, (ca, cb) ->
            assertContourEqualsContour(ca, cb, "Shape contour $index")
        }
    }
}