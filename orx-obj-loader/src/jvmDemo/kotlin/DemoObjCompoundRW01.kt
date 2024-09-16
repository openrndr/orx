import org.openrndr.application
import org.openrndr.extra.objloader.loadOBJMeshData
import org.openrndr.extra.objloader.toObj
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