import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.fx.colormap.GrayscaleColormap
import org.openrndr.extra.fx.colormap.SpectralZucconiColormap
import org.openrndr.extra.fx.colormap.TurboColormap
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.math.Vector2

/**
 * Shows 4 different color representations of the depth map:
 *
 * * the original depth map stored as RED channel values
 * * the same values expressed as gray tones
 * * zucconi6 color map according to natural light dispersion as described
 *   by Alan Zucconi in
 *   [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 *   article
 * * turbo color map according to
 *   [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 *   by Google.
 *
 * Note: the values are normalized in range 0-1, not in meters.
 * @see GrayscaleColormap
 * @see SpectralZucconiColormap
 * @see TurboColormap
 */
fun main() = application {
    val guiOffset = 200
    configure {
        width =  2 * 640 + guiOffset
        height = 2 * 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        fun outputBuffer() = colorBuffer(
            camera.resolution.x,
            camera.resolution.y,
            format = ColorFormat.RGB
        )
        val grayscaleColormap = GrayscaleColormap()
        val spectralZucconiColormap = SpectralZucconiColormap()
        val turboColormap = TurboColormap()
        val grayscaleBuffer = outputBuffer()
        val zucconiBuffer = outputBuffer()
        val turboBuffer = outputBuffer()
        @Suppress("unused")
        val settings = object {

            @BooleanParameter(label = "enabled", order = 0)
            var enabled: Boolean
                get() = camera.enabled
                set(value) { camera.enabled = value }

            @BooleanParameter(label = "flipH", order = 1)
            var flipH: Boolean
                get() = camera.flipH
                set(value) { camera.flipH = value }

            @BooleanParameter(label = "flipV", order = 2)
            var flipV: Boolean
                get() = camera.flipV
                set(value) { camera.flipV = value }

        }
        camera.onFrameReceived { frame ->
            grayscaleColormap.apply(frame, grayscaleBuffer)
            spectralZucconiColormap.apply(frame, zucconiBuffer)
            turboColormap.apply(frame, turboBuffer)
        }
        camera.enabled = true
        extend(GUI()) {
            persistState = false
            compartmentsCollapsedByDefault = false
            add(settings, label = "depth camera")
            add(grayscaleColormap)
            add(spectralZucconiColormap)
            add(turboColormap)
        }
        extend {
            drawer.image(camera.currentFrame, guiOffset.toDouble(), 0.0)
            drawer.image(grayscaleBuffer, guiOffset + camera.resolution.x.toDouble(), 0.0)
            drawer.image(turboBuffer, guiOffset.toDouble(), camera.resolution.y.toDouble())
            drawer.image(zucconiBuffer, Vector2(guiOffset.toDouble(), 0.0) + camera.resolution.vector2)
        }
    }
}
