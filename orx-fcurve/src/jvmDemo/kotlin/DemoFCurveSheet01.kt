import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.fcurve.efcurve
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val fcurveTexts = listOf(
            //"(l 35.0 25.0 h {175-35})[4]", // linear steps
            "(c 33% 0% 67% 67% 35.0 25.0 h {175-35})[4]", // ease-in steps
            "(c 50% 50% 50% 100% 35.0 25.0 h {175-35})[4]", // ease-out steps
            "(c 50% 0% 50% 100% 35.0 25.0 h {175-35})[4]", // ease-in-out steps
            "(c 95% 0% 100% 100% 35.0 25.0 h {175-35})[4]",  // arc-in steps
            "(c 0% 0% 5% 100% 35.0 25.0 h {175-35})[4]", // arc-out steps
            "(c 95% 0% 100% 100% 17.5 12.5 c 0% 0% 5% 100% 17.5 12.5 h {175-35})[4]", // arc-out steps
        )

        val fcurves = fcurveTexts.map { fcurve(efcurve(it)) }

        extend {
            drawer.clear(ColorRGBa.WHITE)

            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
            drawer.translate(10.0, 20.0)

            drawer.stroke = ColorRGBa.PINK
            drawer.lineSegment(mouse.position.x - 10.0, 0.0, mouse.position.x - 10.0, height * 1.0)

            fun color(i: Int): ColorRGBa =
                ColorRGBa.BLUE.toHSVa().shiftHue(i * 30.0).saturate(0.5).shade(0.9).toRGBa()

            for (i in fcurveTexts.indices) {
                drawer.fill = color(i)
                drawer.text(fcurveTexts[i], 0.0, 120.0)

                drawer.stroke = color(i).opacify(0.25)
                drawer.lineSegment(0.0, 100.0, width - 20.0, 100.0)

                drawer.stroke = color(i)
                val y = 100.0 - fcurves[i].value(mouse.position.x - 10.0)
                drawer.contours(fcurves[i].contours(offset = Vector2(0.0, 100.0)))
                drawer.circle(mouse.position.x - 10.0, y, 10.0)

                drawer.translate(0.0, 110.0)
            }
        }
    }
}