import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.extra.triangulation.DelaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestDelaunay {
    /**
     * Test if an empty triangulation can be made
     */
    @Test
    fun testEmpty() {
        val points = listOf<Vector2>()
        val d = Delaunay.from(points)
        assertEquals(0, d.triangles.size)
        assertEquals(0, d.halfedges.size)
        assertEquals(0, d.hull.size)
        assertEquals(0, (d.neighbors(0).toList().size))
    }

    /**
     * Test if a one point triangulation can be made
     */
    @Test
    fun testOnePoint() {
        val points = listOf(Vector2(100.0, 100.0))
        val d = Delaunay.from(points)
        assertEquals(0, (d.neighbors(0).toList().size))
    }

    /**
     * Test if a two point triangulation can be made
     */
    @Test
    fun testTwoPoints() {
        val points = listOf(Vector2(100.0, 100.0), Vector2(300.0, 100.0))
        val d = Delaunay.from(points)
        println(d.triangles.size)
        println("${d.triangles[0]}, ${d.triangles[1]}, ${d.triangles[2]}")

        // this will be one degenerate triangle since we only have 2 points
        assertEquals(3, d.triangles.size)
        assertEquals(2, d.hull.size)

        assertEquals(1, (d.neighbors(0).toList().size))
        assertEquals(1, (d.neighbors(0).toList().first()))

        assertEquals(1, (d.neighbors(1).toList().size))
        assertEquals(0, (d.neighbors(1).toList().first()))
    }

    @Test
    fun testThreePointsCollinear() {
        val points = listOf(Vector2(100.0, 100.0), Vector2(200.0, 100.0), Vector2(300.0, 100.0))
        val d = Delaunay.from(points)
    }

    @Test
    fun testNeighbors() {
        val c = Circle(200.0, 200.0, 150.0).contour.equidistantPositions(20).take(20)
        val d = Delaunay.from(c)
        for (j in c.indices) {
            assertTrue(d.neighbors(j).toList().isNotEmpty())
        }
    }
}