package org.openrndr.extra.onnx.models

import org.openrndr.extra.onnx.models.vae.VAETrainer
import java.io.File

fun main() {

    val data = mutableListOf<DoubleArray>()

    for (i in 0 until 1000) {
        data.add(DoubleArray(3) { Math.random()})
    }

    val train = VAETrainer()
    train.train(File("."), data)

}