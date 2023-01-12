import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.jumpfill.EncodePoints
import org.openrndr.extra.jumpfill.IdContourPoints
import org.openrndr.extra.jumpfill.JumpFlooder
import kotlin.math.cos

fun main() = application {
    configure {
        width = 512
        height = 512
    }
    program {
        val rt = renderTarget(512, 512, 1.0) {
            colorBuffer(type = ColorType.FLOAT32)
        }
        val encoder = EncodePoints()
        val jf = JumpFlooder(512, 512, encodePoints = Passthrough())
        val jf2 = JumpFlooder(512, 512, encodePoints = Passthrough())
        val idcontours = IdContourPoints()
        val contoured = colorBuffer(512, 512, type = ColorType.FLOAT32)
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
            encoder.apply(rt.colorBuffer(0), rt.colorBuffer(0))
            val flooded = jf.jumpFlood(rt.colorBuffer(0))
            drawer.image(flooded)
            idcontours.apply(flooded, contoured)
            drawer.image(contoured)
            val flooded2 = jf2.jumpFlood(contoured)

            drawer.image(flooded2, 512.0, 0.0)

            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    float d = length(va_texCoord0.xy - x_fill.xy);
                    x_fill = vec4(d,d,x_fill.z, 1.0);
                """.trimIndent()
            }
            drawer.image(flooded2, 0.0, 0.0)

        }
    }
}