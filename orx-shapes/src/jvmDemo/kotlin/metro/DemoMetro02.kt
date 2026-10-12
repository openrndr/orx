package metro

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.metro.metro
import org.openrndr.extra.shapes.ordering.hilbertOrder
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {

            backgroundColor = ColorRGBa.WHITE

            val pts = drawer.bounds.scatter(20.0, random = Random(1)).hilbertOrder()
            val curve = hobbyCurve(pts, false)

            val metro = curve.metro(distanceTolerance = 4.0)
            extend(Camera2D())
            extend {
                drawer.contour(metro)
            }


        }
    }
}