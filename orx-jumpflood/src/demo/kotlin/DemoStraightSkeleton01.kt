import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType

import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.jumpfill.fx.StraightSkeleton
import org.openrndr.extra.noise.simplex

suspend fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }

            val straightSkeleton = StraightSkeleton()
            val input = renderTarget(width, height) {
                colorBuffer()
            }
            val field = input.colorBuffer(0).createEquivalent(type = ColorType.FLOAT32)

            extend {
                drawer.isolatedWithTarget(input) {
                    // -- draw something interesting
                    drawer.stroke = null
                    drawer.clear(ColorRGBa.BLACK)
                    drawer.fill = ColorRGBa.WHITE
                    drawer.circle(mouse.position, 300.0)
                    drawer.fill = ColorRGBa.BLACK
                    drawer.circle(mouse.position, 150.0)
                    drawer.fill = ColorRGBa.WHITE
                    for (i in 0 until 30) {
                        val time = seconds * 0.25
                        val x = simplex(i * 20, time) * width / 2 + width / 2
                        val y = simplex(i * 20 + 5, time) * height / 2 + height / 2
                        val r = simplex(i*30, time) * 50.0 + 50.0
                        drawer.circle(x, y, r)
                    }
                }
                straightSkeleton.apply(input.colorBuffer(0), field)
                drawer.image(field)
            }
        }
    }
}