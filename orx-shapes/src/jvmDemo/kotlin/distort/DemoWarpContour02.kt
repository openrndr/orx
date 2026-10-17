package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.primitives.Pulley
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment

/**
 * Advanced demonstration of [warp](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/warp.html).
 *
 * Here, a grid of irregularly sized rectangles is created and displayed in gray. The rectangles are
 * warped using:
 *
 * - A horizontal rectified contour as the warping `base` (in yellow)
 * - A deformed pulley as the `warp` contour (in yellow). `adjustContour` is used to slightly rotate the vertices of the
 *   pulley.
 *
 * The resulting wobbly grid is shown in pink.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val c = adjustContour(
                Pulley(
                    Circle(drawer.bounds.position(0.35, 0.5), 150.0),
                    Circle(drawer.bounds.position(0.65, 0.5), 150.0)
                ).contour
            ) { vertices.forEachIndexed { i, v -> v.rotate(simplex(i, 0.5) * 15.0) } }.rectified()

            val grid = drawer.bounds
                .sub(0.0, 0.31, 1.0, 0.61)
                .grid(40, 10, 0.0, 10.0, 5.0, 5.0)
                .flatten().map { it.offsetEdges(simplex(274, it.corner * 0.01) * 3.0).contour.rectified() }

            val b = LineSegment(0.0, height * 0.5, width + 5.0, height * 0.5).contour.rectified()
            val w = grid.map { it.warp(b, c) }

            extend {
                drawer.fill = null

                drawer.strokeWeight = 1.0 / 3.0
                drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
                drawer.contours(grid.map { it.contour })

                drawer.stroke = ColorRGBa.YELLOW
                drawer.contour(c.contour)
                drawer.contour(b.contour)

                drawer.fill = ColorRGBa.PINK.opacify(0.5)
                drawer.stroke = ColorRGBa.PINK
                drawer.contours(w)
            }
        }
    }
}