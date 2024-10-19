import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.drawImage
import org.openrndr.extra.noise.*

fun main() {
    application {
        program {


            extend {
                drawer.points {
                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val c = fhash3D(100, x + (seconds*60.0).toInt(), y, (0).toInt())
                            //val u = uhash11(x.toUInt()).toDouble() / UInt.MAX_VALUE.toDouble()
                            fill = ColorRGBa(c, c, c, 1.0)
                            point(x.toDouble(), y.toDouble())
                        }
                    }
                }

            }
        }
    }
}