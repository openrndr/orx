package org.openrndr.boofcv.binding

import boofcv.struct.image.GrayF32
import boofcv.struct.image.GrayF64
import boofcv.struct.image.GrayU8
import boofcv.struct.image.Planar
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer

/**
 * Converts this [ColorBuffer] to a grayscale image represented as a [GrayF32] object.
 * The red channel of the color buffer is used to calculate the grayscale values.
 *
 * @param target An optional [GrayF32] instance to store the result.
 * If not provided, a new instance will be created.
 * @return A [GrayF32] instance containing the grayscale representation of the ColorBuffer.
 */
fun ColorBuffer.toGrayF32(target: GrayF32? = null) : GrayF32 {
    val p = target ?: GrayF32(width, height)
    shadow.download()

    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = shadow.read(x, y)
            p.data[offset] = (c.r * 255).toFloat()
            offset++
        }
    }
    return p
}

/**
 * Converts this [ColorBuffer] to a grayscale image represented as a [GrayF64] object.
 * The red channel of the color buffer is used to calculate the grayscale values.
 *
 * @param target An optional [GrayF64] instance to store the result.
 * If not provided, a new instance will be created.
 * @return A [GrayF64] instance containing the grayscale representation of the ColorBuffer.
 */
fun ColorBuffer.toGrayF64(target: GrayF64? = null) : GrayF64 {
    val p = target ?: GrayF64(width, height)
    shadow.download()

    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = shadow.read(x, y)
            p.data[offset] = (c.r * 255)
            offset++
        }
    }
    return p
}

/**
 * Converts this [ColorBuffer] to an image represented as a Planar<GrayF32> object
 * with the same number of channels (bands) as the original.
 *
 * @param target An optional `Planar<GrayF32>` instance to store the result.
 * If not provided, a new instance will be created.
 * @return A `Planar<GrayF32>` instance containing the representation of the ColorBuffer.
 */
fun ColorBuffer.toPlanarF32(target: Planar<GrayF32>? = null) : Planar<GrayF32> {
    val p = target ?: Planar(GrayF32::class.java, width, height, format.componentCount)
    shadow.download()

    val bands = p.bands

    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = shadow.read(x, y)
            bands[0].data[offset] = (c.r * 255).toFloat()
            bands[1].data[offset] = (c.g * 255).toFloat()
            bands[2].data[offset] = (c.b * 255).toFloat()
            offset++
        }
    }
    return p
}

/**
 * Converts this [ColorBuffer] to an image represented as a Planar<GrayU8> object
 * with the same number of channels (bands) as the original.
 *
 * @param target An optional `Planar<GrayU8>` instance to store the result.
 * If not provided, a new instance will be created.
 * @return A `Planar<GrayU8>` instance containing the representation of the ColorBuffer.
 */
fun ColorBuffer.toPlanarU8(target: Planar<GrayU8>? = null) : Planar<GrayU8> {
    val p = target ?: Planar(GrayU8::class.java, width, height, format.componentCount)
    shadow.download()

    val bands = p.bands

    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = shadow.read(x, y)
            bands[0].data[offset] = (c.r * 255).toInt().toByte()
            bands[1].data[offset] = (c.g * 255).toInt().toByte()
            bands[2].data[offset] = (c.b * 255).toInt().toByte()
            offset++
        }
    }
    return p
}

/**
 * Converts this [ColorBuffer] to a grayscale image represented as a [GrayU8] object.
 * The red channel of the color buffer is used to calculate the grayscale values.
 *
 * @param target An optional [GrayU8] instance to store the result.
 * If not provided, a new instance will be created.
 * @return A [GrayU8] instance containing the grayscale representation of the ColorBuffer.
 */
fun ColorBuffer.toGrayU8(target: GrayU8? = null) : GrayU8 {
    val p = target ?: GrayU8(width, height)
    shadow.download()

    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = shadow.read(x, y)
            p.data[offset] = (c.r * 255).toInt().coerceIn(0, 255).toByte()
            offset++
        }
    }
    return p
}


/**
 * Converts a `GrayU8` object into a `ColorBuffer` where each pixel in the grayscale image
 * is mapped to an RGB color with identical red, green, and blue values.
 *
 * If the `target` parameter is provided, the conversion is performed in-place within the specified `ColorBuffer`.
 * Otherwise, a new `ColorBuffer` is created and returned. The returned or updated `ColorBuffer`
 * has an RGB format and uses unsigned 8-bit integers for its color components.
 *
 * @param target An optional `ColorBuffer` to store the converted image. If `null`, a new `ColorBuffer` is created.
 * @return A `ColorBuffer` containing the RGB representation of the `GrayU8` image.
 */
