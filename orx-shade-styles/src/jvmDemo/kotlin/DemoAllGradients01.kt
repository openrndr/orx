import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.shadestyles.*
import org.openrndr.math.Polar
import org.openrndr.shape.Rectangle

/**
 * Example of 5 gradient styles.
 * NPointLinear and NPoingGradient have separate demos.
 */
fun main() {
    application {
        configure {
            width = 1000
            height = 500
        }
        program {
            // Create gradients with initial colors
            val gradients = listOf(
                    RadialGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    AngularGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    NPointGradient(Array(4) {
                        ColorRGBa.PINK.shade(it / 3.0)
                    }),
                    LinearGradient(ColorRGBa.PINK, ColorRGBa.WHITE),
                    HalfAngularGradient(ColorRGBa.PINK, ColorRGBa.WHITE)
            )

            extend {
                gradients.forEachIndexed { gradientId, gradient ->
                    for (column in 0 until 10) {
                        val color1 = ColorRGBa.PINK.toHSVa().shiftHue(column * 12.0)
                                .shade(0.5).toRGBa()

                        val w = width.toDouble() / 10.0
                        val h = height.toDouble() / gradients.size
                        val rect = Rectangle(column * w, gradientId * h, w, h)

                        val offset = Polar((seconds + column) * 15.0, 0.3).cartesian

                        drawer.isolated {
                            when (gradient) {
                                is RadialGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.length = 0.6
                                    gradient.offset = offset
                                }
                                is AngularGradient -> {
                                    gradient.color1 = color1
                                    gradient.exponent = column / 3.0 + 0.3
                                    gradient.rotation = (seconds - column) * 10.0
                                    gradient.offset = offset
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
                                    gradient.offset = offset
                                }
                                is NPointGradient -> {
                                    // Animate points.
                                    // We could also animate colors.
                                    gradient.points = Array(gradient.colors.size) {
                                        rect.center + Polar(it * 90.0 +
                                                column * 36 - seconds * 10,
                                                40.0).cartesian
                                    }
                                }
                            }
                            shadeStyle = gradient
                            rectangle(rect)
                        }
                    }
                }
            }
        }
    }
}