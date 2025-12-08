import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.jumpfill.ShapeSDF
import org.openrndr.extra.jumpfill.draw.SDFStrokeFill
import org.openrndr.extra.jumpfill.ops.SDFOnion
import org.openrndr.extra.jumpfill.ops.SDFSmoothIntersection
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform

/**
 * Advanced demonstration making use of two `ShapeSDF` filters: the first applied to a static
 * SVG loaded from a file, and the second to a rotating version of the same shape.
 *
 * The demo also uses three additional SDF filters:
 * - `SDFSmoothIntersection`, which combines two color buffers containing SDF information
 *   into a new color buffer
 * - `SDFOnion`, which takes one SDF color buffer and outputs one SDF color buffer
 * - `SDFStrokeFill`, used for rendering using configurable fill and stroke properties.
 *
 * The vertical mouse position is used to control the radius of the `SDFSmoothIntersection` effect.
 *
 * The program finally renders the result of the previous operations as one color buffer
 * thanks to the `SDFStrokeFill` effect.
 */
fun main() = application {
    configure {
        width = 720
        height = 405
    }
    program {
        val sdf0 = ShapeSDF()
        val df0 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

        val sdf1 = ShapeSDF()
        val df1 = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.FLOAT32)

        val shapes = loadSVG("orx-jumpflood/src/jvmDemo/resources/name.svg").findShapes().map { it.shape }

        val union = SDFSmoothIntersection()
        val onion = SDFOnion()

        val strokeFill = SDFStrokeFill()

        extend {
            drawer.clear(ColorRGBa.PINK)

            sdf0.setShapes(shapes)

            sdf1.setShapes(shapes.map {
                it.transform(transform {
                    translate(drawer.bounds.center)
                    rotate(Vector3.UNIT_Z, seconds * 45.0 - 30.0)
                    translate(-drawer.bounds.center)
                })
            })

            sdf0.apply(emptyArray(), df0)
            sdf1.apply(emptyArray(), df1)

            union.radius = 10.0 + mouse.position.y.coerceAtMost(100.0)
            union.apply(arrayOf(df0, df1), df0)

            onion.radius = 20.0
            onion.apply(df0, df0)

            strokeFill.strokeWeight = 2.0
            strokeFill.apply(df0, df0)
            drawer.image(df0)
        }
    }
}
