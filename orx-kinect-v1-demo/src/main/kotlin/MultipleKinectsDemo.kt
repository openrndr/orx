package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.extra.kinect.v1.getKinectsV1

/**
 * Stream from 2 kinects side by side.
 */
fun main() = application {
    configure {
        width  = 640 * 2
        height = 480
    }
    program {
        val kinects = getKinectsV1(this)
        val depthCamera1 = kinects.startDevice(0).depthCamera
        val depthCamera2 = kinects.startDevice(1).depthCamera
        depthCamera1.enabled = true
        depthCamera1.mirror = true
        depthCamera2.enabled = true
        depthCamera2.mirror = true
        extend {
            drawer.image(depthCamera1.currentFrame)
            drawer.image(depthCamera2.currentFrame, depthCamera1.width.toDouble(), 0.0)
        }
        keyboard.keyDown.listen { keyEvent ->
            if (keyEvent.name == "1") {depthCamera1.enabled = !depthCamera1.enabled }
            if (keyEvent.name == "2") {depthCamera2.enabled = !depthCamera2.enabled }
            if (keyEvent.name == "q") {depthCamera1.mirror = !depthCamera1.mirror }
            if (keyEvent.name == "w") {depthCamera2.mirror = !depthCamera2.mirror }
        }
    }
}
