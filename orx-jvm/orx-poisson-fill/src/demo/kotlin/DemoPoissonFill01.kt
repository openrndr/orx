import org.openrndr.MouseButton
import org.openrndr.MouseTracker
import org.openrndr.application
import org.openrndr.color.ColorHSVa
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Polar
import org.openrndr.math.clamp
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import kotlin.math.sin


fun main() {
    data class Thing(val color: ColorRGBa, var pos: Polar, val speed: Polar)
    application {
        program {
            val dry = renderTarget(width, height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
            val wet = colorBuffer(width, height, type = ColorType.FLOAT32)

            val fx = PoissonFill()

            var borderOpacity = 0.0

            // Create a list of things with
            // color, polar position and polar speed
            val things = List(10) {
                Thing(
                        ColorHSVa(it * 182.0,
                                Double.uniform(0.3, 0.6),
                                Double.uniform(0.1, 0.9)).toRGBa(),
                        Polar(Double.uniform(0.0, 360.0),
                                100.0 + it * 10.0),
                        Polar(Double.uniform(-1.0, 1.0), 0.0))
            }
            val mouseTracker = MouseTracker(mouse)

            extend {
                drawer.isolatedWithTarget(dry) {
                    stroke = null
                    clear(ColorRGBa.TRANSPARENT)

                    // draw color circles
                    things.forEach { thing ->
                        fill = thing.color.shade(0.9 +
                                0.1 * sin(thing.pos.theta * 0.3))
                        circle(thing.pos.cartesian + bounds.center, 5.0)
                        // A. Use after fix in Polar.kt
                        //thing.pos += thing.speed
                        // B. temporary solution
                        thing.pos = Polar(thing.pos.theta +
                                thing.speed.theta, thing.pos.radius)
                    }

                    // draw dark gray window border.
                    // hold mouse button to fade in.
                    borderOpacity += if (MouseButton.LEFT in mouseTracker.pressedButtons) 0.01 else -0.01
                    borderOpacity = borderOpacity.clamp(0.0, 1.0)
                    stroke = rgb(0.2).opacify(borderOpacity)
                    fill = null
                    strokeWeight = 3.0
                    rectangle(bounds)
                }

                fx.apply(dry.colorBuffer(0), wet)
                drawer.image(wet)

                // draw white rectangle
                drawer.stroke = ColorRGBa.WHITE.opacify(0.9)
                drawer.fill = null
                drawer.strokeWeight = 6.0
                drawer.rectangle(Rectangle.fromCenter(drawer.bounds.center,
                        300.0, 300.0))
            }
        }
    }
}