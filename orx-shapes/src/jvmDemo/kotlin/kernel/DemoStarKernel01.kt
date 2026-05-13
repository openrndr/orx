package kernel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.kernel.findKernel
import org.openrndr.extra.shapes.primitives.regularStar

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val p = regularStar(9, 130.0, 190.0, center = drawer.bounds.center, phase = 180.0).contour.segments.map { it.start }
            val k = findKernel(p)
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = null
                drawer.lineLoop(p)
                drawer.lineLoop(k)


            }
        }
    }
}