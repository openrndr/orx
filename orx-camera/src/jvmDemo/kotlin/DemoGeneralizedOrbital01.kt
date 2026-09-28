import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.GeneralizedOrbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.extra.remotecontrol.RemoteControl
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

/**
 * Demonstrate the use of `GeneralizedOrbital`, an interactive 3D camera -- combining what
 * `Orbital` splits across `OrbitalCamera`/`OrbitalControls` into one extension -- whose
 * projection comes from `generalizedProjection` rather than `Drawer.perspective`/`.ortho`.
 *
 * Press `p` to blend the projection from perspective towards parallel (orthographic) and back;
 * unlike `Orbital`, this is one continuous knob instead of a `ProjectionType` switch, and --
 * like the orbit's rotation/pan/zoom -- it eases smoothly rather than snapping.
 *
 * Press `z` for a dolly zoom (Vertigo effect): the eye dollies to a new distance while `fov`
 * is compensated so the sphere at `lookAt` keeps the same apparent size -- only the receding
 * ring of circles, at a fixed depth independent of the eye, visibly stretches or compresses.
 *
 * Press `o` to shift `offset`, shearing the frustum off-axis (a lens-shift look) without
 * moving or rotating the eye -- the scene slides across the frame rather than the camera
 * panning through it.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        val sphere = sphereMesh(radius = 25.0)
        val cube = boxMesh(20.0, 20.0, 5.0, 5, 5, 2)

        // `Orbital`'s own default eye distance (10.0) sits inside this radius-25 sphere; use
        // the same distance DemoOrbitalCamera01 does for a legible starting view.
        val orbital = GeneralizedOrbital(eye = Vector3.UNIT_Z * 90.0)
        extend(RemoteControl())

        extend(orbital)

        extend {
            drawer.vertexBuffer(sphere, DrawPrimitive.LINE_LOOP)
            drawer.vertexBuffer(cube, DrawPrimitive.LINE_LOOP)
            drawer.fill = null
            drawer.stroke = ColorRGBa.GREEN


            drawer.depthWrite = false
            repeat(10) {
                drawer.translate(0.0, 0.0, 10.0)
                // Note: 2D primitives are not optimized for 3D and can
                // occlude each other
                drawer.circle(0.0, 0.0, 50.0 + it * 10.0)
            }
            drawer.depthWrite = true
        }

        var dolliedOut = false
        var shifted = false
        keyboard.keyDown.listen {
            if (it.name == "p") {
                orbital.perspectiveTo(if (orbital.perspectiveEnd > 0.5) 0.0 else 1.0)
            }

            if (it.name == "l") {
                orbital.perspectiveTo(if (orbital.perspectiveEnd > 0.5) 0.0 else 1.0)
                orbital.rotateTo(0.0, 20.0)
            }

            if (it.name == "z") {
                dolliedOut = !dolliedOut
                orbital.dollyZoomTo(if (dolliedOut) 250.0 else 90.0)
            }
            if (it.name == "o") {
                shifted = !shifted
                orbital.offsetTo(if (shifted) Vector2(60.0, 30.0) else Vector2.ZERO)
            }
        }
    }
}
