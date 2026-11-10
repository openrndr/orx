package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toPath3D
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates how
 * [`Path3D.envelope`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/envelope.html)
 * with a [BezierVolume] deforms 3D paths: free-form deformation.
 *
 * A bundle of coils fills a box. A Bezier volume made with [BezierVolume.fromBox] would leave them as they are; here
 * every layer of its control points along the length of the box is turned around the axis of the box, further for the
 * layers further along, by an angle that swings back and forth over time. The coils twist with the volume. The lattice
 * of control points is drawn too. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val box = Box(Vector3(-6.0, -2.0, -2.0), 12.0, 4.0, 4.0)
        // coils along the length of the box, around 5 axes through it
        val coils = listOf(Vector3(0.0, 0.0, 0.0), Vector3(0.0, 1.2, 1.2), Vector3(0.0, -1.2, 1.2), Vector3(0.0, 1.2, -1.2), Vector3(0.0, -1.2, -1.2)).map { axis ->
            List(97) { i ->
                val a = i * PI / 8
                Vector3(-6.0 + i * 0.125, axis.y + cos(a) * 0.6, axis.z + sin(a) * 0.6)
            }.catmullRom(0.5, false).toPath3D()
        }
        val rest = BezierVolume.fromBox(box)

        extend(Orbital()) {
            eye = Vector3(5.0, 8.0, 18.0)
            fov = 55.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            // the layers of control points along x turn around the axis of the box, the far ones further
            val twist = sin(seconds * 0.8) * PI * 0.75
            val volume = rest.map { p ->
                val a = twist * (p.x - box.corner.x) / box.width
                Vector3(p.x, p.y * cos(a) - p.z * sin(a), p.y * sin(a) + p.z * cos(a))
            }
            // the lattice of control points: its rows along x, y and z
            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            drawer.strokeWeight = 1.5
            for (k in 0 until 4) for (j in 0 until 4) {
                drawer.lineStrip(List(4) { volume.points[k][j][it] })
                drawer.lineStrip(List(4) { volume.points[k][it][j] })
                drawer.lineStrip(List(4) { volume.points[it][k][j] })
            }
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff")
            drawer.strokeWeight = 3.0
            for (coil in coils) drawer.path(coil.envelope(box, volume, errorTolerance = 0.01))
        }
    }
}
