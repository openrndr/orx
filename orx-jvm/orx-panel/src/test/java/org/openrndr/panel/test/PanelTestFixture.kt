package org.openrndr.panel.test

import org.openrndr.KeyEvent
import org.openrndr.KeyEventType
import org.openrndr.KeyModifier
import org.openrndr.MouseButton
import org.openrndr.MouseEvent
import org.openrndr.MouseEventType
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.math.Vector2
import org.openrndr.panel.ControlManager
import org.openrndr.panel.ControlManagerBuilder
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.style
import org.openrndr.panel.style.height
import org.openrndr.panel.style.px
import org.openrndr.panel.style.width

/**
 * A program with a window, fonts, keys and a clipboard, for tests of elements that lay out text, take keys or use the
 * clipboard. Every test class that extends it runs in its own JVM.
 */
abstract class PanelTestFixture : AbstractApplicationTestFixture() {
    /** a control manager of [width] by [height] pixels that [build] adds style sheets and a layout to, laid out */
    fun panel(width: Double = 640.0, height: Double = 480.0, build: ControlManagerBuilder.() -> Unit): ControlManager {
        val cm = program.controlManager(builder = build)
        cm.setup(program)
        cm.body?.style {
            this.width = width.px
            this.height = height.px
        }
        relayout(cm)
        return cm
    }

    /** computes the styles and the layout of [cm] again */
    fun relayout(cm: ControlManager) {
        cm.document?.layout()
    }

    /** presses and releases [key] with [modifiers], for the element with the keyboard focus */
    fun ControlManager.key(key: Int, vararg modifiers: KeyModifier, name: String = "") {
        keyboardInput.press(KeyEvent(KeyEventType.KEY_DOWN, key, name, modifiers.toSet()))
    }

    /** a press of [button] at [position] */
    fun ControlManager.press(position: Vector2, button: MouseButton = MouseButton.LEFT, vararg modifiers: KeyModifier) {
        mouseInput.press(MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_DOWN, button, modifiers.toSet()))
    }

    /** a release of [button] at [position], a click when it is where the press was */
    fun ControlManager.release(position: Vector2, button: MouseButton = MouseButton.LEFT) {
        mouseInput.click(MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_UP, button, emptySet()))
    }

    /** a drag of [button] to [position] */
    fun ControlManager.drag(position: Vector2, button: MouseButton = MouseButton.LEFT) {
        mouseInput.drag(MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.DRAGGED, button, emptySet()))
    }

    /** a move of the pointer to [position] */
    fun ControlManager.move(position: Vector2) {
        mouseInput.move(MouseEvent(position, Vector2.ZERO, Vector2.ZERO, MouseEventType.MOVED, MouseButton.NONE, emptySet()))
    }
}
