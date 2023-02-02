import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.grayscale
import org.openrndr.draw.loadImage
import org.openrndr.extra.marchingsquares.findContours
import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    application {
        configure {
            width = 640
            height = 480
        }
        program {
            val image = loadImage("demo-data/images/image-001.png")
            image.shadow.download()
            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.stroke = ColorRGBa.BLACK
                fun f(v: Vector2): Double {
                    val iv = v.toInt()
                    val d = if (iv.x >= 0 && iv.y >= 0 && iv.x < image.width && iv.y < image.height) image.shadow[iv.x, iv.y].luminance else 0.0
                    return cos(d * PI * 8.0 + seconds)
                }

                val segments = findContours(::f, drawer.bounds.offsetEdges(32.0), 4.0)
                drawer.drawStyle.colorMatrix = grayscale()
                drawer.image(image)
                drawer.lineSegments(segments)
            }
        }
    }
}