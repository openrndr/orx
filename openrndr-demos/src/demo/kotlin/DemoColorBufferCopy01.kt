import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extras.camera.Orbital
import org.openrndr.extras.meshgenerators.boxMesh

suspend fun main() = application {
    program {
        val cb0 = loadImage("demo-data/images/image-001.png")
        val cb1 = cb0.createEquivalent()
        extend {
            cb0.copyTo(cb1)
            drawer.image(cb1)
        }
    }
}