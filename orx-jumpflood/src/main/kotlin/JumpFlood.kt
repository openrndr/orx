package org.openrndr.extra.jumpfill

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

class EncodePoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/encode-points.frag")))

class EncodeSubpixel : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/encode-subpixel.frag"))) {
    var threshold by parameters
    init {
        threshold = 0.5
    }

}

class JumpFlood : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/jumpflood.frag"))) {
    var maxSteps: Int by parameters
    var step: Int by parameters
}

class PixelDirection : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-direction.frag"))) {
    var distanceScale: Double by parameters
    var originalSize: Vector2 by parameters

    init {
        distanceScale = 1.0
        originalSize = Vector2(512.0, 512.0)

    }
}

class PixelDistance : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-distance.frag"))) {
    var distanceScale: Double by parameters
    var originalSize: Vector2 by parameters
    var signedBit: Boolean by parameters
    var signedDistance: Boolean by parameters
    init {
        distanceScale = 1.0
        originalSize = Vector2(512.0, 512.0)
        signedBit = true
        signedDistance = false
    }
}

class ContourPoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/contour-points.frag")))
class Threshold : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/threshold.frag"))) {
    var threshold: Double by parameters

    init {
        threshold = 0.5
    }
}

class AlphaThreshold : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/alpha-threshold.frag"))) {
    var threshold: Double by parameters

    init {
        threshold = 0.5
    }
}


private val encodePoints by lazy { persistent { EncodePoints() } }
private val pixelDistance by lazy {  persistent { PixelDistance()  } }
private val pixelDirection by lazy { persistent { PixelDirection() } }
private val contourPoints by lazy { persistent { ContourPoints() } }
private val threshold by lazy {  persistent { Threshold() } }
private val passthrough by lazy { persistent { Passthrough() } }

class JumpFlooder(val width: Int, val height: Int, format: ColorFormat = ColorFormat.RGB, type: ColorType = ColorType.FLOAT32,
                  val encodePoints: Filter = EncodePoints()) {

    private val dimension = max(width, height)
    private val exp = ceil(Math.log(dimension.toDouble()) / Math.log(2.0)).toInt()
    val squareDim = 2.0.pow(exp.toDouble()).toInt()
    val jumpFlood = JumpFlood()

    private val coordinates =
            listOf(colorBuffer(squareDim, squareDim, format = format, type = type),
                    colorBuffer(squareDim, squareDim, format = format, type = type))


    val final = colorBuffer(squareDim, squareDim, format = format, type = type)

    private val square = colorBuffer(squareDim, squareDim, format = ColorFormat.RGBa, type = type).apply {
        fill(ColorRGBa.BLACK.opacify(0.0))
    }


    fun jumpFlood(input: ColorBuffer): ColorBuffer {
        if (input.width != width || input.height != height) {
            throw IllegalArgumentException("dimensions mismatch")
        }

        input.copyTo(square)
        encodePoints.apply(square, coordinates[0])

        jumpFlood.maxSteps = exp
        for (i in 0 until exp) {
            jumpFlood.step = i
            jumpFlood.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
        }

        coordinates[exp % 2].copyTo(final)

        return final
    }

    fun destroy() {
        coordinates.forEach { it.destroy() }
        square.destroy()
        final.destroy()
    }
}

private fun encodeDecodeBitmap(preprocess: Filter, decoder: Filter, bitmap: ColorBuffer,
                               jumpFlooder: JumpFlooder? = null,
                               result: ColorBuffer? = null
): ColorBuffer {
    val _jumpFlooder = jumpFlooder ?: JumpFlooder(bitmap.width, bitmap.height)
    val _result = result ?: colorBuffer(bitmap.width, bitmap.height, type = ColorType.FLOAT16)

    preprocess.apply(bitmap, _result)

    val encoded = _jumpFlooder.jumpFlood(_result)

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
fun centroidsFromBitmap(bitmap: ColorBuffer,
                        jumpFlooder: JumpFlooder? = null,
                        result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(passthrough, passthrough, bitmap, jumpFlooder, result)

fun distanceFieldFromBitmap(bitmap: ColorBuffer,
                            jumpFlooder: JumpFlooder? = null,
                            result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(contourPoints, pixelDistance, bitmap, jumpFlooder, result)

fun directionFieldFromBitmap(bitmap: ColorBuffer,
                             jumpFlooder: JumpFlooder? = null,
                             result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(contourPoints, pixelDirection, bitmap, jumpFlooder, result)