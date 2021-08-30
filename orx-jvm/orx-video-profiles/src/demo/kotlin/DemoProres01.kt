import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.videoprofiles.ProresProfile
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    program {
        extend(ScreenRecorder()) {
            profile = ProresProfile()
        }
        extend {
            drawer.clear(ColorRGBa.GREEN)
        }
    }
}