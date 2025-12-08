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
import org.openrndr.extra.svg.loadSVG
import org.openrndr.shape.Circle

/**
 * Demonstrates using tow `ShapeSDF` filters. One contairs a vector shape loaded
 * from disk, the other a circular shape.
 *
 * A `Perturb` effect is used to generate a noise UV map, which is then fed into
 * the `ShapeSDF` filters.
 *
 * The two resulting `ColorBuffer`s are combined using a `SDFSmoothDifference` filter,
 * which erases the distorted circular shape from the loaded shape.
 *
 * The `SDFStrokeFill` is used to render the result.
 *
 * A GUI is available to tweak the parameters of the `Perturb` effect.
 * Lowering its `gain` to zero disables the effect, revealing the circle and the
 * smoothness (round corners) of the difference effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 405
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

        val circleShapes = List(1) { Circle(drawer.bounds.center, 200.0).shape }
        val shapes = loadSVG("orx-jumpflood/src/jvmDemo/resources/name.svg").findShapes().map { it.shape }

        sdf0.setShapes(circleShapes)
        sdf1.setShapes(shapes)

        val difference = SDFSmoothDifference()
        val strokeFill = SDFStrokeFill()

        gui.add(perturb)
        extend(gui)
        extend {
            drawer.clear(ColorRGBa.PINK)

            perturb.phase = seconds * 0.1
            perturb.apply(uvmap, uvmap)

            sdf0.useUV = true
            sdf0.apply(uvmap, df0)
            sdf1.apply(uvmap, df1)
            difference.radius = 10.0
            difference.apply(arrayOf(df0, df1), df0)

            strokeFill.strokeWeight = 10.0
            strokeFill.apply(df0, df0)
            drawer.image(df0)
        }
    }
}
