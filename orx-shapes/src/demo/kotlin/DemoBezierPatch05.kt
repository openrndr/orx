import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BufferMultisample
import org.openrndr.extensions.SingleScreenshot
import org.openrndr.extra.shapes.bezierPatch
import org.openrndr.extra.shapes.drawers.bezierPatch
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.Vector3
import org.openrndr.shape.Segment3D

/**
 * Shows how to
 * - create a [bezierPatch] out of 4 [Segment3D]
 * - create a sub-patch out of a [bezierPatch]
 * - create horizontal and vertical [Path3D]s out of [bezierPatch]es
 * - add colors to a [bezierPatch]
 * - draw a [bezierPatch] surface
 *
 * The created contours are horizontal and vertical in "bezier-patch space" but
 * are rendered deformed following the shape of the bezier patch.
 */
fun main() {
    application {
        configure {
            width = 800
            height = 800
            multisample = WindowMultisample.SampleCount(8)
        }
        program {
            val c0 = Segment3D(Vector3(-5.0, 0.0, -9.0), Vector3(5.0, 0.0, -9.0))
            val c1 = Segment3D(Vector3(-5.0, -5.0, -3.0), Vector3(5.0, -5.0, -3.0))
            val c2 = Segment3D(Vector3(-5.0, 5.0, 3.0), Vector3(5.0, 5.0, 3.0))
            val c3 = Segment3D(Vector3(-5.0, 0.0, 9.0), Vector3(5.0, 0.0, 9.0))

            val col = listOf(ColorRGBa.PINK, ColorRGBa.RED, ColorRGBa.BLUE, ColorRGBa.PINK)
            val cols = listOf(col, col, col, col)
            val bp = bezierPatch(c0, c1, c2, c3).withColors(cols)
            val bpSub = bp.sub(0.1, 0.1, 0.6, 0.6)

            val cam = Orbital()
            extend(cam){
                eye = Vector3(x=9.9, y=12.8, z=6.9)
                lookAt = Vector3(x=1.6, y=-1.9, z=1.2)
            }

            extend {
                drawer.clear(ColorRGBa.PINK)

                drawer.translate(-5.0, 0.0, 0.0)
                // Show the segments that form the bezier patch
                drawer.stroke = ColorRGBa.YELLOW
                drawer.strokeWeight = 50.0
                drawer.segments(listOf(c0, c1, c2, c3))

                // Show the grid
                drawer.strokeWeight = 1.0
                val n = 10
                for (i in 0..n) {
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.lineStrip(bp.horizontal(i / n.toDouble()).adaptivePositions(0.01))
                    drawer.lineStrip(bp.vertical(i / n.toDouble()).adaptivePositions(0.01))

                    drawer.stroke = ColorRGBa.RED
                    drawer.lineStrip(bpSub.horizontal(i / n.toDouble()).adaptivePositions(0.01))
                    drawer.lineStrip(bpSub.vertical(i / n.toDouble()).adaptivePositions(0.01))
                }

                // Draw the colored Bezier surface
                drawer.translate(10.0, 0.0, 0.0)
                drawer.bezierPatch(bp)
            }
        }
    }
}