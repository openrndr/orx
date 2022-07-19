import org.openrndr.extra.triangulation.Delaunay
import org.openrndr.extra.triangulation.DelaunayTriangulation
import org.openrndr.shape.Circle
import kotlin.test.Test

class TestDelaunay {
    @Test
    fun testNeighbors() {
        val c = Circle(200.0, 200.0, 150.0).contour.equidistantPositions(20).take(20)
        val d = Delaunay.from(c)
        for (j in c.indices) {
            for (i in d.neighbors(j)) {
                println("$j -> $i")
            }
        }
    }
}