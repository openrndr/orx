package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Here you can see freenect FLOOD log level in action.
 *
 * Note: technically it would be possible to redirect kinect log to
 * slf4j logger in the implementation of [Kinect1], however I removed
 * this callback and left logs on the standard out, because it might get so noisy,
 * that native-to-JVM round trip with conversion into [String] for JVM
 * logging might completely kill the performance and result in
 * stack overflow exception.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        kinect.logLevel = Kinect1.LogLevel.FLOOD
        val device = kinect.openDevice()
        device.depthCamera.enabled = true
        extend {
            drawer.image(device.depthCamera.currentFrame)
        }
    }
}
