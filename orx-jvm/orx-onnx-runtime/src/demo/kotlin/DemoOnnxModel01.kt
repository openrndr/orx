import org.openrndr.extra.onnx.FloatTensor
import org.openrndr.extra.onnx.OnnxModel

/**
 * Runs a model that adds one to each of its inputs, and prints its inputs, outputs and results.
 */
fun main() {
    OnnxModel("orx-jvm/orx-onnx-runtime/test-data/add_one.onnx").use { model ->
        println("inputs ${model.inputShapes.mapValues { it.value.toList() }}")
        println("outputs ${model.outputShapes.mapValues { it.value.toList() }}")
        val input = FloatArray(10) { it.toFloat() }
        val output = model.run(FloatTensor(input, longArrayOf(10))).values.single()
        println("${input.toList()} -> ${output.toFloatArray().toList()}")
    }
}
