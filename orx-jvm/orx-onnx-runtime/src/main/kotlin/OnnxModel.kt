package org.openrndr.extra.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.io.File
import java.nio.FloatBuffer

/**
 * A tensor of 32 bit floats: its [values] in row-major order and its [shape]
 */
class FloatTensor(val values: FloatBuffer, val shape: LongArray) {
    constructor(values: FloatArray, shape: LongArray) : this(FloatBuffer.wrap(values), shape)

    /** the number of values */
    val size: Int get() = values.limit()

    /** the value at [index] in row-major order */
    operator fun get(index: Int): Float = values.get(index)

    /** the values, the array the tensor wraps when it wraps all of one */
    fun toFloatArray(): FloatArray =
        if (values.hasArray() && values.arrayOffset() == 0 && values.array().size == size) values.array()
        else FloatArray(size).also { values.duplicate().apply { rewind() }.get(it) }
}

/** where a model runs */
enum class ExecutionProvider {
    /** on the CPU, everywhere */
    CPU,

    /** with CoreML on macOS, on the GPU or the Neural Engine where it can, the CPU where it cannot */
    CORE_ML
}

/**
 * A model in ONNX, run by ONNX Runtime, with inputs and outputs of 32 bit floats
 * @param path the path of the model, a `.onnx` file, with its external data next to it
 * @param executionProvider where the model runs
 */
class OnnxModel(path: String, executionProvider: ExecutionProvider = ExecutionProvider.CPU) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment()
    private val session = run {
        require(File(path).exists()) { "no model at '$path'" }
        val options = OrtSession.SessionOptions()
        if (executionProvider == ExecutionProvider.CORE_ML) options.addCoreML()
        environment.createSession(path, options)
    }

    /** the names of the inputs, in the order of the model */
    val inputNames: List<String> = session.inputNames.toList()

    /** the shapes of the inputs by name, -1 for a size that is not fixed, like that of a batch */
    val inputShapes: Map<String, LongArray> = session.inputInfo.mapValues { (_, info) -> (info.info as TensorInfo).shape }

    /** the shapes of the outputs by name, -1 for a size that is not fixed */
    val outputShapes: Map<String, LongArray> = session.outputInfo.mapValues { (_, info) -> (info.info as TensorInfo).shape }

    /** the outputs by name, for the inputs by name */
    fun run(inputs: Map<String, FloatTensor>): Map<String, FloatTensor> {
        val tensors = inputs.mapValues { (_, input) ->
            OnnxTensor.createTensor(environment, input.values.duplicate().apply { rewind() }, input.shape)
        }
        try {
            return session.run(tensors).use { result ->
                result.associate { (name, value) ->
                    val tensor = value as OnnxTensor
                    val buffer = tensor.floatBuffer
                    name to FloatTensor(FloatArray(buffer.remaining()).also { buffer.get(it) }, tensor.info.shape)
                }
            }
        } finally {
            tensors.values.forEach { it.close() }
        }
    }

    /** the outputs by name, for the single input of the model */
    fun run(input: FloatTensor): Map<String, FloatTensor> = run(mapOf(inputNames.single() to input))

    override fun close() {
        session.close()
    }
}
