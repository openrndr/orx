import org.openrndr.application
import org.openrndr.draw.loadFont
import org.openrndr.extra.meshgenerators.buildTriangleMesh
import org.openrndr.extra.meshgenerators.sphere
import org.openrndr.extra.objloader.saveOBJ

fun main() = application {
    configure {
        height = 100
    }
    program {
        val path = "demo-data/obj-models/"
        val mesh = buildTriangleMesh {
            repeat(4) { x ->
                repeat(4) { y ->
                    repeat(4) { z ->
                        isolated {
                            translate(x * 1.0, y * 1.0, z * 1.0)
                            sphere(8, 8,
                                (x * 91 + y * 79 + z * 17).mod(5) * 0.2 + 0.1)
                        }
                    }
                }
            }
        }
        mesh.saveOBJ("$path/sphere-composition-exported.obj")

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)

        extend {
            drawer.fontMap = font
            drawer.text("Mesh generated and .obj file saved", 10.0, 80.0)
        }
    }
}