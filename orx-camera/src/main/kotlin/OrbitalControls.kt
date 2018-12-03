package org.openrndr.extras.camera

import org.openrndr.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class OrbitalControls(val orbitalCamera: OrbitalCamera) {
    enum class STATE {
        NONE,
        ROTATE,
        PAN,
    }

    private var state = STATE.NONE
    var fov = 90.0

    private lateinit var program: Program
    private lateinit var lastMousePosition: Vector2

    private fun mouseScrolled(event: MouseEvent) {

        if (Math.abs(event.rotation.x) > 0.1) return

        when {
            event.rotation.y > 0 -> orbitalCamera.dollyIn()
            event.rotation.y < 0 -> orbitalCamera.dollyOut()
        }
    }

    private fun mouseMoved(event: MouseEvent) {

        if (state == STATE.NONE) return
        val delta = lastMousePosition - event.position
        lastMousePosition = event.position

        if (state == STATE.PAN) {

            val offset = Vector3.fromSpherical(orbitalCamera.spherical) - orbitalCamera.lookAt

            // half of the fov is center to top of screen
            val targetDistance = offset.length * Math.tan((fov / 2) * Math.PI / 180)
            val panX = (2 * delta.x * targetDistance / program.window.size.x)
            val panY = (2 * delta.y * targetDistance / program.window.size.y)

            orbitalCamera.pan(panX, -panY, 0.0)

        } else {
            val rotX = 2 * Math.PI * delta.x / program.window.size.x
            val rotY = 2 * Math.PI * delta.y / program.window.size.y
            orbitalCamera.rotate(rotX, rotY)
        }

    }

    private fun mouseButtonDown(event: MouseEvent) {
        val previousState = state

        when (event.button) {
            MouseButton.LEFT -> {
                state = STATE.ROTATE
            }
            MouseButton.RIGHT -> {
                state = STATE.PAN
            }
            MouseButton.CENTER -> {
            }
            MouseButton.NONE -> {
            }
        }

        if (previousState == STATE.NONE) {
            lastMousePosition = event.position
        }
    }

    fun keyPressed(keyEvent: KeyEvent) {
        if (keyEvent.key == KEY_ARROW_RIGHT) {
            orbitalCamera.pan(1.0, 0.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_LEFT) {
            orbitalCamera.pan(-1.0, 0.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_UP) {
            orbitalCamera.pan(0.0, 1.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_DOWN) {
            orbitalCamera.pan(0.0, -1.0, 0.0)
        }
    }

    fun setup(program: Program) {
        this.program = program
        program.mouse.moved.listen { mouseMoved(it) }
        program.mouse.buttonDown.listen { mouseButtonDown(it) }
        program.mouse.buttonUp.listen { state = STATE.NONE }
        program.mouse.scrolled.listen { mouseScrolled(it) }
        program.keyboard.keyDown.listen { keyPressed(it) }
        program.keyboard.keyRepeat.listen{ keyPressed(it) }
    }
}
