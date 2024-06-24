import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.jumpfill.ClusteredField
import org.openrndr.extra.jumpfill.DecodeMode
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.noise.uniformRing
import org.openrndr.math.Vector2

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val rt = renderTarget(720, 720, 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
        }
        val flowfield = colorBuffer(width, height, type = ColorType.FLOAT32)
        val cluster = ClusteredField(decodeMode = DecodeMode.DISTANCE, outputDistanceToContours = true)

        cluster.normalizedDistance = true

        extend {
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa(-1.0, -1.0, -1.0, 0.0))
                val points = drawer.bounds.scatter(20.0)
                drawer.points {
                    for ((index, point) in points.withIndex()) {
                        fill = ColorRGBa((index+1.0)/points.size, 0.0, 0.0, 1.0)
                        for (i in 0 until 30) {
                            point(point + Vector2.uniformRing(15.0, 25.0)* Vector2(1.0, 1.0))
                        }
                    }
                }
            }
            cluster.apply(rt.colorBuffer(0), flowfield)
            drawer.drawStyle.colorMatrix = tint(ColorRGBa(100.0, 100.0, 0.0))
            drawer.image(flowfield)
        }
    }
}