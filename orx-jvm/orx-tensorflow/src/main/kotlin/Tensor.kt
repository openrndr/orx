package org.openrndr.extra.tensorflow


import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.tensorflow.arrays.*
import org.tensorflow.Output
import org.tensorflow.Tensor
import org.tensorflow.ndarray.StdArrays
import org.tensorflow.ndarray.buffer.DataBuffers
import org.tensorflow.op.math.Add
import org.tensorflow.types.*
import org.tensorflow.types.family.TType
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun ColorBuffer.copyTo(tensor: TFloat32) {
    val buffer = ByteBuffer.allocateDirect(effectiveWidth * effectiveHeight * format.componentCount * 4)
    buffer.order(ByteOrder.nativeOrder())
    this.read(buffer, targetType = ColorType.FLOAT32)
    buffer.rewind()
    val dataBuffer = DataBuffers.of(buffer.asFloatBuffer())
    tensor.write(dataBuffer)
}

@JvmName("copyToTUint8")
fun ColorBuffer.copyTo(tensor: TUint8) {
    val buffer = ByteBuffer.allocateDirect(effectiveWidth * effectiveHeight * format.componentCount)
    buffer.order(ByteOrder.nativeOrder())
    this.read(buffer, targetType = ColorType.UINT8)
    buffer.rewind()
    val dataBuffer = DataBuffers.of(buffer)
    tensor.write(dataBuffer)
}



fun TFloat32.copyTo(colorBuffer: ColorBuffer) {
    val s = shape()

    val components = when {
        s.numDimensions() == 2 -> 1
        s.numDimensions() == 3 -> s.get(2).toInt()
        s.numDimensions() == 4 -> s.get(3).toInt()
        else -> error("can't copy to colorbuffer from ${s.numDimensions()}D tensor")
    }

    val format = when (components) {
        4 -> ColorFormat.RGBa
        3 -> ColorFormat.RGB
        2 -> ColorFormat.RG
        1 -> ColorFormat.R
        else -> error("only supports 1, 2, 3, or 4 components")
    }
    val buffer = ByteBuffer.allocateDirect(this.numBytes().toInt())
    buffer.order(ByteOrder.nativeOrder())

    val dataBuffer = DataBuffers.of(buffer.asFloatBuffer())

    this.read(dataBuffer)
    buffer.rewind()
    colorBuffer.write(buffer, sourceFormat = format, sourceType = ColorType.FLOAT32)
}

fun TType.summary() {
    println("type: ${this.dataType().name}")
    println("shape: [${this.shape().asArray().joinToString(", ")}]")
}

fun TInt32.toIntArray(): IntArray {
    val elementCount = this.numBytes() / 4
    val targetArray = IntArray(elementCount.toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TInt64.toLongArray(): LongArray {
    val elementCount = this.numBytes() / 8
    val targetArray = LongArray(elementCount.toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TUint8.toByteArray(): ByteArray {
    val elementCount = this.numBytes() / 8
    val targetArray = ByteArray(elementCount.toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}


fun TFloat32.toFloatArray(): FloatArray {
    val elementCount = this.numBytes() / 4
    val targetArray = FloatArray(elementCount.toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TFloat32.toFloatArray2D(): FloatArray2D {
    val shape = this.shape()
    require(shape.numDimensions() == 2) {
        "tensor has ${shape.numDimensions()} dimensions, need 2"
    }
    val targetArray = floatArray2D(shape.get(0).toInt(), shape.get(1).toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TFloat32.toFloatArray3D(): FloatArray3D {
    val shape = this.shape()
    require(shape.numDimensions() == 3) {
        "tensor has ${shape.numDimensions()} dimensions, need 3"
    }
    val targetArray = floatArray3D(shape.get(0).toInt(), shape.get(1).toInt(), shape.get(2).toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TFloat32.toFloatArray4D(): FloatArray4D {
    val shape = this.shape()
    require(shape.numDimensions() == 4) {
        "tensor has ${shape.numDimensions()} dimensions, need 4"
    }
    val targetArray = floatArray4D(shape.get(0).toInt(), shape.get(1).toInt(), shape.get(2).toInt(), shape.get(3).toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TFloat64.toDoubleArray(): DoubleArray {
    val elementCount = this.numBytes() / 8
    val targetArray = DoubleArray(elementCount.toInt())
    StdArrays.copyFrom(this, targetArray)
    return targetArray
}

fun TFloat32.toColorBuffer(target: ColorBuffer? = null): ColorBuffer {
    val s = shape()
    require(s.numDimensions() == 2 || s.numDimensions() == 3)

    val width = (if (s.numDimensions() == 3) s.get(1) else s.get(0)).toInt()
    val height = (if (s.numDimensions() == 3) s.get(2) else s.get(1)).toInt()
    val components = if (s.numDimensions() == 3) s.get(0).toInt() else 1

    val format = when (components) {
        4 -> ColorFormat.RGBa
        3 -> ColorFormat.RGB
        2 -> ColorFormat.RG
        1 -> ColorFormat.R
        else -> error("only supports 1, 2, 3, or 4 components")
    }

    val targetColorBuffer = target?: colorBuffer(width, height, format = format, type = ColorType.FLOAT32)
    val floatArray = toFloatArray()
    val bb = ByteBuffer.allocateDirect(width * height * components * 4)
    bb.order(ByteOrder.nativeOrder())
    val fb = bb.asFloatBuffer()
    fb.put(floatArray)
    bb.rewind()
    targetColorBuffer.write(bb)
    return targetColorBuffer
}


@JvmName("toColorBufferTInt8")
fun TUint8.toColorBuffer(target: ColorBuffer? = null): ColorBuffer {
    val s = shape()
    require(s.numDimensions() == 2 || s.numDimensions() == 3)

    val width = (if (s.numDimensions() == 3) s.get(1) else s.get(0)).toInt()
    val height = (if (s.numDimensions() == 3) s.get(2) else s.get(1)).toInt()
    val components = if (s.numDimensions() == 3) s.get(0).toInt() else 1

    val format = when (components) {
        4 -> ColorFormat.RGBa
        3 -> ColorFormat.RGB
        2 -> ColorFormat.RG
        1 -> ColorFormat.R
        else -> error("only supports 1, 2, 3, or 4 components")
    }

    val byteArray = toByteArray()
    val targetColorBuffer = target?: colorBuffer(width, height, format = format, type = ColorType.UINT8)
    val bb = ByteBuffer.allocateDirect(width * height * components )
    bb.order(ByteOrder.nativeOrder())
    bb.put(byteArray)
    bb.rewind()
    targetColorBuffer.write(bb)
    return targetColorBuffer
}
