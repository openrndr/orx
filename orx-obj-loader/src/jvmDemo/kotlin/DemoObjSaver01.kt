import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.objloader.loadOBJasVertexBuffer
import org.openrndr.extra.objloader.saveOBJ

/**
 * Demonstrates how to save a `VertexBuffer` as an `.obj` file using the
 * `VertexBuffer.saveOBJ()` method.
 *
 * The program loads an existing OBJ file, then saves it with a new file name.
 */
fun main() = application {
    configure {
        width = 720
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