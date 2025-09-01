import org.openrndr.application
import org.openrndr.extra.fcurve.MultiFCurve
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.extra.fcurve.vector2

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        class XYAnimation : MultiFCurve(
            mapOf(
                "x" to fcurve("M0 Q4,360,5,720"),
                "y" to fcurve("M360 h5")
            )
        ) {
            val position = vector2("x", "y")
        }

        val xyAnimation = XYAnimation()
        val position = xyAnimation.position.sampler()

        extend {
            drawer.circle(position(seconds.mod(5.0)), 100.0)
        }
    }
}
