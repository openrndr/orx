import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.extra.shapes.primitives.invert
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestCircleInvert {

    @Test
    fun testInvertPointOutsideCircle() {
        val circle = Circle(100.0, 100.0, 50.0)
        val point = Vector2(200.0, 100.0)  // Point outside the circle
        val inverted = circle.invert(point)
        
        // The inverted point should be at (125.0, 100.0)
        // This is because:
        // - The point is 100 units away from the center
        // - The radius is 50
        // - The inverted distance is 50²/100 = 25
        // - So the inverted point is 25 units from the center in the same direction
        assertEquals(125.0, inverted.x, 1e-10)
        assertEquals(100.0, inverted.y, 1e-10)
        
        // Verify the inversion property: |OPʹ| × |OP| = r²
        val distanceToPoint = (point - circle.center).length
        val distanceToInverted = (inverted - circle.center).length
        assertTrue(abs(distanceToPoint * distanceToInverted - circle.radius * circle.radius) < 1e-10)
    }
    
    @Test
    fun testInvertPointInsideCircle() {
        val circle = Circle(100.0, 100.0, 50.0)
        val point = Vector2(125.0, 100.0)  // Point inside the circle
        val inverted = circle.invert(point)
        
        // The inverted point should be at (200.0, 100.0)
        // This is because:
        // - The point is 25 units away from the center
        // - The radius is 50
        // - The inverted distance is 50²/25 = 100
        // - So the inverted point is 100 units from the center in the same direction
        assertEquals(200.0, inverted.x, 1e-10)
        assertEquals(100.0, inverted.y, 1e-10)
        
        // Verify the inversion property: |OPʹ| × |OP| = r²
        val distanceToPoint = (point - circle.center).length
        val distanceToInverted = (inverted - circle.center).length
        assertTrue(abs(distanceToPoint * distanceToInverted - circle.radius * circle.radius) < 1e-10)
    }
    
    @Test
    fun testInvertPointOnCircle() {
        val circle = Circle(100.0, 100.0, 50.0)
        val point = Vector2(150.0, 100.0)  // Point on the circle
        val inverted = circle.invert(point)
        
        // The inverted point should be the same as the original point
        // This is because points on the circle invert to themselves
        assertEquals(150.0, inverted.x, 1e-10)
        assertEquals(100.0, inverted.y, 1e-10)
        
        // Verify the inversion property: |OPʹ| × |OP| = r²
        val distanceToPoint = (point - circle.center).length
        val distanceToInverted = (inverted - circle.center).length
        assertTrue(abs(distanceToPoint * distanceToInverted - circle.radius * circle.radius) < 1e-10)
    }
    
    @Test
    fun testInvertPointAtCenter() {
        val circle = Circle(100.0, 100.0, 50.0)
        val point = Vector2(100.0, 100.0)  // Point at the center
        
        // Inverting a point at the center should throw an exception
        assertFailsWith<IllegalArgumentException> {
            circle.invert(point)
        }
    }
}