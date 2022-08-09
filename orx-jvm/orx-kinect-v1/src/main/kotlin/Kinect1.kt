package org.openrndr.extra.kinect.v1

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging
import org.bytedeco.javacpp.Pointer
import org.bytedeco.libfreenect.*
import org.bytedeco.libfreenect.global.freenect.*
import org.bytedeco.libfreenect.presets.freenect
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.kinect.*
import org.openrndr.launch
import org.openrndr.math.IntVector2
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class Kinect1Exception(msg: String) : KinectException(msg)

class Kinect1 : Kinect, Extension {

    override var enabled: Boolean = true

    class DeviceInfo(
        override val serialNumber: String,
    ) : Kinect.Device.Info {
        override fun toString(): String {
            return "Kinect1[$serialNumber]"
        }
    }

    /**
     * Log level for native freenect logging.
     *
     * Note: logs will appear on standard out for performance reasons.
     *
     * @param code the code of corresponding freenect log level.
     * @see Kinect1.logLevel
     */
    @Suppress("unused")
    enum class LogLevel(val code: Int) {

        /** Crashing/non-recoverable errors. */
        FATAL(FREENECT_LOG_FATAL),

        /** Major errors. */
        ERROR(FREENECT_LOG_ERROR),

        /** Warning messages. */
        WARNING(FREENECT_LOG_WARNING),

        /** Important messages. */
        NOTICE(FREENECT_LOG_NOTICE),

        /** Log for normal messages. */
        INFO(FREENECT_LOG_INFO),

        /** Log for useful development messages. */
        DEBUG(FREENECT_LOG_DEBUG),

        /** Log for slightly less useful messages. */
        SPEW(FREENECT_LOG_SPEW),

        /** Log EVERYTHING. May slow performance. */
        FLOOD(FREENECT_LOG_FLOOD);

    }

    /**
     * Kinect native log level, defaults to `INFO`.
     */
    var logLevel: LogLevel
        get() = freenect.logLevel
        set(value) { freenect.logLevel = value }

    private val logger = KotlinLogging.logger {}

    private lateinit var program: Program

    private lateinit var freenect: Freenect


    override fun setup(program: Program) {
        if (!enabled) { return }
        logger.info("Starting Kinect1 support")
        this.program = program
        freenect = Freenect(initialLogLevel = LogLevel.INFO)
    }

    override fun listDevices(): List<DeviceInfo> = freenect.callBlocking(
        "listDevices"
    ) { _, _ ->
        freenect.listDevices()
    }

    override fun openDevice(index: Int): V1Device {
        val result = freenect.callBlocking("openDeviceByIndex") { ctx, _ ->
            val devices = freenect.listDevices()
            if (devices.isEmpty()) {
                throw KinectException("No kinect devices detected, cannot open any")
            } else if (index >= devices.size) {
                throw KinectException("Invalid device index, number of kinect1 devices: ${devices.size}")
            }
            Pair(
                openFreenectDevice(
                    ctx,
                    devices[index].serialNumber
                ),
                devices[index]
            )
        }
        val device = V1Device(result.first, result.second)
        mutableActiveDevices.add(device)
        return device
    }

    override fun openDevice(serialNumber: String): V1Device {
        val dev = freenect.callBlocking("openDeviceBySerial") { ctx, _ ->
            openFreenectDevice(ctx, serialNumber)
        }
        val device = V1Device(dev, DeviceInfo(serialNumber))
        mutableActiveDevices.add(device)
        return device
    }

    private val mutableActiveDevices = LinkedList<V1Device>()

    override val activeDevices: List<Kinect.Device>
        get() = mutableActiveDevices

    private fun openFreenectDevice(
        ctx: freenect_context,
        serialNumber: String,
    ): freenect_device {
        val dev = freenect_device()
        freenect.checkReturn(
            freenect_open_device_by_camera_serial(ctx, dev, serialNumber)
        )
        return dev
    }

