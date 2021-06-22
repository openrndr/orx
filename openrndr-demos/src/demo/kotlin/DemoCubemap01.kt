import org.openrndr.application
import org.openrndr.draw.Cubemap
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Session
import org.openrndr.draw.shadeStyle
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.boxMesh

suspend fun main() = application {
    program {

        val cubemap = Cubemap.fromUrl("file:demo-data/cubemaps/garage_iem.dds", null, session = Session.active)
        val cube = boxMesh()
        extend(Orbital()) {

        }
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    x_fill = texture(p_cubemap, va_position); 
                """
                parameter("cubemap", cubemap)
            }
            drawer.vertexBuffer(cube, DrawPrimitive.TRIANGLES)
        }
    }
}