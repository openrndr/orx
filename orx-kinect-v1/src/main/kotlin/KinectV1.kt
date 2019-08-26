package org.openrndr.extra.kinect.v1

import mu.KotlinLogging
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libfreenect.*
import org.bytedeco.libfreenect.global.freenect.*
import org.bytedeco.libfreenect.presets.freenect
import org.openrndr.Program
import org.openrndr.extra.kinect.*
import org.openrndr.extra.kinect.impl.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.concurrent.thread

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
fun getKinectsV1(program: Program, depthCameraInitializationDelay: Long = 100) : Kinects<Freenect> {
    return DefaultKinects(program, KinectsV1Manager(depthCameraInitializationDelay))
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

    private var taskQueue = LinkedBlockingDeque<FutureTask<*>>()

    private var running = true

    private val runner = thread(
            name = "Kinect1-runner",
            start = false,
            isDaemon = true
    ) {
        initializeFreenect()
        while (running) { mainLoop() }
        shutdownFreenect()
    }

    private var expectingEvents = false

    private val devices: LinkedList<FreenectDevice> = LinkedList()

    private val timeout = freenect.timeval()
    init { timeout.tv_sec(1) }

    private inner class KinectV1CommandsExecutor(val context: Freenect): KinectCommandsExecutor<Freenect> {
        override fun <T> execute(commands: (Freenect) -> T): T {
            return callSync {
                logger.trace { "executing native freenect commands" }
                commands(context)
            }
        }
    }

    private val commandsExecutor = KinectV1CommandsExecutor(ctx)

    override fun initialize() {
        logger.info("Initializing Kinect1 support, set log level to TRACE to see received frames")
        runner.start()
    }

    private fun initializeFreenect() {
        logger.debug("initializing freenect")
        verify(freenect_init(fnCtx, fnUsbCtx))
        freenect_set_log_level(fnCtx, FREENECT_LOG_INFO)
        freenect_select_subdevices(fnCtx, FREENECT_DEVICE_CAMERA)
        val num = verify(freenect_num_devices(fnCtx))
        if (num == 0) {
            logger.warn { "Could not find any Kinect1 device, calling startDevice() will throw exception" }
        }
    }

    private fun mainLoop() {
        if (expectingEvents) {
            val ret = freenect_process_events(fnCtx)
            if (ret != 0) { logger.error { "freenect_process_events returned non-zero value: $ret" } }
            val tasks = taskQueue.iterator()
            for (task in tasks) {
                tasks.remove()
                task.run()
            }
        } else {
            taskQueue.poll(100, TimeUnit.MILLISECONDS)?.run()
        }
    }

    private fun shutdownFreenect() {
        logger.debug("shutting down freenect")
        if (!fnCtx.isNull) {
            devices.forEach { device -> device.shutdown() }
            devices.clear()
            verifyOnShutdown(freenect_shutdown(fnCtx))
        }
    }

    override fun countDevices(): Int {
        return callSync { verify(freenect_num_devices(fnCtx)) }
    }

    override fun startDevice(num: Int): KinectDevice<Freenect> {
        callSync {
            devices.find { device -> device.num == num }
        }?.let {
            throw KinectException("Kinect1 device already started, num: $num")
        }
        val count = countDevices()
        if (num >= count) {
            throw KinectException(
                    "Trying to start non-existent Kinect1 device, " +
                            "device count: $count, num: $num (index starts with 0)"
            )
        }
        val device = callSync {
            val device = FreenectDevice(num)
            devices.add(device)
            device
        }
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

    override fun <T> execute(commands: (Freenect) -> T): T {
        return commandsExecutor.execute(commands)
    }

    override fun shutdown() {
        logger.info("Shutting down Kinect1 support")
        callSync { running = false }
        runner.join()
    }

    private inline fun <T> callSync(crossinline block: () -> T): T {
        val task = FutureTask<T>(Callable { block() })
        taskQueue.add(task)
        return task.get()
    }

    private inner class FreenectDevice(val num: Int) {

        val depthCamera = FreenectDepthCamera()

        val fnDev = freenect_device()

        val devCtx = Freenect(fnCtx, fnUsbCtx, fnDev)

        init {
            logger.info { "Opening Kinect1 device num: $num" }
            verify(freenect_open_device(fnCtx, fnDev, num))
        }

        val expectingEvents: Boolean
            get() = depthCamera.expectingEvents // or other device in the future

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
            init { bytes.order(ByteOrder.nativeOrder()) }

            private val currentBytesRef = AtomicReference<ByteBuffer?>()

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

                override var enabled // usually called from rendering thread
                    get() = atomicEnabled.get()
                    set(value) {
                        if (atomicEnabled.get() == value) {
                            logger.warn { "Current state requested - doing nothing, Kinect1 device: $num, enabled=$value" }
                            return
                        }
                        if (!inProgress.getAndSet(true)) {
                            if (value) {
                                callSync {
                                    try {
                                        start()
                                        atomicEnabled.set(true)
                                        updateExpectingEvents()
                                    } finally { inProgress.set(false) }
                                }
                            } else {
                                callSync {
                                    try {
                                        stop()
                                        atomicEnabled.set(false)
                                        updateExpectingEvents()
                                    } finally { inProgress.set(false) }
                                }
                            }
                        } else {
                            logger.warn { "Operation in progress, Kinect1 device: $num, requested enabled=$value" }
                        }
                    }
            }

            val expectingEvents: Boolean
                get() = depthCamera.enabler.enabled

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

    private fun updateExpectingEvents() {
        expectingEvents = devices.any { device -> device.expectingEvents }
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
