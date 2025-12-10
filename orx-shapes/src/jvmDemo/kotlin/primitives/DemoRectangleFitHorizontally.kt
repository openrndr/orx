package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noise.shapes.uniformSub
import org.openrndr.extra.shapes.primitives.alignToHorizontally
import org.openrndr.extra.shapes.primitives.alignToVertically
import org.openrndr.extra.shapes.primitives.fitHorizontally
import org.openrndr.extra.shapes.primitives.fitVertically
import kotlin.math.cos
import kotlin.random.Random

/**
 * This program animates two sets of 7 rectangles.
 *
 * The first set contains vertical rectangles with random widths.
 * The call to `fitHorizontally()` adjusts the widths of the rectangles
 * so they cover the available horizontal space, leaving a gutter space
 * between the shapes. In this demo the gutter space is adjusted based
 * on the vertical mouse position.
 * The vertical position of the rectangles is animated between
 * top-aligned and bottom-aligned using `seconds` and the sine function.
 *
 * The second set contains horizontal rectangles, their heights are
 * adjusted using `fitVertically()` to cover the available vertical space,
 * and are animated horizontally between left-aligned and right-aligned.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend {
            val rs = (0 until 7).map { drawer.bounds.uniformSub(minWidth = 0.01, maxWidth = 0.1, random = Random(it + 123)) }
                .fitHorizontally(drawer.bounds, gutter = 30.0 * mouse.position.y / height)
                .alignToVertically(drawer.bounds, cos(seconds) * 0.5 + 0.5)

            drawer.rectangles(rs)


            val rsh = (0 until 7).map { drawer.bounds.uniformSub(minHeight = 0.01, maxHeight = 0.1, random = Random(it + 101)) }
                .fitVertically(drawer.bounds, gutter = 30.0 * mouse.position.y / height)
                .alignToHorizontally(drawer.bounds, cos(seconds) * 0.5 + 0.5)

            drawer.rectangles(rsh)
        }
    }
}
