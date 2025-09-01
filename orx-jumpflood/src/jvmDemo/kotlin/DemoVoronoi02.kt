import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.color.colormatrix.tint
import org.openrndr.extra.jumpfill.*
import kotlin.math.cos

fun main() = application {
    configure {
        width = 512
        height = 512
    }
    program {
        val rt = renderTarget(width, height, 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
        }

        val flowfield = colorBuffer(width, height, type = ColorType.FLOAT32)
        val cluster = ClusteredField(decodeMode = DecodeMode.DISTANCE, outputDistanceToContours = true)


        cluster.normalizedDistance = true

        extend {
            fun plot(x: Double, y: Double, id: Double) {
                drawer.fill = ColorRGBa(id, 0.0, 0.0, 1.0)
                drawer.point(x, y)
            }

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa(-1.0, -1.0, -1.0, 0.0))
                val o = cos(seconds) * 200.0 + 200.0

                for (i in 0 until 20) {
                    plot(o + 100.0 + i * 4, 100.0, 0.25)
                }

                for (i in 0 until 20) {
                    plot(200.0 + i * 4, 150.0 + i, 0.5)
                }
                for (i in 0 until 20) {
                    plot(300.0 + i * 4, 250.0 + i, 0.7)
                }

                for (i in 0 until 20) {
                    plot(400.0 + i * 4, 250.0 + i, 0.75)
                }
            }
            cluster.apply(rt.colorBuffer(0), flowfield)
            drawer.drawStyle.colorMatrix = tint(ColorRGBa(10.0, 10.0, 1.0))
            drawer.image(flowfield)


        }
    }
}