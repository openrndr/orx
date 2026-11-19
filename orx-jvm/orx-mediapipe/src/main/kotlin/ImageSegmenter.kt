package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.utils.buffer.MPPBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.floor

/**
 * The segmentation of an image by [ImageSegmenter]: for each category, how likely each pixel is to be of it, in masks of
 * [width] × [height] pixels that cover the whole image, stretched
 * @param categories the names of the categories
 * @param confidences for each category its mask, row by row from the top, from 0 to 1
 */
class Segmentation(
    val categories: List<String>,
    val width: Int,
    val height: Int,
    val confidences: List<FloatArray>
) {
    /** the mask of [category] */
    fun confidence(category: String): FloatArray = confidences[index(category)]

    /**
     * How likely the pixel at ([x], [y]) of the image of [imageWidth] × [imageHeight] pixels is to be of [category], the
     * mask resized to the image linearly, like MediaPipe's masks
     */
    fun confidenceAt(category: String, x: Double, y: Double, imageWidth: Int, imageHeight: Int): Double {
        val mask = confidence(category)
        // the centers of the pixels of the image in the mask
        val mx = ((x + 0.5) * width / imageWidth - 0.5).coerceIn(0.0, width - 1.0)
        val my = ((y + 0.5) * height / imageHeight - 0.5).coerceIn(0.0, height - 1.0)
        val x0 = floor(mx).toInt().coerceAtMost(width - 2).coerceAtLeast(0)
        val y0 = floor(my).toInt().coerceAtMost(height - 2).coerceAtLeast(0)
        val fx = mx - x0
        val fy = my - y0
        fun at(i: Int, j: Int) = mask[(y0 + j) * width + x0 + i].toDouble()
        return (at(0, 0) * (1 - fx) + at(1, 0) * fx) * (1 - fy) + (at(0, 1) * (1 - fx) + at(1, 1) * fx) * fy
    }

    /** for each pixel of the masks the index of its most likely category, or of `person` and none with a single mask */
    fun categoryMask(): IntArray = IntArray(width * height) { i ->
        if (confidences.size == 1) {
            if (confidences[0][i] > 0.5f) 0 else -1
        } else {
            confidences.indices.maxBy { confidences[it][i] }
        }
    }

    /**
     * Writes the mask of [category] into the red channel of [target], a color buffer of [width] × [height] pixels in
     * floating point, which is drawn over the image stretched
     */
    fun write(category: String, target: ColorBuffer) {
        require(target.effectiveWidth == width && target.effectiveHeight == height) {
            "the color buffer is ${target.effectiveWidth} × ${target.effectiveHeight}, the masks $width × $height"
        }
        writeMask(confidence(category), width, height, target)
    }

    private fun index(category: String) = categories.indexOf(category).also {
        require(it >= 0) { "no category '$category' in $categories" }
    }
}

/**
 * Segments images like MediaPipe's image segmenter, with its selfie segmentation models: the image is stretched over the
 * input of the model, the masks cover the whole image.
 *
 * - `selfie_multiclass_256x256.onnx`, the default, finds the categories `background`, `hair`, `body-skin`,
 *   `face-skin`, `clothes` and `others`, like accessories
 * - `selfie_segmenter.onnx` and `selfie_segmenter_landscape.onnx`, faster, for images of 16:9, find `person`
 * - `hair_segmenter.onnx` finds `background` and `hair`; it reads the alpha channel of the image too, as MediaPipe's
 */
class ImageSegmenter(
    modelPath: String = MediaPipeModels.model("selfie_multiclass_256x256.onnx")
) : AutoCloseable {
    private val model = MediaPipeModel(modelPath)

    /** the names of the categories that the model finds */
    val categories: List<String> = when (val channels = model.outputShapes.values.single().last().toInt()) {
        1 -> listOf("person")
        2 -> listOf("background", "hair")
        6 -> listOf("background", "hair", "body-skin", "face-skin", "clothes", "others")
        else -> error("a model with $channels categories")
    }

    /** the segmentation of [image] */
    fun segment(image: RgbaImage): Segmentation {
        val w = model.width
        val h = model.height
        val toImage = Affine.stretch(image.width, image.height, w, h)
        // the multiclass model takes colors from -1 to 1 and gives scores for a softmax, the selfie models take colors
        // from 0 to 1 and give the likelihood of a person, the hair segmenter takes colors and alpha from 0 to 1 and
        // gives scores for a softmax
        val channels = categories.size
        val low = if (channels == 6) -1f else 0f
        val outputs = model.run(image.tensor(w, h, toImage, low, 1f, channels = model.channels)).values.single()
        if (channels == 1) return Segmentation(categories, w, h, listOf(outputs))
        // the softmax over the categories of each pixel
        val confidences = List(channels) { FloatArray(w * h) }
        for (i in 0 until w * h) {
            val o = i * channels
            var max = outputs[o]
            for (c in 1 until channels) max = maxOf(max, outputs[o + c])
            var sum = 0.0
            for (c in 0 until channels) sum += exp((outputs[o + c] - max).toDouble())
            for (c in 0 until channels) confidences[c][i] = (exp((outputs[o + c] - max).toDouble()) / sum).toFloat()
        }
        return Segmentation(categories, w, h, confidences)
    }

    /** the segmentation of [image] */
    fun segment(image: ColorBuffer) = segment(image.toRgbaImage())

    override fun close() = model.close()
}

/** writes [mask] of [width] × [height] into the red channel of [target], a color buffer in floating point */
internal fun writeMask(mask: FloatArray, width: Int, height: Int, target: ColorBuffer) {
    require(target.effectiveWidth == width && target.effectiveHeight == height) {
        "the color buffer is ${target.effectiveWidth} × ${target.effectiveHeight}, the mask $width × $height"
    }
    val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
    // the rows of color buffers from the bottom up, unless they are stored upside down
    for (y in 0 until height) {
        val row = if (target.flipV) y else height - 1 - y
        for (x in 0 until width) buffer.putFloat(mask[row * width + x])
    }
    buffer.rewind()
    target.write(MPPBuffer(buffer), ColorFormat.R, ColorType.FLOAT32)
}
