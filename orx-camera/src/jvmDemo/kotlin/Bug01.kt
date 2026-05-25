import org.openrndr.application
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.camera.OrbitalCamera
import org.openrndr.extra.camera.isolated
import org.openrndr.ffmpeg.ScreenRecorder

fun main() {
    application {
        program {
            val camera = OrbitalCamera()
            var lastSeconds = 0.0

            extend(ScreenRecorder())
            extend(ScreenRecorder())
            extend(ScreenRecorder())

            extend {
                camera.update(seconds - lastSeconds)
                camera.isolated(drawer) {}
                lastSeconds = seconds
            }
        }
    }
}