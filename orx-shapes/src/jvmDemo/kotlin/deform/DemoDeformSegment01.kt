package deform

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.shapes.deform.deform

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {



            extend {
                drawer.stroke = ColorRGBa.PINK

                val b = drawer.bounds.offsetEdges(-20.0)
                val hlines = (0 .. 10).map {
                    b.horizontal(it / 10.0)
                }

                val vlines = (0 .. 10).map {
                    b.vertical(it / 10.0)
                }

                drawer.segments(hlines.flatMap {
                    it.segment.deform( mouse.position, 1.0, 2.0, 100.0)
                })
                drawer.segments(vlines.flatMap {
                    it.segment.deform( mouse.position, 1.0, 2.0, 100.0)
                })
            }
        }
    }
}