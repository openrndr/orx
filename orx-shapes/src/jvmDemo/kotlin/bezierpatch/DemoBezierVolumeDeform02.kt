package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.TriangleMeshBuilder
import org.openrndr.extra.meshgenerators.box
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.envelope
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.extra.shapes.bezierpatches.bezierVolumeDeformFunctionGlsl
import org.openrndr.extra.shapes.bezierpatches.setBezierVolume
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates deforming a mesh by two [BezierVolume]s in one shade style, with [bezierVolumeDeformFunctionGlsl], the
 * form of the deformation that takes its volume as arguments, and uniforms that [setBezierVolume] sets with a prefix
 * for each volume.
 *
 * A bar is first twisted by one volume and then bent by a second one, whose box holds the twisted bar. At the top the
 * two deformations are done on the CPU one after the other, at the bottom on the GPU in one shade style; both bars look
 * the same. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val twistBox = Box(Vector3(-6.0, -1.0, -1.0), 12.0, 2.0, 2.0)
        val bendBox = Box(Vector3(-6.0, -2.0, -2.0), 12.0, 4.0, 4.0)
        fun bar(builder: TriangleMeshBuilder) = builder.box(12.0, 2.0, 2.0, 96, 16, 16)
        val cpu = buildTriangleMesh { bar(this) }
        val gpu = buildTriangleMesh { bar(this) }
        fun twist(time: Double) = BezierVolume.fromBox(twistBox).map { p ->
            val a = sin(time * 0.7) * PI * 0.8 * (p.x - twistBox.corner.x) / twistBox.width
            Vector3(p.x, p.y * cos(a) - p.z * sin(a), p.y * sin(a) + p.z * cos(a))
        }
        fun bend(time: Double) = BezierVolume.fromBox(bendBox).map { p ->
            val f = (p.x - bendBox.corner.x) / bendBox.width
            p + Vector3(0.0, sin(time * 0.5) * 4.0 * (f - 0.5) * (f - 0.5), 0.0)
        }
        val light = shadeStyle {
            fragmentTransform = """
                vec3 n = normalize(v_worldNormal);
                float d = 0.25 + 0.75 * max(0.0, dot(n, normalize(vec3(-0.4, 0.6, 0.7))));
                x_fill.rgb = vec3(0.97, 0.51, 0.47) * d;
            """
        }
        // one shade style, two volumes: the function once, its uniforms by prefix
        val deform = shadeStyle {
            vertexPreamble = bezierVolumeDeformFunctionGlsl
            vertexTransform = """
                bezierVolumeDeform(x_position, x_normal, p_e0_Points, p_e0_Corner, p_e0_Size);
                bezierVolumeDeform(x_position, x_normal, p_e1_Points, p_e1_Corner, p_e1_Size);
            """
        }

        extend(Orbital()) {
            eye = Vector3(0.0, 4.0, 24.0)
            fov = 50.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val t = twist(seconds)
            val b = bend(seconds)
            buildTriangleMesh(cpu) {
                bar(this)
                envelope(twistBox, t)
                envelope(bendBox, b)
            }
            deform.setBezierVolume(twistBox, t, "e0_")
            deform.setBezierVolume(bendBox, b, "e1_")
            drawer.isolated {
                drawer.translate(0.0, 5.0, 0.0)
                drawer.shadeStyle = light
                drawer.vertexBuffer(cpu, DrawPrimitive.TRIANGLES)
            }
            drawer.isolated {
                drawer.translate(0.0, -5.0, 0.0)
                drawer.shadeStyle = deform + light
                drawer.vertexBuffer(gpu, DrawPrimitive.TRIANGLES)
            }
        }
    }
}
