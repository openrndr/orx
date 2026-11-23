import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extra.onnx.models.vae.VAEDecoderModel
import org.openrndr.extra.shapes.primitives.grid

/**
 * Decodes a grid of embeddings with the decoder of a variational autoencoder trained on colors, and draws each cell in
 * its color.
 */
fun main() = application {
    program {
        val decoder = VAEDecoderModel("orx-jvm/orx-onnx-models/test-data/decoder.onnx")
        // the embeddings of a grid of 20 × 20 cells, from -2.5 to 2.5, where the colors of the data are
        val embeddings = (0 until 20).flatMap { j -> (0 until 20).map { i -> doubleArrayOf((i / 19.0 - 0.5) * 5.0, (j / 19.0 - 0.5) * 5.0) } }
        val colors = decoder.decode(embeddings).map { ColorRGBa(it[0], it[1], it[2], 1.0, Linearity.LINEAR) }
        decoder.close()
        extend {
            drawer.stroke = null
            for ((cell, color) in drawer.bounds.grid(20, 20).flatten().zip(colors)) {
                drawer.fill = color
                drawer.rectangle(cell)
            }
        }
    }
}
