package org.openrndr.extra.gui

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.WindowConfiguration
import org.openrndr.extra.parameters.title
import org.openrndr.math.IntVector2
import org.openrndr.panel.style.StyleSheet
import org.openrndr.panel.style.defaultStyles
import org.openrndr.window

class WindowedGUI(val appearance: GUIAppearance = GUIAppearance(), val defaultStyles: List<StyleSheet> = defaultStyles()) : Extension {
    override var enabled: Boolean = true

    val addedObjects = mutableListOf<Pair<Any, String?>>()
    fun <T : Any> add(objectWithParameters: T, label: String? = objectWithParameters.title()): T {
        addedObjects.add(Pair(objectWithParameters, label))
        return objectWithParameters
    }


    override fun setup(program: Program) {
        program.window(WindowConfiguration(width = 200, height = program.height, position = program.window.position.toInt() - IntVector2(200,0) )) {
            val gui = GUI(appearance, defaultStyles)
            for ((obj, label) in addedObjects) {
                gui.add(obj, label)
            }
            extend(gui)
        }
    }
}