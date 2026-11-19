import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.mediapipe.ObjectDetector

/**
 * Finds the objects in an image and draws their boxes and categories.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 768
        height = 768
    }
    program {
        val image = loadImage("demo-data/images/peopleCity01.jpg")
        val objects = ObjectDetector(scoreThreshold = 0.3).use { it.detect(image) }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 12.0)
        extend {
            drawer.scale(width / image.width.toDouble())
            drawer.image(image)
            drawer.fontMap = font
            for (o in objects) {
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.strokeWeight = 2.0
                drawer.rectangle(o.bounds)
                drawer.fill = ColorRGBa.PINK
                drawer.stroke = null
                drawer.text("${o.category.label} ${"%.2f".format(o.category.score)}", o.bounds.x + 3.0, o.bounds.y + 12.0)
            }
        }
    }
}
