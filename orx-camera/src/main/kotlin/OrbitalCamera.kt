package org.openrndr.extras.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.DepthTestPass
import org.openrndr.draw.Drawer
import org.openrndr.math.Matrix44
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import kotlin.math.abs
import org.openrndr.math.transforms.lookAt as lookAt_

class OrbitalCamera(eye: Vector3 = Vector3.ZERO, lookAt: Vector3 = Vector3.UNIT_Z, var fov: Double = 90.0, var near: Double = 0.1, var far: Double = 1000.0) : Extension {
    // current position in spherical coordinates
    var spherical = Spherical.fromVector(eye)
        private set
    var lookAt = lookAt
        private set

    var depthTest = true

    private var sphericalEnd = Spherical.fromVector(eye)
    private var lookAtEnd = lookAt
    private var dirty: Boolean = true
    private var lastSeconds: Double = -1.0

    var fovEnd = fov

    var dampingFactor = 0.05
    var zoomSpeed = 1.0

    fun setView(lookAt: Vector3, spherical: Spherical, fov: Double) {
        this.lookAt = lookAt
        this.lookAtEnd = lookAt
        this.spherical = spherical
        this.sphericalEnd = spherical
        this.fov = fov
        this.fovEnd = fov
    }

    fun rotate(rotX: Double, rotY: Double) {
        sphericalEnd += Spherical(rotX, rotY, 0.0)
        sphericalEnd = sphericalEnd.makeSafe()
        dirty = true
    }

    fun rotateTo(rotX: Double, rotY: Double) {
        sphericalEnd = sphericalEnd.copy(theta = rotX, phi = rotY)
        sphericalEnd = sphericalEnd.makeSafe()
        dirty = true
    }

    fun rotateTo(eye: Vector3) {
        sphericalEnd = Spherical.fromVector(eye)
        sphericalEnd = sphericalEnd.makeSafe()
        dirty = true
    }

    fun dollyIn() {
        val zoomScale = Math.pow(0.95, zoomSpeed)
        dolly(sphericalEnd.radius * zoomScale - sphericalEnd.radius)
    }

    fun dollyOut() {
        val zoomScale = Math.pow(0.95, zoomSpeed)
        dolly(sphericalEnd.radius / zoomScale - sphericalEnd.radius)
    }

    fun dolly(distance: Double) {
        sphericalEnd += Spherical(0.0, 0.0, distance)
        dirty = true
    }

    fun pan(x: Double, y: Double, z: Double) {
        val view = viewMatrix()
        val xColumn = Vector3(view.c0r0, view.c1r0, view.c2r0) * x
        val yColumn = Vector3(view.c0r1, view.c1r1, view.c2r1) * y
        val zColumn = Vector3(view.c0r2, view.c1r2, view.c2r2) * z
        lookAtEnd += xColumn + yColumn + zColumn
        dirty = true
    }

    fun panTo(target: Vector3) {
        lookAtEnd = target
        dirty = true
    }

    fun dollyTo(distance: Double) {
        sphericalEnd = sphericalEnd.copy(radius = distance)
        dirty = true
    }

    fun zoom(degrees: Double) {
        fovEnd += degrees
        dirty = true
    }

    fun zoomTo(degrees: Double) {
        fovEnd = degrees
        dirty = true
    }

    fun update(timeDelta: Double) {
        if (!dirty) return
        dirty = false

        val dampingFactor = dampingFactor * timeDelta / 0.0060
        val sphericalDelta = sphericalEnd - spherical
        val lookAtDelta = lookAtEnd - lookAt
        val fovDelta = fovEnd - fov
        if (
                abs(sphericalEnd.radius) > EPSILON ||
                abs(sphericalEnd.theta) > EPSILON ||
                abs(sphericalEnd.phi) > EPSILON ||
                abs(lookAtDelta.x) > EPSILON ||
                abs(lookAtDelta.y) > EPSILON ||
                abs(lookAtDelta.z) > EPSILON ||
                abs(fovDelta) > EPSILON
        ) {

            fov += (fovDelta * dampingFactor)
            spherical += (sphericalDelta * dampingFactor)
            spherical = spherical.makeSafe()
            lookAt += (lookAtDelta * dampingFactor)
            dirty = true

        } else {
            spherical = sphericalEnd.copy()
            lookAt = lookAtEnd.copy()
        }
        spherical = spherical.makeSafe()
    }

    fun viewMatrix(): Matrix44 {
        return lookAt_(Vector3.fromSpherical(spherical) + lookAt, lookAt, Vector3.UNIT_Y)
    }

    companion object {
        private const val EPSILON = 0.000001
    }

    // EXTENSION
    override var enabled: Boolean = true

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (lastSeconds == -1.0) lastSeconds = program.seconds

        val delta = program.seconds - lastSeconds
        lastSeconds = program.seconds

        update(delta)

        drawer.perspective(fov, program.window.size.x / program.window.size.y, near, far)
        drawer.view = viewMatrix()

        if (depthTest) {
            drawer.drawStyle.depthWrite = true
            drawer.drawStyle.depthTestPass = DepthTestPass.LESS_OR_EQUAL
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.view = Matrix44.IDENTITY
        drawer.ortho()
    }
}


