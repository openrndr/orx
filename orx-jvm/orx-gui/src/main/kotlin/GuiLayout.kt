package org.openrndr.extra.gui

import org.openrndr.panel.elements.Body
import org.openrndr.panel.elements.Canvas
import org.openrndr.panel.elements.Div
import org.openrndr.panel.elements.DropdownButton
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.div

/**
 * Builds the element tree of a [GUI]. The layout arranges the building blocks offered by [GUILayoutScope] inside
 * [GUILayoutScope.body], with containers created using the regular `div {}` builder. A layout must call
 * [GUILayoutScope.sidebar] exactly once, as the sidebar holds the compartments of the GUI.
 *
 * ```
 * gui.layout = {
 *     body.div("fullscreen") {
 *         sideCanvas()
 *         div("container") {
 *             sidebar()
 *             toolbar()
 *         }
 *     }
 * }
 * ```
 *
 * The element classes and their styles are listed with [guiStyles].
 */
typealias GUILayout = GUILayoutScope.() -> Unit

/**
 * The default layout: a column holding the toolbar (when [GUI.showToolbar] is set), the collapse border and the
 * sidebar, followed by the side canvas when [GUI.enableSideCanvas] is set.
 */
val defaultGUILayout: GUILayout = {
    body.div("fullscreen") {
        div("container") {
            id = "container"
            if (gui.showToolbar) {
                toolbar()
            }
            collapseBorder()
            sidebar()
        }
        if (gui.enableSideCanvas) {
            sideCanvas()
        }
    }
}

/**
 * The scope in which a [GUILayout] builds the element tree of [gui].
 */
class GUILayoutScope internal constructor(
    /** the GUI that is being laid out */
    val gui: GUI,
    /** the root element of the GUI's control manager */
    val body: Body
) {
    /**
     * Add the toolbar with the randomize, load and save buttons. Uses the `toolbar` class.
     */
    fun Element.toolbar(): Div = gui.buildToolbar(this)

    /**
     * Add the border that collapses and expands the sidebar when clicked. Uses the `collapse-border` class.
     */
    fun Element.collapseBorder(): Div = gui.buildCollapseBorder(this)

    /**
     * Add the scrollable sidebar that holds the compartments. Uses the `sidebar` class. Must be called exactly once.
     */
    fun Element.sidebar(): Div = gui.buildSidebar(this)

    /**
     * Add a dropdown that selects the compartment to show: the selected compartment is expanded, all others are
     * collapsed. Uses the `compartment-selector` class. Can be called at most once.
     */
    fun Element.compartmentSelector(): DropdownButton = gui.buildCompartmentSelector(this)

    /**
     * Add a canvas that takes up the remaining space, available through [GUI.canvas]. Uses the `full-canvas` class.
     */
    fun Element.sideCanvas(): Canvas = gui.buildSideCanvas(this)
}
