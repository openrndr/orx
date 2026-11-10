package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shapes.distort.warp
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D
import org.openrndr.shape.Segment3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how
 * [`RectifiedPath3D.warp`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/warp.html)
 * moves 3D paths from following one path to following another.
 *
 * Two coils wind around a straight basis line along x, one close and one wide. The warp path is a curve through
 * points that move over time. Every point of the coils is found by its nearest point on the basis, its offset there is
 * measured in the frame of the basis, and it is placed at the same offset in the frame of the warp path at the same
 * place along it: the coils wind around the moving curve as they wind around the line. Where the curve bends more
 * tightly than a coil is wide, the coil folds there. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val basis = Path3D(listOf(Segment3D(Vector3(-8.0, 0.0, 0.0), Vector3(8.0, 0.0, 0.0))), false).rectified()
        fun coil(radius: Double, turns: Int, phase: Double) = List(turns * 16 + 1) { i ->
            val a = i * PI / 8 + phase
            Vector3(-8.0 + i * 16.0 / (turns * 16), cos(a) * radius, sin(a) * radius)
        }.catmullRom(0.5, false).toPath3D().rectified()
        val close = coil(0.4, 12, 0.0)
        val wide = coil(0.9, 6, PI)

        extend(Orbital()) {
            eye = Vector3(0.0, 10.0, 22.0)
            fov = 55.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val warp = List(5) { i ->
                val x = -8.0 + i * 16.0 / 4.0
                Vector3(x, sin(seconds * 0.9 + i * 1.3) * 1.6, cos(seconds * 0.7 + i * 1.1) * 1.6)
            }.catmullRom(0.5, false).toPath3D().rectified()

            drawer.stroke = ColorRGBa.WHITE.opacify(0.25)
            drawer.strokeWeight = 1.5
            drawer.path(basis.path)
            drawer.path(warp.path)
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff")
            drawer.strokeWeight = 3.0
            drawer.path(close.warp(basis, warp, errorTolerance = 0.01))
            drawer.stroke = ColorRGBa.fromHex("#f88379")
            drawer.path(wide.warp(basis, warp, errorTolerance = 0.01))
        }
    }
}
