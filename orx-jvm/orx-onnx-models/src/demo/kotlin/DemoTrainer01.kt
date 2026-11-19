import org.openrndr.extra.onnx.models.vae.VAEDecoderModel
import org.openrndr.extra.onnx.models.vae.VAEEncoderModel
import org.openrndr.extra.onnx.models.vae.VAETrainer
import java.io.File
import kotlin.random.Random

/**
 * Trains a variational autoencoder on random colors, with embeddings of two values, and encodes and decodes a few
 * colors with it. Training needs Python; it installs PyTorch, ONNX and ONNX Runtime in a virtual environment.
 */
fun main() {
    val random = Random(0)
    val colors = List(1000) { DoubleArray(3) { random.nextDouble() } }
    val (encoderFile, decoderFile) = VAETrainer().train(File("vae-colors"), colors, latentDimensions = 2, epochs = 100)
    VAEEncoderModel(encoderFile.path).use { encoder ->
        VAEDecoderModel(decoderFile.path).use { decoder ->
            for (color in colors.take(5)) {
                val embedding = encoder.encode(listOf(color)).single()
                val decoded = decoder.decode(listOf(embedding)).single()
                println("${color.toList()} -> ${embedding.toList()} -> ${decoded.toList()}")
            }
        }
    }
}
