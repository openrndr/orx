package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.font.loadFace
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.patchClip
import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.PatchClip
import org.openrndr.extra.shapes.text.shapesFromText
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Shape
import org.openrndr.shape.bounds
import kotlin.math.sin

/**
 * Demonstrates [PatchClip]: the part of a Bezier patch inside a shape, tessellated, as a solid.
 *
 * The letters of "CLIP" are cut out of a patch whose control points ripple over time, and given a thickness along the
 * normal of the patch, so they stand on the surface as solid letters that bend with it. The tessellation is made once,
 * in the u, v square of the patch, clipped to a grid so that the letters bend inside too; every frame only maps it onto
 * the patch as it is and writes it into the same vertex buffer. The patch itself is drawn as a grid of its curves.
 * Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val face = loadFace("demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(96.0)
        // the letters upside up, in a rectangle around them that maps to the whole patch
        val letters = Shape(shapesFromText(face, "CLIP").flatMap { it.contours }.map { it.transform(transform { scale(1.0, -1.0) }) })
        val basis = letters.bounds.offsetEdges(24.0)
        val clip = PatchClip(letters, basis, gridSize = 48, distanceTolerance = 0.25)

        fun patch(time: Double) = BezierPatch3D(List(4) { j ->
            List(4) { i ->
                Vector3(-8.0 + i * 16.0 / 3.0, -4.0 + j * 8.0 / 3.0, sin(time * 1.1 + i * 1.6 + j * 1.1) * 2.5)
            }
        })
        val mesh = buildTriangleMesh { patchClip(clip, patch(0.0), thickness = 0.6) }
        val light = shadeStyle {
            fragmentTransform = """
                vec3 n = normalize(v_worldNormal);
                float d = 0.3 + 0.7 * max(0.0, dot(n, normalize(vec3(-0.4, 0.5, 0.8))));
                x_fill.rgb = vec3(0.35, 0.69, 1.0) * d;
            """
        }

        extend(Orbital()) {
            eye = Vector3(0.0, -10.0, 14.0)
            fov = 55.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val p = patch(seconds)
            buildTriangleMesh(mesh) { patchClip(clip, p, thickness = 0.6) }

            drawer.stroke = ColorRGBa.WHITE.opacify(0.2)
            drawer.strokeWeight = 1.0
            for (k in 0..8) {
                drawer.path(p.horizontal(k / 8.0))
                drawer.path(p.vertical(k / 8.0))
            }
            drawer.shadeStyle = light
            drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
        }
    }
}
