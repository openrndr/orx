import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.spaces.OKLab
import org.openrndr.extra.color.tools.saturate
import org.openrndr.extra.color.tools.shadeLuminosity
import org.openrndr.extra.color.tools.shiftHue

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val a = ColorRGBa.BLUE.shiftHue<OKHSV>(0.0).saturate<OKHSV>(0.3).shadeLuminosity<OKLab>(0.3)
            val b = ColorRGBa.BLUE.shiftHue<OKHSV>(60.0).saturate<OKHSV>(0.8)

            drawer.isolated {
                for (i in 0 until 60) {
                    val c = mixSpectral(a, b, i / 59.0, 0.0, 0.0).toSRGB()
                    drawer.fill = c
                    drawer.stroke = null
                    drawer.rectangle(0.0, 0.0, width / 60.0, 1.0 * height)

                    drawer.translate(width / 60.0, 0.0)

                }
            }
            drawer.translate(0.0, 0.5 * height / 60.0)
        }
    }
}
