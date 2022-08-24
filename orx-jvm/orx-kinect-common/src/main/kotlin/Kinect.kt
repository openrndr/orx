package org.openrndr.extra.kinect

import org.openrndr.draw.*
import org.openrndr.extra.depth.camera.DepthCamera
import org.openrndr.math.IntVector2
import org.openrndr.resourceUrl
import java.lang.RuntimeException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass

/**
 * Represents all the accessible kinects handled by a specific driver (V1, V2, etc.).
 */
interface Kinect {

    /**
     * Lists available kinect devices.
     */
    fun listDevices(): List<Device.Info>

    /**
     * Opens kinect device of a given index.
     *
     * @param index the kinect device index (starts with 0). If no value specified,
     *          it will default to 0.
     * @throws KinectException if device of such an index does not exist,
     *          or it was already started.
     * @see listDevices
     */
    fun openDevice(index: Int = 0): Device

    /**
     * Opens kinect device of a given serial number.
     *
     * @param serialNumber the kinect device serialNumber.
     * @throws KinectException if device of such a serial number does not exist
     *          , or it was already started.
     * @see listDevices
     */
    fun openDevice(serialNumber: String): Device

    /**
     * The list of kinect devices which are already opened and haven't been closed.
     */
    val activeDevices: List<Device>

    /**
     * Represents physical kinect device.
     */
    interface Device {

        /**
         * Provides information about kinect device.
         *
         * Note: in implementation it can be extended with any
         * additional information next to the serial number.
         */
        interface Info {
            val serialNumber: String
        }

        val info: Info

        val depthCamera: KinectDepthCamera

        fun close()

    }

}

/**
 * Generic interface for all the kinect cameras.
 */
interface KinectCamera {

    var enabled: Boolean

}

interface KinectDepthCamera : KinectCamera, DepthCamera {
    /* no special attributes at the moment */
}

open class KinectException(msg: String) : RuntimeException(msg)

fun kinectRawDepthByteBuffer(resolution: IntVector2): ByteBuffer =
    ByteBuffer.allocateDirect(
        resolution.x * resolution.y * 2
    ).also {
        it.order(ByteOrder.nativeOrder())
    }

fun <T : Kinect> KClass<T>.filterFrom(resource: String, flipH: Boolean, flipV: Boolean): Filter {
    val url = resourceUrl(resource, this)
    val preamble =
        (if (flipH) "#define KINECT_FLIPH\n" else "") +
        (if (flipV) "#define KINECT_FLIPV\n" else "")
    return Filter(
        filterShaderFromCode(
            "$preamble\n${URL(url).readText()}",
            "kinect-shader: $url + flipH: $flipH, flipV: $flipV"
        )
    )
}

class KinectDepthMappers<T : Kinect>(resource: String, `class`: KClass<T>) {

    private val flipHFalseVFalse = `class`.filterFrom(resource, flipH = false, flipV = false)
    private val flipHFalseVTrue = `class`.filterFrom(resource, flipH = false, flipV = true)
    private val flipHTrueVFalse = `class`.filterFrom(resource, flipH = true, flipV = false)
    private val flipHTrueVTrue = `class`.filterFrom(resource, flipH = true, flipV = true)

    fun select(flipH: Boolean, flipV: Boolean): Filter =
        if (flipH) {
            if (flipV) flipHTrueVTrue
            else flipHTrueVFalse
        } else {
            if (flipV) flipHFalseVTrue
            else flipHFalseVFalse
        }

    fun update(resolution: IntVector2) {
        val resolutionXMinus1 = resolution.x - 1
        flipHTrueVFalse.parameters["resolutionXMinus1"] = resolutionXMinus1
        flipHTrueVTrue.parameters["resolutionXMinus1"] = resolutionXMinus1
    }

    fun forEach(block: (filter: Filter) -> Unit) {
        block(flipHFalseVFalse)
        block(flipHFalseVTrue)
        block(flipHTrueVFalse)
        block(flipHTrueVTrue)
    }

}

fun depthToRawNormalizedMappers() = KinectDepthMappers("depth-to-raw-normalized.frag", Kinect::class)
