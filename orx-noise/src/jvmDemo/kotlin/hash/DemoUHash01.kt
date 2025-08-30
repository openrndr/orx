package hash

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.fhash3D

/**
 * Demonstrates how to render a dynamic grid of points where the color of each point
 * is determined using a hash-based noise generation method.
 *
 * The application dynamically updates the visual output by calculating a 3D hash
 * value for each point in the grid, based on the current time and the point's coordinates.
 * The hash value is then used to determine the grayscale color intensity of each point.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        extend {
            drawer.points {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val c = fhash3D(seed = 100, x = x + (seconds * 60.0).toInt(), y, z = 0)
                        //val u = uhash11(x.toUInt()).toDouble() / UInt.MAX_VALUE.toDouble()
                        fill = ColorRGBa(c, c, c, 1.0)
                        point(x.toDouble(), y.toDouble())
                    }
                }
            }
        }
    }
}
