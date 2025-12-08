import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.distort.FluidDistort
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.SDFSmoothDifference
import org.openrndr.extra.svg.loadSVG

/**
 * Advanced demonstration making use the `ShapeSDF` filter applied twice to a static
 * SVG loaded from a file, one with `useUV` set to true.
 *
 * A `FluidDistort` filter is used to generate an animated UV map which is fed into
 * both `ShapeSDF` filters. A `SDFSmoothDifference` filter is then applied to combine
 * both resulting `ColorBuffer` instances, and a `SDFStrokeFill` filter used for
 * rendering the result.
 *
 * The mouse horizontal position determines which of the three used color buffers is
 * displayed.
 */
fun main() = application {
    configure {
        width = 720
        height = 405
    }
    program {
        val sdf0 = ShapeSDF()
        val sdf1 = ShapeSDF()
        val df0 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
        val df1 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

        val fd = FluidDistort()
        fd.outputUV = true

        val uvmap = colorBuffer(width, height, type = ColorType.FLOAT16)

        val shapes = loadSVG("orx-jumpflood/src/jvmDemo/resources/name.svg").findShapes().map { it.shape }
        val union = SDFSmoothDifference()

        sdf0.setShapes(shapes)
        sdf1.setShapes(shapes)

        val strokeFill = SDFStrokeFill()

        extend {
            drawer.clear(ColorRGBa.PINK)

            fd.apply(emptyArray(), uvmap)

            sdf0.useUV = true
            sdf0.apply(uvmap, df0)

            sdf1.apply(uvmap, df1)

            union.radius = 10.0
            union.apply(arrayOf(df0, df1), df0)

            strokeFill.strokeWeight = 10.0
            strokeFill.apply(df0, df0)

            drawer.image(
                when (mouse.position.x) {
                    in 0.0..width * 0.6 -> df0
                    in width * 0.6..width * 0.8 -> df1
                    else -> uvmap
                }
            )
        }
    }
}
