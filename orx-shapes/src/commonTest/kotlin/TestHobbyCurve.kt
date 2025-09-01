import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestHobbyCurve {

    @Test
    fun testSymmetric() {
        val rectangle = Rectangle(0.0, 0.0, 100.0, 100.0).contour
        val h = rectangle.hobbyCurve()
        assertTrue(h.closed)
        assertEquals(4, h.segments.size)
        assertEquals(-1.0, h.direction(0.25).dot(h.direction(0.75)), 1e-6)
        assertEquals(-1.0, h.direction(0.125).dot(h.direction(0.625)), 1e-6)
        assertEquals(-1.0, h.direction(0.375).dot(h.direction(0.875)), 1e-6)
    }
}