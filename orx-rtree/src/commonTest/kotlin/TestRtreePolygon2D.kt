import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.openrndr.extra.rtree.RtreePolygon2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestRtreePolygon2D {
    @Test
    fun testInsertionAndRange() {
        val rtree = RtreePolygon2D()
        val poly1 = listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0))
        val poly2 = listOf(Vector2(20.0, 20.0), Vector2(30.0, 20.0), Vector2(30.0, 30.0), Vector2(20.0, 30.0))
        
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
        val poly1 = listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0))
        val poly2 = listOf(Vector2(20.0, 20.0), Vector2(30.0, 20.0), Vector2(30.0, 30.0), Vector2(20.0, 30.0))
        
        rtree.insert(poly1)
        rtree.insert(poly2)
        
        val nearest = rtree.findKNearest(Vector2(5.0, 5.0), 1)
        assertEquals(1, nearest.size)
        assertEquals(poly1, nearest[0])
        
        val twoNearest = rtree.findKNearest(Vector2(15.0, 15.0), 2)
        assertEquals(2, twoNearest.size)
    }

    @Test
    fun testNearestOpenSpace() {
        val rtree = RtreePolygon2D()
        val poly1 = listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0))
        val poly1Bounds = Rectangle(0.0, 0.0, 10.0, 10.0)
        
        rtree.insert(poly1)
        
        val found = rtree.findInRange(Rectangle(4.0, 4.0, 2.0, 2.0))
        assertEquals(1, found.size, "Should find poly1 with findInRange")

        // Point outside
        val p1 = Vector2(15.0, 15.0)
        assertEquals(p1, rtree.findNearestOpenSpace(p1).first)
        
        // Point inside
        val p2 = Vector2(5.0, 5.0)
        val (open, bound) = rtree.findNearestOpenSpace(p2)
        // Should be on the boundary
        assertTrue(open.x == 0.0 || open.x == 10.0 || open.y == 0.0 || open.y == 10.0, "Expected $open to be on boundary of (0,0,10,10)")
        assertTrue(bound.contains(p2), "Expected $bound to contain $p2")
        assertEquals(poly1Bounds, bound)
    }
}
