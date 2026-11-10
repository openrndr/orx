package bezierpatch

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.box
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.envelope
import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.extra.shapes.bezierpatches.bezierVolumeShadeStyle
import org.openrndr.extra.shapes.bezierpatches.setBezierVolume
import org.openrndr.math.Vector3
import org.openrndr.shape.Box
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Demonstrates deforming meshes in a [BezierVolume], on the CPU and on the GPU.
 *
 * A finely subdivided bar is twisted and bent by a Bezier volume whose control points move over time. At the top the
 * bar is deformed on the CPU, with `TriangleMeshBuilder.envelope`, and written into its vertex buffer every frame. At
 * the bottom the bar is the same undeformed mesh every frame, deformed on the GPU by [bezierVolumeShadeStyle], whose
 * volume is set with [setBezierVolume]. Both map normals along, so the light follows the deformed surface, and both
 * bars look the same. Drag with the mouse to orbit around.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val box = Box(Vector3(-6.0, -1.0, -1.0), 12.0, 2.0, 2.0)
        fun bar(builder: org.openrndr.extra.meshgenerators.TriangleMeshBuilder) = builder.box(12.0, 2.0, 2.0, 96, 16, 16)
        // the same bar in both forms; the CPU one is written again every frame
        val cpu = buildTriangleMesh { bar(this) }
        val gpu = buildTriangleMesh { bar(this) }
        val rest = BezierVolume.fromBox(box)
        fun volume(time: Double) = rest.map { p ->
            val f = (p.x - box.corner.x) / box.width
            val a = sin(time * 0.7) * PI * 0.8 * f
            Vector3(p.x, p.y * cos(a) - p.z * sin(a) + sin(time * 0.5) * 3.0 * f * f, p.y * sin(a) + p.z * cos(a))
        }
        val light = shadeStyle {
            fragmentTransform = """
                vec3 n = normalize(v_worldNormal);
                float d = 0.25 + 0.75 * max(0.0, dot(n, normalize(vec3(-0.4, 0.6, 0.7))));
                x_fill.rgb = vec3(0.35, 0.69, 1.0) * d;
            """
        }
        val deform = bezierVolumeShadeStyle(box, rest)

        extend(Orbital()) {
            eye = Vector3(0.0, 4.0, 24.0)
            fov = 50.0
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val v = volume(seconds)
            buildTriangleMesh(cpu) {
                bar(this)
                envelope(box, v)
            }
            deform.setBezierVolume(box, v)
            drawer.isolated {
                drawer.translate(0.0, 4.5, 0.0)
                drawer.shadeStyle = light
                drawer.vertexBuffer(cpu, DrawPrimitive.TRIANGLES)
            }
            drawer.isolated {
                drawer.translate(0.0, -4.5, 0.0)
                drawer.shadeStyle = deform + light
                drawer.vertexBuffer(gpu, DrawPrimitive.TRIANGLES)
            }
        }
    }
}
