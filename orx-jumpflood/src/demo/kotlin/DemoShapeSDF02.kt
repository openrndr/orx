import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.*
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.svg.loadSVG
import kotlin.math.min

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val sdf0 = ShapeSDF()
            val df0 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val sdf1 = ShapeSDF()
            val df1 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val shapes = loadSVG("orx-jumpflood/src/demo/resources/name.svg").findShapes().map { it.shape }

            val union = SDFSmoothIntersection()
            val onion = SDFOnion()


            val strokeFill = SDFStrokeFill()

            // -- this block is for automation purposes only
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                drawer.clear(ColorRGBa.PINK)

                sdf0.setShapes(shapes.mapIndexed { index, it ->
                    it.transform(transform {
                        translate(1280 / 2.0, 720.0 / 2)
                        translate(-1280 / 2.0, -720.0 / 2.0)
                    })
                })

                sdf1.setShapes(shapes.mapIndexed { index, it ->
                    it.transform(transform {
                        translate(1280 / 2.0, 720.0 / 2)
                        rotate(Vector3.Companion.UNIT_Z, seconds * 45.0 - 30.0)
                        translate(-1280 / 2.0, -720.0 / 2.0)
                    })
                })

                sdf0.apply(emptyArray(), df0)
                sdf1.apply(emptyArray(), df1)
                union.radius = 10.0 + min(mouse.position.y, 100.0)
                union.apply(arrayOf(df0, df1), df0)
                onion.radius = 20.0
                onion.apply(df0, df0)
                strokeFill.strokeWeight = 2.0
                strokeFill.apply(df0, df0);
                drawer.image(df0)
            }
        }
    }
}