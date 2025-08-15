import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.extra.shapes.primitives.invertConformal
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestCircleInvertConformal {

    /**
     * Helper function to check if two circles are tangent
     */
    private fun areTangent(circle1: Circle, circle2: Circle): Boolean {
        val centerDistance = (circle1.center - circle2.center).length
        val radiusSum = circle1.radius + circle2.radius
        val radiusDiff = abs(circle1.radius - circle2.radius)
        
        // Circles are externally tangent if the distance between centers equals the sum of radii
        val externallyTangent = abs(centerDistance - radiusSum) < 1e-10
        
        // Circles are internally tangent if the distance between centers equals the difference of radii
        val internallyTangent = abs(centerDistance - radiusDiff) < 1e-10
        
        return externallyTangent || internallyTangent
    }

    @Test
    fun testInvertConformalPreservesTangency() {
        // Create an inverting circle
        val invertingCircle = Circle(100.0, 100.0, 50.0)
        
        // Create two externally tangent circles
        val circle1 = Circle(200.0, 100.0, 30.0)
        val circle2 = Circle(260.0, 100.0, 30.0)
        
        // Verify that the circles are indeed tangent
        assertTrue(areTangent(circle1, circle2), "The test circles should be tangent")
        
        // Perform conformal inversion
        val inverted1 = invertingCircle.invertConformal(circle1)
        val inverted2 = invertingCircle.invertConformal(circle2)
        
        // Verify that the inverted circles are also tangent
        assertTrue(areTangent(inverted1, inverted2), "The inverted circles should remain tangent")
    }
    
    @Test
    fun testInvertConformalPreservesInternalTangency() {
        // Create an inverting circle
        val invertingCircle = Circle(100.0, 100.0, 50.0)
        
        // Create two internally tangent circles
        // For internal tangency, one circle must be inside the other with their boundaries touching at exactly one point
        val circle1 = Circle(200.0, 100.0, 50.0)
        val circle2 = Circle(230.0, 100.0, 20.0)  // Center is at distance (radius1 - radius2) from circle1's center
        
        // Verify that the circles are indeed tangent
        assertTrue(areTangent(circle1, circle2), "The test circles should be internally tangent")
        
        // Perform conformal inversion
        val inverted1 = invertingCircle.invertConformal(circle1)
        val inverted2 = invertingCircle.invertConformal(circle2)
        
        // Verify that the inverted circles are also tangent
        assertTrue(areTangent(inverted1, inverted2), "The inverted circles should remain tangent")
    }
    
    @Test
    fun testInvertConformalWithCircleAtCenter() {
        // Create an inverting circle
        val invertingCircle = Circle(100.0, 100.0, 50.0)
        
        // Create a circle centered at the center of the inverting circle
        val circle = Circle(100.0, 100.0, 20.0)
        
        // Inverting a circle centered at the center of the inverting circle should throw an exception
        assertFailsWith<IllegalArgumentException> {
            invertingCircle.invertConformal(circle)
        }
    }
}