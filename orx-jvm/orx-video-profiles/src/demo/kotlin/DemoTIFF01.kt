import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.TIFFProfile
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            profile = TIFFProfile()
            outputFile = "frame-%05d.tif"
            maximumFrames = 20
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
            drawer.fill = ColorRGBa.WHITE
            drawer.rectangle(frameCount / 20.0 * width, 0.0, 100.0, 100.0)
        }
    }
}