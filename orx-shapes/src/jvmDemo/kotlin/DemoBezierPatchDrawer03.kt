import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.extra.shapes.grid
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.min
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.min

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.clear(ColorRGBa.BLACK)
                val colors = listOf(
                    listOf(ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa(), ColorRGBa.PINK.toOKLABa()),
                    listOf(ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa(), ColorRGBa.RED.toOKLABa()),
                    listOf(ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa(), ColorRGBa.BLUE.toOKLABa()),
                    listOf(ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa(), ColorRGBa.WHITE.toOKLABa()),
                )

                val grid = drawer.bounds.grid(4,4, marginX = 20.0, marginY = 20.0, gutterX = 10.0, gutterY = 10.0)

                val cellWidth = grid[0][0].width
                val cellHeight = grid[0][0].height

                val a = bezierPatch(Rectangle.fromCenter(Vector2(0.0, 0.0), cellWidth, cellHeight).contour)
                    .withColors(colors)

                val b = bezierPatch(
                    Circle(0.0, 0.0, min(cellWidth, cellHeight) / 2.0).contour.transform(
                        buildTransform {
                            rotate(Vector3.UNIT_Z, 45.0)
                        }
                    )
                ).withColors(colors)

                for (y in grid.indices) {
                    for (x in grid[y].indices) {
                        val f = (y * grid[y].size + x).toDouble() / (grid.size * grid[y].size - 1.0)
                        val blend = a * (1.0 - f) + b * f
                        drawer.isolated {
                            drawer.translate(grid[y][x].center)
                            drawer.bezierPatch(blend)
                        }
                    }
                }
            }
        }
    }
}