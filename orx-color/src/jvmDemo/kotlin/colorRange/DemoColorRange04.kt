package colorRange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.palettes.rangeTo
import org.openrndr.extra.color.spaces.toHSLUVa
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.color.spaces.toOKLCHa
import org.openrndr.extra.color.spaces.toXSLUVa
import org.openrndr.extra.meshgenerators.sphereMesh
import org.openrndr.math.Vector3

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val mesh = sphereMesh(8, 8, radius = 0.1)

        extend(Orbital())

        extend {
            drawer.clear(ColorRGBa.WHITE)

            val colorA = ColorRGBa.BLUE.toHSVa().shiftHue(seconds * 40.0).toRGBa()
            val colorB = ColorRGBa.PINK.toHSVa().shiftHue(-seconds * 34.0).toRGBa()

            val stepCount = 25

            val allSteps = listOf(
                "RGB" to (colorA..colorB blend stepCount),
                "RGB linear" to (colorA.toLinear()..colorB.toLinear() blend stepCount),
                "HSV" to (colorA..colorB.toHSVa() blend stepCount),
                "Lab" to (colorA.toLABa()..colorB.toLABa() blend stepCount),
                "LCh(ab)" to (colorA.toLCHABa()..colorB.toLCHABa() blend stepCount),
                "OKLab" to (colorA.toOKLABa()..colorB.toOKLABa() blend stepCount),
                "OKLCh" to (colorA.toOKLCHa()..colorB.toOKLCHa() blend stepCount),
                "HSLUV" to (colorA.toHSLUVa()..colorB.toHSLUVa() blend stepCount),
                "XSLUV" to (colorA.toXSLUVa()..colorB.toXSLUVa() blend stepCount),
            )

            drawer.stroke = null

            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            for ((_, steps) in allSteps) {
                for (i in steps.indices) {
                    val srgb = steps[i].toSRGB().clip()
                    drawer.fill = srgb
                    drawer.isolated {
                        drawer.translate((srgb.r - 0.5) * 10.0, (srgb.g - 0.5) * 10.0, (srgb.b - 0.5) * 10.0)
                        drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
                    }
                }
                val positions = steps.map {
                    val l = it.toSRGB().clip()
                    Vector3((l.r - 0.5) * 10.0, (l.g - 0.5) * 10.0, (l.b - 0.5) * 10.0)
                }
                drawer.stroke = ColorRGBa.BLACK.opacify(0.25)
                drawer.strokeWeight = 10.0
                drawer.lineStrip(positions)
            }
            drawer.lineSegments(
                listOf(
                    Vector3(-5.0, -5.0, -5.0), Vector3(5.0, -5.0, -5.0),
                    Vector3(-5.0, -5.0, 5.0), Vector3(5.0, -5.0, 5.0),
                    Vector3(-5.0, 5.0, -5.0), Vector3(5.0, 5.0, -5.0),
                    Vector3(-5.0, 5.0, 5.0), Vector3(5.0, 5.0, 5.0),

                    Vector3(-5.0, -5.0, -5.0), Vector3(-5.0, 5.0, -5.0),
                    Vector3(5.0, -5.0, -5.0), Vector3(5.0, 5.0, -5.0),
                    Vector3(-5.0, -5.0, 5.0), Vector3(-5.0, 5.0, 5.0),
                    Vector3(5.0, -5.0, 5.0), Vector3(5.0, 5.0, 5.0),

                    Vector3(-5.0, -5.0, -5.0), Vector3(-5.0, -5.0, 5.0),
                    Vector3(5.0, -5.0, -5.0), Vector3(5.0, -5.0, 5.0),
                    Vector3(-5.0, 5.0, -5.0), Vector3(-5.0, 5.0, 5.0),
                    Vector3(5.0, 5.0, -5.0), Vector3(5.0, 5.0, 5.0),
                )
            )
        }
    }
}
