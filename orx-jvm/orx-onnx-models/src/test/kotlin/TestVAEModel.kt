import org.openrndr.extra.onnx.models.vae.VAEDecoderModel
import org.openrndr.extra.onnx.models.vae.VAEEncoderModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestVAEModel {
    @Test
    fun `colors are encoded in two values and decoded into colors`() {
        VAEEncoderModel("test-data/encoder.onnx").use { encoder ->
            VAEDecoderModel("test-data/decoder.onnx").use { decoder ->
                assertEquals(3, encoder.inputDimensions)
                assertEquals(2, encoder.latentDimensions)
                assertEquals(2, decoder.latentDimensions)
                assertEquals(3, decoder.outputDimensions)

                val colors = listOf(doubleArrayOf(0.1, 0.5, 0.9), doubleArrayOf(0.8, 0.2, 0.4), doubleArrayOf(0.5, 0.5, 0.5))
                val embeddings = encoder.encode(colors)
                assertEquals(3, embeddings.size)
                assertTrue(embeddings.all { it.size == 2 })
                // a batch encodes like its vectors one by one
                for ((color, embedding) in colors.zip(embeddings)) {
                    assertTrue(encoder.encode(listOf(color)).single().zip(embedding).all { (a, b) -> kotlin.math.abs(a - b) < 1E-5 })
                }
                val decoded = decoder.decode(embeddings)
                assertEquals(3, decoded.size)
                // the decoder ends in a sigmoid over the range of the data
                assertTrue(decoded.all { v -> v.size == 3 && v.all { it in -0.01..1.01 } }, decoded.map { it.toList() }.toString())
            }
        }
    }
}
