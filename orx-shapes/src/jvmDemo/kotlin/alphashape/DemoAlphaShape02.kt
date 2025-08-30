package alphashape

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.shapes.uniform
import org.openrndr.extra.shapes.alphashape.AlphaShape
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Demonstrates the use of [AlphaShape] to create ten
 * [org.openrndr.shape.ShapeContour] instances out of a collection of random [Vector2] points.
 *
 * The same points are used for each contour, but an increased alpha parameter
 * is passed to the AlphaShape algorithm. Higher values return more convex shapes
 * = shapes with a larger surface.
 *
 * The list of shapes is reversed to draw the smaller contours on top, otherwise only
 * the last one would be visible.
 *
 * An instance of [Random] with a fixed seed is used to ensure the resulting
 * random shape is always the same.
 */
fun main() = application {
    program {
        val rand = Random(242)
        val points = List(40) {
            drawer.bounds.uniform(rand)
        }
        val alphaShape = AlphaShape(points)
        val minAlpha = alphaShape.determineContourAlpha()

        val contours = List(10) {
            alphaShape.createContour(minAlpha + it * it * it)
        }.reversed()
        extend {
            drawer.stroke = null
            contours.forEachIndexed { index, contour ->
                drawer.fill = ColorRGBa.PINK.shade(0.5 + index * 0.07)
                drawer.contour(contour)
            }

            drawer.fill = ColorRGBa.WHITE
            drawer.circles(points, 4.0)
        }
    }
}