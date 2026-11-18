package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An image of [width] × [height] pixels in 8 bit RGBA, its rows from the top down: the image that the models see.
 */
class RgbaImage(val width: Int, val height: Int, val pixels: ByteArray) {
    init {
        require(pixels.size == width * height * 4) { "${pixels.size} bytes for $width × $height pixels" }
    }
}

/**
 * The pixels of the color buffer, with its rows from the top down. Positions found in it are in the pixels of the
 * color buffer, [ColorBuffer.effectiveWidth] × [ColorBuffer.effectiveHeight].
 */
fun ColorBuffer.toRgbaImage(): RgbaImage {
    val w = effectiveWidth
    val h = effectiveHeight
    val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
    read(buffer, ColorFormat.RGBa, ColorType.UINT8)
    val pixels = ByteArray(w * h * 4)
    val row = w * 4
    // the rows are read from the bottom up, except from color buffers that are stored upside down
    for (y in 0 until h) {
        buffer.position((if (flipV) y else h - 1 - y) * row)
        buffer.get(pixels, y * row, row)
    }
    return RgbaImage(w, h, pixels)
}

/** writes the pixels into [target], a color buffer of the same size */
fun RgbaImage.write(target: ColorBuffer) {
    require(target.effectiveWidth == width && target.effectiveHeight == height) {
        "the color buffer is ${target.effectiveWidth} × ${target.effectiveHeight}, the image $width × $height"
    }
    val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
    val row = width * 4
    // the rows from the bottom up, except into color buffers that are stored upside down
    for (y in 0 until height) {
        buffer.position((if (target.flipV) y else height - 1 - y) * row)
        buffer.put(pixels, y * row, row)
    }
    buffer.rewind()
    target.write(MPPBuffer(buffer), ColorFormat.RGBa, ColorType.UINT8)
}

/** a new color buffer with the pixels */
fun RgbaImage.toColorBuffer(): ColorBuffer = colorBuffer(width, height, format = ColorFormat.RGBa, type = ColorType.UINT8).also { write(it) }
