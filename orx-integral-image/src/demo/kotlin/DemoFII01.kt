/**
 * Apply box blurs with large windows
 */

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.integralimage.*

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val fii = FastIntegralImage()
        val integralImage = colorBuffer(width, height, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        val rt = renderTarget(width, height) {
            colorBuffer()
        }
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.PINK.shade(1.0)
                drawer.circle(mouse.position, 128.0)
            }
            fii.apply(rt.colorBuffer(0), integralImage)

            // -- here we sample from the integral image
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    float w = 64.0;
                    vec2 step = 1.0 / vec2(textureSize(image, 0));
                    vec4 t11 = texture(image, va_texCoord0 + step * vec2(w+1.0,w+1.0));
                    vec4 t01 = texture(image, va_texCoord0 + step * vec2(-w,w+1.0));
                    vec4 t00 = texture(image, va_texCoord0 + step * vec2(-w,-w));
                    vec4 t10 = texture(image, va_texCoord0 + step * vec2(w+1.0,-w));
                    x_fill = (t11 - t01 - t10 + t00) / ((2.0 * w +1.0) * (2.0 * w + 1.0));
                """.trimIndent()
            }
            drawer.image(integralImage)
        }
    }
}