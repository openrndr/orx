import org.openrndr.application
import org.openrndr.extra.mesh.loadOBJMeshData
import org.openrndr.extra.mesh.toObj
import java.io.File

fun main() {
    application {
        program {
            val path = "demo-data/obj-models"
            val cm = loadOBJMeshData(File("$path/suzanne/Suzanne.obj"))

            println(cm.toObj())
        }
    }
}