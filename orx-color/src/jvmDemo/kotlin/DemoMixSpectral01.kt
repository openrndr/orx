import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {

            extend {

                for (j in 0 until 60) {
                    val p = 3.0
                    val a = ColorRGBa.BLUE.shiftHue<OKHSV>(j * p)
                    val b = ColorRGBa.BLUE.shiftHue<OKHSV>(j * p + 180.0)

                    drawer.isolated {
                        for (i in 0 until 60) {

                            val c = mixSpectral(a, b, i / 59.0, 0.0, 0.0).toSRGB()
                            drawer.fill = c
                            drawer.stroke = null
                            drawer.rectangle(0.0, 0.0, width/60.0, 0.5 * height/60.0)

                            drawer.fill = a.mix(b, i / 59.0)
                            drawer.rectangle(0.0, 0.5 * height, width/60.0, 0.5 * height/60.0)

                            drawer.translate(width/60.0, 0.0)

                        }
                    }
                    drawer.translate(0.0, 0.5 * height/60.0)
                }
            }
        }
    }
}