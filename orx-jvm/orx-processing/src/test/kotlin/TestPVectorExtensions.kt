import org.openrndr.extra.processing.PVector
import org.openrndr.extra.processing.toPVector
import org.openrndr.extra.processing.toVector2
import org.openrndr.extra.processing.toVector3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class TestPVectorExtensions {
    val epsF = 1e-6f
    val epsD = 1e-6

    @Test
    fun `Verify components match when constructing a PVector out of a Vector3`() {
        val a = Vector3(0.11, 0.22, 0.33)
        val b = PVector(a)

        assertEquals(a.x.toFloat(), b.x, epsF, "PVector and Vector3 x component should match")
        assertEquals(a.y.toFloat(), b.y, epsF, "PVector and Vector3 y component should match")
        assertEquals(a.z.toFloat(), b.z, epsF, "PVector and Vector3 z component should match")

        val c = b.toVector3()
        assertEquals(a.x, c.x, epsD, "x component should match after converting PVector back to Vector3")
        assertEquals(a.y, c.y, epsD, "y component should match after converting PVector back to Vector3")
        assertEquals(a.z, c.z, epsD, "z component should match after converting PVector back to Vector3")
    }

    @Test
    fun `Verify components match when constructing a PVector out of a Vector2`() {
        val a = Vector2(0.11, 0.22)
        val b = PVector(a)

        assertEquals(a.x.toFloat(), b.x, epsF, "PVector and Vector2 x component should match")
        assertEquals(a.y.toFloat(), b.y, epsF, "PVector and Vector2 y component should match")
        assertEquals(0f, b.z, epsF, "PVector constructed from Vector2 should have z = 0f")

        val c = b.toVector2()
        assertEquals(a.x, c.x, epsD, "x component should match after converting PVector back to Vector2")
        assertEquals(a.y, c.y, epsD, "y component should match after converting PVector back to Vector2")
    }

    @Test
    fun `Verify components match when calling toPVector on a Vector3`() {
        val a = Vector3(0.4, 0.5, 0.6)
        val b = a.toPVector()

        assertEquals(a.x.toFloat(), b.x, epsF, "PVector constructed from Vector3 via toPVector should have matching x component")
        assertEquals(a.y.toFloat(), b.y, epsF, "PVector constructed from Vector3 via toPVector should have matching y component")
        assertEquals(a.z.toFloat(), b.z, epsF, "PVector constructed from Vector3 via toPVector should have matching z component")
    }

    @Test
    fun `Verify components match when calling toPVector on a Vector2`() {
        val a = Vector2(0.4, 0.5)
        val b = a.toPVector()

        assertEquals(a.x.toFloat(), b.x, epsF, "PVector constructed from Vector2 via toPVector should have matching x component")
        assertEquals(a.y.toFloat(), b.y, epsF, "PVector constructed from Vector2 via toPVector should have matching y component")
        assertEquals(0f, b.z, epsF, "PVector constructed from Vector2 via toPVector should have z = 0f")
    }
}