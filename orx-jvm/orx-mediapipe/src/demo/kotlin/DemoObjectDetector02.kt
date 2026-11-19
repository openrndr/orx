import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.draw.loadFont
import org.openrndr.extra.mediapipe.ObjectDetection
import org.openrndr.extra.mediapipe.ObjectDetector
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * Finds the objects in front of the webcam in every frame and draws their boxes and categories.
 * The models and images are downloaded from [orx-models](https://github.com/openrndr/orx-models) when first used.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
    }
    program {
        val webcam = loadVideoDevice(width = 1280, height = 720)
        webcam.play()
        val detector = ObjectDetector(scoreThreshold = 0.4)
        var objects = emptyList<ObjectDetection>()
        var millis = 0.0
        webcam.newFrame.listen {
            val start = System.nanoTime()
            objects = detector.detect(it.frame)
            millis = (System.nanoTime() - start) / 1E6
        }
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 20.0)
        extend {
            webcam.draw(drawer, blind = true)
            val frame = webcam.colorBuffer ?: return@extend
            val scale = width / frame.effectiveWidth.toDouble()
            drawer.isolated {
                drawer.scale(scale)
                drawer.image(frame)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.strokeWeight = 3.0 / scale
                drawer.rectangles(objects.map { it.bounds })
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.PINK
            for (o in objects) drawer.text("${o.category.label} ${"%.2f".format(o.category.score)}", o.bounds.x * scale + 4.0, o.bounds.y * scale + 20.0)
            drawer.fill = ColorRGBa.WHITE
            drawer.text("${objects.size} objects, ${"%.1f".format(millis)} ms", 20.0, height - 20.0)
        }
    }
}
