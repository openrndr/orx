package simplexrange

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.isolated
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.math.simplexrange.SimplexRange3D
import org.openrndr.math.Vector3

/**
 * Demonstrates the use of the `SimplexRange3D` class. Its constructor takes 4 instances of a `LinearType`
 * (something that can be interpolated linearly, like `ColorRGBa`). The `SimplexRange3D` instance provides
 * a `value()` method that returns a `LinearType` interpolated across the 4 constructor arguments using
 * a normalized 3D coordinate.
 *
 * This demo program creates a 3D grid of 20x20x20 unit 3D cubes. Their color is set by interpolating
 * their XYZ index across the 4 input colors.
 *
 * 2D, 4D and ND varieties are also provided by `SimplexRange`.
 *
 * *Simplex Range* is not to be confused with *Simplex Noise*.
 */
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