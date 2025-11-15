package primitives

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.primitives.place
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Demonstrates the use of the `Rectangle.place()` method which can be used to place rectangles
 * relative to another rectangle (the drawer bounds in this case).
 *
 * The program renders a 10x10 grid of rectangles within the bounds of the canvas.
 * Each rectangle's position is calculated relative to its anchors, filling the entire
 * canvas with evenly placed items.
 *
 * The rectangles are drawn using the default white color. The `Rectangle.place()` function is applied to each
 * rectangle to position them dynamically based on their relative anchor points within the bounding area.
 *
 * This serves as a demonstration of positioning and rendering shapes in a structured grid layout.
 *
 * Note that `place()` has `anchor` and `itemAnchor` arguments.
 * By default `itemAnchor` equals `anchor`.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                val item = Rectangle(200.0, 200.0, 40.0, 40.0)

                drawer.fill = ColorRGBa.WHITE
                for (j in 0 until 10) {
                    for (i in 0 until 10) {
                        drawer.rectangle(drawer.bounds.place(item, Vector2(i / 9.0, j / 9.0)))
                    }
                }
            }
        }
    }
}