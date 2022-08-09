package org.openrndr.extra.depth.camera

import kotlinx.coroutines.flow.Flow
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

interface DepthCamera {
    val resolution: IntVector2
    var depthMeasurement: DepthMeasurement
    var flipH: Boolean
    var flipV: Boolean
    val currentFrame: ColorBuffer
    val frameFlow: Flow<ColorBuffer>
}
