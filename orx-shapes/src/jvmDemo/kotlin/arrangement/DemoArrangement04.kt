package arrangement

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.saturate
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.shapes.arrangement.Arrangement
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.sqrt
import kotlin.random.Random

fun main() {
    application {
        program {
            val circles = listOf(
                Circle(drawer.bounds.center -Vector2(50.0, 0.0), 50.0),
                Circle(drawer.bounds.center + Vector2(50.0, 0.0),  50.0),
                Circle(drawer.bounds.center + Vector2(0.0, 50.0),  50.0),
                Circle(drawer.bounds.center - Vector2(0.0, 50.0),  50.0),
                Circle(drawer.bounds.center -Vector2(50.0, 0.0), sqrt(50.0*50.0+50.0*50.0)-49.9),
                Circle(drawer.bounds.center +Vector2(50.0, 0.0), sqrt(50.0*50.0+50.0*50.0)-49.9),
                Circle(drawer.bounds.center -Vector2(0.0, 50.0), sqrt(50.0*50.0+50.0*50.0)-49.9),
                Circle(drawer.bounds.center +Vector2(0.0, 50.0), sqrt(50.0*50.0+50.0*50.0)-49.9),
            ).shuffled()

            val  arr = Arrangement(circles)

            extend {
                val r = Random(100)
                drawer.stroke = ColorRGBa.WHITE
                for (f in arr.boundedFaces) {
                    drawer.fill =

                        rgb(Double.uniform(0.0, 1.0, r), Double.uniform(0.0, 1.0, r), Double.uniform(0.0, 1.0, r)).saturate<OKHSV>(0.25)
                    drawer.contour(f.contour)
                }
            }

        }
    }
}