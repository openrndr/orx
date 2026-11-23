import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extra.onnx.models.vae.VAEEncoderModel
import org.openrndr.math.Vector2
import kotlin.random.Random

/**
 * Encodes random colors with the encoder of a variational autoencoder trained on colors, and draws each color at its
 * embedding.
 */
fun main() = application {
    program {
        val encoder = VAEEncoderModel("orx-jvm/orx-onnx-models/test-data/encoder.onnx")
        val random = Random(0)
        val colors = List(2000) { DoubleArray(encoder.inputDimensions) { random.nextDouble() } }
        val embeddings = encoder.encode(colors)
        encoder.close()
        extend {
            drawer.stroke = null
            for ((color, embedding) in colors.zip(embeddings)) {
                drawer.fill = ColorRGBa(color[0], color[1], color[2], 1.0, Linearity.LINEAR)
                // embeddings from -4 to 4 over the window
                drawer.circle(Vector2((embedding[0] / 8.0 + 0.5) * width, (embedding[1] / 8.0 + 0.5) * height), 4.0)
            }
        }
    }
}
