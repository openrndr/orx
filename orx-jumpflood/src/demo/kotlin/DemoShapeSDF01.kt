import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG

suspend fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val sdf = ShapeSDF()
            val df = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val shapes = loadSVG("orx-jumpflood/src/demo/resources/name.svg").findShapes().map { it.shape }

            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                sdf.setShapes(shapes.mapIndexed { index, it ->
                    it.transform(transform {
                        translate(1280/2.0, 720.0/2)

                        translate(-1280/2.0, -720.0/2.0)
                    })
                })
                sdf.apply(emptyArray(), df)
                drawer.image(df)
            }
        }
    }
}