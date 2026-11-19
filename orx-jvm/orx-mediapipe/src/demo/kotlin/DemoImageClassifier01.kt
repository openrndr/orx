import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.ImageClassifier
import org.openrndr.extra.mediapipe.ImageEmbedder

/**
 * Classifies images into the categories of ImageNet and draws their three most likely categories, and how much the
 * images look alike by the cosine similarities of their embeddings.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 960
        height = 480
    }
    program {
        val images = listOf("vw-beetle.jpg", "peopleCity01.jpg", "life-cover.jpg").map { loadImage("demo-data/images/$it") }
        val categories = ImageClassifier().use { classifier -> images.map { classifier.classify(it, maxResults = 3) } }
        val embeddings = ImageEmbedder().use { embedder -> images.map { embedder.embed(it) } }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 16.0)
        extend {
            drawer.fontMap = font
            for ((i, image) in images.withIndex()) {
                val x = i * 320.0
                // within a square of 320 pixels, keeping the aspect ratio
                val scale = 320.0 / maxOf(image.width, image.height)
                drawer.image(image, x + (320.0 - image.width * scale) / 2.0, 0.0, image.width * scale, image.height * scale)
                drawer.fill = ColorRGBa.WHITE
                for ((j, c) in categories[i].withIndex()) drawer.text("${c.label} ${"%.2f".format(c.score)}", x + 10.0, 360.0 + j * 22.0)
                // the similarity to the next image
                val next = (i + 1) % images.size
                drawer.fill = ColorRGBa.PINK
                drawer.text("like ${next + 1}: ${"%.2f".format(ImageEmbedder.cosineSimilarity(embeddings[i], embeddings[next]))}", x + 10.0, 440.0)
            }
        }
    }
}
