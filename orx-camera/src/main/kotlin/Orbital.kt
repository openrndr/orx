package org.openrndr.extras.camera

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.math.Vector3

/**
 * Extension that provides orbital camera view and controls.
 */
class Orbital : Extension {
    override var enabled: Boolean = true

    var eye = Vector3.UNIT_Z * 10.0
    var lookAt = Vector3.ZERO
    var near = 0.1
    var far = 1000.0
    var fov = 90.0
    var userInteraction = true
    var keySpeed = 1.0
    var projectionType = ProjectionType.PERSPECTIVE

    val camera by lazy { println("creating camera $projectionType"); OrbitalCamera(eye, lookAt, fov, near, far, projectionType) }
    val controls by lazy { OrbitalControls(camera, userInteraction, keySpeed) }

    override fun setup(program: Program) {
        controls.setup(program)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        camera.beforeDraw(drawer, program)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        camera.afterDraw(drawer, program)
    }
}