fun GrayU8.toColorBuffer(target: ColorBuffer? = null) : ColorBuffer {
    val cb = target ?: colorBuffer(width, height, 1.0, ColorFormat.RGB, ColorType.UINT8)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = (data[offset].toInt() and 0xff).toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, r, r, 1.0))
        }
    }
    shadow.upload()
    return cb
}

/**
 * Converts a `GrayF32` object into a `ColorBuffer` where each pixel in the grayscale image
 * is mapped to an RGB color with identical red, green, and blue values.
 *
 * If the `target` parameter is provided, the conversion is performed in-place within the specified `ColorBuffer`.
 * Otherwise, a new `ColorBuffer` is created and returned. The returned or updated `ColorBuffer`
 * has an RGB format and uses 32-bit floats for its color components.
 *
 * @param target An optional `ColorBuffer` to store the converted image. If `null`, a new `ColorBuffer` is created.
 * @return A `ColorBuffer` containing the RGB representation of the `GrayF32` image.
 */
fun GrayF32.toColorBuffer(target: ColorBuffer? = null) : ColorBuffer {
    val cb = target ?: colorBuffer(width, height, 1.0, ColorFormat.RGB, ColorType.FLOAT32)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = data[offset].toDouble() / 255.0
            offset++
            shadow.write(x, y, ColorRGBa(r, r, r))
        }
    }
    shadow.upload()
    return cb
}

/**
 * Converts a `Planar<GrayU8>` object into a `ColorBuffer` with the same number of channels (bands).
 *
 * If the `target` parameter is provided, the conversion is performed in-place within the specified `ColorBuffer`.
 * Otherwise, a new `ColorBuffer` is created and returned. The returned or updated `ColorBuffer`
 * has an RGB format and uses unsigned 8-bit integers for its color components.
 *
 * @param target An optional `ColorBuffer` to store the converted image. If `null`, a new `ColorBuffer` is created.
 * @return A `ColorBuffer` containing the RGB representation of the `Planar<GrayU8>` image.
 */
fun Planar<GrayU8>.toColorBuffer(target: ColorBuffer? = null) : ColorBuffer {
    val bandCount = bands.size
    val format = when (bandCount) {
        1 -> ColorFormat.R
        2 -> ColorFormat.RG
        3 -> ColorFormat.RGB
        4 -> ColorFormat.RGBa
        else -> throw IllegalArgumentException("only 1 to 4 bands supported")
    }

    val bands = bands
    val cb = target ?: colorBuffer(width, height, 1.0, format, ColorType.UINT8)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = (bands[0].data[offset].toInt() and 0xff).toDouble() / 255.0
            val g = if (bandCount >= 2) (bands[1].data[offset].toInt() and 0xff).toDouble() / 255.0 else 0.0
            val b = if (bandCount >= 3) (bands[2].data[offset].toInt() and 0xff).toDouble() / 255.0 else 0.0
            val a = if (bandCount >= 4) (bands[2].data[offset].toInt() and 0xff).toDouble() / 255.0 else 1.0
            offset++
            shadow.write(x, y, ColorRGBa(r, g, b, a))
        }
    }
    shadow.upload()
    return cb
}

/**
 * Converts a `Planar<GrayF32>` object into a `ColorBuffer` with the same number of channels (bands).
 *
 * If the `target` parameter is provided, the conversion is performed in-place within the specified `ColorBuffer`.
 * Otherwise, a new `ColorBuffer` is created and returned. The returned or updated `ColorBuffer`
 * has an RGB format and uses 32-bit floats for its color components.
 *
 * @param target An optional `ColorBuffer` to store the converted image. If `null`, a new `ColorBuffer` is created.
 * @return A `ColorBuffer` containing the RGB representation of the `Planar<GrayF32>` image.
 */
@JvmName("grayF32ToColorBuffer")
fun Planar<GrayF32>.toColorBuffer(target: ColorBuffer? = null) : ColorBuffer {
    val bandCount = bands.size
    val format = when (bandCount) {
        1 -> ColorFormat.R
        2 -> ColorFormat.RG
        3 -> ColorFormat.RGB
        4 -> ColorFormat.RGBa
        else -> throw IllegalArgumentException("only 1 to 4 bands supported")
    }

    val bands = bands
    val cb = target ?: colorBuffer(width, height, 1.0, format, ColorType.UINT8)
    val shadow = cb.shadow
    shadow.buffer.rewind()
    var offset = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = bands[0].data[offset] / 255.0
            val g = if (bandCount >= 2) bands[1].data[offset] / 255.0 else 0.0
            val b = if (bandCount >= 3) bands[2].data[offset] / 255.0 else 0.0
            val a = if (bandCount >= 4) bands[3].data[offset] / 255.0 else 1.0
            offset++
            shadow.write(x, y, ColorRGBa(r, g, b, a))
        }
    }
    shadow.upload()
    return cb
}