package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
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
