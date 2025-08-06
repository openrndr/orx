import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.AxisHelper
import org.openrndr.extra.camera.GridHelper
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.OrbitalControls
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3

fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        val camera = OrbitalCamera(
            Vector3.UNIT_Z * 90.0, Vector3.ZERO, 90.0, 0.1, 5000.0
        )
        val controls = OrbitalControls(camera, keySpeed = 10.0)
        controls.userInteraction = false

        val sphere = sphereMesh(radius = 25.0)
        val cube = boxMesh(20.0, 20.0, 5.0, 5, 5, 2)

        extend(camera)

        // shows XYZ axes as RGB lines
        extend(AxisHelper())

        // debug ground plane
        extend(GridHelper(100))

        // adds mouse and keyboard bindings
        extend(controls)

        extend {
            // mouse and keyboard input can be toggled on and off
            controls.userInteraction = true

            drawer.vertexBuffer(sphere, DrawPrimitive.LINE_LOOP)
            drawer.vertexBuffer(cube, DrawPrimitive.LINE_LOOP)

            drawer.stroke = if(controls.userInteraction) ColorRGBa.GREEN else ColorRGBa.WHITE
            drawer.fill = null

            repeat(10) {
                drawer.translate(0.0, 0.0, 10.0)
                // Note: 2D primitives are not optimized for 3D and can
                // occlude each other
                drawer.circle(0.0, 0.0, 50.0)
            }
        }
    }
}