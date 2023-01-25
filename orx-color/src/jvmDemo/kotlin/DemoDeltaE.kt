import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.toOKHSVa
import org.openrndr.extra.color.statistics.deltaE76
import org.openrndr.math.Polar

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
                for (i in 10 until 270 step 10) {
                    drawer.circle(drawer.bounds.center, i.toDouble())
                }

                drawer.stroke = null

                val startColor = ColorRGBa.RED.toOKHSVa().shiftHue(seconds*36.0).toRGBa()
                drawer.circles {
                    for (j in 99 downTo 0) {
                        for (i in 0 until 360 step 10) {
                            val color = startColor.toOKHSVa().shiftHue(i.toDouble()).saturate(j / 99.0).toRGBa()
                            val distance = color.deltaE76(startColor)
                            val p = Polar(seconds * 36.0 + i.toDouble(), distance).cartesian + drawer.bounds.center
                            fill = color
                            circle(p, 2.0)
                        }
                    }
                }
            }
        }
    }
}