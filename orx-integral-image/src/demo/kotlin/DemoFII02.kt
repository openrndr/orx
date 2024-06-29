import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.integralimage.*
import kotlin.math.PI

/**
 * Implement an FM like video synthesizer using [FastIntegralImage]
 */

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {

        val image = loadImage("demo-data/images/image-001.png")
        val fii = FastIntegralImage()
        val integralImage = colorBuffer(width, height, 1.0, ColorFormat.RGBa, ColorType.FLOAT32)
        val rt = renderTarget(width, height) {
            colorBuffer()
        }
        extend {
            drawer.clear(ColorRGBa.PINK)

            /*
            Draw an input image
             */
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa.BLACK)
                drawer.imageFit(image, drawer.bounds)
                drawer.fill = ColorRGBa.PINK.shade(1.0)
                drawer.circle(mouse.position, 256.0)
            }

            /*
            Update the integral image
             */
            fii.apply(rt.colorBuffer(0), integralImage)


            /*
            Use a shade style to sample from the integral image
             */

            drawer.shadeStyle = shadeStyle {
                fragmentPreamble = """
                    vec3 linePhase(vec2 uv) {
                        vec2 step = 1.0 / vec2(textureSize(image, 0));
                        vec4 t11 = texture(image, uv + step * vec2(1.0,1.0));
                        vec4 t01 = texture(image, vec2(0.0, uv.y) + step * vec2(0,1.0));
                        vec4 t00 = texture(image, vec2(0.0, uv.y));
                        vec4 t10 = texture(image, uv + step * vec2(1.0, 0.0));
                        vec4 r = (t11 - t01 - t10 + t00);
                        return r.xyz;
                    }
                    
                """.trimIndent()

                fragmentTransform = """
                    vec2 s = 1.0 / vec2(textureSize(image, 0));
                    
                    float spread = 1.0;
                                        
                    vec3 phase0 = linePhase(va_texCoord0 + s * vec2(-spread, 0.0));
                    vec3 phase1 = linePhase(va_texCoord0);
                    
                    float carrierFreq = 40.0 * 2.0 * ${PI};
                    float carrierPhase = va_texCoord0.x + va_texCoord0.y;
                    float signalFreq = s.x * 100.0 * 2.0 * ${PI};
                    
                    vec3 mo0 = cos(phase0 * signalFreq  + carrierPhase * carrierFreq);
                    vec3 mo1 = cos(phase1 * signalFreq + (carrierPhase - s.x * spread) * carrierFreq);
                    
                    x_fill.rgb = (mo1 - mo0) * 2.0;
                    x_fill.a = 1.0;
                """.trimIndent()
            }
            drawer.image(integralImage)
        }
    }
}