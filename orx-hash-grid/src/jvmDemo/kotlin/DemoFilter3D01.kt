import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.hashgrid.filter
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Demonstrates how to use a 3D hash-grid `filter` operation to remove points from a random 3D point-collection
 * that are too close to each other. The resulting points are displayed as small spheres.
 *
 * The program performs the following key steps:
 * - Generates 10,000 random 3D points located between a minimum and maximum radius.
 * - Filters the points to ensure a minimum distance between any two points using a spatial hash grid.
 * - Creates a small sphere mesh that will be instanced for each filtered point.
 * - Sets up an orbital camera to allow viewing the 3D scene interactively.
 * - Renders the filtered points by translating the sphere mesh to each point's position and applying a shader that modifies the fragment color based on the view normal.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val r = Random(0)
        val points = (0 until 10000).map {
            Vector3.uniformRing(0.0, 10.0, r)
        }
        val sphere = sphereMesh(radius = 0.25)
        val filteredPoints = points.filter(0.5)

        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 15.0)
        }
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """x_fill.rgb *= abs(v_viewNormal.z);"""
            }
            for (point in filteredPoints) {
                drawer.isolated {
                    drawer.translate(point)
                    drawer.vertexBuffer(sphere, DrawPrimitive.TRIANGLES)
                }
            }
        }
    }
}