package org.openrndr.extras.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix33
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3


@Suppress("unused")
class AxisHelper(var size: Int = 80) : Extension {
    override var enabled: Boolean = true

    val fbo = renderTarget(size, size) {
        colorBuffer()
        depthBuffer()
    }
    val side = size.toDouble()
    val axisLength = size / 2.0
    val planeLength = axisLength + 10.0

    override fun afterDraw(drawer: Drawer, program: Program) {
        draw(drawer)
    }

    fun draw(drawer: Drawer) {
        val viewMatrix = drawer.view
        val x = drawer.width - (size + 5.0)
        val y = 5.0

        drawer.isolatedWithTarget(fbo) {
            drawer.defaults()
            drawer.ortho(fbo)

            drawer.background(ColorRGBa.TRANSPARENT)
            drawer.stroke = null
            drawer.fill = ColorRGBa.PINK.opacify(0.7)
            drawer.circle(axisLength, axisLength, axisLength)

            drawer.ortho(-planeLength, planeLength, -planeLength, planeLength, -planeLength, planeLength)
            drawer.view = getRotation(viewMatrix)
            drawer.strokeWeight = 0.6

            drawer.fill = ColorRGBa.RED
            drawer.stroke = ColorRGBa.RED
            drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_X * axisLength)

            drawer.fill = ColorRGBa.GREEN
            drawer.stroke = ColorRGBa.GREEN
            drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Y * axisLength)

            drawer.fill = ColorRGBa.BLUE
            drawer.stroke = ColorRGBa.BLUE
            drawer.lineSegment(Vector3.ZERO, Vector3.UNIT_Z * axisLength)
        }

        drawer.isolated {
            drawer.defaults()

            drawer.image(fbo.colorBuffer(0), x, y, side, side)
        }
    }
}

internal fun getRotation(mat: Matrix44): Matrix44 {
    val mat3 = mat.matrix33 // without translation
    val c0 = mat3[0].length
    val c1 = mat3[1].length
    val c2 = mat3[2].length

    return Matrix33(
            mat3.c0r0 / c0, mat3.c1r0 / c1, mat3.c2r0 / c2,
            mat3.c0r1 / c0, mat3.c1r1 / c1, mat3.c2r1 / c2,
            mat3.c0r2 / c0, mat3.c1r2 / c1, mat3.c2r2 / c2
    ).matrix44
}