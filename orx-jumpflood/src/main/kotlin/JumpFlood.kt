package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.filter.filterShaderFromUrl
import org.openrndr.math.Matrix44
import org.openrndr.resourceUrl

class EncodePoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/encode-points.frag")))
class JumpFlood : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/jumpflood.frag"))) {
    var maxSteps: Int by parameters
    var step: Int by parameters
}

class PixelDistance : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-distance.frag")))
class ContourPoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/contour-points.frag")))
class Threshold : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/threshold.frag"))) {
    var threshold by parameters
    init {
        threshold = 0.5
    }
}

val encodePoints by lazy { EncodePoints() }
val jumpFlood by lazy { JumpFlood() }
val pixelDistance by lazy { PixelDistance() }
val contourPoints by lazy { ContourPoints() }
val threshold by lazy { Threshold() }


class JumpFlooder(val width: Int, val height: Int) {
    private val dimension = Math.max(width, height)
    private val exp = Math.ceil(Math.log(dimension.toDouble()) / Math.log(2.0)).toInt()
    private val squareDim = Math.pow(2.0, exp.toDouble()).toInt()

    private val coordinates =
            listOf(colorBuffer(squareDim, squareDim, format = ColorFormat.RG, type = ColorType.FLOAT32),
                    colorBuffer(squareDim, squareDim, format = ColorFormat.RG, type = ColorType.FLOAT32))

    private val final = renderTarget(width, height) {
        colorBuffer(type = ColorType.FLOAT32)
    }

    val result: ColorBuffer get() = final.colorBuffer(0)

    private val square = renderTarget(squareDim, squareDim) {
        colorBuffer()
    }

    fun jumpFlood(drawer: Drawer, input: ColorBuffer) {
        if (input.width != width || input.height != height) {
            throw IllegalArgumentException("dimensions mismatch")
        }

        drawer.isolatedWithTarget(square) {
            drawer.ortho(square)
            drawer.view = Matrix44.IDENTITY
            drawer.model = Matrix44.IDENTITY
            drawer.image(input)
        }
        encodePoints.apply(square.colorBuffer(0), coordinates[0])
        val exp = Math.ceil(Math.log(input.width.toDouble()) / Math.log(2.0)).toInt()
        for (i in 0 until exp) {
            jumpFlood.step = i
            jumpFlood.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
        }

        pixelDistance.apply(coordinates[exp % 2], coordinates[exp % 2])
        drawer.isolatedWithTarget(final) {
            drawer.ortho(final)
            drawer.view = Matrix44.IDENTITY
            drawer.model = Matrix44.IDENTITY
            drawer.image(coordinates[exp % 2])
        }
    }

    fun destroy(destroyFinal: Boolean = true) {
        coordinates.forEach { it.destroy() }

        square.colorBuffer(0).destroy()
        square.detachColorBuffers()
        square.destroy()

        if (destroyFinal) {
            final.colorBuffer(0).destroy()
        }
        final.detachColorBuffers()

        final.destroy()

    }

}

fun jumpFlood(drawer: Drawer, points: ColorBuffer): ColorBuffer {
    val jumpFlooder = JumpFlooder(points.width, points.height)
    jumpFlooder.jumpFlood(drawer, points)
    val result = jumpFlooder.result
    jumpFlooder.destroy(false)
    return result
}