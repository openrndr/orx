import org.openrndr.extra.gradientdescent.minimizeModel
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals

class TestMinimizeModel {
    @Test
    fun `a model of doubles is minimized`() {
        val m = object {
            var x = 0.0
            var y = 0.0
        }
        minimizeModel(m) { (it.x - 4.0) * (it.x - 4.0) + (it.y - 3.0) * (it.y - 3.0) }
        assertEquals(4.0, m.x, 0.01)
        assertEquals(3.0, m.y, 0.01)
    }

    @Test
    fun `a model with a Vector2 property is minimized`() {
        val m = object {
            var position = Vector2.ZERO
        }
        minimizeModel(m) { (it.position.x - 4.0) * (it.position.x - 4.0) + (it.position.y - 3.0) * (it.position.y - 3.0) }
        assertEquals(4.0, m.position.x, 0.01)
        assertEquals(3.0, m.position.y, 0.01)
    }
}
