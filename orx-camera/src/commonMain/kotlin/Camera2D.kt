package org.openrndr.extra.camera

import org.openrndr.Extension
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

/**
 * The [Camera2D] extension enables panning, rotating and zooming the view
 * with the mouse:
 * - left click and drag to **pan**
 * - right click and drag to **rotate**
 * - use the mouse wheel to **zoom** in and out
 *
 * Usage: `extend(Camera2D())`
 */
class Camera2D : Extension, ChangeEvents {
    override var enabled = true

    private lateinit var program: Program
    private var controlInitialized = false
    var view = Matrix44.IDENTITY
    var rotationCenter = Vector2.ZERO

    override val changed = Event<Unit>()

    private var dirty = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
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
    fun isolated(function: Drawer.() -> Unit) {
        program.drawer.isolated {
            program.drawer.ortho(RenderTarget.active)

            program.drawer.view = this@Camera2D.view
            program.drawer.function()
        }
    }

    /**
     * Configures the mouse interaction events for controlling the camera view and handling
     * transformations such as translation, rotation, and scaling via mouse inputs.
     *
     * @param mouse the MouseEvents instance that provides mouse interaction data, including
     *              button presses, dragging, and scrolling events.
     */
    fun setupMouseEvents(mouse: MouseEvents) {
        mouse.buttonDown.listen {
            rotationCenter = it.position

            if (it.button == MouseButton.CENTER) {
                view = Matrix44.IDENTITY
                dirty = true
            }
        }
        mouse.dragged.listen {
            if (!it.propagationCancelled) {
                when (it.button) {
                    MouseButton.LEFT -> view = buildTransform {
                        translate(it.dragDisplacement)
                    } * view

                    MouseButton.RIGHT -> view = buildTransform {
                        translate(rotationCenter)
                        rotate(it.dragDisplacement.x + it.dragDisplacement.y)
                        translate(-rotationCenter)
                    } * view

                    else -> Unit
                }
                dirty = true
            }
        }
        mouse.scrolled.listen {
            if (!it.propagationCancelled) {
                val scaleFactor = 1.0 - it.rotation.y * 0.03
                view = buildTransform {
                    translate(it.position)
                    scale(scaleFactor)
                    translate(-it.position)
                } * view
                dirty = true
            }
        }
        controlInitialized = true
    }

    override fun setup(program: Program) {
        this.program = program
        if (!controlInitialized) {
            setupMouseEvents(program.mouse)
        }
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
 * Creates a new instance of the Camera2D extension suitable for manual application.
 *
 * @return a configured Camera2D instance ready to be used with the calling Program.
 */
fun Program.Camera2DManual(): Camera2D {
    val camera = Camera2D()
    camera.setup(this)
    return camera
}
