package org.openrndr.extra.mediapipe

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** a map from the pixels of a tensor to those of an image: x' = a x + b y + tx, y' = c x + d y + ty */
internal class Affine(val a: Double, val b: Double, val c: Double, val d: Double, val tx: Double, val ty: Double) {
    operator fun invoke(p: Vector2) = Vector2(a * p.x + b * p.y + tx, c * p.x + d * p.y + ty)

    companion object {
        /** the whole image in a square tensor of [size] pixels, keeping its aspect ratio, with black bars */
        fun letterbox(width: Int, height: Int, size: Int): Affine {
            val scale = max(width, height).toDouble() / size
            return Affine(scale, 0.0, 0.0, scale, (width - size * scale) / 2.0, (height - size * scale) / 2.0)
        }

        /** the square of [side] pixels around [center], turned by [rotation] radians, in a tensor of [size] pixels */
        fun rotatedSquare(center: Vector2, side: Double, rotation: Double, size: Int): Affine {
            val s = side / size
            val cr = cos(rotation)
            val sr = sin(rotation)
            // the center of the tensor is at the center, its x axis along the rotation
            return Affine(
                cr * s, -sr * s, sr * s, cr * s,
                center.x - (cr * side - sr * side) / 2.0,
                center.y - (sr * side + cr * side) / 2.0
            )
        }
    }
}

/** the part of the image at [toImage] in a tensor of [size] × [size] RGB pixels from [low] to [high], black outside */
internal fun RgbaImage.tensor(size: Int, toImage: Affine, low: Float, high: Float): FloatBuffer {
    val tensor = ByteBuffer.allocateDirect(size * size * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    val scale = (high - low) / 255f
    val rgb = FloatArray(3)
    for (v in 0 until size) {
        for (u in 0 until size) {
            // bilinear, the pixels of the tensor and the image at their indices like OpenCV's warpAffine, which
            // MediaPipe uses
            val p = toImage(Vector2(u.toDouble(), v.toDouble()))
            sample(p.x, p.y, rgb)
            tensor.put(low + rgb[0] * scale)
            tensor.put(low + rgb[1] * scale)
            tensor.put(low + rgb[2] * scale)
        }
    }
    tensor.rewind()
    return tensor
}

/** the color at ([x], [y]), pixel (i, j) at (i, j), bilinear, black outside the image */
private fun RgbaImage.sample(x: Double, y: Double, rgb: FloatArray) {
    val x0 = floor(x).toInt()
    val y0 = floor(y).toInt()
    val fx = (x - x0).toFloat()
    val fy = (y - y0).toFloat()
    rgb.fill(0f)
    for (j in 0..1) for (i in 0..1) {
        val px = x0 + i
        val py = y0 + j
        if (px < 0 || py < 0 || px >= width || py >= height) continue
        val w = (if (i == 0) 1f - fx else fx) * (if (j == 0) 1f - fy else fy)
        val o = (py * width + px) * 4
        for (k in 0..2) rgb[k] += w * (pixels[o + k].toInt() and 0xff)
    }
}

/** the bounds of [points] */
internal fun bounds(points: List<Vector2>): Rectangle {
    val x0 = points.minOf { it.x }
    val y0 = points.minOf { it.y }
    return Rectangle(x0, y0, points.maxOf { it.x } - x0, points.maxOf { it.y } - y0)
}
