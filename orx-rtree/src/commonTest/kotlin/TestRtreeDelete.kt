import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.extra.rtree.RtreePolygon2D
import org.openrndr.extra.shapes.polygon.Polygon2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestRtreeDelete {
    @Test
    fun testDelete() {
        val rtree = RtreePolygon2D(minEntries = 1, maxEntries = 2)
        val poly1 = Polygon2D(listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0)))
        val poly2 = Polygon2D(listOf(Vector2(20.0, 20.0), Vector2(30.0, 20.0), Vector2(30.0, 30.0), Vector2(20.0, 30.0)))
        
        rtree.insert(poly1)
        rtree.insert(poly2)
        
        assertEquals(2, rtree.findInRange(Rectangle(0.0, 0.0, 40.0, 40.0)).size)

        rtree.delete(poly1)
        
        assertEquals(1, rtree.findInRange(Rectangle(0.0, 0.0, 40.0, 40.0)).size)
        val remaining = rtree.findInRange(Rectangle(0.0, 0.0, 40.0, 40.0))
        assertEquals(poly2, remaining[0])

        rtree.delete(poly2)
        assertEquals(0, rtree.findInRange(Rectangle(0.0, 0.0, 40.0, 40.0)).size)
    }

    @Test
    fun testDeleteWithCondense() {
        // minEntries = 2, so deleting one will trigger condensation
        val rtree = RtreePolygon2D(minEntries = 2, maxEntries = 4)
        val polys = (0 until 10).map { i ->
            val x = i * 10.0
            Polygon2D(listOf(Vector2(x, 0.0), Vector2(x + 5.0, 0.0), Vector2(x + 5.0, 5.0), Vector2(x, 5.0)))
        }

        for (p in polys) {
            rtree.insert(p)
        }

        assertEquals(10, rtree.findInRange(Rectangle(0.0, 0.0, 100.0, 100.0)).size)

        rtree.delete(polys[0])
        assertEquals(9, rtree.findInRange(Rectangle(0.0, 0.0, 100.0, 100.0)).size)
        
        for (i in 1 until 10) {
            assertTrue(rtree.findInRange(Rectangle(0.0, 0.0, 100.0, 100.0)).contains(polys[i]))
        }
    }

    @Test
    fun testDeleteAll() {
        val rtree = RtreePolygon2D(minEntries = 2, maxEntries = 4)
        val polys = (0 until 10).map { i ->
            val x = i * 10.0
            Polygon2D(listOf(Vector2(x, 0.0), Vector2(x + 5.0, 0.0), Vector2(x + 5.0, 5.0), Vector2(x, 5.0)))
        }

        for (p in polys) {
            rtree.insert(p)
        }

        for (p in polys) {
            rtree.delete(p)
        }

        assertEquals(0, rtree.findInRange(Rectangle(-100.0, -100.0, 200.0, 200.0)).size)
    }
}
