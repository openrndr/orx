package org.openrndr.extra.gui

import org.openrndr.*
import org.openrndr.extra.parameters.title
import org.openrndr.internal.Driver
import org.openrndr.math.IntVector2
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.defaultStyles

private val childWindows = mutableMapOf<Long, ApplicationWindow>()

class WindowedGUI(
    val appearance: GUIAppearance = GUIAppearance(),
    val defaultStyles: List<StyleSheet> = defaultStyles(),
    val windowClosable: Boolean = false,
    val windowAlwaysOntop: Boolean = false,
) : Extension {
    override var enabled: Boolean = true
    val gui: GUI = GUI(appearance, defaultStyles)

    private val addedObjects = mutableListOf<Pair<Any, String?>>()

    fun <T : Any> add(objectWithParameters: T, label: String? = objectWithParameters.title()): T {
        addedObjects.add(Pair(objectWithParameters, label))
        return objectWithParameters
    }

    override fun setup(program: Program) {
        val window = childWindows[Driver.instance.contextID]
        if (window != null) {
            window.program.mouse.exited.listeners.clear()
            window.program.mouse.entered.listeners.clear()
            window.program.mouse.buttonUp.listeners.clear()
            window.program.mouse.buttonDown.listeners.clear()
            window.program.mouse.dragged.listeners.clear()
            window.program.mouse.scrolled.listeners.clear()
            window.program.mouse.moved.listeners.clear()
            window.program.keyboard.keyUp.listeners.clear()
            window.program.keyboard.keyDown.listeners.clear()
            window.program.keyboard.keyRepeat.listeners.clear()
            window.program.keyboard.character.listeners.clear()
            window.program.extensions.clear()
            window.program.produceAssets.listeners.clear()
            window.program.requestAssets.listeners.clear()
        }

        val cw = childWindows.getOrPut(Driver.instance.contextID) {
            program.window(
                WindowConfiguration(
                    closable = windowClosable,
                    alwaysOnTop = windowAlwaysOntop,
                    width = appearance.barWidth,
                    height = program.height,
                    position = program.window.position.toInt() - IntVector2(200, 0)
                )
            ) {
                //
            }
        }

        // launch because at this stage Driver.instance.contextID points to the context of the parent window
        cw.program.launch {
            for ((obj, label) in addedObjects) {
                gui.add(obj, label)
            }
            cw.program.extend(gui)
            program.produceAssets.listen {
                cw.program.produceAssets.trigger(it)
            }
        }
    }
}

@JvmName("addToWindowedGui")
fun <T : Any> T.addTo(gui: WindowedGUI, label: String? = this.title()): T {
    gui.add(this, label)
    return this
}
