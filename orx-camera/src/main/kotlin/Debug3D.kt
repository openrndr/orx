package org.openrndr.extras.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3

@Suppress("unused")
class Debug3D(eye: Vector3 = Vector3(0.0, 0.0, 10.0), lookAt: Vector3 = Vector3.ZERO, fov:Double = 90.0, userInteraction:Boolean = true) : Extension {

    override var enabled: Boolean = true
    var showGrid = false
    val orbitalCamera = OrbitalCamera(eye, lookAt, fov)
    private val orbitalControls = OrbitalControls(orbitalCamera, userInteraction)
    private var lastSeconds: Double = -1.0

    private val grid = vertexBuffer(
            vertexFormat {
                position(3)
            }
            , 4 * 21).apply {
        put {
            for (x in -10..10) {
                write(Vector3(x.toDouble(), 0.0, -10.0))
                write(Vector3(x.toDouble(), 0.0, 10.0))
                write(Vector3(-10.0, 0.0, x.toDouble()))
                write(Vector3(10.0, 0.0, x.toDouble()))
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (lastSeconds == -1.0) lastSeconds = program.seconds

        val delta = program.seconds - lastSeconds
        lastSeconds = program.seconds
        orbitalCamera.update(delta)

        drawer.background(ColorRGBa.BLACK)
        drawer.perspective(orbitalCamera.fov, program.window.size.x / program.window.size.y, 0.1, 1000.0)
        drawer.view = orbitalCamera.viewMatrix()

        if (showGrid) {
            drawer.isolated {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = ColorRGBa.WHITE
                drawer.vertexBuffer(grid, DrawPrimitive.LINES)

                // Axis cross
                drawer.fill = ColorRGBa.RED
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_X)

                drawer.fill = ColorRGBa.GREEN
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Y)

                drawer.fill = ColorRGBa.BLUE
                drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Z)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.view = Matrix44.IDENTITY
            drawer.ortho()
        }
    }

    override fun setup(program: Program) {
        orbitalControls.setup(program)
    }
}