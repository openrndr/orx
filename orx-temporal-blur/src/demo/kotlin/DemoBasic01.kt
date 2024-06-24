import org.openrndr.application
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.math.Polar

fun main() = application {
    program {
        extend(TemporalBlur()) {
            samples = 10
            duration = 0.9
        }
        extend {
            drawer.circle(Polar(seconds * 360.0, 200.0).cartesian + drawer.bounds.center, 50.0)
        }
    }
}