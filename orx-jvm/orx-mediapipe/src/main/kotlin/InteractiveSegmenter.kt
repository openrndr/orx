package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Segments the object at a point or along a scribble in an image like MediaPipe's interactive segmenter, MagicTouch:
 * the point or the scribble is drawn into the alpha channel of the image, which is stretched over the input of the
 * model. The [Segmentation] has a single category, `object`.
 */
class InteractiveSegmenter(
    modelPath: String = "orx-jvm/orx-mediapipe/models/magic_touch.onnx"
) : AutoCloseable {
    private val model = OnnxModel(modelPath)

    /** the segmentation of the object at [point], in the pixels of [image] */
    fun segment(image: RgbaImage, point: Vector2): Segmentation = segment(image) { alpha, thickness ->
        // a disc, like the points of MediaPipe's annotations
        val x = min(floor(point.x).toInt(), image.width - 1)
        val y = min(floor(point.y).toInt(), image.height - 1)
        val radius = thickness.roundToInt()
        for (j in max(0, y - radius)..min(image.height - 1, y + radius)) {
            for (i in max(0, x - radius)..min(image.width - 1, x + radius)) {
                if ((i - x) * (i - x) + (j - y) * (j - y) <= radius * radius) alpha[j * image.width + i] = 0xff.toByte()
            }
        }
    }

    /** the segmentation of the object along [scribble], points in the pixels of [image] */
    fun segment(image: RgbaImage, scribble: List<Vector2>): Segmentation = segment(image) { alpha, thickness ->
        require(scribble.isNotEmpty()) { "a scribble without points" }
        // lines as wide as the thickness, like the scribbles of MediaPipe's annotations
        val half = max(thickness.roundToInt() / 2.0, 0.5)
        val segments = if (scribble.size == 1) listOf(scribble[0] to scribble[0]) else scribble.zipWithNext()
        for ((a, b) in segments) {
            val x0 = max(0, floor(min(a.x, b.x) - half).toInt())
            val x1 = min(image.width - 1, ceil(max(a.x, b.x) + half).toInt())
            val y0 = max(0, floor(min(a.y, b.y) - half).toInt())
            val y1 = min(image.height - 1, ceil(max(a.y, b.y) + half).toInt())
            val d = b - a
            val length2 = d.squaredLength
            for (j in y0..y1) for (i in x0..x1) {
                val p = Vector2(i.toDouble(), j.toDouble())
                val t = if (length2 == 0.0) 0.0 else ((p - a).dot(d) / length2).coerceIn(0.0, 1.0)
                if ((a + d * t).distanceTo(p) <= half) alpha[j * image.width + i] = 0xff.toByte()
            }
        }
    }

    /** the segmentation of the object at [point], in the pixels of [image] */
    fun segment(image: ColorBuffer, point: Vector2) = segment(image.toRgbaImage(), point)

    /** the segmentation of the object along [scribble], points in the pixels of [image] */
    fun segment(image: ColorBuffer, scribble: List<Vector2>) = segment(image.toRgbaImage(), scribble)

    /**
     * The segmentation of [image] with the region of interest that [draw] draws into its alpha channel, with lines as
     * thick as the image is larger than the input of the model
     */
    private fun segment(image: RgbaImage, draw: (alpha: ByteArray, thickness: Double) -> Unit): Segmentation {
        val w = model.width
        val h = model.height
        val alpha = ByteArray(image.width * image.height)
        draw(alpha, max(max(image.width.toDouble() / w, image.height.toDouble() / h), 1.0))
        val pixels = image.pixels.copyOf()
        for (i in alpha.indices) pixels[i * 4 + 3] = alpha[i]
        val withRoi = RgbaImage(image.width, image.height, pixels)
        val output = model.run(withRoi.tensor(w, h, Affine.stretch(image.width, image.height, w, h), 0f, 1f, channels = 4))
        return Segmentation(listOf("object"), w, h, listOf(output.values.single()))
    }

    override fun close() = model.close()
}
