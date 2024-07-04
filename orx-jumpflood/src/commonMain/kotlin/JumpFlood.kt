package org.openrndr.extra.jumpfill

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.jumpflood.*

import org.openrndr.math.Vector2
import kotlin.math.*

class EncodePoints : Filter(filterShaderFromCode(jf_encode_points, "encode-points"))

class EncodeSubpixel : Filter(filterShaderFromCode(jf_encode_subpixel, "encode-subpixel")) {
    var threshold by parameters

    init {
        threshold = 0.5
    }
}

class JumpFlood : Filter(filterShaderFromCode(jf_jumpflood, "jumpflood")) {
    var maxSteps: Int by parameters
    var step: Int by parameters
}

enum class DecodeMode(val shaderDefine: String) {
    DISTANCE("OUTPUT_DISTANCE"),
    DIRECTION("OUTPUT_DIRECTION")
}

class PixelDirection(val decodeMode: DecodeMode = DecodeMode.DIRECTION) :
    Filter(
        filterShaderFromCode(
            "#define ${decodeMode.shaderDefine}\n $jf_pixel_direction",
            "pixel-direction")
    ) {
    var distanceScale: Double by parameters
    var originalSize: Vector2 by parameters
    var normalizedDistance: Boolean by parameters
    var unitDirection: Boolean by parameters
    var signedMagnitude: Boolean by parameters
    var flipV: Boolean by parameters
    var outputIds: Boolean by parameters


    init {
        distanceScale = 1.0
        originalSize = Vector2(512.0, 512.0)
        normalizedDistance = false
        unitDirection = false
        flipV = true
        outputIds = false
        signedMagnitude = false
    }
}

class PixelDistance : Filter(filterShaderFromCode(jf_pixel_distance, "pixel-distance")) {
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

class ContourPoints : Filter(filterShaderFromCode(jf_contour_points, "contour-points"))

class IdContourPoints : Filter(filterShaderFromCode(jf_id_contours, "id-contour-points"))


class Threshold : Filter(filterShaderFromCode(jf_threshold, "threshold")) {
    var threshold: Double by parameters

    init {
        threshold = 0.5
    }
}

class AlphaThreshold : Filter(filterShaderFromCode(jf_alpha_threshold, "alpha-threshold")) {
    var threshold: Double by parameters

    init {
        threshold = 0.5
    }
}


private val encodePoints by lazy { persistent { EncodePoints() } }
private val pixelDistance by lazy { persistent { PixelDistance() } }
private val pixelDirection by lazy { persistent { PixelDirection() } }
private val contourPoints by lazy { persistent { ContourPoints() } }
private val threshold by lazy { persistent { Threshold() } }
private val passthrough by lazy { persistent { Passthrough() } }

class JumpFlooder(
    val width: Int, val height: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32,
    val encodePoints: Filter = EncodePoints()
) {

    private val dimension = max(width, height)
    private val exp = ceil(log2(dimension.toDouble())).toInt()
    val squareDim = 2.0.pow(exp.toDouble()).toInt()
    val jumpFlood = JumpFlood()

    private val coordinates =
        listOf(
            colorBuffer(squareDim, squareDim, format = format, type = type),
            colorBuffer(squareDim, squareDim, format = format, type = type)
        )


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

private fun encodeDecodeBitmap(
    preprocess: Filter, decoder: Filter, bitmap: ColorBuffer,
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
fun centroidsFromBitmap(
    bitmap: ColorBuffer,
    jumpFlooder: JumpFlooder? = null,
    result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(passthrough, passthrough, bitmap, jumpFlooder, result)

fun distanceFieldFromBitmap(
    bitmap: ColorBuffer,
    jumpFlooder: JumpFlooder? = null,
    result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(contourPoints, pixelDistance, bitmap, jumpFlooder, result)

fun directionFieldFromBitmap(
    bitmap: ColorBuffer,
    jumpFlooder: JumpFlooder? = null,
    result: ColorBuffer? = null
): ColorBuffer = encodeDecodeBitmap(contourPoints, pixelDirection, bitmap, jumpFlooder, result)