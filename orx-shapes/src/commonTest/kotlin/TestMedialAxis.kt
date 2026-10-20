import org.openrndr.extra.shapes.medialaxis.medialAxis
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMedialAxis {

    @Test
    fun rectangle() {
        val shape = Rectangle(0.0, 0.0, 200.0, 100.0).shape
        val axis = shape.medialAxis(samplingDistance = 2.0)

        // two junctions connected by a single branch, four branches to the corners
        assertEquals(5, axis.branches.size, "branches")
        assertEquals(6, axis.nodes.size, "nodes")

        val corners = listOf(Vector2(0.0, 0.0), Vector2(200.0, 0.0), Vector2(0.0, 100.0), Vector2(200.0, 100.0))
        for (corner in corners) {
            assertTrue(axis.nodes.any { it.distanceTo(corner) < 1E-6 }, "node at corner $corner, nodes ${axis.nodes}")
        }
        for (junction in listOf(Vector2(50.0, 50.0), Vector2(150.0, 50.0))) {
            assertTrue(axis.nodes.any { it.distanceTo(junction) < 1E-3 }, "junction $junction, nodes ${axis.nodes}")
        }

        // every point of the medial axis is equidistant to the two nearest sides
        for (contour in axis.contours) {
            for (p in (0..50).map { contour.position(it / 50.0) }) {
                val d = listOf(p.x, 200.0 - p.x, p.y, 100.0 - p.y).sorted()
                assertTrue(abs(d[0] - d[1]) < 1E-2, "point $p not on the medial axis")
            }
        }
    }

    @Test
    fun ellipse() {
        val shape = Ellipse(0.0, 0.0, 100.0, 50.0).shape
        val axis = shape.medialAxis(samplingDistance = 1.0)

        assertEquals(1, axis.branches.size, "branches")
        val contour = axis.contours.first()
        for (p in (0..50).map { contour.position(it / 50.0) }) {
            assertTrue(abs(p.y) < 0.05, "point $p not on the major axis")
        }
        // the medial axis of an ellipse ends at the centers of curvature of its vertices, x = ±(a - b²/a)
        val ends = listOf(contour.position(0.0).x, contour.position(1.0).x).sorted()
        assertEquals(-75.0, ends[0], 0.5)
        assertEquals(75.0, ends[1], 0.5)
    }

    @Test
    fun annulus() {
        val shape = Shape(listOf(Circle(0.0, 0.0, 100.0).contour, Circle(0.0, 0.0, 50.0).contour.reversed))
        val axis = shape.medialAxis(samplingDistance = 2.0, fitTolerance = 0.02)

        assertEquals(1, axis.branches.size, "branches")
        val contour = axis.contours.first()
        assertTrue(contour.closed)
        for (p in (0..100).map { contour.position(it / 100.0) }) {
            // circles are Bézier approximations, their medial axis deviates slightly from r = 75
            assertEquals(75.0, p.length, 0.05, "point $p not on the medial circle")
        }
    }
}
