package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Render depth data from 2 kinect1 devices side-by-side.
 */
fun main() = application {
    configure {
        width  = 640 * 2
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        /*
         on production system you might consider using stable kinect serial numbers,
         instead of index numbers, to avoid reordering of devices already installed
         in physical space.
        */
        val depthCamera1 = kinect.openDevice(0).depthCamera
        val depthCamera2 = kinect.openDevice(1).depthCamera
        depthCamera1.enabled = true
        depthCamera1.flipH = true
        depthCamera2.enabled = true
        depthCamera2.flipH = true
        extend {
            drawer.image(depthCamera1.currentFrame)
            drawer.image(depthCamera2.currentFrame, depthCamera1.resolution.x.toDouble(), 0.0)
        }
    }
}
