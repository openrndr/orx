import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DepthTestPass
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.composition.CompositionFonts
import org.openrndr.extra.composition.composition3d
import org.openrndr.extra.svg.parseSVG
import org.openrndr.math.Vector3
import kotlin.math.sin

/**
 * Demonstrates drawing a composition in 3D with [composition3d]: its fills and strokes are tessellated into triangles
 * in the plane of the composition, so it can be turned and seen in perspective with an orbital camera, with depth
 * testing. The composition has fills with holes, strokes with miter, round and bevel joins and butt, round and square
 * caps, a dashed stroke, a linear gradient, translucent strokes that overlap themselves, a translucent stroke with round joins
 * and caps that keeps its width on the screen, and text.
 *
 * The composition turns slowly; drag to turn the camera, scroll to zoom.
 */
fun main() = application {
    configure {
        width = 960
        height = 640
        // the edges of the triangles are smoothed by multisampling
        multisample = WindowMultisample.SampleCount(4)
    }
    program {
        CompositionFonts.register("Plex", "demo-data/fonts/IBMPlexMono-Regular.ttf")
        val composition = parseSVG(
            """
            <svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400">
              <defs>
                <linearGradient id="g" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0" stop-color="#5ab0ff"/>
                  <stop offset="1" stop-color="#f88379"/>
                </linearGradient>
              </defs>
              <rect x="0" y="0" width="600" height="400" fill="#20242c"/>
              <path d="M40 40 h160 v160 h-160 z M80 80 v80 h80 v-80 z" fill="url(#g)" fill-rule="evenodd"/>
              <polyline points="240,180 280,60 320,180" fill="none" stroke="#ffe08a" stroke-width="18" stroke-linejoin="miter" stroke-linecap="butt"/>
              <polyline points="340,180 380,60 420,180" fill="none" stroke="#ffe08a" stroke-width="18" stroke-linejoin="round" stroke-linecap="round"/>
              <polyline points="440,180 480,60 520,180" fill="none" stroke="#ffe08a" stroke-width="18" stroke-linejoin="bevel" stroke-linecap="square"/>
              <circle cx="120" cy="300" r="60" fill="#7fd1b9" stroke="#ffffff" stroke-width="6" stroke-dasharray="14 8"/>
              <path d="M240 250 C 300 380, 360 200, 420 330 S 520 260, 560 340" fill="none" stroke="#c49bff" stroke-width="28" stroke-opacity="0.5"/>
              <polyline points="240,370 330,345 420,375 510,340 570,372" fill="none" stroke="#ffffff" stroke-opacity="0.7" stroke-width="5" stroke-linejoin="round" stroke-linecap="round" vector-effect="non-scaling-stroke"/>
              <text x="40" y="385" font-family="Plex" font-size="24" fill="#ffffff">composition3d</text>
            </svg>
            """.trimIndent()
        )
        extend(Orbital()) {
            eye = Vector3(0.0, 120.0, 520.0)
            lookAt = Vector3(0.0, 0.0, 0.0)
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#101216"))
            drawer.drawStyle.depthWrite = true
            drawer.drawStyle.depthTestPass = DepthTestPass.LESS_OR_EQUAL
            // the composition turns slowly, it stands in the xy-plane, its center at the origin, its y up
            drawer.rotate(Vector3.UNIT_Y, sin(seconds * 0.5) * 50.0)
            drawer.translate(-300.0, 200.0, 0.0)
            drawer.scale(1.0, -1.0, 1.0)
            drawer.composition3d(composition)
        }
    }
}

