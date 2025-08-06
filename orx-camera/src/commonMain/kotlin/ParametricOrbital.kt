@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.Vector3Parameter
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3

/**
 * Extension that provides orbital camera through annotated parameters
 */
@Description("Orbital camera")
class ParametricOrbital : Extension, ChangeEvents {
    override var enabled: Boolean = true

    override val changed = Event<Unit>()
    override val hasChanged: Boolean
        get() = dirty
    private var dirty = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
            }
            field = value
        }
    @DoubleParameter("fov", 1.0, 90.0, order = 0)
    var fov = 45.0
        set(value) {
            if (field != value) {
                dirty = true
            }
            field = value
            camera.zoomTo(fov)
        }

    val camera by lazy {
        OrbitalCamera(Spherical(theta, phi, radius).cartesian, center, fov, 0.1, 1000.0, projectionType).apply {
            dampingFactor = 0.0
        }
    }

    @DoubleParameter("phi", 0.0, 180.0, order = 2)
    var phi = 0.0
        set(value) {
            if (field != value) {
                dirty = true
            }
            field = value
            camera.rotateTo(theta, phi.coerceAtLeast(1E-3))
        }

    @DoubleParameter("theta", -180.0, 180.0, order = 1)
    var theta = 0.0
        set(value) {
            if (field != value) {
                dirty = true
            }
            field = value
            camera.rotateTo(theta, phi.coerceAtLeast(1E-3))
        }


    @DoubleParameter("orbit radius", 0.1, 100.0, order = 3)
    var radius = 10.0
        set(value) {
            if (field != value) {
                dirty = true
            }
            field = value
            camera.dollyTo(radius)
        }


    @Vector3Parameter("center", order = 4)
    var center = Vector3.ZERO
        set(value) {
            if (field != value) {
                dirty = true
            }
            field = value
            camera.panTo(value)
        }


    var projectionType = ProjectionType.PERSPECTIVE


    override fun setup(program: Program) {
        camera.setup(program)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        camera.beforeDraw(drawer, program)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        dirty = false
        camera.afterDraw(drawer, program)
    }
}