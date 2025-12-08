import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.temporalblur.TemporalBlur
import org.openrndr.math.Polar
import kotlin.math.cos

/**
 * A more advance demonstration of `TemporalBlur`,
 * showing how a function can be used to apply a varying
 * `colorMatrix` transformation across the sampling
 * duration.
 *
 * This example makes the samples fade
 * from white to blue in each frame.
 *
 * The `gain` parameter is used to increase the brightness
 * of the resulting pixels.
 */
fun main() = application {
    program {
        extend(TemporalBlur()) {
            samples = 100
            duration = 10.0
            colorMatrix = {
                // `it` is 0.0 at start of frame, 1.0 at end of frame
                tint(ColorRGBa.WHITE.mix(ColorRGBa.BLUE, it))
            }
            gain = 1.2
        }

        extend {
            for (i in 0 until 10) {
                drawer.circle(
                    Polar(
                        seconds * 760.0 + i * 30,
                        140.0 + cos(i + seconds) * 40.0
                    ).cartesian + drawer.bounds.center,
                    50.0
                )
            }
        }
    }
}