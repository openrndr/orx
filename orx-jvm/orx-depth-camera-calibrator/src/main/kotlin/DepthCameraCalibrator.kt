package org.openrndr.extra.depth.camera.calibrator

import org.openrndr.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.isolated
import org.openrndr.extra.depth.camera.DepthCamera
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.fx.colormap.TurboColormap
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

/**
 * Depth camera calibrator extension.
 *
 * @param program the program using this extension, Note: normally
 *          we would pass program in [setup], however there is a
 *          cyclic dependency between GUI and calibrator, so some
 *          dimensions have to be established before setup.
 *          See Kinect1Demo10DepthCameraCalibration.kt.
 * @param depthCameras depth cameras to calibrate.
 */
class DepthCameraCalibrator(
    private val program: Program,
    vararg depthCameras: DepthCamera
) : Extension {

    init {
        check(depthCameras.isNotEmpty()) {
            "depthCameras cannot be empty"
        }
        depthCameras.forEach {
            check(it.depthMeasurement == DepthMeasurement.METERS) {
                "depthCameras: calibration requires depthMeasurement of each camera to be set to METERS"
            }
        }
    }

    override var enabled: Boolean
        get() = commonParameters.calibratorView
        set(value) { commonParameters.calibratorView = value }

    private val resolution = IntVector2(program.width, program.height).vector2

    private val calibrations = depthCameras.map { Calibration(it) }.toList()

    private val colormap = TurboColormap()

    private var onCalibrationChange: (calibration: Calibration) -> Unit =
        { _ -> } // empty on startup

    override fun setup(program: Program) {
        program.keyboard.keyDown.listen {
            if (enabled) {
                handleKeyDown(it)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        calibrations.forEach {
            colormap.minValue = it.minDepth
            colormap.maxValue = it.maxDepth
            colormap.apply(it.camera.currentFrame, it.colorBuffer)
            drawer.isolatedWithCalibration(it) {
                image(
                    colorBuffer = it.colorBuffer,
                    position = it.position,
                    width = it.width,
                    height = it.height
                )
            }
        }
    }

    fun handleKeyDown(event: KeyEvent) {
        when(event.name) {
            "1" -> commonParameters.allMinDepth -= CENTIMETER
            "2" -> commonParameters.allMinDepth += CENTIMETER
            "3" -> commonParameters.allMaxDepth -= CENTIMETER
            "4" -> commonParameters.allMaxDepth += CENTIMETER
        }
        calibrations
            .filter { it.tuneWithKeyboard }
            .forEach {
                when(event.key) {
                    KEY_ARROW_LEFT -> it.offset += Direction.LEFT * OFFSET_CHANGE_SCALE
                    KEY_ARROW_RIGHT -> it.offset += Direction.RIGHT * OFFSET_CHANGE_SCALE
                    KEY_ARROW_UP -> it.offset += Direction.UP * OFFSET_CHANGE_SCALE
                    KEY_ARROW_DOWN -> it.offset += Direction.DOWN * OFFSET_CHANGE_SCALE
                }
                when(event.name) {
                    "-" -> it.scale -= SCALE_CHANGE
                    "=" -> it.scale += SCALE_CHANGE
                    "l" -> it.rotation -= ROTATION_CHANGE
                    "r" -> it.rotation += ROTATION_CHANGE
                    "a" -> it.minDepth -= CENTIMETER
                    "s" -> it.minDepth += CENTIMETER
                    "d" -> it.maxDepth -= CENTIMETER
                    "f" -> it.maxDepth += CENTIMETER
                }
            }
    }

    fun addControlsTo(gui: GUI) {
        gui.add(commonParameters)
        calibrations.forEachIndexed { index, calibration ->
            gui.add(calibration, label = "depth camera $index")
        }
    }

    fun getCalibration(camera: DepthCamera): Calibration = calibrations
        .find { it.camera === camera }
        ?: throw IllegalArgumentException("No calibration for provided depth camera")

    fun onCalibrationChange(block: (calibration: Calibration) -> Unit) {
        onCalibrationChange = block
        calibrations.forEach { // run on first install
            block(it)
        }
    }

    private val commonParameters = @Description("calibration: all depth cameras") object {

        @BooleanParameter(label = "calibrator view [k]", order = 0)
        var calibratorView: Boolean = false

        @DoubleParameter(label = "min depth [1/2]", low = 0.2, high = 10.0, order = 1)
        var allMinDepth: Double = 0.1
            set(value) {
                field = value
                calibrations.forEach {
                    it.minDepth = value
                }
            }

        @DoubleParameter(label = "max depth [3/4]", low = 0.2, high = 10.0, order = 2)
        var allMaxDepth: Double = 10.0
            set(value) {
                field = value
                calibrations.forEach {
                    it.maxDepth = value
                }
            }

    }

    @Suppress("unused") // used by reflection
    inner class Calibration(
        val camera: DepthCamera,
        val colorBuffer: ColorBuffer = colorBuffer(
            camera.resolution.x,
            camera.resolution.y
        )
    ) {

        @BooleanParameter(label = "tune with keyboard", order = 0)
        var tuneWithKeyboard: Boolean = true

        @BooleanParameter(label = "flipH", order = 1)
        var flipH
            get() = camera.flipH
            set(value) { camera.flipH = value }

        @BooleanParameter(label = "flipV", order = 2)
        var flipV
            get() = camera.flipV
            set(value) { camera.flipV = value }

        @XYParameter(
            label = "offset [arrows]",
            minX = -1.0,
            minY = -1.0,
            maxX = 1.0,
            maxY = 1.0,
            order = 3,
            invertY = true
        )
        var offset: Vector2 = Vector2.ZERO
            set(value) {
                field = value
                onCalibrationChange(this)
            }

        @DoubleParameter(label = "rotation [l/r]", low = -360.0, high = 360.0, order = 4)
        var rotation: Double = 0.0
            set(value) {
                field = value
                onCalibrationChange(this)
            }

        @DoubleParameter(label = "scale [+/-]", low = 0.0, high = 10.0, order = 5)
        var scale: Double = 1.0
            set(value) {
                field = value
                onCalibrationChange(this)
            }

        @DoubleParameter(label = "min depth [a/s]", low = 0.0, high = 10.0, order = 6)
        var minDepth: Double = 0.2
            set(value) {
                field = value
                onCalibrationChange(this)
            }

        @DoubleParameter(label = "max depth [d/f]", low = 0.0, high = 10.0, order = 7)
        var maxDepth: Double = 10.0
            set(value) {
                field = value
                onCalibrationChange(this)
            }

        @ActionParameter(label = "reset", order = 8)
        fun reset() {
            offset = Vector2.ZERO
            rotation = 0.0
            scale = 1.0
            minDepth = 0.2
            maxDepth = 10.0
        }

        val width: Double =
            camera.resolution.x * resolution.y /
                    camera.resolution.y

        val height: Double = resolution.y

        val position: Vector2 =
            -(resolution - Vector2(resolution.x - width, 0.0)) / 2.0

    }

}

fun Drawer.isolatedWithCalibration(
    calibration: DepthCameraCalibrator.Calibration,
    block: Drawer.() -> Unit
) {
    this.isolated {
        translate(
            IntVector2(width, height).vector2 / 2.0
                    + calibration.offset * Vector2(1.0, -1.0) * height.toDouble()
        )
        rotate(calibration.rotation)
        scale(calibration.scale)
        block()
    }
}

enum class Direction(val vector: Vector2) {
    LEFT(Vector2(-1.0, 0.0)),
    RIGHT(Vector2(1.0, 0.0)),
    UP(Vector2(0.0, 1.0)),
    DOWN(Vector2(0.0, -1.0));

    operator fun times(scale: Double): Vector2 = this.vector * scale

}

private const val CENTIMETER = .01
private const val OFFSET_CHANGE_SCALE = .001
private const val ROTATION_CHANGE = .1
private const val SCALE_CHANGE = .001