    override fun shutdown(program: Program) {
        if (!enabled) { return }
        logger.info { "Shutting down Kinect1 support" }
        logger.debug("Closing active devices, count: ${mutableActiveDevices.size}")
        mutableActiveDevices.forEach {
            it.close()
        }
        mutableActiveDevices.clear()
        freenect.close()
    }

    @Suppress("unused")
    fun executeInFreenectContext(
        name: String,
        block: (ctx: freenect_context, usbCtx: freenect_usb_context) -> Unit
    ) {
        freenect.call(name) { ctx, usbCtx ->
            block(ctx, usbCtx)
        }
    }

    fun <T> executeInFreenectContextBlocking(
        name: String,
        block: (ctx: freenect_context, usbCtx: freenect_usb_context) -> T
    ): T = freenect.callBlocking(name) { ctx, usbCtx ->
        block(ctx, usbCtx)
    }

    inner class V1Device(
        private val dev: freenect_device,
        override val info: DeviceInfo
    ) : Kinect.Device {

        inner class V1DepthCamera(
            override val resolution: IntVector2,
        ) : KinectDepthCamera {

            private val enabledState = AtomicBoolean(false)

            private var bytesFront = kinectRawDepthByteBuffer(resolution)
            private var bytesBack = kinectRawDepthByteBuffer(resolution)
            private val bytesFlow = MutableStateFlow(bytesBack) // the first frame will come from bytesFront

            private val rawBuffer = colorBuffer(
                resolution.x,
                resolution.y,
                format = ColorFormat.R,
                type = ColorType.UINT16_INT
            ).also {
                it.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
            }

            private val processedFrameBuffer = colorBuffer(
                resolution.x,
                resolution.y,
                format = ColorFormat.R,
                type = ColorType.FLOAT16 // in the future we might want to choose the precision here
            ).also {
                it.filter(MinifyingFilter.LINEAR, MagnifyingFilter.LINEAR)
            }

            private var mutableCurrentFrame = processedFrameBuffer

            private val depthMappers = Kinect1DepthMappers().apply {
                update(resolution)
            }

            private val mutableFrameFlow = MutableSharedFlow<ColorBuffer>()

            override val currentFrame get() = mutableCurrentFrame

            override val frameFlow: Flow<ColorBuffer> = mutableFrameFlow

            private val frameEmitterJob: Job = program.launch {
                bytesFlow.collect { bytes ->
                    rawBuffer.write(bytes)
                    depthMappers.mapper?.apply(rawBuffer, processedFrameBuffer)
                    mutableFrameFlow.emit(mutableCurrentFrame)
                }
            }

            private val freenectDepthCallback = object : freenect_depth_cb() {
                override fun call(
                    dev: freenect_device,
                    depth: Pointer,
                    timestamp: Int
                ) {
                    bytesFlow.tryEmit(bytesFront)
                    val bytesTmp = bytesBack
                    bytesBack = bytesFront
                    bytesFront = bytesTmp
                    freenect.checkReturn(
                        freenect_set_depth_buffer(dev, Pointer(bytesFront))
                    )
                }
            }

            override var enabled: Boolean
                get() = enabledState.get()
                set(value) {
                    freenect.call("$info.enabled = $value") { _, _ ->
                        freenect.expectingEvents = value
                        if (enabledState.get() != value) {
                            if (value) start() else stop()
                            enabledState.set(value)
                        }
                    }
                }

            override var depthMeasurement: DepthMeasurement
                get() = depthMappers.depthMeasurement
                set(value) {
                    logger.debug { "$info.depthMeasurement = $value" }
                    depthMappers.depthMeasurement = value
                    mutableCurrentFrame =
                        if (value == DepthMeasurement.RAW) rawBuffer
                        else processedFrameBuffer
                }

            override var flipH: Boolean
                get() = depthMappers.flipH
                set(value) {
                    logger.debug { "$info.flipH = $value" }
                    depthMappers.flipH = value
                }

            override var flipV: Boolean
                get() = depthMappers.flipV
                set(value) {
                    logger.debug { "$info.flipV = $value" }
                    depthMappers.flipV = value
                }

            private fun start() {
                logger.info { "$info.start()" }
                freenect_set_depth_callback(dev, freenectDepthCallback)
                freenect.checkReturn(freenect_set_depth_mode(
                    dev, freenect_find_depth_mode(FREENECT_RESOLUTION_MEDIUM, FREENECT_DEPTH_11BIT))
                )
                freenect.checkReturn(freenect_set_depth_buffer(dev, Pointer(bytesFront)))
                freenect.checkReturn(freenect_start_depth(dev))
            }

            private fun stop() {
                logger.info { "$info.stop()" }
                freenect.checkReturn(freenect_stop_depth(dev))
            }

            internal fun close() {
                frameEmitterJob.cancel()
            }

        }

        override val depthCamera: V1DepthCamera = V1DepthCamera(
            resolution = KINECT1_DEPTH_RESOLUTION
        )

        fun executeInFreenectDeviceContext(
            name: String,
            block: (ctx: freenect_context, usbCtx: freenect_usb_context, dev: freenect_device) -> Unit
        ) {
            freenect.call("$info: $name") { ctx, usbCtx ->
                block(ctx, usbCtx, dev)
            }
        }

        @Suppress("unused")
        fun <T> executeInFreenectDeviceContextBlocking(
            name: String,
            block: (ctx: freenect_context, usbCtx: freenect_usb_context, dev: freenect_device) -> T
        ): T = freenect.callBlocking("$info: $name") { ctx, usbCtx ->
            block(ctx, usbCtx, dev)
        }

        override fun close() {
            logger.info { "$info.close()" }
            depthCamera.enabled = false
            freenect.callBlocking("$info.closeDevice") { _, _ ->
                freenect.checkReturn(freenect_close_device(dev))
                mutableActiveDevices.remove(this)
            }
            depthCamera.close()
        }

    }

}

