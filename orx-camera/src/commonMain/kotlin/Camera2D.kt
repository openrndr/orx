package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.MouseEvents
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.events.Event
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.buildTransform

/**
 * The [Camera2D] extension enables:
 * - **panning** the view by moving the mouse while a mouse button is pressed
 * - **zooming** in and out by using the mouse wheel
 *
 * Usage: `extend(Camera2D())`
 */
class Camera2D : Extension, ChangeEvents {
    override var enabled = true

    var view = Matrix44.IDENTITY

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
        mouse.dragged.listen {
            view = buildTransform { translate(it.dragDisplacement) } * view
            dirty = true
        }
        mouse.scrolled.listen {
            val scaleFactor = 1.0 - it.rotation.y * 0.03
            view = buildTransform {
                translate(it.position)
                scale(scaleFactor)
                translate(-it.position)
            } * view
            dirty = true
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


