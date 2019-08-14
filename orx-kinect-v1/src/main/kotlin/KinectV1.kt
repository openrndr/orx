package org.openrndr.extra.kinect

import mu.KotlinLogging
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libfreenect.freenect_context
import org.bytedeco.libfreenect.freenect_depth_cb
import org.bytedeco.libfreenect.freenect_device
import org.bytedeco.libfreenect.freenect_usb_context
import org.bytedeco.libfreenect.global.freenect.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.concurrent.thread

fun getKinectsV1() : Kinects {
    return DefaultKinects(KinectsV1Manager())
}

class KinectsV1Manager : KinectsManager {

    private val logger = KotlinLogging.logger {}

    private val fnCtx = freenect_context()

    private val fnUsbCtx = freenect_usb_context()

    private var running: Boolean = true

    private val devices: LinkedList<KinectV1Device> = LinkedList()

    private val poller: Thread = thread(name = "Kinect1-poll", start = false, isDaemon = true) {
        while (running && freenect_process_events(fnCtx) >= 0) {}
    }

    inner class KinectV1Device(private val num: Int) : KinectDevice() {
        override val depthCamera = KinectV1DepthCamera()
        val fnDev = freenect_device()
        init {
            logger.info { "Opening Kinect1 device num: $num" }
            verify(freenect_open_device(fnCtx, fnDev, num))
        }
        fun shutdown() {
            logger.info { "Shutting down Kinect1 device num: $num" }
            if (!fnDev.isNull) {
                verifyOnShutdown(freenect_stop_depth(fnDev))
                verifyOnShutdown(freenect_close_device(fnDev))
            }
        }
        inner class KinectV1DepthCamera : AbstractKinectDepthCamera(
                width = 640,
                height = 480,
                depthScale = 32.0
        ) {
            private val logger = KotlinLogging.logger {}
            private val bytes = ByteBuffer.allocateDirect(width * height * 2)
            private val freenectDepthCb = object : freenect_depth_cb() {
                override fun call(dev: freenect_device?, depth: Pointer?, timestamp: Int) {
                    logger.trace { "depth frame received for Kinect1 device: $num, at: $timestamp" }
                    byteBufferRef.set(bytes)
                }
            }
            init {
                bytes.order(ByteOrder.nativeOrder())
            }
            override var enabled: Boolean = false
                set(value) {
                    field = if (value) {
                        start()
                        true
                    } else {
                        stop()
                        false
                    }
                }
            private fun start() {
                logger.info { "Enabling Kinect1 depth camera, device num: $num" }
                verify(freenect_set_depth_mode(
                        fnDev, freenect_find_depth_mode(FREENECT_RESOLUTION_MEDIUM, FREENECT_DEPTH_11BIT))
                )
                verify(freenect_set_depth_buffer(fnDev, Pointer(depthCamera.bytes)))
                freenect_set_depth_callback(fnDev, depthCamera.freenectDepthCb)
                verify(freenect_start_depth(fnDev))
            }
            private fun stop() {
                logger.info { "Disabling Kinect1 depth camera, device num: $num" }
                verify(freenect_stop_depth(fnDev))
            }
        }
    }

    override fun init() {
        logger.info("Initializing Kinect1 support, set log level to TRACE to see received frames")
        verify(freenect_init(fnCtx, fnUsbCtx))
        freenect_set_log_level(fnCtx, FREENECT_LOG_DEBUG)
        freenect_select_subdevices(fnCtx, FREENECT_DEVICE_CAMERA)
        val num = verify(freenect_num_devices(fnCtx))
        if (num == 0) {
            logger.warn { "Could not find any Kinect1 device, calling startDevice() will throw exception" }
        }
        logger.debug("Initializing Kinect1 poller thread")
        poller.start()
        // it seems that we have to wait a bit until kinect is actually initialized
        Thread.sleep(100)
    }

    override fun countDevices(): Int {
        return verify(freenect_num_devices(fnCtx))
    }

    override fun startDevice(num: Int): KinectDevice {
        val count = countDevices()
        if (num >= count) {
            throw KinectException("Non-existent Kinect1 device, num: $num")
        }
        val device = KinectV1Device(num)
        devices.add(device)
        return device
    }

    override fun shutdown() {
        logger.info("Shutting down Kinect1 support")
        running = false
        poller.join()
        devices.forEach {
            device -> device.shutdown()
        }
        if (!fnCtx.isNull) {
            verifyOnShutdown(freenect_shutdown(fnCtx))
        }
    }

    private fun verifyOnShutdown(ret: Int) {
        if (ret != 0) {
            logger.error("Unexpected return value while shutting down kinect support: {}", ret)
        }
    }

    private fun verify(ret: Int): Int {
        if (ret < 0) {
            fail("ret=$ret")
        }
        return ret
    }

    private fun fail(message: String) {
        throw KinectException("Kinect1 error: $message")
    }

}
