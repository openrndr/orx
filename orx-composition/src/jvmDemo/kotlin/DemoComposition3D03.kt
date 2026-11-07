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
 * Demonstrates clip paths and masks in 3D with [composition3d]: a clip path with a hole, a clip in the bounds of a
 * group, clips in clips, a luminance mask with a gradient that fades stripes out, and an alpha mask of text. The clips
 * and masks are rendered with the transforms of the composition, so they clip in perspective. The composition turns
 * slowly; space shows it in 2D, as [composition] draws it, to compare.
 */
fun main() = application {
    configure {
        width = 900
        height = 600
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val stripes = (0 until 30).joinToString("") { i ->
            """<rect x="${i * 20}" y="0" width="10" height="400" fill="${if (i % 2 == 0) "#5ab0ff" else "#f88379"}"/>"""
        }
        val composition = parseSVG(
            """
            <svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400">
              <defs>
                <clipPath id="ring" clip-rule="evenodd">
                  <path d="M40 40 h160 v160 h-160 z M80 80 v80 h80 v-80 z" clip-rule="evenodd"/>
                </clipPath>
                <clipPath id="disc" clipPathUnits="objectBoundingBox">
                  <circle cx="0.5" cy="0.5" r="0.5"/>
                </clipPath>
                <clipPath id="band">
                  <rect x="230" y="90" width="200" height="60"/>
                </clipPath>
                <linearGradient id="fade" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0" stop-color="#ffffff"/>
                  <stop offset="1" stop-color="#000000"/>
                </linearGradient>
                <mask id="fadeMask">
                  <rect x="40" y="240" width="300" height="120" fill="url(#fade)"/>
                </mask>
                <mask id="textMask" style="mask-type: alpha">
                  <text x="370" y="320" font-family="Plex" font-size="64" fill="#000000">mask</text>
                </mask>
              </defs>
              <rect x="0" y="0" width="600" height="400" fill="#20242c"/>
              <g clip-path="url(#ring)">$stripes</g>
              <g clip-path="url(#band)">
                <g clip-path="url(#disc)">
                  <rect x="250" y="40" width="160" height="160" fill="#ffe08a"/>
                  <rect x="250" y="40" width="80" height="80" fill="#7fd1b9"/>
                </g>
              </g>
              <g mask="url(#fadeMask)">$stripes</g>
              <g mask="url(#textMask)">
                <rect x="360" y="250" width="220" height="90" fill="url(#fade)"/>
                <rect x="360" y="250" width="220" height="90" fill="#c49bff" fill-opacity="0.8"/>
              </g>
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
