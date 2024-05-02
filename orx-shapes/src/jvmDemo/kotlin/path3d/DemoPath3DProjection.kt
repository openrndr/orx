package path3d

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.path3d.projectToContour
import org.openrndr.math.Vector3
import org.openrndr.shape.path3D

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val path = path3D {
                var p = Vector3(1.0, 0.0, 0.0)
                moveTo(p * 6.0)
                for (i in 0 until 400) {
                    p += Vector3.uniformRing(0.2, 0.5)
                    p = p.normalized
                    arcTo(5.0, cursor.atan2(p * 6.0), false, false, p * 6.0)
                }
            }
            extend(Orbital())
            extend {
                drawer.stroke = ColorRGBa.PINK
                drawer.path(path)
                val c = path.projectToContour(drawer.projection, drawer.view, width, height)
                drawer.defaults()
                drawer.stroke = ColorRGBa.WHITE
                drawer.lineJoin = LineJoin.ROUND
                drawer.strokeWeight = 2.0
                drawer.contour(c)
            }
        }
    }
}