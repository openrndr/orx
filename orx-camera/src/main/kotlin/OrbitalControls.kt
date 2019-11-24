package org.openrndr.extras.camera

import org.openrndr.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.tan

class OrbitalControls(val orbitalCamera: OrbitalCamera , val userInteraction: Boolean = true, val keySpeed: Double = 1.0) : Extension {
    enum class STATE {
        NONE,
        ROTATE,
        PAN,
    }

    private var state = STATE.NONE
    var fov = orbitalCamera.fov

    private lateinit var program: Program
    private lateinit var lastMousePosition: Vector2

    private fun mouseScrolled(event: MouseEvent) {
        if (abs(event.rotation.x) > 0.1) return

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
            val targetDistance = offset.length * tan((fov / 2) * PI / 180)
            val panX = (2 * delta.x * targetDistance / program.window.size.x)
            val panY = (2 * delta.y * targetDistance / program.window.size.y)

            orbitalCamera.pan(panX, -panY, 0.0)

        } else {
            val rotX = 2 * PI * delta.x / program.window.size.x
            val rotY = 2 * PI * delta.y / program.window.size.y
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
            orbitalCamera.pan(keySpeed, 0.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_LEFT) {
            orbitalCamera.pan(-keySpeed, 0.0, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_UP) {
            orbitalCamera.pan(0.0, keySpeed, 0.0)
        }
        if (keyEvent.key == KEY_ARROW_DOWN) {
            orbitalCamera.pan(0.0, -keySpeed, 0.0)
        }

        if (keyEvent.name == "q") {
            orbitalCamera.pan(0.0, -keySpeed, 0.0)
        }
        if (keyEvent.name == "e") {
            orbitalCamera.pan(0.0, keySpeed, 0.0)
        }
        if (keyEvent.name == "w") {
            orbitalCamera.pan(0.0, 0.0, -keySpeed)
        }
        if (keyEvent.name == "s") {
            orbitalCamera.pan(0.0, 0.0, keySpeed)
        }
        if (keyEvent.name == "a") {
            orbitalCamera.pan(-keySpeed, 0.0, 0.0)
        }
        if (keyEvent.name == "d") {
            orbitalCamera.pan(keySpeed, 0.0, 0.0)
        }

        if (keyEvent.key == KEY_PAGE_UP) {
            orbitalCamera.zoom(keySpeed)
        }
        if (keyEvent.key == KEY_PAGE_DOWN) {
            orbitalCamera.zoom(-keySpeed)
        }
    }

    // EXTENSION
    override var enabled: Boolean = true

    override fun setup(program: Program) {
        this.program = program

        if (userInteraction) {
            program.mouse.moved.listen { mouseMoved(it) }
            program.mouse.buttonDown.listen { mouseButtonDown(it) }
            program.mouse.buttonUp.listen { state = STATE.NONE }
            program.mouse.scrolled.listen { mouseScrolled(it) }
            program.keyboard.keyDown.listen { keyPressed(it) }
            program.keyboard.keyRepeat.listen{ keyPressed(it) }
        }
    }
}
