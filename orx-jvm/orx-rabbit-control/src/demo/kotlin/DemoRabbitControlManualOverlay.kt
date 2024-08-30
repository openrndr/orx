import org.openrndr.KEY_HOME
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.parameters.*


fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {
        val rabbit = RabbitControlServer(showQRUntilClientConnects = false)

        val settings = object {
            @BooleanParameter("White on black")
            var whiteOnBlack: Boolean = true
        }

        rabbit.add(settings)
        extend(rabbit)

        /**
         * Example: only show the QR code when the [KEY_HOME] button is pressed
         */
        keyboard.keyDown.listen {
            when (it.key) {
                KEY_HOME -> rabbit.showQRCode = true
            }
        }

        keyboard.keyUp.listen {
            when (it.key) {
                KEY_HOME -> rabbit.showQRCode = false
            }
        }

        extend {
            drawer.clear(if (settings.whiteOnBlack) ColorRGBa.BLACK else ColorRGBa.WHITE)
            drawer.fill = if (settings.whiteOnBlack) ColorRGBa.WHITE else ColorRGBa.BLACK
            drawer.circle(drawer.bounds.center, 250.0)
        }
    }
}
