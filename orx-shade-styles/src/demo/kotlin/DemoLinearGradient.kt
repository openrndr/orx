import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shadestyles.linearGradient

import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.extras.color.spaces.toOKLABa
import kotlin.math.cos

fun main() {
    application {
        program {
            extend {
                drawer.shadeStyle = linearGradient(
                    ColorRGBa.RED.toOKLABa(),
                    ColorRGBa.BLUE.toOKLABa(),
                )
                drawer.rectangle(120.0, 40.0, 200.0, 400.0)

                drawer.shadeStyle = linearGradient(
                    ColorRGBa.RED,
                    ColorRGBa.BLUE
                )

                drawer.rectangle(120.0+200.0, 40.0, 200.0, 400.0)
            }
        }
    }
}