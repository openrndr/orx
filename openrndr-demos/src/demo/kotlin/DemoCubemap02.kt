import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh

fun main() = application {
    program {
        val cubemap1 = loadCubemap("demo-data/cubemaps/garage_iem.dds", null, session = Session.active)
        val cube = boxMesh()
        val cubemap2 = cubemap(
            cubemap1.width,
            format = cubemap1.format,
            type = cubemap1.type,
            levels = 2,
            session = Session.active
        )
        cubemap1.copyTo(cubemap2, 0, 0)
        cubemap2.generateMipmaps()

        extend(Orbital())
        extend {
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    x_fill = texture(p_cubemap, va_position); 
                """
                parameter("cubemap", cubemap2)
            }
            drawer.vertexBuffer(cube, DrawPrimitive.TRIANGLES)
        }
    }
}