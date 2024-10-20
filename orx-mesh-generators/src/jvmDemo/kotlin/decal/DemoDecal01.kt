package decal

import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.mesh.*
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.meshgenerators.decal.decal
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import java.io.File

/**
 * Demonstrate decal generator as an object slicer
 * @see <img src="https://raw.githubusercontent.com/openrndr/orx/media/orx-mesh-generators/images/decal-DemoDecal01Kt.png">
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val obj = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData().triangulate()

            val slices = 25
            val sliceStep = 0.1
            val sliceWidth = 0.14

            val sliceVBs = (0 until slices).map {
                val projector = buildTransform {
                    translate(0.0, 0.0, -1.0 + it * sliceStep)
                }
                val decal = obj.decal(projector, Vector3(4.0, 4.0, sliceWidth))
                val vb = decal.toVertexBuffer()
                vb
            }

            extend(Orbital()) {
                eye = Vector3(0.0, 0.0, 2.0)
            }
            extend {
                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """x_fill.rgb = v_viewNormal.rgb * 0.5 + 0.5; """
                }

                drawer.translate(0.0, 0.0, slices * 0.5 * 0.5)
                for (i in 0 until sliceVBs.size) {
                    drawer.vertexBuffer(sliceVBs[i], DrawPrimitive.TRIANGLES)
                    drawer.translate(0.0, 0.0, -0.5)
                }
            }
        }
    }
}