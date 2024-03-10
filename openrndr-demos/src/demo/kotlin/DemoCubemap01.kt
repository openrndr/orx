import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh

fun main() = application {
    program {

        val cubemap = loadCubemap("demo-data/cubemaps/garage_iem.dds", null, session = Session.active)
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