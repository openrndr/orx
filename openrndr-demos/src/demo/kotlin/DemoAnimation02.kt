import org.openrndr.animatable.Animatable
import org.openrndr.animatable.easing.Easing
import org.openrndr.application
import org.openrndr.math.Vector2
import org.openrndr.shape.contour

suspend fun main() = application {
    program {
        val a = object {
            var x = 0.0
        }

        animate {
            updateAnimation()
            a::x.animate(1000.0, 5000, Easing.CubicInOut)
            a::x.complete()
            a::x.animate(0.0, 5000, Easing.CubicInOut)

        }
        extend {
            drawer.circle(a.x, height/2.0, 40.0)
        }

    }
}