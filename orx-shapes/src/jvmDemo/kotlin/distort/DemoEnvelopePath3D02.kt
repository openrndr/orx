package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.distort.envelope
import org.openrndr.extra.shapes.path3d.toPath3D
import org.openrndr.extra.shapes.text.shapesFromText
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.bounds
import kotlin.math.sin

/**
 * Demonstrates how
 * [`Path3D.envelope`](https://orx.openrndr.org/orx-shapes/org.openrndr.extra.shapes.distort/envelope.html)
 * with a [BezierPatch3D] lays 3D paths onto a curved surface.
 *
 * The outlines of the text "ENVELOPE" are turned into 3D paths in the plane of z = 0. Their x and y in the bounds of
 * the text give the u and v on a Bezier patch whose control points ripple over time, so the text lies on the surface.
 * A copy of the outlines is lifted off the surface along its normal by giving its points a z: the distance follows
 * the normal wherever the surface bends. The patch is drawn as a grid of its curves. Drag with the mouse to orbit
 * around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(64.0)
        // the outlines of the letters, upside up, as paths in the plane of z = 0
        val outlines = shapesFromText(face, "ENVELOPE").flatMap { it.contours }
            .map { it.transform(transform { scale(1.0, -1.0) }).toPath3D() }
        val basis = shapesFromText(face, "ENVELOPE").flatMap { it.contours }
            .map { it.transform(transform { scale(1.0, -1.0) }) }.map { it.bounds }.bounds.offsetEdges(16.0)
        val lifted = outlines.map { p -> p.transform(transform { translate(0.0, 0.0, 0.6) }) }

        extend(Orbital()) {
            eye = Vector3(0.0, -12.0, 17.0)
            fov = 55.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            // a patch 16 wide and 6 high, its control points rippling up and down
            val patch = BezierPatch3D(List(4) { j ->
                List(4) { i ->
                    Vector3(-8.0 + i * 16.0 / 3.0, -3.0 + j * 2.0, sin(seconds * 1.3 + i * 1.7 + j * 0.9) * 2.0)
                }
            })
            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            drawer.strokeWeight = 1.5
            for (k in 0..8) {
                drawer.path(patch.horizontal(k / 8.0))
                drawer.path(patch.vertical(k / 8.0))
            }
            drawer.strokeWeight = 3.0
            drawer.stroke = ColorRGBa.fromHex("#5ab0ff")
            for (o in outlines) drawer.path(o.envelope(basis, patch, errorTolerance = 0.005))
            drawer.stroke = ColorRGBa.fromHex("#f88379")
            for (o in lifted) drawer.path(o.envelope(basis, patch, errorTolerance = 0.005))
        }
    }
}
