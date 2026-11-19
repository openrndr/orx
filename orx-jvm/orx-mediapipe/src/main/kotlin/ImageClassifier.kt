package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import java.io.File
import kotlin.math.sqrt

/** a category of an image, or of an object, and how likely it is */
class Category(val label: String, val score: Double)

/** the labels of a model, from the file next to it that [MediaPipeModels] downloads or `tools/convert_models.py` writes */
internal fun labels(modelPath: String): List<String> {
    val file = File(modelPath.removeSuffix(".onnx") + ".labels.txt")
    require(file.exists()) { "no labels at '$file'; MediaPipeModels.model() downloads the models of orx-models, tools/convert_models.py converts them" }
    return file.readLines().filter { it.isNotBlank() }
}

/**
 * Classifies images like MediaPipe's image classifier, into the 1000 categories of ImageNet, with
 * `efficientnet_lite0.onnx` or the slower and more precise `efficientnet_lite2.onnx`: the image is stretched over the
 * input of the model.
 */
class ImageClassifier(
    modelPath: String = MediaPipeModels.model("efficientnet_lite0.onnx")
) : AutoCloseable {
    private val model = MediaPipeModel(modelPath)

    /** the labels of the categories */
    val labels = labels(modelPath)

    /** the [maxResults] most likely categories of [image], the most likely first */
    fun classify(image: RgbaImage, maxResults: Int = 5): List<Category> {
        val toImage = Affine.stretch(image.width, image.height, model.width, model.height)
        val scores = model.run(image.tensor(model.width, model.height, toImage, -1f, 1f)).values.single()
        return scores.indices.sortedByDescending { scores[it] }.take(maxResults).map { Category(labels[it], scores[it].toDouble()) }
    }

    /** the [maxResults] most likely categories of [image], the most likely first */
    fun classify(image: ColorBuffer, maxResults: Int = 5) = classify(image.toRgbaImage(), maxResults)

    override fun close() = model.close()
}

/**
 * Embeds images like MediaPipe's image embedder: a vector for each image, whose cosine similarity with that of another
 * image says how much they look alike, with `mobilenet_v3_small.onnx`, 1024 numbers, or `mobilenet_v3_large.onnx`,
 * 1280 numbers. The image is stretched over the input of the model.
 */
class ImageEmbedder(
    modelPath: String = MediaPipeModels.model("mobilenet_v3_small.onnx")
) : AutoCloseable {
    private val model = MediaPipeModel(modelPath)

    /** the embedding of [image], of length 1 */
    fun embed(image: RgbaImage): FloatArray {
        val toImage = Affine.stretch(image.width, image.height, model.width, model.height)
        val embedding = model.run(image.tensor(model.width, model.height, toImage, 0f, 1f)).values.single()
        val length = sqrt(embedding.sumOf { it.toDouble() * it }).toFloat()
        return FloatArray(embedding.size) { embedding[it] / length }
    }

    /** the embedding of [image], of length 1 */
    fun embed(image: ColorBuffer) = embed(image.toRgbaImage())

    override fun close() = model.close()

    companion object {
        /** the cosine similarity of two embeddings, 1 for images that look alike, around 0 for unrelated images */
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
            require(a.size == b.size) { "embeddings of ${a.size} and ${b.size} numbers" }
            var dot = 0.0
            var aa = 0.0
            var bb = 0.0
            for (i in a.indices) {
                dot += a[i].toDouble() * b[i]
                aa += a[i].toDouble() * a[i]
                bb += b[i].toDouble() * b[i]
            }
            return dot / sqrt(aa * bb)
        }
    }
}
