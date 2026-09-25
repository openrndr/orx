package org.openrndr.extra.onnx.models

import ai.onnxruntime.OrtEnvironment
import org.openrndr.application
import org.openrndr.extra.onnx.models.vae.VAEDecoderModel
import java.io.File
import java.nio.FloatBuffer

fun main() {
    application {
        program {
            val env = OrtEnvironment.getEnvironment()
            val decoder = VAEDecoderModel.loadFromOnnx(env, File("decoder.onnx"))

            val input = FloatBuffer.allocate(200 )

            for (i in 0 until 100) {
                input.put(Math.random().toFloat())
                input.put(Math.random().toFloat())
            }
            input.rewind()

            val output = FloatBuffer.allocate(100 * decoder.outputDimensions)


            extend {

            }
        }
    }
}