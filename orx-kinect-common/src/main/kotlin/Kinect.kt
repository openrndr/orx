package org.openrndr.extra.kinect

import org.openrndr.Extension
import org.openrndr.draw.*
import java.lang.RuntimeException

/**
 * Represents all the accessible kinects handled by a specific driver (V1, V2).
 *
 * @param <CTX> data needed to make low level kinect support calls.
 */
interface Kinects<CTX> {
    fun countDevices(): Int

    /**
     * Starts kinect device of a given number.
     *
     * @throws KinectException if device of such a number does not exist,
     *          better to count them first.
     * @see countDevices
     */
    fun startDevice(num: Int = 0): KinectDevice<CTX>

    /**
     * Executes low level Kinect commands in the kinect thread.
     */
    fun execute(commands: (CTX) -> Any) : Any
}

/**
 * Represents specific device.
 *
 * @param <CTX> data needed to make low level kinect support calls.
 */
interface KinectDevice<CTX> : Extension {
    val depthCamera: KinectDepthCamera

    /**
     * Executes low level Kinect commands in the kinect thread in the context of this device.
     */
    fun execute(commands: (CTX) -> Any): Any
}

interface KinectCamera {
    var enabled: Boolean
    val width: Int
    val height: Int
    var mirror: Boolean
    val currentFrame: ColorBuffer
    fun getLatestFrame(): ColorBuffer?
}

interface KinectDepthCamera : KinectCamera {
    /* no special attributes at the moment */
}

class KinectException(msg: String) : RuntimeException(msg)
