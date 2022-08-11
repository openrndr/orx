package org.openrndr.extra.depth.camera

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.IntVector2

/**
 * Defines how pixel values encoded in depth [ColorBuffer] will be interpreted.
 */
enum class DepthMeasurement {

    /**
     * Raw values, but normalized to the range 0-1.
     * Useful for debugging, because full range of captured values can be rendered
     * as a texture. Therefore it's a default setting.
     */
    RAW_NORMALIZED,

    /**
     * Raw values, exactly as they are provided by the device.
     * Note: it might imply that [ColorBuffer] of the depth camera frame
     * is provided in integer-based format (for example in case of Kinect devices).
     */
    RAW,

    /**
     * Expressed in meters.
     * It is using floating point numbers.
     * Note: values above `1.0` will not be visible if displayed as a texture.
     */
    METERS,

}

/**
 * General API of any depth camera.
 */
interface DepthCamera {

    /**
     * Current operating resolution.
     */
    val resolution: IntVector2

    /**
     * The units/mapping in which depth is expressed on received frames.
     */
    var depthMeasurement: DepthMeasurement

    /**
     * Flips source depth data image in horizontal axis (mirror).
     */
    var flipH: Boolean

    /**
     * Flips source depth data image in vertical axis (upside-down).
     */
    var flipV: Boolean

    /**
     * The most recent frame received from the depth camera.
     */
    val currentFrame: ColorBuffer

    /**
     * Will execute the supplied block of code with each most recent frame
     * from the depth camera as an input.
     *
     * @param block the code to execute when the new frame is received.
     */
    fun onFrameReceived(block: (frame: ColorBuffer) -> Unit)

}
