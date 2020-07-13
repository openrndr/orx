import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extensions.Screenshots


fun main() = application {
    program {

        val screenshots = extend(Screenshots()) {

        }

        val volumeTexture = VolumeTexture.create(128,128,32)
        val rt = renderTarget(128, 128) {
            volumeTexture(volumeTexture, 0)
        }

        val cb = colorBuffer(128, 128)
        extend {

            screenshots.afterScreenshot

            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa.PINK)
            }
            volumeTexture.copyTo(cb, 0)
            drawer.image(cb)
        }

    }
}