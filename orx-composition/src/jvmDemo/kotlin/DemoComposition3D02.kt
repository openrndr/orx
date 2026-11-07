import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition3d
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector3
import kotlin.math.sin

/**
 * Demonstrates text in 3D with [composition3d]: the glyphs of text are drawn as shapes, with gradients over the bounds
 * of a text element, a tspan with a gradient of its own, gradient strokes, and text along a path, like
 * DemoCompositionTextGradients01 draws them in 2D. The composition turns slowly; drag to turn the camera.
 */
fun main() = application {
    configure {
        width = 900
        height = 600
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = loadSVG("orx-composition/src/jvmDemo/resources/text-gradients.svg")
        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 500.0)
            lookAt = Vector3.ZERO
        }
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.rotate(Vector3.UNIT_Y, sin(seconds * 0.5) * 45.0)
            // the composition in the xy-plane, its center at the origin, its y up
            drawer.translate(-300.0, 200.0, 0.0)
            drawer.scale(1.0, -1.0, 1.0)
            drawer.composition3d(composition)
        }
    }
}
