import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VolumeTexture
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget

fun main() = application {
    program {
        val volumeTexture = VolumeTexture.create(128,128,32)
        val rt = renderTarget(128, 128) {
            volumeTexture(volumeTexture, 0)
        }

        val cb = colorBuffer(128, 128)
        extend {
            drawer.isolatedWithTarget(rt) {
                drawer.ortho(rt)
                drawer.clear(ColorRGBa.PINK)
            }
            volumeTexture.copyTo(cb, 0)
            drawer.image(cb)
        }
    }
}