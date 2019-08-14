package org.openrndr.extra.kinect

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

interface Kinects {
    fun countDevices(): Int

    /**
     * Starts kinect device of a given number.
     *
     * @throws KinectException if device of such a number does not exist.
     */
    fun startDevice(num: Int = 0): KinectDevice
}

class DefaultKinects(private val manager: KinectsManager) : Kinects {
    init {
        manager.init()
        // as we don't have explicit shutdown mechanism in OPENRNDR
        // we need to install a shutdown hook for now
        Runtime.getRuntime().addShutdownHook(
            thread(
                name = "${manager.javaClass.simpleName}-closer",
                start = false,
                isDaemon = false
            ) {
                manager.shutdown()
            }
        )
    }
    override fun countDevices(): Int {
        return manager.countDevices()
    }
    override fun startDevice(num: Int): KinectDevice {
        return manager.startDevice(num)
    }
}

interface KinectsManager {
    fun init()
    fun countDevices(): Int
    fun startDevice(num: Int): KinectDevice
    fun shutdown()
}

abstract class KinectDevice : Extension {
    override var enabled: Boolean = true
    abstract val depthCamera: KinectDepthCamera
    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (depthCamera.enabled) {
            depthCamera.update()
        }
    }
}

interface KinectCamera {
    var enabled: Boolean
    val width: Int
    val height: Int
    var mirror: Boolean
    val currentFrame: ColorBuffer
    fun getLatestFrame(): ColorBuffer?
    fun update()
}

interface KinectDepthCamera : KinectCamera {
    val depthScale: Double
}

abstract class AbstractKinectDepthCamera(
    final override val width: Int,
    final override val height: Int,
    final override val depthScale: Double
) : KinectDepthCamera {
    protected val byteBufferRef: AtomicReference<ByteBuffer> = AtomicReference()
    private val rawBuffer: ColorBuffer = colorBuffer(
        width,
        height,
        format = ColorFormat.R,
        type = ColorType.UINT16 // it would be perfect if we could use isampler in the shader
    )
    override val currentFrame: ColorBuffer = colorBuffer(
        width,
        height,
        format = ColorFormat.R,
        type = ColorType.FLOAT16 // in the future we might want to choose the precision here
    )
    private val depthMapper = KinectRawDataToDepthMapper()
    init {
        depthMapper.depthScale = depthScale
        depthMapper.mirror = false
        depthMapper.resolution = Vector2(width.toDouble(), height.toDouble())
    }
    private val newFrameRef = AtomicReference<ColorBuffer>()
    override fun getLatestFrame(): ColorBuffer? {
        return newFrameRef.getAndSet(null)
    }
    override fun update() {
        byteBufferRef.getAndSet(null)?.let { bytes ->
            rawBuffer.write(bytes)
            depthMapper.apply(rawBuffer, currentFrame)
            newFrameRef.set(currentFrame)
        }
    }
    override var mirror: Boolean
        get() = depthMapper.mirror
        set(value) { depthMapper.mirror = value }
}

class KinectRawDataToDepthMapper :
        Filter(filterShaderFromUrl(resourceUrl("kinect-raw-to-depth.frag", Kinects::class.java))) {
    var depthScale: Double by parameters
    var mirror: Boolean by parameters
    var resolution: Vector2 by parameters
}

class KinectException(msg: String) : RuntimeException(msg)
