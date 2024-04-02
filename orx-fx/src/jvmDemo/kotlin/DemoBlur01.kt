import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.*
import org.openrndr.math.Polar
import kotlin.math.sin

fun main() {
    application {
        program {
            // In this buffer we will draw some simple shapes
            val dry = renderTarget(width / 3, height / 3) {
                colorBuffer()
            }

            // The list of effects to demo
            val effects = listOf(
                    BoxBlur(),
                    ApproximateGaussianBlur(),
                    HashBlur(),
                    GaussianBlur(),
                    GaussianBloom(),
                    FrameBlur(),
                    ZoomBlur(),
                    LaserBlur()
            )

            // On this buffer we will draw the dry buffer with an effect applied
            val wet = colorBuffer(dry.width, dry.height)

            val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)

            extend {
                // Draw two moving circles
                drawer.isolatedWithTarget(dry) {
                    clear(ColorRGBa.BLACK)

                    fill = null
                    stroke = ColorRGBa.PINK
                    strokeWeight = 25.0
                    circle(bounds.center +
                            Polar(seconds * 50.0, 100.0).cartesian,
                            200.0 + 50.0 * sin(seconds * 2.0))

                    fill = ColorRGBa.PINK
                    stroke = null
                    circle(bounds.center +
                            Polar(seconds * 50.0 + 60, 100.0).cartesian,
                            100.0 + 20.0 * sin(seconds * 2.0 + 1))
                }

                effects.forEachIndexed { i, blur ->
                    // Adjust the effect settings.
                    // All the values could be animated.
                    when (blur) {
                        is BoxBlur -> {
                            blur.window = 30
                        }
                        is ApproximateGaussianBlur -> {
                            blur.window = 25
                            blur.sigma = 15.0
                        }
                        is HashBlur -> {
                            blur.samples = 50
                            blur.radius = 5.0
                            blur.time = seconds
                        }
                        is GaussianBlur -> {
                            blur.window = 25
                            blur.sigma = 15.0
                        }
                        is GaussianBloom -> {
                            blur.window = 5
                            blur.sigma = 3.0
                            blur.gain = 3.0
                            blur.noiseSeed = seconds
                        }
                        is FrameBlur -> {
                            blur.blend = 0.05
                        }
                        is ZoomBlur -> {
                            blur.center = Polar(seconds * 77.0, 0.5)
                                    .cartesian
                            blur.strength = 0.8
                        }
                        is LaserBlur -> {
                            blur.center = Polar(seconds * 77.0, 0.5)
                                    .cartesian
                            blur.aberration = 0.03
                            blur.radius = 0.5

                        }
                    }

                    // Apply the effect on `dry` writing the result to `wet`
                    blur.apply(dry.colorBuffer(0), wet)

                    // Draw `wet` and write the effect name on top
                    drawer.isolated {
                        translate((i % 3) * width / 3.0,
                                (i / 3) * height / 3.0)
                        image(wet)
                        fontMap = font
                        text(blur.javaClass.simpleName, 20.0, 30.0)
                    }
                }
            }
        }
    }
}