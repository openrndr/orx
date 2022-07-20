import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.extra.triangulation.delaunayTriangulation
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestVoronoiDiagram {
    @Test
    fun testNeighbors() {
        val c = Circle(200.0, 200.0, 150.0).contour.equidistantPositions(20).take(20)
        val d = Delaunay.from(c)
        val v = d.voronoi(Rectangle(0.0, 0.0, 400.0, 400.0))
        for (j in c.indices) {
            assertTrue(v.neighbors(j).toList().isNotEmpty())
        }
    }

    @Test
    fun testEmpty() {
        val dt = listOf<Vector2>().delaunayTriangulation()
        val v = dt.voronoiDiagram(Rectangle(0.0, 0.0, 400.0, 400.0))
        assertEquals(0, dt.triangles().size)
        assertEquals(0, v.cellPolygons().size)
    }

    @Test
    fun testOnePoint() {
        val dt = listOf(Vector2(100.0, 100.0)).delaunayTriangulation()
        val v = dt.voronoiDiagram(Rectangle(0.0, 0.0, 400.0, 400.0))
        assertEquals(0, dt.triangles().size)
        assertEquals(1, v.cellPolygons().size)
    }


    @Test
    fun testTwoPoints() {
        val dt = listOf(Vector2(100.0, 100.0), Vector2(300.0, 300.0)).delaunayTriangulation()
        val v = dt.voronoiDiagram(Rectangle(0.0, 0.0, 400.0, 400.0))
        assertEquals(1, dt.triangles().size)
        assertEquals(2, v.cellPolygons().size)
    }

    @Test
    fun testThreePointsCollinear() {
        val dt = listOf(Vector2(100.0, 100.0), Vector2(200.0, 200.0), Vector2(300.0, 300.0)).delaunayTriangulation()
        val v = dt.voronoiDiagram(Rectangle(0.0, 0.0, 400.0, 400.0))
        assertEquals(1, dt.triangles().size)
        assertEquals(3, v.cellPolygons().size)
    }


}