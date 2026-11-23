package org.openrndr.extra.mediapipe

import org.openrndr.extra.onnx.FloatTensor
import org.openrndr.extra.onnx.OnnxModel
import java.io.File
import java.nio.FloatBuffer

/**
 * A model of MediaPipe, an [OnnxModel] with one input, an image of [width] × [height] pixels of [channels] channels
 * unless [imageInput] is false, and outputs of floats
 */
internal class MediaPipeModel(path: String, imageInput: Boolean = true) : AutoCloseable {
    private val model = run {
        require(File(path).exists()) { "no model at '$path'; MediaPipeModels.model() downloads the models of orx-models, tools/convert_models.py converts them" }
        OnnxModel(path)
    }
    private val inputName = model.inputNames.first()

    private val inputShape = if (imageInput) model.inputShapes.getValue(inputName) else longArrayOf(0, 0, 0, 0)

    /** the channels of the input image, 3 for RGB, 4 for RGBA */
    val channels: Int = inputShape[3].toInt()

    /** the height of the input image */
    val height: Int = inputShape[1].toInt()

    /** the width of the input image */
    val width: Int = inputShape[2].toInt()

    /** the shapes of the inputs by name, -1 for a size that is not fixed */
    val inputShapes: Map<String, LongArray> get() = model.inputShapes

    /** the shapes of the outputs by name */
    val outputShapes: Map<String, LongArray> get() = model.outputShapes

    /** the width and height of a square input image */
    val size: Int get() = width.also { require(width == height) { "the input is $width × $height, not square" } }

    /** the outputs by name, for an input image in NHWC */
    fun run(input: FloatBuffer): Map<String, FloatArray> = run(input, longArrayOf(1, height.toLong(), width.toLong(), channels.toLong()))

    /** the outputs by name, for an input of [shape] */
    fun run(input: FloatBuffer, shape: LongArray): Map<String, FloatArray> = run(mapOf(inputName to (input to shape)))

    /** the outputs by name, for the inputs by name, each with its shape */
    fun run(inputs: Map<String, Pair<FloatBuffer, LongArray>>): Map<String, FloatArray> =
        model.run(inputs.mapValues { (_, input) -> FloatTensor(input.first, input.second) }).mapValues { it.value.toFloatArray() }

    override fun close() = model.close()
}
