package kernel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.kernel.findKernel
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val p2d = regularStar(9, 130.0, 190.0, center = drawer.bounds.center, phase = 180.0).contour.segments.map { it.start }
            
            // Transform to 3D
            val m = transform {
                translate(drawer.bounds.center.xy0)
                rotate(Vector3.UNIT_X, 45.0)
                rotate(Vector3.UNIT_Y, 45.0)
                translate(-drawer.bounds.center.xy0)
            }
            
            val p3d = p2d.map { (m * it.xy01).xyz }
            val k3d = findKernel(p3d)
            
            extend {
                drawer.clear(ColorRGBa.PINK)
                drawer.fill = null
                drawer.stroke = ColorRGBa.BLACK
                drawer.lineLoop(p3d.map { it.xy }) // Simple projection for visualization
                
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineLoop(k3d.map { it.xy })
            }
        }
    }
}
