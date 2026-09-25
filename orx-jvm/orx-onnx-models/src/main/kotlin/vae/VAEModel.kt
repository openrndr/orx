package org.openrndr.extra.onnx.models.vae

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import ai.onnxruntime.TensorInfo
import java.io.File
import java.nio.FloatBuffer
import kotlin.getValue

class VAEDecoderModel(val env: OrtEnvironment, val session: OrtSession): AutoCloseable {

    val latentDimensions: Int by lazy { (session.inputInfo["embedding"]!!.info as TensorInfo).shape[1].toInt() }
    val outputDimensions: Int by lazy { (session.outputInfo["reconstruction"]!!.info as TensorInfo).shape[1].toInt() }

    fun decode(input: FloatBuffer, output: FloatBuffer): FloatBuffer {
        require(input.remaining() % latentDimensions == 0)
        val batches = input.remaining() / latentDimensions


        val inputTensor = OnnxTensor.createTensor(env, input, longArrayOf(batches.toLong(), latentDimensions.toLong()))

        val result = session.run(mapOf("embedding" to inputTensor))
        val outputTensor = result["reconstruction"]!!.get() as OnnxTensor
        output.mark()
        output.put(outputTensor.floatBuffer)
        output.flip()
        result.close()

        inputTensor.close()
        return output
    }

    override fun close() {
        session.close()
    }

    companion object {
        fun loadFromOnnx(env: OrtEnvironment, model: File): VAEDecoderModel {
            val options = SessionOptions()
            val session = env.createSession(model.absolutePath, options)
            return VAEDecoderModel(env, session)
        }
    }
}


class VAEEncoderModel(val env: OrtEnvironment, val session: OrtSession): AutoCloseable {
    val latentDimensions: Int by lazy { (session.outputInfo["embedding"]!!.info as TensorInfo).shape[1].toInt() }
    val inputDimensions: Int by lazy { (session.inputInfo["input"]!!.info as TensorInfo).shape[1].toInt() }

    fun encode(input: FloatBuffer, output: FloatBuffer): FloatBuffer {
        require(input.remaining() % inputDimensions == 0)
        val batches = input.remaining() / inputDimensions

        val inputTensor = OnnxTensor.createTensor(env, input, longArrayOf(batches.toLong(), inputDimensions.toLong()))

        val result = session.run(mapOf("input" to inputTensor))
        val outputTensor = result["embedding"]!!.get() as OnnxTensor
        output.mark()
        output.put(outputTensor.floatBuffer)
        output.flip()
        result.close()

        inputTensor.close()
        return output
    }

    override fun close() {
        session.close()
    }

    companion object {
        fun loadFromOnnx(env: OrtEnvironment, model: File): VAEEncoderModel {
            val options = SessionOptions()
            val session = env.createSession(model.absolutePath, options)
            return VAEEncoderModel(env, session)
        }
    }
}