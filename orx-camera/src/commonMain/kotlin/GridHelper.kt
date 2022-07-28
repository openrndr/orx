package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector3

@Suppress("unused")
class GridHelper(val size: Int = 10, val divisions: Int = 10) : Extension {
    override var enabled: Boolean = true
    private val step = size / divisions.toDouble()

    private val grid = vertexBuffer(
            vertexFormat {
                position(3)
            }
            , 4 * (size * divisions + 1)).apply {
        put {
            val halfSize = size / 2.0
            var k = -halfSize

            for(i in 0 until divisions + 1) {
                write(Vector3(-halfSize, 0.0, k))
                write(Vector3(halfSize, 0.0, k))

                write(Vector3(k, 0.0, -halfSize))
                write(Vector3(k, 0.0, halfSize))

                k += step
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        draw(drawer)
    }

    fun draw(drawer: Drawer) {
        drawer.isolated {
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.WHITE
            drawer.vertexBuffer(grid, DrawPrimitive.LINES)

            // Axis cross
            drawer.stroke = ColorRGBa.RED
            drawer.lineSegment(Vector3.ZERO, Vector3(step, 0.0, 0.0))

            drawer.stroke = ColorRGBa.GREEN
            drawer.lineSegment(Vector3.ZERO, Vector3(0.0, step, 0.0))

            drawer.stroke = ColorRGBa.BLUE
            drawer.lineSegment(Vector3.ZERO, Vector3(0.0, 0.0, step))
        }
    }
}