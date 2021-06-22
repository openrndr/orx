package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.extra.kinect.v1.getKinectsV1

/**
 * Basic kinect use case showing continuous stream from the depth camera.
 *
 * Note: kinect depth map is stored only on the RED color channel to save
 *       space. Therefore depth map is displayed only in the red tones.
 */
suspend fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinects = getKinectsV1(this)
        val kinect = kinects.startDevice()
        kinect.depthCamera.enabled = true
        kinect.depthCamera.mirror = true
        extend {
            drawer.image(kinect.depthCamera.currentFrame)
        }
    }
}
