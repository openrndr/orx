import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.color.spaces.ColorOKHSLa
import org.openrndr.extra.color.spaces.ColorOKHSVa

fun main() = application {
    configure {
        width = 720
        height = 160
    }
    program {
        extend {
            drawer.clear(rgb(0.2))

            val c = ColorRGBa.GREEN
            val okhsv = ColorOKHSVa.fromColorRGBa(c)
            val hsv = c.toHSVa()
            val hsl = c.toHSLa()
            val okhsl = ColorOKHSLa.fromColorRGBa(c)

            for (i in 0 until 36) {
                drawer.fill = okhsv.shiftHue(i * 10.0).saturate(1.0).toRGBa()
                drawer.rectangle(i * 10.0, 40.0, 10.0, 10.0)
                drawer.fill = hsv.shiftHue(i * 10.0).saturate(1.0).toRGBa()
                drawer.rectangle(i * 10.0, 60.0, 10.0, 10.0)

                drawer.fill = okhsl.shiftHue(i * 10.0).saturate(1.0).toRGBa()
                drawer.rectangle(i * 10.0, 80.0, 10.0, 10.0)
                drawer.fill = hsl.shiftHue(i * 10.0).saturate(1.0).toRGBa()
                drawer.rectangle(i * 10.0, 100.0, 10.0, 10.0)

            }
        }
    }
}