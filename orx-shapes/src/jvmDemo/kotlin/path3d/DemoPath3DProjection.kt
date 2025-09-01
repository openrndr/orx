package path3d

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineJoin
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shapes.path3d.projectToContour
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.shape.path3D

fun main() = application {
    program {
        val path = path3D {
            var p = Vector3(6.0, 0.0, 0.0)
            moveTo(p)
            for (i in 0 until 100) {
                p = Spherical((i % 6) * 45.0, (i % 4 + 1) * 30.0 + i * 0.1, 6.0).cartesian
                arcTo(5.0, cursor.atan2(p), false, false, p)
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
