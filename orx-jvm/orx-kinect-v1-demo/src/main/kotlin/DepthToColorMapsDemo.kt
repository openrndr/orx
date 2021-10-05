package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.kinect.*
import org.openrndr.extra.kinect.v1.getKinectsV1

/**
 * Shows 4 different representations of the depth map.
 * <ol>
 *     <li>the original depth map stored as RED channel values</li>
 *     <li>the same values expressed as gray tones</li>
 *     <li>
 *         color map according to natural light dispersion as described
 *         by Alan Zucconi in the
 *         <a href="https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/">Improving the Rainbow</a>
 *         article.
 *     </li>
 *     <li>
 *         color map according to
 *         <a href="https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html">
 *             Turbo, An Improved Rainbow Colormap for Visualization
 *         </a>
 *         by Google.
 *     </li>
 * </ol>
 *
 * @see DepthToGrayscaleMapper
 * @see DepthToColorsZucconi6Mapper
 * @see DepthToColorsTurboMapper
 */
fun main() = application {
    configure {
        width =  2 * 640
        height = 2 * 480
    }
    program {
        val kinects = getKinectsV1(this)
        val kinect = kinects.startDevice()
        kinect.depthCamera.enabled = true
        kinect.depthCamera.mirror = true
        val camera = kinect.depthCamera
        val grayscaleFilter = DepthToGrayscaleMapper()
        val zucconiFilter = DepthToColorsZucconi6Mapper()
        val turboFilter = DepthToColorsTurboMapper()
        val grayscaleBuffer = kinectColorBuffer(camera)
        val zucconiBuffer = kinectColorBuffer(camera)
        val turboBuffer = kinectColorBuffer(camera)
        extend {
            /*
             * Note: getting the latest frame this way will guarantee
             * that filters are being applied only if the actual new frame
             * from kinect was received. Kinect has different refresh rate
             * than usual screen (30 fps).
             */
            kinect.depthCamera.getLatestFrame()?.let { frame ->
                grayscaleFilter.apply(frame, grayscaleBuffer)
                zucconiFilter.apply(frame, zucconiBuffer)
                turboFilter.apply(frame, turboBuffer)
            }
            drawer.image(camera.currentFrame)
            drawer.image(grayscaleBuffer, camera.width.toDouble(), 0.0)
            drawer.image(turboBuffer, 0.0, camera.height.toDouble())
            drawer.image(zucconiBuffer, camera.width.toDouble(), camera.height.toDouble())
        }
    }
}

private fun kinectColorBuffer(camera: KinectCamera): ColorBuffer {
    return colorBuffer(camera.width, camera.height, format = ColorFormat.RGB)
}