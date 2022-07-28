// Demonstration of circles that always face the camera

import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.camera.Orbital
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3


fun main() = application {

    configure {
        multisample = WindowMultisample.SampleCount(8)
    }
    program {
        extend(Orbital())


        val circlePositions = vertexBuffer(vertexFormat {
            attribute("position", VertexElementType.VECTOR3_FLOAT32)
            attribute("scale", VertexElementType.FLOAT32)
        }, 1000)

        circlePositions.put {
            for (i in 0 until circlePositions.vertexCount) {
                write(Vector3.uniformRing(0.0, 3.0))
                write(Math.random().toFloat()*0.1f)
            }
        }


        extend {
            drawer.perspective(90.0, width*1.0/height*1.0, 0.1, 100.0)

            drawer.fill = ColorRGBa.PINK
            drawer.stroke = ColorRGBa.GREEN
            drawer.strokeWeight = 0.05
            drawer.drawStyle.alphaToCoverage = true

            drawer.depthWrite = true
            drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL

            drawer.shadeStyle = shadeStyle {
                vertexTransform = """
                    vec3 viewOffset = (x_viewMatrix * x_modelMatrix * vec4(i_position, 1.0)).xyz;
                    vec2 i = vec2(0.0, 1.0);
                    x_viewMatrix = mat4(i.yxxx, i.xyxx, i.xxyx, i.xxxy);
                    x_modelMatrix = mat4(i.yxxx, i.xyxx, i.xxyx, i.xxxy);
                    x_position = viewOffset + vec3(a_position.xy * i_scale, 0.0);
                    vi_radius = vec2(i_scale);
                """.trimIndent()
                attributes(circlePositions)
            }

            drawer.circles((0 until circlePositions.vertexCount).map {  Vector2.ZERO }, 0.0)
        }
    }
}