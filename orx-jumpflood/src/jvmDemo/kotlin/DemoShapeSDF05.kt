import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.SDFSmoothDifference
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.svg.loadSVG
import kotlin.math.cos
import kotlin.math.sin

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
            val uvmap2 = colorBuffer(width, height, type = ColorType.FLOAT16)

            val circleShapes = List(1) { Circle(width/2.0, height/2.0, 200.0).shape}
            val shapes = loadSVG("orx-jumpflood/src/jvmDemo/resources/name.svg").findShapes().map { it.shape }

            sdf0.setShapes(circleShapes)
            sdf1.setShapes(shapes)

            val difference = SDFSmoothDifference()
            val strokeFill = SDFStrokeFill()
            sdf0.useUV = true
            gui.add(sdf0)
            gui.add(perturb)
            gui.add(strokeFill)
            gui.add(difference)

            extend(gui)
            extend {
                drawer.clear(ColorRGBa.PINK)

                perturb.offset = Vector2(cos(seconds*0.2), sin(seconds*0.2))
                perturb.outputUV = true
                perturb.phase = seconds * 0.1
                perturb.apply(uvmap, uvmap)

                perturb.offset = Vector2.ZERO
                perturb.outputUV = false
                perturb.phase = seconds * 0.05
                perturb.apply(uvmap, uvmap2)

                sdf0.apply(uvmap2, df0)
                sdf1.apply(uvmap2, df1)

                difference.apply(arrayOf(df0, df1), df0)

                strokeFill.apply(df0, df0)
                drawer.image(df0)
            }
        }
    }
}