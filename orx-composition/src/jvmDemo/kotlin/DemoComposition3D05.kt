import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.composition3d
import org.openrndr.extra.svg.parseSVG
import org.openrndr.math.Vector3
import kotlin.math.sin

/**
 * Demonstrates markers in 3D with [composition3d]: arrowheads that turn with the path and the reversed start of
 * auto-start-reverse, dots at the vertices of a polyline that take the stroke of the line as context-stroke, and flags
 * in user units that take the fill of the path as context-fill. The content of the markers is drawn in 3D like the
 * rest of the composition. The composition turns slowly; space shows it in 2D, as [composition] draws it, to compare.
 */
fun main() = application {
    configure {
        width = 900
        height = 600
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = parseSVG(
            """
            <svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400">
              <defs>
                <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="4" markerHeight="4" orient="auto-start-reverse">
                  <path d="M0 0 L10 5 L0 10 z" fill="context-stroke"/>
                </marker>
                <marker id="dot" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="3" markerHeight="3">
                  <circle cx="5" cy="5" r="4" fill="#ffffff" stroke="context-stroke" stroke-width="2"/>
                </marker>
                <marker id="flag" viewBox="0 0 20 20" refX="0" refY="20" markerUnits="userSpaceOnUse" markerWidth="30" markerHeight="30" orient="0">
                  <path d="M0 0 V20 M0 0 L16 5 L0 10" fill="context-fill" stroke="#20242c" stroke-width="1.5"/>
                </marker>
              </defs>
              <rect x="0" y="0" width="600" height="400" fill="#20242c"/>
              <path d="M60 120 C 160 20, 260 220, 360 120 S 520 60, 540 160" fill="none" stroke="#5ab0ff" stroke-width="6"
                    marker-start="url(#arrow)" marker-end="url(#arrow)"/>
              <polyline points="60,300 160,230 260,320 360,240 460,310 540,250" fill="none" stroke="#f88379" stroke-width="5"
                        marker-start="url(#dot)" marker-mid="url(#dot)" marker-end="url(#arrow)"/>
              <path d="M100 360 L250 360 L400 380" fill="none" stroke="#7fd1b9" stroke-width="3" fill-opacity="1"
                    style="fill: #ffe08a" marker-mid="url(#flag)" marker-end="url(#flag)"/>
            </svg>
            """.trimIndent()
        )
        var flat = false
        val orbital = extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 520.0)
            lookAt = Vector3.ZERO
        }
        // the camera is switched between frames, it sets up the view before the frame and undoes it after it
        keyboard.keyDown.listen {
            if (it.name == "space") {
                flat = !flat
                orbital.enabled = !flat
            }
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#101216"))
            if (flat) {
                drawer.isolated {
                    drawer.defaults()
                    drawer.ortho()
                    drawer.view = org.openrndr.math.Matrix44.IDENTITY
                    drawer.model = org.openrndr.math.Matrix44.IDENTITY
                    drawer.translate((width - 600.0) / 2.0, (height - 400.0) / 2.0)
                    drawer.composition(composition)
                }
            } else {
                drawer.rotate(Vector3.UNIT_Y, sin(seconds * 0.5) * 45.0)
                drawer.translate(-300.0, 200.0, 0.0)
                drawer.scale(1.0, -1.0, 1.0)
                drawer.composition3d(composition)
            }
        }
    }
}
