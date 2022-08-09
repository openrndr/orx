package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.fx.colormap.TurboColormap
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.launch

/**
 * A use case where "virtual walls" can be established within certain
 * depth ranges. Useful for actual installations, like interactive
 * projections in the form of a "mirror" for the human silhouette.
 * The measurement in meters helps in calibration.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        camera.flipH = true // to make a mirror
        camera.depthMeasurement = DepthMeasurement.METERS
        val turboColormap = TurboColormap().apply {
            minValue = .5
            maxValue = 5.0
            curve = 1.0
        }
        val outputBuffer = colorBuffer(
            camera.resolution.x,
            camera.resolution.y
        )
        device.depthCamera.enabled = true

        /*
         * Note: the frameFlow will deliver new frames as soon as they
         * arrive from kinect, therefore some rendering can be already
         * performed when GPU is idle.
         *
         * Also the filters are being applied only if the actual new frame
         * from kinect was received. Kinect has different refresh rate (30 fps)
         * than usual display will have.
         *
         * We are dispatching coroutine here, for details check:
         * https://guide.openrndr.org/advancedDrawing/concurrencyAndMultithreading.html
         */
        launch {
            device.depthCamera.frameFlow.collect { frame ->
                turboColormap.apply(frame, outputBuffer)
            }
        }
        val settings = object {

            @BooleanParameter(label = "enabled", order = 0)
            var enabled: Boolean
                get() = camera.enabled
                set(value) {
                    camera.enabled = value
                }

            @BooleanParameter(label = "flipH", order = 1)
            var flipH: Boolean
                get() = camera.flipH
                set(value) {
                    camera.flipH = value
                }

            @BooleanParameter(label = "flipV", order = 2)
            var flipV: Boolean
                get() = camera.flipV
                set(value) {
                    camera.flipV = value
                }

            // Note: we could use turboColormap parameters directly in the GUI, however the
            // high range is cap to 1.0 there, and we want to use calibration in meters.
            // Increase 5.0 to something higher if you are calibrating for a bigger space.
            @DoubleParameter(label = "min distance", order = 3, low = 0.2, high = 5.0)
            var minDistance: Double
                get() = turboColormap.minValue
                set(value) {
                    turboColormap.minValue = value
                }

            @DoubleParameter(label = "max distance", order = 4, low = 0.2, high = 5.0)
            var maxDistance: Double
                get() = turboColormap.maxValue
                set(value) { turboColormap.maxValue = value }

            @DoubleParameter(label = "distance curve", order = 5, low = 0.01, high = 10.0)
            var curve: Double
                get() = turboColormap.curve
                set(value) {
                    turboColormap.curve = value
                }

        }
        extend(GUI()) {
            persistState = false
            compartmentsCollapsedByDefault = false
            add(settings, label = "depth camera")
        }
        extend {
            drawer.image(outputBuffer)
        }
    }
}
