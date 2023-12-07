import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals


class TestAdjustContour {
    @Test
    fun testSingleLinearSegment() {
        val adjusted = adjustContour(LineSegment(0.0, 0.0, 100.0, 100.0).contour) {
            selectVertex(0)
            vertex.moveTo(Vector2(50.0, 50.0))

            selectVertex(1)
            vertex.moveTo(Vector2(150.0, 150.0))
        }
        assertEquals(1, adjusted.segments.size)
        assertEquals(50.0, adjusted.segments[0].start.x, 1E-6)
        assertEquals(50.0, adjusted.segments[0].start.y, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.x, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.y, 1E-6)
    }

    @Test
    fun testSingleLinearSegmentDefaultVertexSelection() {
        val adjusted = adjustContour(LineSegment(0.0, 0.0, 100.0, 100.0).contour) {
            for (v in vertices) {
                v.moveBy(Vector2(50.0, 50.0))
            }
        }
        assertEquals(1, adjusted.segments.size)
        assertEquals(50.0, adjusted.segments[0].start.x, 1E-6)
        assertEquals(50.0, adjusted.segments[0].start.y, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.x, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.y, 1E-6)
    }

    @Test
    fun testSingleLinearSegmentDefaultEdgeSelection() {
        val adjusted = adjustContour(LineSegment(0.0, 0.0, 100.0, 100.0).contour) {
            for (e in edges) {
                e.moveBy(Vector2(50.0, 50.0))
            }
        }
        assertEquals(1, adjusted.segments.size)
        assertEquals(50.0, adjusted.segments[0].start.x, 1E-6)
        assertEquals(50.0, adjusted.segments[0].start.y, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.x, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.y, 1E-6)
    }


    @Test
    fun testSingleQuadraticSegment() {
        val adjusted = adjustContour(LineSegment(0.0, 0.0, 100.0, 100.0).segment.quadratic.contour) {
            selectVertex(0)
            vertex.moveTo(Vector2(50.0, 50.0))

            selectVertex(1)
            vertex.moveTo(Vector2(150.0, 150.0))
        }
        assertEquals(1, adjusted.segments.size)
        assertEquals(50.0, adjusted.segments[0].start.x, 1E-6)
        assertEquals(50.0, adjusted.segments[0].start.y, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.x, 1E-6)
        assertEquals(150.0, adjusted.segments[0].end.y, 1E-6)
    }

    @Test
    fun testEdgeTransform() {
        val adjusted = adjustContour(LineSegment(0.0, 0.0, 100.0, 100.0).segment.quadratic.contour) {
            for (e in edges) {
                e.moveBy(Vector2(50.0, 50.0))
            }
        }
    }

    @Test
    fun testRectangleEdgeTransform() {
        var r = Rectangle(0.0, 0.0, 400.0, 400.0).contour
        val seconds = 0.0
        r = adjustContour(r) {
            selectEdge(1)
            for (edge in edges) {
                edge.moveBy(Vector2(sin(seconds) * 10.0, cos(seconds) * 5.0))
            }
        }


    }

}