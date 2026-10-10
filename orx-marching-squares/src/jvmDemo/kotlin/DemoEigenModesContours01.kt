import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.extra.marchingsquares.findContoursAdaptive
import org.openrndr.extra.shapes.eigenmodes.extractEigenModes
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.regularPolygon
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.contains
import org.openrndr.shape.intersection
import kotlin.math.abs
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {

        //val shape = Circle(Vector2.ZERO, 720/12.0).shape
        val shape = regularPolygon(6, Vector2.ZERO, 720/12.0).shape
        val shapeM = regularPolygon(6, Vector2.ZERO, 720/12.0 - 2.0).shape
        //val shape = Rectangle(drawer.bounds.center, 100.0, 100.0).shape
        val eigenModes = shape.extractEigenModes(36, 720/6, random = Random(0))

        val grid = drawer.bounds.grid(6, 6).flatten()

        val contours = grid.mapIndexed { index, rectangle ->
            // GridEigenMode.value() is exactly 0.0 outside the domain it was rasterized to, but
            // findContoursAdaptive searches shape.bounds -- the full bounding box, which extends
            // past the hexagon into its corners. Without this guard, marching squares traces a
            // spurious "contour" along the edge of that flat zero region (a rasterization
            // artifact, not a real level curve). Returning a sentinel far outside the modes'
            // value range outside the shape avoids ever creating that crossing.
            fun f(p: Vector2) = if (p in shape) eigenModes[index].value(p) else 1.0
            findContoursAdaptive(::f, shape.bounds, 7)
        }

        // A raster of each mode's raw values, negative-to-positive mapped blue-black-pink, drawn
        // as the cell background so the extracted/clipped contour outline can be visually checked
        // against the actual field it's supposed to trace -- any contour that doesn't hug the
        // field's own sign boundary points at a bug in extraction or clipping, not just a
        // plausible-looking but wrong result.
        fun heatColor(v: Double): ColorRGBa {
            val t = v.coerceIn(-1.0, 1.0)
            return if (t >= 0.0) ColorRGBa(t, 0.15 * t, 0.45 * t) else ColorRGBa(0.0, 0.0, -t)
        }

        val heatmaps = eigenModes.map { mode ->
            val cb = colorBuffer(mode.width, mode.height)
            val maxAbs = mode.values.maxOf { abs(it) }.coerceAtLeast(1e-12)
            val shadow = cb.shadow
            for (y in 0 until mode.height) {
                for (x in 0 until mode.width) {
                    shadow[x, y] = heatColor(mode.values[y * mode.width + x] / maxAbs)
                }
            }
            shadow.upload()
            cb
        }

        extend {
            for ((index, contour) in contours.withIndex()) drawer.isolated {
                drawer.translate(grid[index].center)

                drawer.image(heatmaps[index], shape.bounds.x, shape.bounds.y, shape.bounds.width, shape.bounds.height)

                val extracted = Shape(contour)
                val clipped = intersection(shapeM, extracted)
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.shape(clipped)

                drawer.stroke = ColorRGBa.WHITE.opacify(0.3)
                drawer.shape(shape)
            }
        }
    }
}
