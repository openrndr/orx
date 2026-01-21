package loft

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.loft.loft
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector3
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment3D
import org.openrndr.shape.Segment3D

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Orbital())

        val pts = List(14) {
            Vector3.uniform(-4.0, 4.0)
        }
        val path = hobbyCurve(pts, true)
        val pose = PosePath3D(path.rectified(lengthScale = 10.0), Vector3.UNIT_Y)

        val crossSection = Circle(0.0, 0.0, 0.2).contour.rectified()

        val loft = crossSection.loft(pose)

        val points = (0 until 3000).map {
            val u = (it / 30.0).mod(1.0)
            val v = it / 3000.0
            loft.position(u, v) to loft.normal(u, v).normalized * 0.1

        }
        val segments = points.map { Segment3D(it.first, it.first + it.second) }
        extend {
            drawer.stroke = ColorRGBa.GREEN
              drawer.path(path)
            drawer.segments(segments)

        }
    }
}