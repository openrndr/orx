import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.extra.rtree.RtreePolygon2D
import org.openrndr.extra.shapes.polygon.Polygon2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestRtreePolygon2D {
    @Test
    fun testInsertionAndRange() {
        val rtree = RtreePolygon2D()
        val poly1 = Polygon2D(listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0)))
        val poly2 = Polygon2D(listOf(Vector2(20.0, 20.0), Vector2(30.0, 20.0), Vector2(30.0, 30.0), Vector2(20.0, 30.0)))

        rtree.insert(poly1)
        rtree.insert(poly2)

        val range1 = rtree.findInRange(Rectangle(0.0, 0.0, 15.0, 15.0))
        assertEquals(1, range1.size)
        assertEquals(poly1, range1[0])

        val range2 = rtree.findInRange(Rectangle(0.0, 0.0, 40.0, 40.0))
        assertEquals(2, range2.size)
    }

    @Test
    fun testKNearest() {
        val rtree = RtreePolygon2D()
        val poly1 = Polygon2D(listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0)))
        val poly2 = Polygon2D(listOf(Vector2(20.0, 20.0), Vector2(30.0, 20.0), Vector2(30.0, 30.0), Vector2(20.0, 30.0)))

        rtree.insert(poly1)
        rtree.insert(poly2)

        val nearest = rtree.findKNearest(Vector2(5.0, 5.0), 1)
        assertEquals(1, nearest.size)
        assertEquals(poly1, nearest[0])

        val twoNearest = rtree.findKNearest(Vector2(15.0, 15.0), 2)
        assertEquals(2, twoNearest.size)
    }
}