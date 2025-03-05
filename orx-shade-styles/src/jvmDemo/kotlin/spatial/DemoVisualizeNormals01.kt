package spatial

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.shadestyles.spatial.visualizeNormals
import org.openrndr.math.Vector3

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

                drawer.shadeStyle = visualizeNormals
                drawer.vertexBuffer(obj, DrawPrimitive.TRIANGLES)

            }

        }
    }
}