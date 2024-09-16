import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.objloader.saveOBJ

fun main() = application {
    configure {
        height = 100
    }
    program {
        val path = "demo-data/obj-models"
        val mesh = loadOBJasVertexBuffer("$path/suzanne/Suzanne.obj")
        mesh.saveOBJ("$path/Suzanne-exported.obj")

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)

        extend {
            drawer.fontMap = font
            drawer.text(".obj file loaded and saved", 10.0, 80.0)
        }
    }
}