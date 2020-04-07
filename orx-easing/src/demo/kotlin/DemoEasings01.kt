import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extras.easing.*
import org.openrndr.math.Vector2

fun main() {
    application {

        configure {
            width = 1280
            height = 1080
        }
        program {
            fun drawEasing(f: EasingFunction) {
                drawer.stroke = ColorRGBa.PINK
                val points = mutableListOf<Vector2>()
                for (i in 0 .. 40) {
                    val y = 40.0 - f(i / 40.0, 0.0, 1.0, 1.0) * 40.0
                    points.add(Vector2(i*10.0, y))
                }
                drawer.lineStrip(points)

                drawer.stroke = ColorRGBa.GRAY
                drawer.lineSegment(0.0, 40.0, 400.0, 40.0)


                drawer.lineSegment(0.0, 20.0, 400.0, 20.0)

            }
            extend {
                drawer.stroke = ColorRGBa.WHITE

                val functions = listOf(
                        ::easeLinear,
                        ::easeQuadIn,
                        ::easeQuadOut,
                        ::easeQuadInOut,
                        ::easeCubicIn,
                        ::easeCubicOut,
                        ::easeCubicInOut,
                        ::easeCircIn,
                        ::easeCircOut,
                        ::easeCircInOut,
                        ::easeQuartIn,
                        ::easeQuartOut,
                        ::easeQuartInOut,
                        ::easeExpoIn,
                        ::easeExpoOut,
                        ::easeExpoInOut,
                        ::easeQuintIn,
                        ::easeQuintOut,
                        ::easeQuintInOut,
                        ::easeSineIn,
                        ::easeSineOut,
                        ::easeSineInOut,
                        ::easeBackIn,
                        ::easeBackOut,
                        ::easeBackInOut,
                        ::easeElasticIn,
                        ::easeElasticOut,
                        ::easeElasticInOut,
                        ::easeBounceIn,
                        ::easeBounceOut,
                        ::easeBounceInOut

                )

                var i = 0
                for (f in functions) {
                    drawEasing(f)
                    drawer.translate(0.0, 50.0)
                    i ++
                    if (i > 19) {
                        drawer.translate(450.0, -20 * 50.0)
                        i = 0
                    }
                }
            }
        }
    }

}