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
 * Demonstrates filters and blend modes in 3D with [composition3d]: a blur, a drop shadow, a hue rotation of a group,
 * text displaced by turbulence, and circles blended by multiply, screen and difference. A node with filters or a blend
 * mode is drawn flat on a layer of the resolution it has on the screen, which is drawn in the plane, so it turns in
 * perspective with the rest. The composition turns slowly; space shows it in 2D, as [composition] draws it, to compare.
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
                <filter id="blur"><feGaussianBlur stdDeviation="6"/></filter>
                <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
                  <feDropShadow dx="8" dy="10" stdDeviation="5" flood-color="#000000" flood-opacity="0.7"/>
                </filter>
                <filter id="hue"><feColorMatrix type="hueRotate" values="140"/></filter>
                <filter id="wobble" x="-10%" y="-10%" width="120%" height="120%">
                  <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="2" result="noise"/>
                  <feDisplacementMap in="SourceGraphic" in2="noise" scale="18" xChannelSelector="R" yChannelSelector="G"/>
                </filter>
              </defs>
              <rect x="0" y="0" width="600" height="400" fill="#f4efe6"/>
              <circle cx="90" cy="100" r="50" fill="#5ab0ff" filter="url(#blur)"/>
              <rect x="180" y="50" width="110" height="100" rx="12" fill="#ffe08a" filter="url(#shadow)"/>
              <g filter="url(#hue)">
                <circle cx="390" cy="100" r="50" fill="#f88379"/>
                <circle cx="430" cy="100" r="30" fill="#5ab0ff"/>
              </g>
              <text x="470" y="115" font-family="Plex" font-size="42" fill="#20242c" filter="url(#wobble)">wob</text>
              <g>
                <circle cx="120" cy="290" r="70" fill="#5ab0ff"/>
                <circle cx="190" cy="290" r="70" fill="#f88379" style="mix-blend-mode: multiply"/>
                <circle cx="330" cy="290" r="70" fill="#7fd1b9"/>
                <circle cx="400" cy="290" r="70" fill="#c49bff" style="mix-blend-mode: screen"/>
                <rect x="470" y="220" width="100" height="140" fill="#20242c"/>
                <circle cx="520" cy="290" r="55" fill="#ffe08a" style="mix-blend-mode: difference"/>
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
