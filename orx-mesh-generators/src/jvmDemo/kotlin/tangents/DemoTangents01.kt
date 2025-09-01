package tangents

import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.mesh.toVertexBuffer
import org.openrndr.extra.meshgenerators.tangents.estimateTangents
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.math.Vector3
import java.io.File

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val obj = loadOBJMeshData(File("demo-data/obj-models/suzanne/Suzanne.obj")).toMeshData().triangulate()
        val tangentObj = obj.estimateTangents()

        val objVB = tangentObj.toVertexBuffer()

        extend(Orbital()) {
            eye = Vector3(0.0, 0.0, 2.0)
        }
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec3 viewTangent = (u_viewNormalMatrix * u_modelNormalMatrix * vec4(va_tangent, 0.0)).xyz;
                    vec3 viewBitangent = (u_viewNormalMatrix * u_modelNormalMatrix * vec4(va_bitangent, 0.0)).xyz;
                    float c = cos(100.0*dot(v_worldPosition, va_normal)) * 0.5 + 0.5;
                    
                    //x_fill.rgb = normalize(viewTangent)*0.5+0.5;
                     x_fill.rgb = vec3(c); 
                     """.trimIndent()

            }

            drawer.vertexBuffer(objVB, DrawPrimitive.TRIANGLES)
        }
    }
}