import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4


fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        // -- this block is for automation purposes only
        if (System.getProperty("takeScreenshot") == "true") {
            extend(SingleScreenshot()) {
                this.outputFile = System.getProperty("screenshotPath")
            }
        }

        val rabbit = RabbitControlServer()
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 20.0)
        val settings = object {
            @TextParameter("A string")
            var s: String = "Hello"

            @DoubleParameter("A double", 0.0, 10.0)
            var d: Double = 10.0

            @BooleanParameter("A bool")
            var b: Boolean = true

            @ColorParameter("A fill color")
            var fill = ColorRGBa.PINK

            @ColorParameter("A stroke color")
            var stroke = ColorRGBa.WHITE

            @Vector2Parameter("A vector2")
            var v2 = Vector2(200.0,200.0)

            @Vector3Parameter("A vector3")
            var v3 = Vector3(200.0, 200.0, 200.0)

            @Vector4Parameter("A vector4")
            var v4 = Vector4(200.0, 200.0, 200.0, 200.0)

            @ActionParameter("Action test")
            fun clicked() {
                println("Clicked from RabbitControl")
            }
        }

        rabbit.add(settings)
        extend(rabbit)
        extend {
            drawer.clear(if (settings.b) ColorRGBa.BLUE else ColorRGBa.BLACK)
            drawer.fontMap = font
            drawer.fill = settings.fill
            drawer.stroke = settings.stroke
            drawer.circle(settings.v2, settings.d)
            drawer.text(settings.s, 10.0, 20.0)
        }
    }
}
