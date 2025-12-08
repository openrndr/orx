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
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Polar
import org.openrndr.math.clamp
import org.openrndr.poissonfill.PoissonFill
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * Demonstrates how the `PoisonFill()` effect fills the transparent pixels of a
 * `ColorBuffer` using the surrounding opaque pixels.
 *
 * The program creates a collection of `ColoredMovingPoint`s, then updates and
 * renders them into a `RenderTarget` on every animation frame.
 *
 * If the mouse pointer is on the right half of the window, the render target
 * is displayed as-is. Otherwise, the `PoisonFill` effect is applied, the
 * result stored into the `wet` `ColorBuffer`, then displayed.
 *
 * A sharp white rectangle is drawn on top just for contrast against the blurry background. *
 */
fun main() {
    data class ColoredMovingPoint(val color: ColorRGBa, var pos: Polar, val speed: Polar)
    application {
        program {
            val dry = renderTarget(width, height) {
                colorBuffer(type = ColorType.FLOAT32)
            }
            val wet = colorBuffer(width, height, type = ColorType.FLOAT32)

            val fx = PoissonFill()

            var borderOpacity = 0.0

            // Create a list of points with
            // color, polar position and polar speed
            val points = List(10) {
                ColoredMovingPoint(
                    ColorHSVa(
                        it * 182.0,
                        Double.uniform(0.3, 0.6),
                        Double.uniform(0.1, 0.9)
                    ).toRGBa(),
                    Polar(
                        Double.uniform(0.0, 360.0),
                        100.0 + it * 10.0
                    ),
                    Polar(Double.uniform(-1.0, 1.0), 0.0)
                )
            }
            val mouseTracker = MouseTracker(mouse)

            extend {
                drawer.isolatedWithTarget(dry) {
                    stroke = null
                    clear(ColorRGBa.TRANSPARENT)

                    // draw color circles
                    points.forEach { point ->
                        fill = point.color.shade(
                            0.9 +
                                    0.1 * sin(point.pos.theta * 0.3)
                        )
                        circle(point.pos.cartesian + bounds.center, 5.0)
                        point.pos += point.speed
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

                if (mouse.position.x > width * 0.5)
                    drawer.image(dry.colorBuffer(0))
                else {
                    fx.apply(dry.colorBuffer(0), wet)
                    drawer.image(wet)
                }

                // draw white rectangle
                drawer.stroke = ColorRGBa.WHITE.opacify(0.9)
                drawer.fill = null
                drawer.strokeWeight = 6.0
                drawer.rectangle(
                    Rectangle.fromCenter(
                        drawer.bounds.center,
                        300.0, 300.0
                    )
                )
            }
        }
    }
}