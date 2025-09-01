package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.presets.CORAL
import org.openrndr.extra.noise.uniforms
import org.openrndr.extra.shapes.primitives.column
import org.openrndr.extra.shapes.primitives.irregularGrid
import org.openrndr.extra.shapes.primitives.row
import kotlin.random.Random

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val r = Random(100)
            val grid = drawer.bounds.irregularGrid(
                Double.uniforms(13, 0.1, 0.5, r),
                Double.uniforms(13, 0.1, 0.5, r),
                20.0, 20.0
            )

            drawer.fill = null
            drawer.stroke = ColorRGBa.WHITE
            drawer.rectangles(grid.flatten())

            drawer.stroke = ColorRGBa.BLACK
            drawer.fill = ColorRGBa.PINK.opacify(0.5)
            drawer.rectangles(grid.column(2))

            drawer.fill = ColorRGBa.CORAL.opacify(0.5)
            drawer.rectangles(grid.row(6))
        }
    }
}