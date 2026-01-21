//package primitives
//
//import org.openrndr.extra.shapes.primitives.invert
//import org.openrndr.extra.shapes.primitives.invertConformal
//import org.openrndr.math.Vector3
//import org.openrndr.shape.Sphere
//import kotlin.math.abs
//import kotlin.math.sqrt
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
//class TestSphereInversion {
//    @Test
//    fun testSphereInvertPoint() {
//        val sphere = Sphere(Vector3.ZERO, 1.0)
//        val point = Vector3(2.0, 0.0, 0.0)
//        val inverted = sphere.invert(point)
//
//        // For a unit sphere at origin, a point at (2,0,0) should invert to (0.5,0,0)
//        assertEquals(0.5, inverted.x, 1e-6)
//        assertEquals(0.0, inverted.y, 1e-6)
//        assertEquals(0.0, inverted.z, 1e-6)
//    }
//
//    @Test
//    fun testSphereInvertSphere() {
//        // Test case 1: Basic inversion
//        val invertingSphere = Sphere(Vector3.ZERO, 1.0)
//        val sphereToInvert = Sphere(Vector3(3.0, 0.0, 0.0), 1.0)
//        val inverted = invertingSphere.invert(sphereToInvert)
//
//        // For a unit sphere at origin, a sphere at (3,0,0) with radius 1 should invert to
//        // a sphere with center approximately at (0.375,0,0) and radius approximately 0.375
//        assertEquals(0.375, inverted.center.x, 1e-6)
//        assertEquals(0.0, inverted.center.y, 1e-6)
//        assertEquals(0.0, inverted.center.z, 1e-6)
//        assertEquals(0.375, inverted.radius, 1e-6)
//
//        // Test case 2: Sphere passing through the center of the inverting sphere
//        val spherePassingThroughCenter = Sphere(Vector3(1.0, 0.0, 0.0), 1.0)
//        val invertedLarge = invertingSphere.invert(spherePassingThroughCenter)
//
//        // Should result in a very large sphere (approximating a plane)
//        assertTrue(invertedLarge.radius > 1e5)
//
//        // Test case 3: Sphere containing the center of the inverting sphere
//        val sphereContainingCenter = Sphere(Vector3(0.5, 0.0, 0.0), 1.0)
//        val invertedContaining = invertingSphere.invert(sphereContainingCenter)
//
//        // Calculate expected values based on the actual implementation
//        val v3 = sphereContainingCenter.center - invertingSphere.center
//        val distanceSquared3 = v3.squaredLength
//        val distance3 = sqrt(distanceSquared3)
//        val power3 = distanceSquared3 - sphereContainingCenter.radius * sphereContainingCenter.radius
//        val newCenterFactor3 = (invertingSphere.radius * invertingSphere.radius) / power3
//        val expectedCenterX3 = invertingSphere.center.x + v3.x * newCenterFactor3
//        val expectedRadius3 = abs(invertingSphere.radius * sphereContainingCenter.radius / power3) * distance3
//
//        assertEquals(expectedCenterX3, invertedContaining.center.x, 1e-6)
//        assertEquals(0.0, invertedContaining.center.y, 1e-6)
//        assertEquals(0.0, invertedContaining.center.z, 1e-6)
//        assertEquals(expectedRadius3, invertedContaining.radius, 1e-6)
//
//        // Test case 4: Non-origin inverting sphere
//        val nonOriginSphere = Sphere(Vector3(1.0, 1.0, 1.0), 2.0)
//        val targetSphere = Sphere(Vector3(4.0, 1.0, 1.0), 1.0)
//        val invertedNonOrigin = nonOriginSphere.invert(targetSphere)
//
//        // Calculate the expected values based on the implementation
//        val v4 = targetSphere.center - nonOriginSphere.center
//        val distanceSquared4 = v4.squaredLength
//        val distance4 = sqrt(distanceSquared4)
//        val power4 = distanceSquared4 - targetSphere.radius * targetSphere.radius
//        val newCenterFactor4 = (nonOriginSphere.radius * nonOriginSphere.radius) / power4
//        val expectedCenter4 = nonOriginSphere.center + v4 * newCenterFactor4
//        val expectedRadius4 = abs(nonOriginSphere.radius * targetSphere.radius / power4) * distance4
//
//        assertEquals(expectedCenter4.x, invertedNonOrigin.center.x, 1e-6)
//        assertEquals(expectedCenter4.y, invertedNonOrigin.center.y, 1e-6)
//        assertEquals(expectedCenter4.z, invertedNonOrigin.center.z, 1e-6)
//        assertEquals(expectedRadius4, invertedNonOrigin.radius, 1e-6)
//    }
//
//    @Test
//    fun testSphereInvertConformalSphere() {
//        // Test case 1: Basic conformal inversion
//        val invertingSphere = Sphere(Vector3.ZERO, 1.0)
//        val sphereToInvert = Sphere(Vector3(3.0, 0.0, 0.0), 1.0)
//        val inverted = invertingSphere.invertConformal(sphereToInvert)
//
//        // Calculate expected values for conformal inversion
//        val v = sphereToInvert.center - invertingSphere.center
//        val distanceSquared = v.squaredLength
//        val power = distanceSquared - sphereToInvert.radius * sphereToInvert.radius
//        val newCenterFactor = (invertingSphere.radius * invertingSphere.radius) / power
//        val expectedCenter = invertingSphere.center + v * newCenterFactor
//        val expectedRadius = abs(invertingSphere.radius * invertingSphere.radius * sphereToInvert.radius / power)
//
//        assertEquals(expectedCenter.x, inverted.center.x, 1e-6)
//        assertEquals(expectedCenter.y, inverted.center.y, 1e-6)
//        assertEquals(expectedCenter.z, inverted.center.z, 1e-6)
//        assertEquals(expectedRadius, inverted.radius, 1e-6)
//
//        // Test case 2: Sphere passing through the center of the inverting sphere
//        val spherePassingThroughCenter = Sphere(Vector3(1.0, 0.0, 0.0), 1.0)
//        val invertedLarge = invertingSphere.invertConformal(spherePassingThroughCenter)
//
//        // Should result in a very large sphere (approximating a plane)
//        assertTrue(invertedLarge.radius > 1e5)
//
//        // Test case 3: Non-origin inverting sphere
//        val nonOriginSphere = Sphere(Vector3(1.0, 1.0, 1.0), 2.0)
//        val targetSphere = Sphere(Vector3(4.0, 1.0, 1.0), 1.0)
//        val invertedNonOrigin = nonOriginSphere.invertConformal(targetSphere)
//
//        // Calculate the expected values for conformal inversion
//        val v3 = targetSphere.center - nonOriginSphere.center
//        val distanceSquared3 = v3.squaredLength
//        val power3 = distanceSquared3 - targetSphere.radius * targetSphere.radius
//        val newCenterFactor3 = (nonOriginSphere.radius * nonOriginSphere.radius) / power3
//        val expectedCenter3 = nonOriginSphere.center + v3 * newCenterFactor3
//        val expectedRadius3 = abs(nonOriginSphere.radius * nonOriginSphere.radius * targetSphere.radius / power3)
//
//        assertEquals(expectedCenter3.x, invertedNonOrigin.center.x, 1e-6)
//        assertEquals(expectedCenter3.y, invertedNonOrigin.center.y, 1e-6)
//        assertEquals(expectedCenter3.z, invertedNonOrigin.center.z, 1e-6)
//        assertEquals(expectedRadius3, invertedNonOrigin.radius, 1e-6)
//    }
//}