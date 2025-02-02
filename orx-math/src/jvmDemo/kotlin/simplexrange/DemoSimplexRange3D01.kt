package simplexrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import org.openrndr.math.Vector3

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }

        program {
            val box = boxMesh()
            extend(Orbital()) {
                eye = Vector3(1.0, -1.0, 1.0).normalized * 140.0
                fov = 15.0
            }
            extend {
                val sr = SimplexRange3D(
                    ColorRGBa.PINK.toLinear(),
                    ColorRGBa.RED.toLinear(),
                    ColorRGBa.MAGENTA.toLinear(),
                    ColorRGBa.BLUE.toLinear()
                )

                for (z in 0 until 20)
                    for (y in 0 until 20)
                        for (x in 0 until 20) {
                            drawer.isolated {
                                drawer.translate(x - 10.0, y - 10.0, z - 10.0)
                                drawer.fill = sr.value(x / 20.0, y / 20.0, z / 20.0)
                                drawer.vertexBuffer(box, DrawPrimitive.TRIANGLES)
                            }
                        }
            }
        }
    }
}