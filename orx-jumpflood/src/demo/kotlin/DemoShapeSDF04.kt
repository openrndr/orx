package sketches

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.*
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val gui = GUI()
            val sdf0 = ShapeSDF()
            val sdf1 = ShapeSDF()
            val df0 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
            val df1 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

            val perturb = Perturb()
            perturb.outputUV = true

            val uvmap = colorBuffer(width, height, type = ColorType.FLOAT16)

            val circleShapes = List(1) { Circle(width/2.0, height/2.0, 200.0).shape}

            val shapes = loadSVG("orx-jumpflood/src/demo/resources/name.svg").findShapes().map { it.shape }
            val difference = SDFSmoothDifference()
            val strokeFill = SDFStrokeFill()

            gui.add(perturb)
            if (System.getProperty("takeScreenshot") == "true") {
                extend(SingleScreenshot()) {
                    this.outputFile = System.getProperty("screenshotPath")
                }
            }
            extend(gui)
            extend {
                drawer.background(ColorRGBa.PINK)

                perturb.phase = seconds * 0.1
                perturb.apply(uvmap, uvmap)

                sdf0.setShapes(circleShapes.mapIndexed { index, it ->
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
                difference.radius = 10.0
                difference.apply(arrayOf(df0, df1), df0)

                strokeFill.strokeWeight = 10.0
                strokeFill.apply(df0, df0);
                drawer.image(df0)
            }
        }
    }
}