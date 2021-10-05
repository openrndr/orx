@file:Suppress("UNUSED_LAMBDA_EXPRESSION")
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

{ program: Program ->
    program.apply {
        extend {
            drawer.clear(ColorRGBa.GRAY)
            drawer.fill = ColorRGBa.PINK
            drawer.circle(width/2.0, height/2.0 ,200.0)
        }
    }
}