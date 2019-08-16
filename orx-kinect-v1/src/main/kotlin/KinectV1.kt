package org.openrndr.extra.kinect.v1

import mu.KotlinLogging
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libfreenect.*
import org.bytedeco.libfreenect.global.freenect.*
import org.bytedeco.libfreenect.presets.freenect
import org.openrndr.extra.kinect.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

/**
 * Returns support for kinect version 1.
 *
 * @param depthCameraInitializationDelay defaults to 100 ms. Delay seems to be
 *          necessary due to either my misunderstanding or some weird freenect bug.
 *          Without the delay between starting depth camera and registering
 *          depth callback, no frames are transferred at all. However this
 *          problem happens only on the first try with freshly connected
 *          kinect. Subsequent runs of the same program don't require
 *          this delay at all.
 */
fun getKinectsV1(depthCameraInitializationDelay: Long = 100) : Kinects<Freenect> {
    return DefaultKinects(KinectsV1Manager(depthCameraInitializationDelay))
}

/** Provides low level freenect context for calling native freenect methods. */
class Freenect(
        val fnCtx: freenect_context,
        val fnUsbCtx: freenect_usb_context,
        val fnDev: freenect_device? = null // only available for device level commands
)

private class KinectsV1Manager(val depthCameraInitializationDelay: Long) : KinectsManager<Freenect> {

    private val logger = KotlinLogging.logger {}

    private val fnCtx = freenect_context()

    private val fnUsbCtx = freenect_usb_context()

    private val ctx = Freenect(fnCtx, fnUsbCtx)

    private var running = true

    private val devices: LinkedList<FreenectDevice> = LinkedList()

    private val timeout = freenect.timeval()
    init { timeout.tv_sec(1) }

    private val executor = Executors.newSingleThreadExecutor{
        runnable -> Thread(runnable, "Kinect1-runner")
    }

    private inner class KinectV1CommandsExecutor(val context: Freenect) : KinectCommandsExecutor<Freenect> {
        override fun execute(commands: (Freenect) -> Any): Any {
            return executor.submit(Callable {
                logger.debug { "Executing native freenect commands" }
                commands(context)
            }).get()
        }
    }

    private val commandsExecutor = KinectV1CommandsExecutor(ctx)

    override fun initialize() {
        logger.info("Initializing Kinect1 support, set log level to TRACE to see received frames")
        executor.execute {
            logger.debug("Initializing freenect")
            verify(freenect_init(fnCtx, fnUsbCtx))
            freenect_set_log_level(fnCtx, FREENECT_LOG_INFO)
            freenect_select_subdevices(fnCtx, FREENECT_DEVICE_CAMERA)
            val num = verify(freenect_num_devices(fnCtx))
            if (num == 0) {
                logger.warn { "Could not find any Kinect1 device, calling startDevice() will throw exception" }
            }
        }
        executor.execute(object : Runnable {
            override fun run() {
                if (!running) { return }
                val ret = freenect_process_events_timeout(fnCtx, timeout)
                if (ret != 0) {
                    logger.error { "freenect_process_events_timeout returned non-zero value: $ret" }
                }
                executor.execute(this) // loop
            }
        })
    }

    override fun countDevices(): Int {
        return executor.submit(
            Callable { verify(freenect_num_devices(fnCtx)) }
        ).get()
    }

    // FIXME we should prevent from starting the same device multiple times
    override fun startDevice(num: Int): KinectDevice<Freenect> {
        val count = countDevices()
        if (num >= count) {
            throw KinectException(
                    "Trying to start non-existent Kinect1 device, device count: $count, num: $num"
            )
        }
        val device = executor.submit(
            Callable {
                val device = FreenectDevice(num)
                devices.add(device)
                device
            }
        ).get()
        return DefaultKinectDevice(
                DefaultKinectDepthCamera(
                        device.depthCamera.width,
                        device.depthCamera.height,
                        32.0,
                        device.depthCamera.enabler,
                        device.depthCamera.bytesSupplier
                ),
                KinectV1CommandsExecutor(device.devCtx)
        )
    }

    override fun execute(commands: (Freenect) -> Any): Any {
        return commandsExecutor.execute(commands)
    }

    override fun shutdown() {
        logger.info("Shutting down Kinect1 support")
        executor.submit { running = false }.get()
        executor.submit {
            if (!fnCtx.isNull) {
                devices.forEach { device -> device.shutdown() }
                devices.clear()
            }
        }.get() // wait to finish
        executor.shutdown()
        executor.awaitTermination(1100, TimeUnit.MILLISECONDS)
        // value slightly higher than 1sec polling timeout, just in case
    }

    private inner class FreenectDevice(private val num: Int) {
        val depthCamera = FreenectDepthCamera()
        val fnDev = freenect_device()
        val devCtx = Freenect(fnCtx, fnUsbCtx, fnDev)
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
        inner class FreenectDepthCamera {
            val width: Int = 640
            val height: Int = 480
            private val bytes = ByteBuffer.allocateDirect(width * height * 2)
            private val currentBytesRef = AtomicReference<ByteBuffer?>()
            init { bytes.order(ByteOrder.nativeOrder()) }
            private val freenectDepthCb = object : freenect_depth_cb() {
                override fun call(dev: freenect_device?, depth: Pointer?, timestamp: Int) {
                    logger.trace { "depth frame received for Kinect1 device: $num, at: $timestamp" }
                    currentBytesRef.set(bytes)
                }
            }
            val bytesSupplier = Supplier<ByteBuffer?> { currentBytesRef.getAndSet(null) }
            val enabler = object : KinectFeatureEnabler {
                private val atomicEnabled = AtomicBoolean(false)
                private val inProgress = AtomicBoolean(false)
                override var enabled
                    get() = atomicEnabled.get()
                    set(value) {
                        if (atomicEnabled.get() == value) {
                            logger.warn { "Current state requested - doing nothing, Kinect1 device: $num, enabled=$value" }
                            return
                        }
                        if (!inProgress.getAndSet(true)) {
                            if (value) {
                                executor.execute {
                                    try {
                                        start()
                                    } finally {
                                        inProgress.set(false)
                                    }
                                }
                                atomicEnabled.set(true)
                            } else {
                                executor.execute {
                                    try {
                                        stop()
                                    } finally {
                                        inProgress.set(false)
                                    }
                                }
                                atomicEnabled.set(false)
                            }
                        } else {
                            logger.warn { "Operation in progress, Kinect1 device: $num, requested enabled=$value" }
                        }
                    }
            }
            private fun start() {
                logger.info { "Enabling Kinect1 depth camera, device num: $num" }
                verify(freenect_set_depth_mode(
                        fnDev, freenect_find_depth_mode(FREENECT_RESOLUTION_MEDIUM, FREENECT_DEPTH_11BIT))
                )
                verify(freenect_set_depth_buffer(fnDev, Pointer(bytes)))
                verify(freenect_start_depth(fnDev))
                Thread.sleep(depthCameraInitializationDelay) // here is the hack
                freenect_set_depth_callback(fnDev, freenectDepthCb)
            }
            private fun stop() {
                logger.info { "Disabling Kinect1 depth camera, device num: $num" }
                verify(freenect_stop_depth(fnDev))
            }
        }
    }

    private fun verifyOnShutdown(ret: Int) {
        if (ret != 0) {
            logger.error { "Unexpected return value while shutting down Kinect1 support: $ret" }
        }
    }

    private fun verify(ret: Int): Int {
        if (ret < 0) {
            throw KinectException("Kinect1 error: ret=$ret")
        }
        return ret
    }

}
