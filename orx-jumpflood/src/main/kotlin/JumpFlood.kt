package org.openrndr.extra.jumpfill

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough


import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class EncodePoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/encode-points.frag")))
class JumpFlood : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/jumpflood.frag"))) {
    var maxSteps: Int by parameters
    var step: Int by parameters
}

class PixelDirection : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-direction.frag"))) {
    var originalSize: Vector2 by parameters
}
class PixelDistance : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-distance.frag"))) {
    var originalSize: Vector2 by parameters
}
class ContourPoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/contour-points.frag")))
class Threshold : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/threshold.frag"))) {
    var threshold by parameters

    init {
        threshold = 0.5
    }
}

private val encodePoints by lazy { EncodePoints() }
private val jumpFlood by lazy { JumpFlood() }
private val pixelDistance by lazy { PixelDistance() }
private val pixelDirection by lazy { PixelDirection() }
private val contourPoints by lazy { ContourPoints() }
private val threshold by lazy { Threshold() }
private val passthrough by lazy { Passthrough() }

class JumpFlooder(val width: Int, val height: Int, format:ColorFormat = ColorFormat.RGB, type:ColorType = ColorType.FLOAT32) {

    private val dimension = max(width, height)
    private val exp = ceil(Math.log(dimension.toDouble()) / Math.log(2.0)).toInt()
    val squareDim = 2.0.pow(exp.toDouble()).toInt()

    private val coordinates =
            listOf(colorBuffer(squareDim, squareDim, format = format, type = type),
                    colorBuffer(squareDim, squareDim, format = format, type = type))

    private val final = renderTarget(width, height) {
        colorBuffer(format = format, type = type)
    }

    val encoded: ColorBuffer get() = final.colorBuffer(0)

    private val square = renderTarget(squareDim, squareDim) {
        colorBuffer(format = format, type = type)
    }

    fun jumpFlood(drawer: Drawer, input: ColorBuffer): ColorBuffer {
        if (input.width != width || input.height != height) {
            throw IllegalArgumentException("dimensions mismatch")
        }

        drawer.isolatedWithTarget(square) {
            drawer.background(ColorRGBa.BLACK)
            drawer.ortho(square)
            drawer.view = Matrix44.IDENTITY
            drawer.model = Matrix44.IDENTITY
            drawer.image(input)
        }
        encodePoints.apply(square.colorBuffer(0), coordinates[0])

        for (i in 0 until exp) {
            jumpFlood.step = i
            jumpFlood.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
        }

        drawer.isolatedWithTarget(final) {
            drawer.background(ColorRGBa.BLACK)
            drawer.ortho(final)
            drawer.view = Matrix44.IDENTITY
            drawer.model = Matrix44.IDENTITY
            drawer.image(coordinates[exp % 2])
        }
        return encoded
    }

    fun destroy() {
        coordinates.forEach { it.destroy() }
        square.colorBuffer(0).destroy()
        square.detachColorBuffers()
        square.destroy()

        final.colorBuffer(0).destroy()
        final.detachColorBuffers()
        final.destroy()
    }
}

private fun encodeDecodeBitmap(drawer: Drawer, preprocess: Filter, decoder: Filter, bitmap: ColorBuffer,
                               jumpFlooder: JumpFlooder? = null,
                               result: ColorBuffer? = null
): ColorBuffer {
    val _jumpFlooder = jumpFlooder ?: JumpFlooder(bitmap.width, bitmap.height)
    val _result = result ?: colorBuffer(bitmap.width, bitmap.height, type = ColorType.FLOAT16)

    preprocess.apply(bitmap, _result)

    val encoded = _jumpFlooder.jumpFlood(drawer, _result)

    decoder.parameters["originalSize"] = Vector2(_jumpFlooder.squareDim.toDouble(), _jumpFlooder.squareDim.toDouble())
    decoder.apply(arrayOf(encoded, bitmap), _result)
    if (jumpFlooder == null) {
        _jumpFlooder.destroy()
    }

    return _result
}

/**
 * Creates a color buffer containing the coordinates of the nearest centroids
 * @param bitmap a ColorBuffer with centroids in red (> 0)
 */
fun centroidsFromBitmap(drawer: Drawer, bitmap: ColorBuffer,
                        jumpFlooder: JumpFlooder? = null,
                        result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(drawer, passthrough, passthrough, bitmap, jumpFlooder, result)

fun distanceFieldFromBitmap(drawer: Drawer, bitmap: ColorBuffer,
                            jumpFlooder: JumpFlooder? = null,
                            result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(drawer, contourPoints, pixelDistance, bitmap, jumpFlooder, result)

fun directionFieldFromBitmap(drawer: Drawer, bitmap: ColorBuffer,
                             jumpFlooder: JumpFlooder? = null,
                             result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(drawer, contourPoints, pixelDirection, bitmap, jumpFlooder, result)
