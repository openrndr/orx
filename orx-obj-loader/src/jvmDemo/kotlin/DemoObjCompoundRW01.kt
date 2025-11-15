import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.objloader.toObj
import org.openrndr.math.Vector2
import java.io.File

/**
 * This program loads an OBJ mesh as a CompoundMeshData and demonstrates
 * how to convert it to a OBJ String representation, then
 * draws the beginning of this String on the program window.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val path = "demo-data/obj-models"
        val cm = loadOBJMeshData(File("$path/suzanne/Suzanne.obj"))

        // Convert mesh data to Wavefront OBJ String representation
        val obj = cm.toObj()

        //println(obj)

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)

        extend {
            // Draw part of the OBJ data as text
            drawer.fontMap = font
            drawer.texts(obj.split("\n").take(50), List(50) {
                Vector2(10.0, 20.0 + it * 20.0)
            })
        }
    }
}
