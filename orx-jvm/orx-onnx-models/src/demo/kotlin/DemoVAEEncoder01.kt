package org.openrndr.extra.onnx.models

import ai.onnxruntime.OrtEnvironment
import org.openrndr.application
import org.openrndr.extra.onnx.models.vae.VAEDecoderModel
import org.openrndr.extra.onnx.models.vae.VAEEncoderModel
import java.io.File
import java.nio.FloatBuffer

fun main() {
    application {
        program {
            val env = OrtEnvironment.getEnvironment()
            val encoder = VAEEncoderModel.loadFromOnnx(env, File("encoder.onnx"))


            println(encoder.inputDimensions)
            println(encoder.latentDimensions)

            val input = FloatBuffer.allocate(300 )

            for (i in 0 until 100) {
                input.put(Math.random().toFloat())
                input.put(Math.random().toFloat())
                input.put(Math.random().toFloat())
            }
            input.rewind()

            val output = FloatBuffer.allocate(100 * encoder.latentDimensions)

            encoder.encode(input, output)

            for (i in 0 until 100) {
                print("$i: ")
                for (j in 0 until 2) {
                    val v = output.get()
                    print(v.toString() + ", ")
                }
                println()
            }
            extend {

            }
        }
    }
}