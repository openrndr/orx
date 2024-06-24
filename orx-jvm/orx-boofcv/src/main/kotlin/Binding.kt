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

fun ColorBuffer.toGrayF32() : GrayF32 {
    val p = GrayF32(width, height)
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

fun ColorBuffer.toGrayF64() : GrayF64 {
    val p = GrayF64(width, height)
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

fun ColorBuffer.toPlanarF32() : Planar<GrayF32> {
    val p = Planar<GrayF32>(GrayF32::class.java, width, height, format.componentCount)
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

fun ColorBuffer.toPlanarU8() : Planar<GrayU8> {
    val p = Planar<GrayU8>(GrayU8::class.java, width, height, format.componentCount)
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

fun ColorBuffer.toGrayU8() : GrayU8 {
    val p = GrayU8(width, height)
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


fun GrayU8.toColorBuffer() : ColorBuffer {
    val cb = colorBuffer(width, height, 1.0, ColorFormat.RGB, ColorType.UINT8)
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

fun GrayF32.toColorBuffer() : ColorBuffer {
    val cb = colorBuffer(width, height, 1.0, ColorFormat.RGB, ColorType.FLOAT32)
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

fun Planar<GrayU8>.toColorBuffer() : ColorBuffer {
    val bandCount = bands.size
    val format = when (bandCount) {
        1 -> ColorFormat.R
        2 -> ColorFormat.RG
        3 -> ColorFormat.RGB
        4 -> ColorFormat.RGBa
        else -> throw IllegalArgumentException("only 1 to 4 bands supported")
    }

    val bands = bands
    val cb = colorBuffer(width, height, 1.0, format, ColorType.UINT8)
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

@JvmName("grayF32ToColorBuffer")
fun Planar<GrayF32>.toColorBuffer() : ColorBuffer {
    val bandCount = bands.size
    val format = when (bandCount) {
        1 -> ColorFormat.R
        2 -> ColorFormat.RG
        3 -> ColorFormat.RGB
        4 -> ColorFormat.RGBa
        else -> throw IllegalArgumentException("only 1 to 4 bands supported")
    }

    val bands = bands
    val cb = colorBuffer(width, height, 1.0, format, ColorType.UINT8)
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