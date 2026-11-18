package org.openrndr.extra.mediapipe

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer

/**
 * A model with one input, an image of [width] × [height] pixels of [channels] channels unless [imageInput] is false,
 * and outputs of floats
 */
internal class OnnxModel(path: String, imageInput: Boolean = true) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = run {
        require(File(path).exists()) { "no model at '$path', see orx-jvm/orx-mediapipe/tools/convert_models.py" }
        environment.createSession(path, OrtSession.SessionOptions())
    }
    private val inputName = session.inputNames.first()

    private val inputShape = if (imageInput) (session.inputInfo.getValue(inputName).info as ai.onnxruntime.TensorInfo).shape else longArrayOf(0, 0, 0, 0)

    /** the channels of the input image, 3 for RGB, 4 for RGBA */
    val channels: Int = inputShape[3].toInt()

    /** the height of the input image */
    val height: Int = inputShape[1].toInt()

    /** the width of the input image */
    val width: Int = inputShape[2].toInt()

    /** the shapes of the outputs by name */
    val outputShapes: Map<String, LongArray> =
        session.outputInfo.mapValues { (_, info) -> (info.info as ai.onnxruntime.TensorInfo).shape }

    /** the width and height of a square input image */
    val size: Int get() = width.also { require(width == height) { "the input is $width × $height, not square" } }

    /** the outputs by name, for an input image in NHWC */
    fun run(input: FloatBuffer): Map<String, FloatArray> = run(input, longArrayOf(1, height.toLong(), width.toLong(), channels.toLong()))

    /** the outputs by name, for an input of [shape] */
    fun run(input: FloatBuffer, shape: LongArray): Map<String, FloatArray> =
        OnnxTensor.createTensor(environment, input, shape).use { tensor ->
            session.run(mapOf(inputName to tensor)).use { result ->
                result.associate { (name, value) ->
                    val buffer = (value as OnnxTensor).floatBuffer
                    name to FloatArray(buffer.remaining()).also { buffer.get(it) }
                }
            }
        }

    override fun close() {
        session.close()
    }
}
