package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.shapes.bezierpatches.bezierPatch
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import kotlin.math.min

/**
 * Demonstrates how to render a grid of bezier patches that morph between a rectangle and
 * a rotated circle contour.
 * These shapes are transformed into bezier patches, and their colors are interpolated through a blend
 * factor calculated for each cell in the grid.
 *
 * The grid layout contains 4 columns and 4 rows with margins and gutters.
 * Each cell's center serves as the drawing position for a blended bezier patch.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val colors = listOf(
            listOf(
                ColorRGBa.PINK.toOKLABa(),
                ColorRGBa.PINK.toOKLABa(),
                ColorRGBa.PINK.toOKLABa(),
                ColorRGBa.PINK.toOKLABa()
            ),
            listOf(
                ColorRGBa.RED.toOKLABa(),
                ColorRGBa.RED.toOKLABa(),
                ColorRGBa.RED.toOKLABa(),
                ColorRGBa.RED.toOKLABa()
            ),
            listOf(
                ColorRGBa.BLUE.toOKLABa(),
                ColorRGBa.BLUE.toOKLABa(),
                ColorRGBa.BLUE.toOKLABa(),
                ColorRGBa.BLUE.toOKLABa()
            ),
            listOf(
                ColorRGBa.WHITE.toOKLABa(),
                ColorRGBa.WHITE.toOKLABa(),
                ColorRGBa.WHITE.toOKLABa(),
                ColorRGBa.WHITE.toOKLABa()
            ),
        )

        val grid = drawer.bounds.grid(4, 4, marginX = 20.0, marginY = 20.0, gutterX = 10.0, gutterY = 10.0)

        val cellWidth = grid[0][0].width
        val cellHeight = grid[0][0].height

        val a = bezierPatch(Rectangle.fromCenter(Vector2.ZERO, cellWidth, cellHeight).contour)
            .withColors(colors)

        val b = bezierPatch(
            Circle(Vector2.ZERO, min(cellWidth, cellHeight) / 2.0).contour.transform(
                buildTransform {
                    rotate(Vector3.UNIT_Z, 45.0)
                }
            )
        ).withColors(colors)

        extend {
            drawer.clear(ColorRGBa.BLACK)

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
