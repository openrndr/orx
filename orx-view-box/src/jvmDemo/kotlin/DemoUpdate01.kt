import org.openrndr.application
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val vbx = viewBox(Rectangle(0.0, 0.0, 200.0, 200.0)) {
                extend(Camera2D())
                extend {
                    drawer.rectangle(20.0, 20.0, 100.0, 100.0)
                }
            }

            extend {
                vbx.update()
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        drawer.image(vbx.result, j * 200.0, i * 200.0)
                    }
                }
            }
        }
    }
}