package spatial

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.color.presets.SADDLE_BROWN
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.shadestyles.spatial.HemisphereLight
import org.openrndr.math.Vector3

/**
 * Demonstrates the [HemisphereLight] shade style, a simple shader
 * that can be used for simple illumination of 3D meshes.
 *
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
            multisample = WindowMultisample.SampleCount(8)
        }

        program {
            val obj = loadOBJasVertexBuffer("demo-data/obj-models/suzanne/Suzanne.obj")

            extend(Orbital()) {
                eye = Vector3(0.0, 0.0, 2.0)
            }
            extend {
                drawer.shadeStyle = HemisphereLight().apply {
                    downColor = ColorRGBa.SADDLE_BROWN
                }
                drawer.vertexBuffer(obj, DrawPrimitive.TRIANGLES)
            }
        }
    }
}