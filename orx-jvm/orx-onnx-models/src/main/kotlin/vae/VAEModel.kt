package org.openrndr.extra.onnx.models.vae

import org.openrndr.extra.onnx.ExecutionProvider
import org.openrndr.extra.onnx.FloatTensor
import org.openrndr.extra.onnx.OnnxModel

/**
 * The encoder of a variational autoencoder, like those [VAETrainer] trains: it maps vectors of [inputDimensions] to
 * embeddings of [latentDimensions], the model has an input `input` and an output `embedding`
 */
class VAEEncoderModel(path: String, executionProvider: ExecutionProvider = ExecutionProvider.CPU) : AutoCloseable {
    private val model = OnnxModel(path, executionProvider)

    /** the number of values of an input vector */
    val inputDimensions: Int = model.inputShapes.getValue("input")[1].toInt()

    /** the number of values of an embedding */
    val latentDimensions: Int = model.outputShapes.getValue("embedding")[1].toInt()

    /** the embeddings of a batch of vectors, [inputDimensions] values each, one after the other */
    fun encode(input: FloatArray): FloatArray {
        require(input.size % inputDimensions == 0) { "${input.size} values, not vectors of $inputDimensions" }
        val batch = input.size / inputDimensions
        return model.run(FloatTensor(input, longArrayOf(batch.toLong(), inputDimensions.toLong()))).getValue("embedding").toFloatArray()
    }

    /** the embeddings of [inputs] */
    fun encode(inputs: List<DoubleArray>): List<DoubleArray> =
        encode(inputs.flatMap { v -> v.map { it.toFloat() } }.toFloatArray()).toList().chunked(latentDimensions) { c -> DoubleArray(c.size) { c[it].toDouble() } }

    override fun close() = model.close()
}

/**
 * The decoder of a variational autoencoder, like those [VAETrainer] trains: it maps embeddings of [latentDimensions]
 * back to vectors of [outputDimensions], the model has an input `embedding` and an output `reconstruction`
 */
class VAEDecoderModel(path: String, executionProvider: ExecutionProvider = ExecutionProvider.CPU) : AutoCloseable {
    private val model = OnnxModel(path, executionProvider)

    /** the number of values of an embedding */
    val latentDimensions: Int = model.inputShapes.getValue("embedding")[1].toInt()

    /** the number of values of a reconstructed vector */
    val outputDimensions: Int = model.outputShapes.getValue("reconstruction")[1].toInt()

    /** the vectors of a batch of embeddings, [latentDimensions] values each, one after the other */
    fun decode(input: FloatArray): FloatArray {
        require(input.size % latentDimensions == 0) { "${input.size} values, not embeddings of $latentDimensions" }
        val batch = input.size / latentDimensions
        return model.run(FloatTensor(input, longArrayOf(batch.toLong(), latentDimensions.toLong()))).getValue("reconstruction").toFloatArray()
    }

    /** the vectors of [embeddings] */
    fun decode(embeddings: List<DoubleArray>): List<DoubleArray> =
        decode(embeddings.flatMap { v -> v.map { it.toFloat() } }.toFloatArray()).toList().chunked(outputDimensions) { c -> DoubleArray(c.size) { c[it].toDouble() } }

    override fun close() = model.close()
}
