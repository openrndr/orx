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
import org.openrndr.shape.Segment3D

/**
 * Demonstrates the use of the `RectifiedContour.loft()` method.
 *
 * The program creates a list with random 3D points and uses `hobbyCurve` to
 * convert them to a `Path3D` and a `PosePath3D`, needed by the `loft()` method.
 *
 * Next, the program creates a circular cross-section (a RectifiedContour).
 *
 * The `loft()` method does not produce a mesh or anything drawable, but a
 * data structure that we can query at (u, v) coordinates to produce a mesh
 * or another drawable item.
 *
 * In this case, a list containing `Segment3D` instances is produced.
 *
 * Finally, the core path and the segments are rendered using an interactive
 * 3D Orbital camera.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Orbital())

        // Create the core curve
        val pts = List(14) {
            Vector3.uniform(-4.0, 4.0)
        }
        val path = hobbyCurve(pts, true)
        val pose = PosePath3D(path.rectified(lengthScale = 10.0), Vector3.UNIT_Y)

        // A cross-section to move along the core
        val crossSection = Circle(0.0, 0.0, 0.2).contour.rectified()

        // A queryable loft
        val loft = crossSection.loft(pose)

        // Creates a List<Pair<Vector3,Vector3>> containing positions and
        // scaled down normals by querying the loft at various locations
        val points = (0 until 3000).map {
            val u = (it / 30.0).mod(1.0)
            val v = it / 3000.0
            loft.position(u, v) to loft.normal(u, v).normalized * 0.1

        }

        // Create segments along the core curve and pointing away from it
        val segments = points.map { Segment3D(it.first, it.first + it.second) }

        extend {
            drawer.stroke = ColorRGBa.GREEN
            drawer.path(path)
            drawer.segments(segments)
        }
    }
}