import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.extra.fx.transform.FlipVertically
import org.openrndr.extra.runway.FaceLandmarksRequest
import org.openrndr.extra.runway.FaceLandmarksResponse
import org.openrndr.extra.runway.runwayQuery
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

suspend fun main() = application {
    configure {
        width = 768
        height = 576
    }

    program {
        val image = colorBuffer(640, 360)
        val camera = VideoPlayerFFMPEG.fromDevice(imageWidth = 640, imageHeight = 360)
        camera.play()
        val flip = FlipVertically()

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 12.0)
        camera.newFrame.listen {
            flip.apply(it.frame, image)
        }
        extend {
            camera.draw(drawer)
            drawer.fontMap = font
            try {
                val response: FaceLandmarksResponse =
                        runwayQuery("http://localhost:8000/query", FaceLandmarksRequest(image.toDataUrl()))

                val rx = image.width / 720.0
                val ry = 360.0 / image.height

                (response.labels zip response.points).forEach {
                    val position = Vector2(it.second[0] * 720.0 * rx, it.second[1] * 360.0 * ry)
                    drawer.fill = ColorRGBa.PINK
                    drawer.circle(position, 10.0)
                    drawer.text(it.first, position)
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }
}