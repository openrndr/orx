package org.openrndr.boofcv.binding

import boofcv.factory.flow.FactoryDenseOpticalFlow
import boofcv.struct.image.GrayF32
import boofcv.struct.image.InterleavedF32
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Expects a [target] with `ColorFormat.RG` and `ColorType.FLOAT32`.
 * [InterleavedF32] is the type returned by `DenseOpticalFlow.process()`.
 */
fun InterleavedF32.toColorBuffer(target: ColorBuffer? = null): ColorBuffer {
    val cb = target ?: colorBuffer(width, height, format = ColorFormat.RG, type = ColorType.FLOAT32)

    val bb = ByteBuffer.allocateDirect(width * height * 8)
    bb.order(ByteOrder.nativeOrder())
    val r = FloatArray(2)
    for (y in 0 until height) {
        for (x in 0 until width) {
            get(x, y, r)
            bb.putFloat(r[0])
            bb.putFloat(r[1])
        }
    }

    (bb as Buffer).rewind()
    cb.write(bb)
    cb.flipV = true
    return cb
}

/**
 * Helper class for processing the image flow between two color buffers
 * and writing the result to a third one. All color buffers are expected
 * to have the same width and height.
 */
class ImageFlowProcessor(val width: Int, val height: Int) {
    private val bb = ByteBuffer.allocateDirect(width * height * 8).also {
        it.order(ByteOrder.nativeOrder())
    }
    private val r = FloatArray(2)
    private val denseFlow = FactoryDenseOpticalFlow.broxWarping(
        null, GrayF32::class.java
    )

    private val srcGray = GrayF32(width, height)
    private val destGray = GrayF32(width, height)
    private val flow = InterleavedF32(width, height, 2)

    /**
     * Calculates the image flow between [src] and [dest], writing the result
     * into [result].
     */
    fun process(src: ColorBuffer, dest: ColorBuffer, result: ColorBuffer) {
        src.toGrayF32(srcGray)
        dest.toGrayF32(destGray)
        denseFlow.process(srcGray, destGray, flow)

        for (y in 0 until height) {
            for (x in 0 until width) {
                flow.get(x, y, r)
                bb.putFloat(r[0])
                bb.putFloat(r[1])
            }
        }

        (bb as Buffer).rewind()
        result.write(bb)
        result.flipV = true
    }
}