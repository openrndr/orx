
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.viewbox.viewBox
import org.openrndr.shape.Rectangle

fun main() {
    application {
        configure {
            width = 800
            height = 800
        }
        program {
            val vbx = viewBox(Rectangle(0.0, 0.0, 200.0, 800.0)) {
                extend(Screenshots())
                extend(Camera2D())
                extend {
                    drawer.rectangle(20.0, 20.0, 100.0, 100.0)
                }
            }

            val vbx2 = viewBox(Rectangle(200.0, 0.0, 200.0, 800.0)) {
                extend(Post()) {
                    val blur = ApproximateGaussianBlur()
                    blur.sigma = 10.0
                    blur.window = 25
                    post { i, o ->
                        blur.apply(i, o)
                    }
                }
                extend(Camera2D())
                extend {
                    drawer.rectangle(20.0, 20.0, 100.0, 100.0)
                }
            }

            val vbx3d = viewBox(Rectangle(400.0, 0.0, 400.0, 800.0)) {
                extend(Orbital())
                val cube = boxMesh()
                extend {
                    drawer.vertexBuffer(cube, DrawPrimitive.TRIANGLES)
                }
            }

            extend {
                vbx.draw()
                vbx2.draw()
                vbx3d.draw()
            }
        }
    }
}