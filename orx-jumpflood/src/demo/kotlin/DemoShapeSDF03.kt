import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.*
import org.openrndr.math.transforms.transform
import org.openrndr.svg.loadSVG

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val sdf0 = ShapeSDF()
            val sdf1 = ShapeSDF()
            val df0 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
            val df1 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val fd = FluidDistort()
            fd.outputUV = true

            val uvmap = colorBuffer(width, height, type = ColorType.FLOAT16)

            val shapes = loadSVG("orx-jumpflood/src/demo/resources/name.svg").findShapes().map { it.shape }
            val union = SDFSmoothDifference()

            val strokeFill = SDFStrokeFill()

            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend {
                drawer.clear(ColorRGBa.PINK)

                fd.apply(emptyArray(), uvmap)

                sdf0.setShapes(shapes.mapIndexed { index, it ->
                    it.transform(transform {
                        translate(1280 / 2.0, 720.0 / 2)
                        translate(-1280 / 2.0, -720.0 / 2.0)
                    })
                })
                sdf1.setShapes(shapes.mapIndexed { index, it ->
                    it.transform(transform {
                        translate(1280 / 2.0, 720.0 / 2)
                        translate(-1280 / 2.0, -720.0 / 2.0)
                    })
                })

                sdf0.useUV = true
                sdf0.apply(uvmap, df0)
                sdf1.apply(uvmap, df1)
                union.radius = 10.0
                union.apply(arrayOf(df0, df1), df0)

                strokeFill.strokeWeight = 10.0
                strokeFill.apply(df0, df0);
                drawer.image(df0)
            }
        }
    }
}