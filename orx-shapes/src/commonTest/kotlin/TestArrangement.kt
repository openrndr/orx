import org.openrndr.extra.shapes.arrangement.Arrangement
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestArrangement {

    /** Every connected component has its own unbounded face, so V - E + F = 2C with C the number of components. */
    private fun assertEuler(arr: Arrangement) {
        val vertices = arr.halfEdges.map { it.source }.toSet().size
        val c = arr.unboundedFaces.size
        assertEquals(2 * c, vertices - arr.edges.size + arr.faces.size, "Euler characteristic")
    }

    private fun assertSnapped(arr: Arrangement) {
        for (he in arr.halfEdges) {
            assertEquals(he.source.pos, he.contour.segments.first().start, "half-edge starts at its vertex")
            assertEquals(he.target.pos, he.contour.segments.last().end, "half-edge ends at its vertex")
        }
    }

    private fun scene(): List<ShapeContour> = listOf(
        Circle(Vector2(0.0, 0.0), 100.0).contour,
        Circle(Vector2(80.0, 0.0), 60.0).contour,
        Rectangle(-50.0, -120.0, 100.0, 240.0).contour,
        LineSegment(-150.0, 0.0, 150.0, 0.0).contour,
    )

    @Test
    fun scaleInvariance() {
        val reference = Arrangement(scene())
        assertEuler(reference)
        assertSnapped(reference)
        for (s in listOf(1E-3, 1E3)) {
            val m = transform { scale(s, s, 1.0) }
            val arr = Arrangement(scene().map { it.transform(m) })
            assertEuler(arr)
            assertSnapped(arr)
            assertEquals(reference.vertices.size, arr.vertices.size, "vertices at scale $s")
            assertEquals(reference.edges.size, arr.edges.size, "edges at scale $s")
            assertEquals(reference.boundedFaces.size, arr.boundedFaces.size, "bounded faces at scale $s")
        }
    }

    @Test
    fun tinyTriangleIsKept() {
        // Three long lines forming a triangle with sides of 0.05, inside a square
        val d60 = Vector2(cos(PI / 3.0), sin(PI / 3.0)) * 150.0
        val d120 = Vector2(cos(2.0 * PI / 3.0), sin(2.0 * PI / 3.0)) * 150.0
        val c3 = Vector2(0.05, 0.0)
        val arr = Arrangement(
            LineSegment(-150.0, 0.0, 150.0, 0.0),
            LineSegment(-d60, d60),
            LineSegment(c3 - d120, c3 + d120),
            Rectangle(-100.0, -100.0, 200.0, 200.0),
        )
        assertEuler(arr)
        assertSnapped(arr)
        // 6 line end points, 6 crossings with the square, 3 corners of the triangle
        assertEquals(15, arr.vertices.size)
        // 6 wedges and the triangle
        assertEquals(7, arr.boundedFaces.size)
    }

    @Test
    fun linesThroughOnePoint() {
        // Six lines through the origin produce a single vertex there, not a cluster of nearby ones
        val lines = List(6) { k ->
            val a = k * PI / 6.0 + 0.1
            val d = Vector2(cos(a), sin(a)) * 100.0
            LineSegment(-d, d).contour
        }
        val arr = Arrangement(lines)
        assertEuler(arr)
        assertEquals(13, arr.vertices.size)
        assertEquals(1, arr.vertices.count { it.pos.length < 1E-3 })
    }

    @Test
    fun verticesLieOnTheirCurves() {
        // Kartifex is accurate to about 1e-5 in curve parameter; the arrangement polishes its intersections.
        val shapes = listOf(
            Circle(Vector2(0.0, 0.0), 100.0).contour,
            Circle(Vector2(70.0, 20.0), 80.0).contour,
            Circle(Vector2(-30.0, 60.0), 55.0).contour,
        )
        val arr = Arrangement(shapes)
        assertEquals(6, arr.vertices.size)
        for (v in arr.vertices) {
            val distances = shapes.map { it.nearest(v.pos).position.distanceTo(v.pos) }.sorted()
            assertTrue(distances[1] < 1E-9, "vertex ${v.pos} lies on two curves, distances $distances")
        }
    }

    @Test
    fun nestedComponentsAreHoles() {
        // As in DemoArrangement03: circles inscribed in squares. The two innermost pairs touch only each
        // other, so they form components nested inside the disc of the third circle.
        val circles = (1..4).map { Circle(Vector2.ZERO, it * 20.0).contour }
        val squares = (1..4).map { Rectangle.fromCenter(Vector2.ZERO, it * 40.0, it * 40.0).contour }
        val arr = Arrangement(circles + squares)
        assertEuler(arr)
        assertEquals(3, arr.unboundedFaces.size)
        assertEquals(1, arr.unboundedFaces.count { it.parent == null })

        // Faces with their holes cut out tile the outer square exactly once.
        val total = arr.boundedFaces.sumOf { it.shape.area }
        assertTrue(kotlin.math.abs(total - 160.0 * 160.0) < 1E-6 * total, "area $total")
    }
}
