package loft

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.DepthTestPass
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.functions.poseFunction
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.extra.shapes.loft.Loft
import org.openrndr.extra.shapes.loft.loft
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.shape.Ellipse
import org.openrndr.shape.LineSegment3D
import org.openrndr.shape.Segment3D
import kotlin.math.cos

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Screenshots())
        extend(Orbital())


        val pts = List(8) {
            Vector3.uniform(-4.0, 4.0)
        }
        val path = hobbyCurve(pts, true)
        val pose = PosePath3D(path.rectified(distanceTolerance = 0.1, lengthScale = 70.0), Vector3.UNIT_Y)

        val crossSection = { u:Double, v: Double ->
            Ellipse(0.0, 0.0, 0.1, 0.1).contour.transform(
                transform {
                    rotate(v * 360.0 * 10.0)
                }).rectified().position(u)
        }

        //val loft = crossSection.loft(pose)
        val loft = Loft(crossSection = crossSection, pose = pose.poseFunction)

        val points = (0 until 4000).map {
            val u = (it/2.0).mod(1.0)
            val v = it / 4000.0
            loft.position(u, v) to loft.normal(u, v).normalized

        }
        val segments = points.map { Segment3D(it.first, it.first + it.second * 3.0) }
        extend {
            drawer.drawStyle.depthTestPass = DepthTestPass.ALWAYS
            drawer.drawStyle.blendMode = BlendMode.ADD
            drawer.strokeWeight = 0.25
            drawer.stroke = ColorRGBa.CYAN.shade(0.25)
            //  drawer.path(path)
            drawer.segments(segments)

        }
    }
}