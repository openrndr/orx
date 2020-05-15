import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shadestyles.*
import org.openrndr.math.Polar

fun main() {
    application {
        configure {
            width = 1000
        }
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            // Create gradients with initial colors
            val gradients = listOf(
                    radialGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    angularGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    linearGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    halfAngularGradient(ColorRGBa.PINK, ColorRGBa.WHITE)
            )

            extend {
                gradients.forEachIndexed { i, gradient ->
                    for (column in 0 until 10) {
                        val color1 = ColorRGBa.PINK.toHSVa().shiftHue(column * 12.0)
                                .scaleValue(0.5).toRGBa()

                        drawer.isolated {
                            when (gradient) {
                                is RadialGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.length = 0.6
                                    gradient.offset = Polar(
                                            (seconds + column) * 15.0, 0.3).cartesian
                                }
                                is AngularGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.rotation = (seconds - column) * 10.0
                                    gradient.offset = Polar(
                                            (seconds + column) * 30.0, 0.3).cartesian
                                }
                                is LinearGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.rotation = seconds * 10.0
                                }
                                is HalfAngularGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.rotation = (column - seconds) * 10.0
                                    gradient.offset = Polar(
                                            (seconds + column) * 8.0, 0.3).cartesian
                                }
                            }
                            shadeStyle = gradient
                            rectangle(column * width / 10.0, i * height / 4.0,
                                    width / 10.0, height / 4.0)
                        }
                    }
                }
            }
        }
    }
}