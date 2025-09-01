import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.OrbitalManual
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3

/**
 * Demonstrate the use of an orbital camera to render a sphere and a cube in 3D space as wireframe meshes, positioned
 * and rendered independently using the camera's isolated drawing state. A stationary pink circle is also drawn in the
 * center of the scene.
 *
 * Functionality:
 * - Initializes a sphere mesh and a cube mesh with predefined dimensions.
 * - Spawns an orbital camera, initially positioned away from the origin, to allow for focused rendering.
 * - Renders 3D wireframe shapes (sphere and cube) using the camera's isolated perspective.
 * - Draws a static 2D pink circle overlay at the window center.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }

    program {
        val sphere = sphereMesh(radius = 25.0)
        val cube = boxMesh(20.0, 20.0, 5.0, 5, 5, 2)

        val camera = OrbitalManual()
        camera.camera.rotateTo(Vector3(0.0, 0.0, 30.0), instant = true)
        extend {
            camera.isolated {
                drawer.fill = ColorRGBa.WHITE
                drawer.vertexBuffer(sphere, DrawPrimitive.LINE_LOOP)
            }

            drawer.fill = ColorRGBa.PINK
            drawer.circle(drawer.bounds.center, 250.0)

            camera.isolated {
                drawer.fill = ColorRGBa.WHITE
                drawer.vertexBuffer(cube, DrawPrimitive.LINE_LOOP)
            }
        }
    }
}