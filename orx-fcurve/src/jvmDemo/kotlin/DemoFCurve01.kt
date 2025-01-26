import org.openrndr.application
import org.openrndr.extra.fcurve.fcurve

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val xpos = fcurve("M0 Q4,360,5,720").sampler()
        val ypos = fcurve("M360 h5").sampler()

        extend {
            drawer.circle(xpos(seconds.mod(5.0)), ypos(seconds.mod(5.0)), 100.0)
        }
    }
}
