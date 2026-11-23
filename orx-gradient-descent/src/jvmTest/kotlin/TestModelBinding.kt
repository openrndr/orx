import org.openrndr.extra.gradientdescent.arrayToModel
import org.openrndr.extra.gradientdescent.minimizeModel
import org.openrndr.extra.gradientdescent.modelToArray
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestModelBinding {
    class Model {
        var v = Vector4(1.0, 2.0, 3.0, 4.0)
        var x = 5.0
        var p = Vector2(6.0, 7.0)
    }

    @Test
    fun `the properties after a Vector4 get their own values`() {
        val m = Model()
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0), modelToArray(m))
        arrayToModel(doubleArrayOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0), m)
        assertEquals(Vector4(10.0, 20.0, 30.0, 40.0), m.v)
        assertEquals(50.0, m.x)
        assertEquals(Vector2(60.0, 70.0), m.p)
    }

    @Test
    fun `a model with a Vector4 is minimized`() {
        val m = Model()
        minimizeModel(m) {
            (it.v - Vector4(-1.0, 0.5, 2.0, 8.0)).squaredLength + (it.x - 3.0) * (it.x - 3.0) + (it.p - Vector2(1.0, 1.0)).squaredLength
        }
        assertEquals(8.0, m.v.w, 1E-3)
        assertEquals(3.0, m.x, 1E-3)
        assertEquals(1.0, m.p.y, 1E-3)
    }
}
