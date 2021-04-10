package org.openrndr.extra.kinect.impl

import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extra.kinect.KinectDepthCamera
import org.openrndr.extra.kinect.KinectDevice
import org.openrndr.extra.kinect.Kinects
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlin.concurrent.thread

class DefaultKinects<CTX>(
    private val program: Program,
    private val manager: KinectsManager<CTX>
) : Kinects<CTX> {

    private inner class Destroyer : Thread() {
        override fun run() {
            manager.shutdown()
        }
    }

    init {
        manager.initialize()
        // as we don't have explicit shutdown mechanism in OPENRNDR
        // we need to install a shutdown hook for now
        Runtime.getRuntime().addShutdownHook(Destroyer())
    }

    override fun countDevices(): Int {
        return manager.countDevices()
    }

    override fun startDevice(num: Int): KinectDevice<CTX> {
        val device = manager.startDevice(num)
        program.extend(device)
        return device
    }

    override fun <T> execute(commands: (CTX) -> T): T {
        return manager.execute(commands)
    }

}

interface KinectsManager<CTX> {
    fun initialize()
    fun countDevices(): Int
    fun startDevice(num: Int): KinectDevice<CTX>
    fun <T> execute(commands: (CTX) -> T): T
    fun shutdown()
}

interface KinectFeatureEnabler {
    var enabled: Boolean
}

interface KinectCommandsExecutor<CTX> {
    fun <T> execute(commands: (CTX) -> T): T
}

class DefaultKinectDevice<CTX>(
    override val depthCamera: DefaultKinectDepthCamera,
    private val commandsExecutor: KinectCommandsExecutor<CTX>
) : KinectDevice<CTX> {
    override var enabled: Boolean = true
    override fun beforeDraw(drawer: Drawer, program: Program) {
        depthCamera.update()
    }

    override fun <T> execute(commands: (CTX) -> T): T {
        return commandsExecutor.execute(commands)
    }
}

class DefaultKinectDepthCamera(
    override val width: Int,
    override val height: Int,
    depthScale: Double,
    private val enabler: KinectFeatureEnabler,
    private val bytesSupplier: Supplier<ByteBuffer?>
) :
    KinectDepthCamera, UpdatableKinectCamera {

    override var enabled: Boolean
        get() = enabler.enabled
        set(value) {
            enabler.enabled = value
        }

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
        if (enabled) {
            bytesSupplier.get()?.let { bytes ->
                rawBuffer.write(bytes)
                depthMapper.apply(rawBuffer, currentFrame)
                newFrameRef.set(currentFrame)
            }
        }
    }

    override var mirror: Boolean
        get() = depthMapper.mirror
        set(value) {
            depthMapper.mirror = value
        }

}

private class KinectRawDataToDepthMapper :
    Filter(
        filterShaderFromUrl(
            resourceUrl(
                "kinect-raw-to-depth.frag",
                DefaultKinects::class.java
            )
        )
    ) {
    var depthScale: Double by parameters
    var mirror: Boolean by parameters
    var resolution: Vector2 by parameters
}

private interface UpdatableKinectCamera {
    fun update()
}
