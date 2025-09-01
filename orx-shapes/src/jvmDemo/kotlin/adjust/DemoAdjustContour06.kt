package adjust

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.adjust.adjustContour
import org.openrndr.shape.Circle
import kotlin.math.cos

/**
 * Demonstrates the use of `adjustContour`
 * to create an animated effect where edges are split, vertices are selected,
 * and transformations such as scaling are applied.
 *
 * The program creates a circular contour which is modified on each animation frame.
 *
 * - Edges of the circular contour are split dynamically based on a time-based cosine function.
 * - Newly created vertices are selected and scaled around the center of the contour
 *   using time-dependent transformations.
 *
 * The selection of vertices happens automatically thanks to
 * `parameters.clearSelectedVertices` and `parameters.selectInsertedVertices`
 *
 * The modified animated contour is finally drawn.
 */
fun main() = application {
    configure {
        width = 800
        height = 800
    }
    program {
        extend {
            var contour =
                Circle(drawer.bounds.center, 300.0).contour

            contour = adjustContour(contour) {
                parameters.clearSelectedVertices = true
                parameters.selectInsertedVertices = true


                for (i in 0 until 4) {
                    val splitT = cos(seconds + i * Math.PI * 0.5) * 0.2 + 0.5
                    selectEdges { it -> true }
                    for (e in edges) {
                        e.splitAt(splitT)
                    }
                    // as a result of the clearSelectedVertices and selectInsertedVertices settings,
                    // the vertex selection is set to the newly inserted vertices
                    for ((index, v) in vertices.withIndex()) {
                        v.scale(cos(seconds + i + index) * 0.5 * (1.0 / (1.0 + i)) + 1.0, drawer.bounds.center)
                    }
                }
            }
            drawer.stroke = ColorRGBa.RED
            drawer.contour(contour)
        }
    }
}
