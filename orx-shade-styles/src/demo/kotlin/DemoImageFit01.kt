import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.shadestyles.imageFit
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import kotlin.math.cos
import kotlin.math.sin

fun main() = application {
    program {
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = imageFit(image, Vector2(cos(seconds), sin(seconds))) + linearGradient(ColorRGBa.RED, ColorRGBa.BLUE)
            drawer.circle(drawer.bounds.center, 200.0)
            drawer.rectangle(10.0, 10.0, 400.0, 50.0)
            drawer.rectangle(10.0, 10.0, 50.0, 400.0)
            drawer.contour(Circle(width/2.0, height/2.0, 50.0).contour)
        }
    }
}

