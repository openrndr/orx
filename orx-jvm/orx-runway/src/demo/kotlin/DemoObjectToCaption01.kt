import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Writer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.fx.transform.FlipVertically
import org.openrndr.extra.runway.DenseCapRequest
import org.openrndr.extra.runway.DenseCapResponse
import org.openrndr.extra.runway.runwayQuery
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.shape.Rectangle


/**
 * This demonstrates an object to text network. It generates caption texts from objects detected in
 * the camera image
 * This example requires a `runway/DenseCap` model to be active in Runway.
 */

fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = colorBuffer(640, 360)
        val camera = VideoPlayerFFMPEG.fromDevice(imageWidth = 640, imageHeight = 360)
        camera.play()
        val flip = FlipVertically()

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
        camera.newFrame.listen {
            flip.apply(it.frame, image)
        }
        extend(ScreenRecorder()) {
            frameRate = 1
        }
        extend {
            camera.draw(drawer)
            drawer.fontMap = font
            val response: DenseCapResponse =
                    runwayQuery("http://localhost:8000/query", DenseCapRequest(image.toDataUrl(), maxDetections = 1))

            drawer.fontMap = font
            drawer.image(image)

            for (i in response.bboxes.indices) {
                val width = (response.bboxes[i][2] - response.bboxes[i][0]) * image.width
                val height = (response.bboxes[i][3] - response.bboxes[i][1]) * image.height
                val x = response.bboxes[i][0] * image.width
                val y = response.bboxes[i][1] * image.height
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.rectangle(x, y, width, height)
                drawer.stroke = null
                drawer.fill = ColorRGBa.PINK
                val w = Writer(drawer)
                w.box = Rectangle(x + 10.0, y + 10.0, width - 20.0, height - 20.0)
                w.newLine()
                w.text(response.classes[i])
            }
        }
    }
}