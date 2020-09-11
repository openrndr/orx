package org.openrndr.boofcv.binding

import boofcv.abst.distort.FDistort
import boofcv.struct.image.ImageBase
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorType
import kotlin.math.roundToInt

fun <T : ImageBase<out ImageBase<*>>?> ImageBase<T>.resizeBy(scaleX: Double, scaleY: Double = scaleX): T {
    val scaled = this.createNew((this.width * scaleX).toInt(), (this.height * scaleY).toInt())

    FDistort(this, scaled).scaleExt().apply()

    return scaled
}

fun <T : ImageBase<out ImageBase<*>>?> ImageBase<T>.resizeTo(newWidth: Int? = null, newHeight: Int? = null): T {
    val ar = this.width / this.height.toDouble()

    val scaled = (if (newWidth != null && newHeight != null) {
        val w = newWidth
        val h = newHeight

        this.createNew(w, h)
    } else if (newWidth != null && newHeight == null) {
        val w = newWidth
        val h = newWidth / ar

        this.createNew(w, h.roundToInt())
    } else if (newWidth == null && newHeight != null) {
        val w = newHeight * ar
        val h = newHeight

        this.createNew(w.roundToInt(), h)
    } else {
        this.createNew(this.width, this.height)
    })

    FDistort(this, scaled).scaleExt().apply()

    return scaled
}

fun ColorBuffer.resizeBy(scaleX: Double, scaleY: Double = scaleX, convertToGray: Boolean = false): ColorBuffer {
    return if (convertToGray) {
        when (this.type) {
            ColorType.FLOAT32, ColorType.FLOAT16 -> this.toGrayF32().resizeBy(scaleX, scaleY).toColorBuffer()
            else -> this.toGrayU8().resizeBy(scaleX, scaleY).toColorBuffer()
        }
    } else {
        when (this.type) {
            ColorType.FLOAT32, ColorType.FLOAT16 -> this.toPlanarF32().resizeBy(scaleX, scaleY).toColorBuffer()
            else -> this.toPlanarU8().resizeBy(scaleX, scaleY).toColorBuffer()
        }
    }
}

fun ColorBuffer.resizeTo(newWidth: Int? = null, newHeight: Int? = null, convertToGray: Boolean = false): ColorBuffer {
    return if (convertToGray) {
        when (this.type) {
            ColorType.FLOAT32, ColorType.FLOAT16 -> this.toGrayF32().resizeTo(newWidth, newHeight).toColorBuffer()
            else -> this.toGrayU8().resizeTo(newWidth, newHeight).toColorBuffer()
        }
    } else {
        when (this.type) {
            ColorType.FLOAT32, ColorType.FLOAT16 -> this.toPlanarF32().resizeTo(newWidth, newHeight).toColorBuffer()
            else -> this.toPlanarU8().resizeTo(newWidth, newHeight).toColorBuffer()
        }
    }
}