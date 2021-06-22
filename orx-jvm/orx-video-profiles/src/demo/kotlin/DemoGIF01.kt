import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.GIFProfile
import org.openrndr.extra.videoprofiles.ProresProfile
import org.openrndr.ffmpeg.ScreenRecorder

suspend fun main() = application {
    program {
        extend(ScreenRecorder()) {
            profile = GIFProfile()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}