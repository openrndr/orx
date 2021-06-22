import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extras.meshgenerators.planeMesh
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3


suspend fun main() = application {
    program {

        val shader = Shader.createFromCode(vsCode =
                """
#version 430
in vec3 a_position;
in vec2 a_texCoord0;
in vec3 a_normal;
uniform mat4 projMatrix;
uniform mat4 viewMatrix;

void main() {
    gl_Position = projMatrix * vec4(a_position, 1.0);  
}
        """,
               fsCode = """
#version 430
out vec4 o_color;
layout(rgba8) uniform image2D bla;
void main() {
    imageStore(bla, ivec2(30,30), vec4(1.0, 0.0, 0.0, 1.0));
    o_color =  vec4(1.0);
}
                """, name = "ils")
        val cb = colorBuffer(128, 128)
        val mesh = planeMesh(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_Y, -Vector3.UNIT_Z, 100.0, 100.0)

        extend {
            drawer.clear(ColorRGBa.PINK)
            shader.begin()
            shader.image("bla", 0, cb.imageBinding(0, ImageAccess.READ_WRITE))
            shader.uniform("viewMatrix", drawer.view)
            shader.uniform("projMatrix", drawer.projection)

            Driver.instance.drawVertexBuffer(shader, listOf(mesh), DrawPrimitive.TRIANGLES, 0, mesh.vertexCount)
            shader.end()
            drawer.clear(ColorRGBa.BLACK)
            drawer.image(cb)
        }

    }
}