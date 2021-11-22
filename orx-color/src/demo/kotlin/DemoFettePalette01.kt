import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.extra.color.fettepalette.ColorRamp
import org.openrndr.extra.color.fettepalette.Lamé
import org.openrndr.extra.color.fettepalette.generateColorRamp
import org.openrndr.math.Vector2
import kotlin.random.Random

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val total = 9

            extend {
                val ramp = generateColorRamp(
                    total = total,
                    centerHue = (mouse.position.x / width) * 360.0,
                    curveMethod = Lamé,
                    hueCycle = mouse.position.y / height,
                    curveAccent = 0.0,
                    offsetTint = 0.01,
                    offsetShade = 0.01,
                    tintShadeHueShift = 0.01,
                    offsetCurveModTint = 0.03,
                    offsetCurveModShade = 0.03,
                    minSaturationLight = Vector2.ZERO,
                    maxSaturationLight = Vector2.ONE,
                    useOK = true
                )

                fun rampSquare(ramp: ColorRamp, random: Random, position: Vector2, width: Double) {
                    drawer.isolated {
                        drawer.fill = ramp.baseColors.random(random).toRGBa()
                        drawer.stroke = null
                        drawer.rectangle(position, width, width)

                        drawer.fill = ramp.lightColors.random(random).toRGBa()
                        drawer.rectangle(position + Vector2(width / 4.0, width / 4.0), width / 4.0, width / 2.0)

                        val dc = ramp.darkColors.shuffled(random).take(2)

                        drawer.fill = dc[0].toRGBa()
                        drawer.rectangle(position + Vector2(width / 2.0, width / 4.0), width / 4.0, width / 4.0)

                        drawer.fill = dc[1].toRGBa()
                        drawer.rectangle(position + Vector2(width / 2.0, width / 2.0), width / 4.0, width / 4.0)
                    }
                }


                drawer.isolated {
                    for ((index, i) in ramp.lightColors.withIndex()) {
                        drawer.stroke = null
                        drawer.fill = i.toRGBa()
                        drawer.rectangle(20.0, 20.0, 50.0, 50.0)
                        drawer.translate(50.0, 0.0)
                    }
                }
                drawer.isolated {
                    for ((index, i) in ramp.baseColors.withIndex()) {
                        drawer.stroke = null
                        drawer.fill = i.toRGBa()
                        drawer.rectangle(20.0, 70.0, 50.0, 50.0)
                        drawer.translate(50.0, 0.0)
                    }
                }
                drawer.isolated {
                    for ((index, i) in ramp.darkColors.withIndex()) {
                        drawer.stroke = null
                        drawer.fill = i.toRGBa()
                        drawer.rectangle(20.0, 120.0, 50.0, 50.0)
                        drawer.translate(50.0, 0.0)
                    }
                }

                val random = Random(seconds.toInt())
                rampSquare(ramp, random, Vector2(180.0, 180.0), 360.0)
            }
        }
    }
}