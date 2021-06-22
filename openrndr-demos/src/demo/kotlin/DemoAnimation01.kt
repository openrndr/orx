import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

suspend fun main() = application {
    program {
      class A: Animatable() {
            var x = 0.0
            var y = Vector2(200.0, 200.0)
        }

        val a = A()
        a.apply {
            ::y.animate(Vector2.ZERO, 10000, Easing.CubicInOut)
            ::x.animate(100.0, 5000).completed.listen {
                println("hello world")
                ::x.animate(1.0, 5000).completed.listen {
                    println("we meet again")
                }
            }
        }

        extend {
            a.updateAnimation()
            drawer.circle(a.y, 10.0)
        }
    }
}