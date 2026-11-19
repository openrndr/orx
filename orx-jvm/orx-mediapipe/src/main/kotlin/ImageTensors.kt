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

    /** the map from the pixels of the image to those of the tensor */
    fun inversed(): Affine {
        val det = a * d - b * c
        val ia = d / det
        val ib = -b / det
        val ic = -c / det
        val id = a / det
        return Affine(ia, ib, ic, id, -(ia * tx + ib * ty), -(ic * tx + id * ty))
    }

    companion object {
        /** the whole image in a tensor of [width] × [height] pixels, stretched */
        fun stretch(imageWidth: Int, imageHeight: Int, width: Int, height: Int) =
            Affine(imageWidth.toDouble() / width, 0.0, 0.0, imageHeight.toDouble() / height, 0.0, 0.0)

        /** the whole image in a square tensor of [size] pixels, keeping its aspect ratio, with black bars */
        fun letterbox(width: Int, height: Int, size: Int): Affine {
            val scale = max(width, height).toDouble() / size
            return Affine(scale, 0.0, 0.0, scale, (width - size * scale) / 2.0, (height - size * scale) / 2.0)
        }

        /** [rect] in a tensor of [size] × [size] pixels, stretched when it is not square */
        fun rotatedRect(rect: RotatedRect, size: Int): Affine {
            val sx = rect.width / size
            val sy = rect.height / size
            val cr = cos(rect.rotation)
            val sr = sin(rect.rotation)
            // the center of the tensor is at the center, its x axis along the rotation
            return Affine(
                cr * sx, -sr * sy, sr * sx, cr * sy,
                rect.center.x - (cr * rect.width - sr * rect.height) / 2.0,
                rect.center.y - (sr * rect.width + cr * rect.height) / 2.0
            )
        }
    }
}

/** the part of the image at [toImage] in a tensor of [size] × [size] RGB pixels from [low] to [high], black outside */
internal fun RgbaImage.tensor(size: Int, toImage: Affine, low: Float, high: Float) = tensor(size, size, toImage, low, high)

/**
 * The part of the image at [toImage] in a tensor of [width] × [height] pixels from [low] to [high], black outside or,
 * with [replicate], the colors of the edges, of the RGB channels or, with [channels] 4, the RGBA channels
 */
internal fun RgbaImage.tensor(
    width: Int, height: Int, toImage: Affine, low: Float, high: Float, channels: Int = 3, replicate: Boolean = false
): FloatBuffer {
    val tensor = ByteBuffer.allocateDirect(width * height * channels * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    val scale = (high - low) / 255f
    val rgb = FloatArray(channels)
    for (v in 0 until height) {
        for (u in 0 until width) {
            // bilinear, the pixels of the tensor and the image at their indices like OpenCV's warpAffine, which
            // MediaPipe uses
            val p = toImage(Vector2(u.toDouble(), v.toDouble()))
            // the width and height of the image, not of the tensor
            if (replicate) {
                sample(p.x.coerceIn(0.0, this@tensor.width - 1.0), p.y.coerceIn(0.0, this@tensor.height - 1.0), rgb)
            } else {
                sample(p.x, p.y, rgb)
            }
            for (k in 0 until channels) tensor.put(low + rgb[k] * scale)
        }
    }
    tensor.rewind()
    return tensor
}

/** the color at ([x], [y]), pixel (i, j) at (i, j), bilinear, black outside the image; the first `rgb.size` channels */
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
        for (k in rgb.indices) rgb[k] += w * (pixels[o + k].toInt() and 0xff)
    }
}

/** the bounds of [points] */
internal fun bounds(points: List<Vector2>): Rectangle {
    val x0 = points.minOf { it.x }
    val y0 = points.minOf { it.y }
    return Rectangle(x0, y0, points.maxOf { it.x } - x0, points.maxOf { it.y } - y0)
}
