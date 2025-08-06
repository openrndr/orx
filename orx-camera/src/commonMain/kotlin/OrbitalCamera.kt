package org.openrndr.extra.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.DepthTestPass
import org.openrndr.draw.Drawer
import org.openrndr.events.Event
import org.openrndr.math.Matrix44
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import org.openrndr.math.transforms.lookAt as lookAt_

enum class ProjectionType {
    PERSPECTIVE,
    ORTHOGONAL
}


class OrbitalCamera(
    eye: Vector3 = Vector3.ZERO,
    lookAt: Vector3 = Vector3.UNIT_Z,
    var fov: Double = 90.0,
    var near: Double = 0.1,
    var far: Double = 1000.0,
    var projectionType: ProjectionType = ProjectionType.PERSPECTIVE
) : Extension, ChangeEvents {

    internal lateinit var program: Program
    override val changed = Event<Unit>()

    override val hasChanged: Boolean
        get() = dirty


    // current position in spherical coordinates
    var spherical = Spherical.fromVector(eye)
        private set
    var lookAt = lookAt
        private set

    var depthTest = true

    var magnitude = 100.0
    var magnitudeEnd = magnitude

    private var sphericalEnd = Spherical.fromVector(eye)
    private var lookAtEnd = lookAt
    private var dirty: Boolean = true
        set(value) {
            if (value && !field) {
                changed.trigger(Unit)
            }
            field = value
        }
    private var lastSeconds: Double = -1.0

    var fovEnd = fov

    var dampingFactor = 0.05
    var zoomSpeed = 1.0

    var orthoNear = -1000.0
    var orthoFar = 1000.0

    /**
     * Sets the view for the orbital camera by updating the look-at position, spherical coordinates,
     * and field of view (FOV). This method initializes both the target and current states of these properties.
     *
     * @param lookAt the target position the camera should look at, represented as a 3D vector
     * @param spherical the spherical coordinates defining the camera's orientation
     * @param fov the field of view (in degrees) for the camera
     */
    fun setView(lookAt: Vector3, spherical: Spherical, fov: Double) {
        this.lookAt = lookAt
        this.lookAtEnd = lookAt
        this.spherical = spherical
        this.sphericalEnd = spherical
        this.fov = fov
        this.fovEnd = fov
    }


    /**
     * Rotates the orbital camera by the specified angles in the horizontal and vertical directions.
     * The rotation can be applied instantly or smoothly interpolated over time.
     *
     * @param degreesX the rotation angle in degrees around the horizontal axis (theta)
     * @param degreesY the rotation angle in degrees around the vertical axis (phi)
     * @param instant whether the rotation is applied immediately; if false, it interpolates over time (default is false)
     */
    fun rotate(degreesX: Double, degreesY: Double, instant: Boolean = false) {
        sphericalEnd += Spherical(degreesX, degreesY, 0.0)
        sphericalEnd = sphericalEnd.makeSafe()
        if (instant) {
            spherical = sphericalEnd
        }
        dirty = true
    }

    /**
     * Rotates the camera to the specified spherical angles. The rotation can occur instantly or
     * smoothly over time based on the `instant` parameter.
     *
     * @param degreesX the target horizontal rotation angle (theta) in degrees
     * @param degreesY the target vertical rotation angle (phi) in degrees
     * @param instant whether the rotation should be applied immediately (default is `false`)
     */
    fun rotateTo(degreesX: Double, degreesY: Double, instant: Boolean = false) {
        sphericalEnd = sphericalEnd.copy(theta = degreesX, phi = degreesY)
        sphericalEnd = sphericalEnd.makeSafe()

        if (instant) {
            spherical = sphericalEnd
        }
        dirty = true
    }

    /**
     * Rotates the orbital camera to the specified position defined by the `eye` vector.
     * The rotation can either occur instantly or smoothly interpolated over time,
     * depending on the `instant` parameter.
     *
     * @param eye the target position to rotate the camera to, represented as a 3D vector
     * @param instant whether the rotation should be applied immediately (default is `false`)
     */
    fun rotateTo(eye: Vector3, instant: Boolean = false) {
        sphericalEnd = Spherical.fromVector(eye)
        sphericalEnd = sphericalEnd.makeSafe()
        if (instant) {
            spherical = sphericalEnd
        }
        dirty = true
    }

    /**
     * Zooms the camera in by decreasing the distance to the target. The zoom is based on
     * an exponential scale factor determined by the `zoomSpeed` field. If the `instant`
     * parameter is set to `true`, the zoom effect is applied immediately; otherwise, it
     * will interpolate the change over time.
     *
     * @param instant whether the zoom-in effect should occur instantly (default is `false`)
     */
    fun dollyIn(instant: Boolean = false) {
        val zoomScale = pow(0.95, zoomSpeed)
        dolly(sphericalEnd.radius * zoomScale - sphericalEnd.radius, instant)
    }

    /**
     * Zooms the camera out by increasing the distance to the target. The zoom operation
     * is based on an exponential scale factor determined by the `zoomSpeed` field.
     *
     * @param instant whether the zoom-out effect should occur instantly (default is `false`)
     */
    fun dollyOut(instant: Boolean = false) {
        val zoomScale = pow(0.95, zoomSpeed)
        dolly(sphericalEnd.radius / zoomScale - sphericalEnd.radius, instant)
    }

    /**
     * Adjusts the camera's distance from the target by the specified amount.
     * The change in distance is applied immediately if `instant` is set to `true`,
     * otherwise it will be interpolated over time with smoothing.
     *
     * @param distance the amount to adjust the camera's distance by
     * @param instant whether the adjustment should be applied immediately (default is `false`)
     */
    fun dolly(distance: Double, instant: Boolean = false) {
        sphericalEnd += Spherical(0.0, 0.0, distance)
        if (instant) {
            spherical = sphericalEnd
        }
        dirty = true
    }

    fun pan(x: Double, y: Double, z: Double, instant: Boolean = false) {
        val view = viewMatrix()
        val xColumn = Vector3(view.c0r0, view.c1r0, view.c2r0) * x
        val yColumn = Vector3(view.c0r1, view.c1r1, view.c2r1) * y
        val zColumn = Vector3(view.c0r2, view.c1r2, view.c2r2) * z
        lookAtEnd += xColumn + yColumn + zColumn
        if (instant) {
            lookAt = lookAtEnd
        }
        dirty = true
    }


    /**
     * Smoothly pans the camera to a specified target position. If the `instant` parameter is set
     * to `true`, the panning occurs immediately; otherwise, it will be interpolated over time.
     *
     * @param target the target position to pan the camera to, represented as a 3D vector
     * @param instant whether the panning should occur instantly (default is `false`)
     */
    fun panTo(target: Vector3, instant: Boolean = false) {
        lookAtEnd = target
        if (instant) {
            lookAt = lookAtEnd
        }
        dirty = true
    }

    /**
     * Adjusts the camera's distance (radius) to the specified value. If the `instant` parameter
     * is set to true, the distance change is applied immediately; otherwise, it will be interpolated
     * over time during updates.
     *
     * @param distance the target distance (radius) that the camera should move to
     * @param instant whether the distance adjustment should occur instantly (default is `false`)
     */
    fun dollyTo(distance: Double, instant: Boolean = false) {
        sphericalEnd = sphericalEnd.copy(radius = distance)
        if (instant) {
            spherical = sphericalEnd
        }
        dirty = true
    }

    /**
     * Adjusts the magnitude of the orbital camera by the specified scale factor.
     * If the `instant` parameter is set to true, the adjustment is applied immediately;
     * otherwise, it will be interpolated over time during updates.
     *
     * @param scale the amount by which to adjust the camera's magnitude
     * @param instant whether the scale adjustment should be applied instantly (default is `false`)
     */
    fun scale(scale: Double, instant: Boolean = false) {
        magnitudeEnd += scale
        if (instant) {
            magnitude = magnitudeEnd
        }
        dirty = true
    }

    /**
     * Adjusts the camera's scaling factor to the specified value. The scaling can either
     * be applied instantly or interpolated over time during updates.
     *
     * @param scale the target scaling factor for the camera
     * @param instant whether the scaling should be applied instantly (default is `false`)
     */

    fun scaleTo(scale: Double, instant: Boolean = false) {
        magnitudeEnd = scale
        if (instant) {
            magnitude = magnitudeEnd
        }
        dirty = true
    }

    /**
     * Adjusts the camera's field of view (FOV) by the specified number of degrees. The transition can either
     * happen instantly or be interpolated over time during updates.
     *
     * @param degrees the number of degrees to adjust the field of view by
     * @param instant whether the adjustment should occur instantly (default is `false`)
     */
    fun zoom(degrees: Double, instant: Boolean = false) {
        fovEnd += degrees
        if (instant) {
            fov = fovEnd
        }
        dirty = true
    }

    /**
     * Adjusts the camera's field of view (FOV) to the specified number of degrees. If the `instant`
     * parameter is set to `true`, the FOV immediately transitions to the specified value; otherwise,
     * it will be interpolated over time during updates.
     *
     * @param degrees the target field of view (in degrees) for the camera
     * @param instant whether the transition to the target FOV should occur instantly (default is `false`)
     */
    fun zoomTo(degrees: Double, instant: Boolean = false) {
        fovEnd = degrees
        if (instant) {
            fov = fovEnd
        }
        dirty = true
    }

    /**
     * Updates the orbital camera state by iteratively applying updates to the camera's parameters
     * based on a fixed time step. The method ensures smooth interpolation of the camera properties
     * (e.g., position, orientation) over a specified time delta.
     *
     * @param timeDelta the time elapsed for which the camera state should be updated, in seconds
     */
    fun update(timeDelta: Double) {
        if (!dirty) return
        dirty = false

        val stepSize = 1.0/60.0
        val steps = max(timeDelta/stepSize, 1.0).toInt()
        for (step in 0 until steps) {
            updateStep(stepSize)
        }
    }

    /**
     * Updates the camera position, orientation, and view properties such as spherical coordinates,
     * look-at point, field of view, and magnitude based on damping factors and time delta.
     *
     * @param timeDelta the time step used to update the interpolation of camera parameters
     */
    fun updateStep(timeDelta: Double) {

        val dampingFactor = if (dampingFactor > 0.0) {
            dampingFactor * timeDelta / 0.0060
        } else 1.0
        val sphericalDelta = sphericalEnd - spherical
        val lookAtDelta = lookAtEnd - lookAt
        val fovDelta = fovEnd - fov
        val magnitudeDelta = magnitudeEnd - magnitude
        if (
            abs(sphericalDelta.radius) > EPSILON ||
            abs(sphericalDelta.theta) > EPSILON ||
            abs(sphericalDelta.phi) > EPSILON ||
            abs(lookAtDelta.x) > EPSILON ||
            abs(lookAtDelta.y) > EPSILON ||
            abs(lookAtDelta.z) > EPSILON ||
            abs(fovDelta) > EPSILON
        ) {
            fov += (fovDelta * dampingFactor)
            spherical += (sphericalDelta * dampingFactor)
            spherical = spherical.makeSafe()
            lookAt += (lookAtDelta * dampingFactor)
            magnitude += (magnitudeDelta * dampingFactor)
            dirty = true
        } else {
            magnitude = magnitudeEnd
            spherical = sphericalEnd.copy()
            lookAt = lookAtEnd.copy()
            fov = fovEnd
        }
        spherical = spherical.makeSafe()
    }

    /**
     * Computes and returns the view matrix for the orbital camera. The view matrix is
     * calculated using the current spherical coordinates, look-at position, and the up vector (Vector3.UNIT_Y).
     *
     * @return a 4x4 matrix representing the current view transformation of the camera
     */
    fun viewMatrix(): Matrix44 {
        return lookAt_(Vector3.fromSpherical(spherical) + lookAt, lookAt, Vector3.UNIT_Y)
    }

    companion object {
        private const val EPSILON = 0.000001
    }

    // EXTENSION
    override var enabled: Boolean = true

    override fun setup(program: Program) {
        this.program = program
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        drawer.pushTransforms()
        applyTo(drawer)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.popTransforms()
    }

    /**
     * Enables the perspective camera. Use this faster method instead of .isolated()
     * if you don't need to revert back to the orthographic projection.
     */
    fun OrbitalCamera.applyTo(drawer: Drawer) {

        if (lastSeconds == -1.0) lastSeconds = program.seconds

        val delta = program.seconds - lastSeconds
        lastSeconds = program.seconds

        update(delta)

        if (projectionType == ProjectionType.PERSPECTIVE) {
            drawer.perspective(fov, drawer.width.toDouble() / drawer.height, near, far)
        } else {
            val ar = drawer.width * 1.0 / drawer.height
            drawer.ortho(-ar * magnitude, ar * magnitude, -1.0 * magnitude, 1.0 * magnitude, orthoNear, orthoFar)
        }
        drawer.view = viewMatrix()

        if (depthTest) {
            drawer.drawStyle.depthWrite = true
            drawer.drawStyle.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        }
    }

}

/**
 * Temporarily enables this camera, calls function to draw using
 * that camera, then disables it by popping the last matrix changes.
 * It makes it easy to combine perspective and orthographic projections
 * in the same program.
 * @param function the function that is called in the isolation
 */
fun OrbitalCamera.isolated(drawer: Drawer, function: Drawer.() -> Unit) {
    drawer.pushTransforms()
    drawer.pushStyle()

    applyTo(drawer)
    function(drawer)

    drawer.popStyle()
    drawer.popTransforms()
}


private fun pow(a: Double, x: Double): Double = a.pow(x)


/**
 * Creates an instance of the Orbital extension, sets it up with the calling Program,
 * and returns the configured instance.
 *
 * @return a configured Orbital instance ready for use with the calling Program.
 */
fun Program.OrbitalManual(): Orbital {
    val orbital = Orbital()
    orbital.setup(this)
    return orbital
}