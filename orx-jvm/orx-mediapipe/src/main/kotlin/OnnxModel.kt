package org.openrndr.extra.mediapipe

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer

/** a model with one image input, of [size] × [size] RGB pixels, and outputs of floats */
internal class OnnxModel(path: String) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = run {
        require(File(path).exists()) { "no model at '$path', see orx-jvm/orx-mediapipe/tools/convert_models.py" }
        environment.createSession(path, OrtSession.SessionOptions())
    }
    private val inputName = session.inputNames.first()

    /** the width and height of the input */
    val size: Int = (session.inputInfo.getValue(inputName).info as ai.onnxruntime.TensorInfo).shape[1].toInt()

    /** the outputs by name, for an input in NHWC */
    fun run(input: FloatBuffer): Map<String, FloatArray> =
        OnnxTensor.createTensor(environment, input, longArrayOf(1, size.toLong(), size.toLong(), 3)).use { tensor ->
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
