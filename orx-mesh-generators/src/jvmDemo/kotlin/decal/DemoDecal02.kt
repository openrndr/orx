package decal

import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.mesh.toVertexBuffer
import org.openrndr.extra.meshgenerators.decal.decal
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import java.io.File
import kotlin.math.PI

/**
 * Demonstrate decal generation and rendering
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            /** base object */
            val obj = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj"))
                .toMeshData() // convert from CompoundMeshData to MeshData
                .triangulate() // convert to triangles, we need this for the decal generation steps

            /** object [VertexBuffer] */
            val objVB = obj.toVertexBuffer()


            /** positions for the decal projectors */
            val decalPositions = listOf(
                Vector3(0.35, 0.245, 0.8),
                Vector3(-0.35, 0.245, 0.8)
            )

            /** decal vertex buffers */
            val decalVBs = decalPositions.map {
                val projector = buildTransform {
                    translate(it)
                }
                val decal = obj.decal(projector, Vector3(2.0, 2.0, 0.5))
                val vb = decal.toVertexBuffer()
                vb
            }

            extend(Orbital()) {
                eye = Vector3(0.0, 0.0, 2.0)
            }
            extend {
                /* draw the base mesh */
                drawer.isolated {
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """x_fill.rgb = vec3(v_viewNormal * 0.5 + 0.5); """
                    }
                    drawer.vertexBuffer(objVB, DrawPrimitive.TRIANGLES)
                }

                /* draw the decals */
                drawer.isolated {
                    for ((index, decal) in decalVBs.withIndex()) {
                        /* offset the projection transform to avoid z-fighting */
                        drawer.projection = buildTransform {
                            translate(0.0, 0.0, -1e-4)
                        } * drawer.projection

                        /* draw effects on the decal geometry */
                        drawer.shadeStyle = shadeStyle {
                            fragmentTransform = """
                                float d = length(va_texCoord0.xy - vec2(0.5));
                                float sd = smoothstep(-0.01, 0.01, cos(p_time + d * 3.1415 * 2.0 * 10.0));
                                float l = max(0.0, va_normal.z);
                                x_fill = vec4(0.0, 0.0, 0.0, l * sd * 0.5); """
                            parameter("time", seconds * PI * 2 + index * PI)
                        }
                        drawer.vertexBuffer(decal, DrawPrimitive.TRIANGLES)
                    }
                }
            }
        }
    }
}