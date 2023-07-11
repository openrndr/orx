package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.MouseButton
import org.openrndr.MouseEvents
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
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

    fun setupMouseEvents(mouse: MouseEvents) {
        mouse.buttonDown.listen {
            rotationCenter = it.position
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
    }

    override fun setup(program: Program) {
        setupMouseEvents(program.mouse)
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