/**
 * This class provides a low level API for accessing a kinect1 device.
 * All the operations are executed in a single thread responsible for calling
 * freenect API.
 *
 * @param initialLogLevel the log level to use when freenect is initialized.
 */
class Freenect(initialLogLevel: Kinect1.LogLevel) {

    private var currentLogLevel = initialLogLevel

    private val logger = KotlinLogging.logger {}

    var logLevel: Kinect1.LogLevel
        get() = currentLogLevel
        set(value) {
            call("logLevel[$value]") { ctx, _ ->
                freenect_set_log_level(ctx, value.code)
            }
            currentLogLevel = value
        }

    var expectingEvents: Boolean = false

    private val ctx = freenect_context()

    private val usbCtx = freenect_usb_context()

    private var running: Boolean = true

    private val runner = thread(name = "kinect1", start = true, isDaemon = true) {
        logger.info("Starting Kinect1 thread")
        checkReturn(freenect_init(ctx, usbCtx))
        val num = checkReturn(freenect_num_devices(ctx))
        if (num == 0) {
            logger.warn { "Could not find any Kinect1 devices, calling openDevice() will throw exception" }
        } else {
            val devices = listDevices()
            logger.info { "Kinect1 detected, device count: ${devices.size}" }
            devices.forEachIndexed { index, info ->
                logger.info { "  |-[$index]: ${info.serialNumber}" }
            }
        }

        val timeout = freenect.timeval()
        timeout.tv_sec(1)
        while (running) {
            if (expectingEvents) {
                val ret = freenect_process_events(ctx)
                if (ret != 0) {
                    logger.error { "freenect_process_events returned non-zero value: $ret" }
                }
                val tasks = freenectCallQueue.iterator()
                for (task in tasks) {
                    tasks.remove()
                    task.run()
                }
            } else {
                freenectCallQueue.pollFirst()?.run()
            }
        }

        checkReturn(freenect_shutdown(ctx))
    }

