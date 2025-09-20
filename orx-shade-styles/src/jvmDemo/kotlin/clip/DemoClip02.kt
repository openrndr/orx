package clip

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.clip.clip
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.extra.shapes.primitives.placeIn
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import kotlin.math.PI
import kotlin.math.cos

/**
 * Animated demonstration on how to use the `clip` shade style to mask-out
 * part of an image (or anything else drawn while the shade style is active).
 * The clipping uses different fit modes on each row, and different aspect
 * ratios in each column.
 *
 * This example uses a rotating `star`-shaped clipping with 24 sides.
 * Other available clipping shapes are `circle`, `rectangle`, `line` and `ellipse`.
 *
 * Press a mouse button to toggle the `feather` property between 0.0 and 0.5.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }

    program {
        var gf = 0.0
        mouse.buttonDown.listen {
            gf = 0.1 - gf
        }

        val image = loadImage("demo-data/images/image-001.png")
        extend {

            val grid = drawer.bounds.grid(3, 3)
            for ((index, cell) in grid.flatten().withIndex()) {

                drawer.shadeStyle = clip {
                    clipFit = FillFit.entries[index / 3]
                    feather = gf

                    clipTransform = transform {
                        translate(Vector2(0.5, 0.5))
                        rotate(36.0 * seconds)
                        translate(Vector2(-0.5, -0.5))
                    }

                    star {
                        radius = 0.5
                        center = Vector2(0.5, 0.5)
                        sharpness = cos(2 * PI * index / 9.0 + seconds) * 0.25 + 0.5
                        sides = 24
                    }
                }

                // Use sub() on squares to create vertical or horizontal rectangles
                val acell = when (val i = index % 3) {
                    1 -> cell.sub(0.0..0.5, 0.0..1.0)
                    2 -> cell.sub(0.0..1.0, 0.0..0.5)
                    else -> cell
                }

                drawer.imageFit(image, acell.placeIn(cell))
            }
        }
    }
}