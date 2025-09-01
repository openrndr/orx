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
                    clipFit = FillFit.entries[index/3]
                    feather = gf

                    clipTransform = transform {
                        translate(Vector2(0.5, 0.5))
                        rotate(36.0 * seconds)
                        translate(Vector2(-0.5, -0.5))
                    }

                    ellipse {
                        radiusX = 0.5
                        radiusY = 0.25
                    }
                }

                val acell = when(val i = index%3) {
                    1 -> cell.sub(0.0..0.5, 0.0..1.0)
                    2 -> cell.sub(0.0..1.0, 0.0..0.5)
                    else -> cell
                }

                drawer.imageFit(image, acell.placeIn(cell))
            }
        }
    }
}