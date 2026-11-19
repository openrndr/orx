import org.openrndr.extra.onnx.ExecutionProvider
import org.openrndr.extra.onnx.FloatTensor
import org.openrndr.extra.onnx.OnnxModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestOnnxModel {
    @Test
    fun `a model adds one to its inputs, on the CPU and with CoreML`() {
        val providers = listOf(ExecutionProvider.CPU) +
            if (System.getProperty("os.name").startsWith("Mac")) listOf(ExecutionProvider.CORE_ML) else emptyList()
        for (provider in providers) {
            OnnxModel("test-data/add_one.onnx", provider).use { model ->
                assertEquals(listOf("input"), model.inputNames)
                assertEquals(listOf("output"), model.outputShapes.keys.toList())
                val input = FloatArray(10) { it.toFloat() }
                val output = model.run(FloatTensor(input, longArrayOf(10))).getValue("output")
                assertTrue(output.toFloatArray().contentEquals(FloatArray(10) { it + 1f }), "$provider")
                assertEquals(listOf(10L), output.shape.toList())
            }
        }
    }
}