    private val freenectCallQueue = LinkedBlockingDeque<FutureTask<*>>()

    fun call(
        name: String,
        block: (
            ctx: freenect_context,
            usbCtx: freenect_usb_context
        ) -> Unit
    ) {
        logger.debug { "'$name' requested (non-blocking)" }
        val task = FutureTask {
            logger.trace { "'$name': started" }
            try {
                block(ctx, usbCtx)
                logger.trace { "'$name': ended" }
            } catch (e: Exception) {
                logger.error("'$name': failed", e)
            }
        }
        freenectCallQueue.add(task)
    }

    fun <T> callBlocking(
        name: String,
        block: (
            ctx: freenect_context,
            usbCtx: freenect_usb_context
        ) -> T
    ): T {
        logger.debug { "'$name' requested (blocking)" }
        val task = FutureTask {
            logger.trace { "'$name': started" }
            try {
                val result = block(ctx, usbCtx)
                logger.trace { "'$name': ended" }
                Result.success(result)
            } catch (e: Exception) {
                logger.error("'$name': failed", e)
                Result.failure(e)
            }
        }
        freenectCallQueue.add(task)
        val result = task.get()
        logger.trace { "'$name': returned result" }
        return result.getOrThrow()
    }

    fun listDevices() : List<Kinect1.DeviceInfo> {
        val attributes = freenect_device_attributes()
        freenect_list_device_attributes(ctx, attributes)
        try {
            val devices = buildList {
                var item: freenect_device_attributes? =
                    if (attributes.isNull) null
                    else attributes
                while (item != null) {
                    val serialNumber = item.camera_serial().string
                    add(Kinect1.DeviceInfo(serialNumber))
                    item = item.next()
                }
            }
            return devices
        } finally {
            if (!attributes.isNull) {
                freenect_free_device_attributes(attributes)
            }
        }
    }

    fun close() {
        logger.debug("Closing Kinect1 runner")
        running = false
        logger.debug("Waiting for runner thread to finish")
        runner.join()
    }

    fun checkReturn(ret: Int): Int =
        if (ret >= 0) ret
        else {
            throw Kinect1Exception("Freenect error: ret=$ret")
        }

}

internal const val KINECT1_MAX_DEPTH_VALUE: Double = 2047.0

internal val KINECT1_DEPTH_RESOLUTION: IntVector2 = IntVector2(640, 480)

internal class Kinect1DepthMappers {

    private var depthMeasurementState: DepthMeasurement = DepthMeasurement.RAW_NORMALIZED
    private var flipHState: Boolean = false
    private var flipVState: Boolean = false

    private val depthToRawNormalized = depthToRawNormalizedMappers().apply {
        forEach {
            it.parameters["maxDepthValue"] = KINECT1_MAX_DEPTH_VALUE
        }
    }

    private val depthToMeters = KinectDepthMappers(
        "kinect1-depth-to-meters.frag",
        Kinect1::class
    )

    var depthMeasurement: DepthMeasurement
        get() = depthMeasurementState
        set(value) {
            depthMeasurementState = value
            selectMapper()
        }

    var flipH: Boolean
        get() = flipHState
        set(value) {
            flipHState = value
            selectMapper()
        }

    var flipV: Boolean
        get() = flipVState
        set(value) {
            flipVState = value
            selectMapper()
        }

    var mapperState: Filter? = depthToRawNormalized.select(
        flipH = false,
        flipV = false
    )
    val mapper: Filter? get() = mapperState

    fun update(resolution: IntVector2) {
        depthToRawNormalized.update(resolution)
        depthToMeters.update(resolution)
    }

    private fun selectMapper() {
        mapperState = when (depthMeasurementState) {
            DepthMeasurement.RAW -> null
            DepthMeasurement.RAW_NORMALIZED -> {
                depthToRawNormalized.select(flipHState, flipVState)
            }
            DepthMeasurement.METERS -> {
                depthToMeters.select(flipHState, flipVState)
            }
        }
    }

}
