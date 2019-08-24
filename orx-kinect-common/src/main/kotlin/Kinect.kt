package org.openrndr.extra.kinect

import org.openrndr.Extension
import org.openrndr.draw.*
import org.openrndr.resourceUrl
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
     * @param num the kinect device index (starts with 0). If no value specified,
     *          it will default to 0.
     * @throws KinectException if device of such a number does not exist
     *          (better to count them first), or it was already started.
     * @see countDevices
     */
    fun startDevice(num: Int = 0): KinectDevice<CTX>

    /**
     * Executes low level Kinect commands in the kinect thread.
     */
    fun <T> execute(commands: (CTX) -> T) : T

}

/**
 * Represents specific device.
 *
 * @param CTX type of data needed to make low level kinect support calls (e.g. freenect contexts).
 */
interface KinectDevice<CTX> : Extension {

    val depthCamera: KinectDepthCamera

    /**
     * Executes low level Kinect commands in the kinect thread in the context of this device.
     */
    fun <T> execute(commands: (CTX) -> T): T

}

interface KinectCamera {
    var enabled: Boolean
    val width: Int
    val height: Int
    var mirror: Boolean
    val currentFrame: ColorBuffer
    /**
     * Returns the latest frame, but only once. Useful for the scenarios
     * where each new frame triggers extra computation. Therefore the same
     * expensive operation might happen only once, especially when the refresh
     * rate of the target screen is higher than kinect's 30 fps.
     * <p>
     * Example usage:
     * <pre>
     * kinect.depthCamera.getLatestFrame()?.let { frame ->
     *     grayscaleFilter.apply(frame, grayscaleBuffer)
     * }
     * </pre>
     */
    fun getLatestFrame(): ColorBuffer?
}

interface KinectDepthCamera : KinectCamera {
    /* no special attributes at the moment */
}

class KinectException(msg: String) : RuntimeException(msg)

/**
 * Maps depth values to grayscale.
 */
class DepthToGrayscaleMapper : Filter(
        filterShaderFromUrl(resourceUrl("depth-to-grayscale.frag", Kinects::class.java))
)

/**
 * Maps depth values to color map according to natural light dispersion as described
 * by Alan Zucconi in the
 * <a href="https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/">Improving the Rainbow</a>
 * article.
 */
class DepthToColorsZucconi6Mapper : Filter(
        filterShaderFromUrl(resourceUrl("depth-to-colors-zucconi6.frag", Kinects::class.java))
)

/**
 * Maps depth values to color map according to
 * <a href="https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html">
 *     Turbo, An Improved Rainbow Colormap for Visualization
 * </a>
 * by Google.
 */
class DepthToColorsTurboMapper : Filter(
        filterShaderFromUrl(resourceUrl("depth-to-colors-turbo.frag", Kinects::class.java))
)
