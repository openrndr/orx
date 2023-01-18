import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blur.HashBlurDynamic
import org.openrndr.extra.fx.patterns.Checkers
import kotlin.math.cos

fun main() {
    application {
        program {
            val c = compose {
                layer {
                    val a = aside(colorType = ColorType.FLOAT32) {
                        post(Checkers()) {
                            this.size = cos(seconds)*0.5 + 0.5
                        }
                    }
                    draw {
                        drawer.clear(ColorRGBa.GRAY.shade(0.5))
                        drawer.circle(width/2.0, height/2.0, 100.0)
                    }
                    post(HashBlurDynamic(), a) {
                        time = seconds
                        radius = 25.0
                    }
                }
            }
            extend {
                c.draw(drawer)
            }
        }
    }
}