import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.test.Test

class TestVoronoi {

    @Test
    fun testNeighbors() {
        val c = Circle(200.0, 200.0, 150.0).contour.equidistantPositions(20).take(20)
        val d = Delaunay.from(c)
        val v = d.voronoi(Rectangle(0.0, 0.0, 400.0, 400.0))
        for (j in c.indices) {
            for (i in v.neighbors(j)) {
                println("$j -> $i")
            }
        }
    }



}