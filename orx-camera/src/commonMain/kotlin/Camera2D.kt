package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.KeyEvents
import org.openrndr.MouseButton
import org.openrndr.MouseEvents
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolated
import org.openrndr.events.Event
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * The [Camera2D] extension enables panning, rotating, and zooming the view
 * with the mouse:
 * - left click and drag to **pan**
 * - right click and drag to **rotate**
 * - use the mouse wheel to **zoom** in and out
 *
 * Usage: `extend(Camera2D())`
 */
class Camera2D : Extension, ChangeEvents {
    override var enabled = true

    var userInteraction = true

    private lateinit var program: Program
    private var controlInitialized = false

    /**
     * Represents the 4x4 transformation matrix of the camera view in a 2D drawing environment.
     * This matrix is used to apply custom transformations such as translation, rotation,
     * or scaling to the viewport. By default, it is set to the identity matrix.
     *
     * When modified, the `dirty` flag is automatically set to `true` to indicate
     * that the view matrix has been updated and subsequent transformations might
     * need recalculation or application.
     */
    var view = Matrix44.IDENTITY
        set(value) {
            field = value
            dirty = true
        }

    /**
     * Represents the center of rotation for the camera in 2D space.
     *
     * Changes to this property will mark the camera's state as dirty, necessitating
     * a re-calculation of the view transformation.
     *
     * Default value is [Vector2.ZERO].
     */
    var rotationCenter = Vector2.ZERO
        set(value) {
            field = value
            dirty = true
        }

    override val changed = Event<Unit>()

    private var dirty = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
                program.window.requestDraw()
            }
            field = value
        }
    override val hasChanged: Boolean
        get() = dirty


    /**
     * Executes the provided drawing function in an isolated scope, preserving the current
     * drawing state and then restoring it after the function is executed. The `ortho` projection
     * and custom view transformation are applied during the isolated drawing session.
     *
     * @param function the drawing function to be applied within the isolated scope of the `Drawer`.
     */
    @OptIn(ExperimentalContracts::class)
    fun isolated(function: Drawer.() -> Unit) {
        contract {
            callsInPlace(function, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
        program.drawer.isolated {
            program.drawer.ortho(RenderTarget.active)

            program.drawer.view = this@Camera2D.view
            program.drawer.function()
        }
    }

    /**
     * Reinitialize the camera to its default state, where no transformations
     * (such as rotation, translation, or scaling) are applied.
     */
    var defaults = {
        view = Matrix44.IDENTITY
        rotationCenter = Vector2.ZERO
    }

    /**
     * Applies a panning transformation to the camera view. The method modifies the current view
     * by translating it based on the provided displacement vector, effectively shifting the
     * camera's view in the scene.
     *
     * @param displacement the vector by which the camera view is translated.
     */
    fun pan(displacement: Vector2) {
        view = buildTransform {
            translate(displacement)
        } * view
    }

    /**
     * Rotates the camera view by a specified angle around its rotation center.
     *
     * @param angle the angle in degrees by which the view is rotated.
     */
    fun rotate(angle: Double) {
        view = buildTransform {
            translate(rotationCenter)
            rotate(angle)
            translate(-rotationCenter)
        } * view
    }

    /**
     * Applies a zoom transformation to the camera view. The transformation is centered
     * around the specified point while adjusting the zoom level by the given factor.
     *
     * @param center The point in space around which the zoom transformation is centered.
     * @param factor The zoom factor, where values greater than 1.0 zoom in and values less than 1.0 zoom out.
     */
    fun zoom(center: Vector2, factor: Double) {
        view = buildTransform {
            translate(center)
            scale(factor, factor, 1.0)
            translate(-center)
        } * view
    }

    /**
     * Sets up and applies mouse and keyboard controls for interacting with the camera.
     * This variable provides event-driven logic to handle user input for panning, rotation, and zooming.
     *
     * - Mouse button interactions are used to configure the center of rotation and reset the view.
     * - Mouse drag events control panning and rotation with the left and right mouse buttons respectively.
     * - Mouse scrolling adjusts the zoom level based on the scroll direction and position.
     *
     * @param mouse an instance of `MouseEvents` providing data for mouse interactions,
     *              such as button presses, movement, and scrolling.
     * @param keyboard an instance of `KeyEvents` providing the framework for handling keyboard inputs,
     *                 though currently unused in this implementation.
     */
    var controls = { mouse: MouseEvents, keyboard: KeyEvents ->
        mouse.buttonDown.listen {
            if(userInteraction) {
                rotationCenter = it.position
                if (it.button == MouseButton.CENTER) {
                    defaults()
                }
            }
        }
        mouse.dragged.listen {
            if (!it.propagationCancelled && userInteraction) {
                when (it.button) {
                    MouseButton.LEFT -> pan(it.dragDisplacement)
                    MouseButton.RIGHT -> rotate(it.dragDisplacement.x + it.dragDisplacement.y)
                    else -> Unit
                }
            }
        }
        mouse.scrolled.listen {
            if (!it.propagationCancelled && userInteraction) {
                val scaleFactor = 1.0 - it.rotation.y * 0.03
                zoom(it.position, scaleFactor)
            }
        }
        Unit
    }

    /**
     * Configures the mouse interaction events for controlling the camera view and handling
     * transformations such as translation, rotation, and scaling via mouse inputs.
     *
     * @param mouse the MouseEvents instance that provides mouse interaction data, including
     *              button presses, dragging, and scrolling events.
     */
    fun setupControls(mouse: MouseEvents, keyboard: KeyEvents) {
        if (!controlInitialized) {
            controls(mouse, keyboard)
        }
        controlInitialized = true
    }

    override fun setup(program: Program) {
        this.program = program
        if (!controlInitialized) {
            setupControls(program.mouse, program.keyboard)
        }
        defaults()
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        drawer.ortho(RenderTarget.active)
        drawer.view = view
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        dirty = false
        drawer.popTransforms()
    }
}

/**
 * Creates and sets up a custom-configured Camera2D instance within a Program.
 *
 * This function initializes a new Camera2D, applies the provided configuration block,
 * and sets it up with the current Program context for interactive 2D transformations
 * such as panning, rotating, and zooming.
 *
 * @param configure an optional configuration block where you can set up the Camera2D
 *                  instance (e.g., setting view or rotation center). The default is an empty block.
 * @return the configured Camera2D instance.
 */
fun Program.Camera2DManual(configure: Camera2D.() -> Unit = { }): Camera2D {
    val camera = Camera2D()
    camera.configure()
    camera.setup(this)
    return camera
}
