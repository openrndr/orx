package org.openrndr.extras.camera

import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.lookAt as lookAt_

class OrbitalCamera(eye: Vector3, lookAt: Vector3) {

    // current position in spherical coordinates
    var spherical = Spherical.fromVector(eye)
        private set
    var lookAt = lookAt
        private set

    private var sphericalEnd = Spherical.fromVector(eye)
    private var lookAtEnd = lookAt.copy()
    private var dirty: Boolean = true

    var dampingFactor = 0.05
    var zoomSpeed = 1.0

    fun rotate(rotX: Double, rotY: Double) {
        sphericalEnd += Spherical(0.0, rotX, rotY)
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

    private fun dolly(distance: Double) {
        sphericalEnd += Spherical(distance, 0.0, 0.0)
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

    fun panTo(target : Vector3) {
        lookAtEnd = target
        dirty = true
    }

    fun dollyTo(distance: Double) {
        sphericalEnd = sphericalEnd.copy(radius = distance )
        dirty = true
    }

    fun update(timeDelta: Double) {
        if (!dirty) return
        dirty = false

        val dampingFactor = dampingFactor * timeDelta / 0.0060
        val sphericalDelta = sphericalEnd - spherical
        val lookAtDelta = lookAtEnd - lookAt

        if (
                Math.abs(sphericalEnd.radius) > EPSILON ||
                Math.abs(sphericalEnd.theta) > EPSILON ||
                Math.abs(sphericalEnd.phi) > EPSILON ||
                Math.abs(lookAtDelta.x) > EPSILON ||
                Math.abs(lookAtDelta.y) > EPSILON ||
                Math.abs(lookAtDelta.z) > EPSILON
        ) {

            spherical += (sphericalDelta * dampingFactor)
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
}


