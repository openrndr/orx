package org.openrndr.boofcv.binding

import boofcv.struct.flow.ImageFlow
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ImageFlow.toColorBuffer(): ColorBuffer {

    val cb = colorBuffer(
        width, height, format = ColorFormat.RG,
        type = ColorType.FLOAT32
    )

    val bb = ByteBuffer.allocateDirect(width * height * 8)
    bb.order(ByteOrder.nativeOrder())
    for (y in 0 until height) {
        for (x in 0 until width) {
            val f = get(x, y)
            bb.putFloat(f.x)
            bb.putFloat(f.y)
        }
    }

    (bb as Buffer).rewind()
    cb.write(bb)
    cb.flipV = true
    return cb
}