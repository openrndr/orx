import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shadestyles.angularGradient
import org.openrndr.extra.shadestyles.halfAngularGradient
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.math.map

fun main() {
    application {
        program {
            extend {
                val x = mouse.position.x.map(
                        0.0, width.toDouble(),
                        -1.0, 1.0)
                val expo = mouse.position.y.map(
                        0.0, height.toDouble(),
                        0.1, 10.0)

                (0..3).forEach {
                    drawer.shadeStyle = when (it) {
                        0 -> {
                            linearGradient(
                                    ColorRGBa.BLACK,
                                    ColorRGBa.PINK,
                                    rotation = x * 180,
                                    exponent = expo
                            )
                        }
                        1 -> {
                            radialGradient(
                                    ColorRGBa.BLACK,
                                    ColorRGBa.PINK,
                                    length = 1.1 + x,
                                    exponent = expo
                            )
                        }
                        2 -> {
                            angularGradient(
                                    ColorRGBa.BLACK,
                                    ColorRGBa.PINK,
                                    rotation = x * 180,
                                    exponent = expo
                            )
                        }
                        else -> {
                            halfAngularGradient(
                                    ColorRGBa.BLACK,
                                    ColorRGBa.PINK,
                                    rotation = x * 180,
                                    exponent = expo
                            )
                        }
                    }
                    drawer.rectangle(50.0, 50.0, 100.0, 300.0)
                    drawer.translate(120.0, 0.0)
                }
            }
        }
    }
}