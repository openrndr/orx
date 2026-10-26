package org.openrndr.extra.gui

import org.openrndr.color.ColorRGBa
import org.openrndr.panel.elements.div
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Display

/**
 * A compact version of [GUI] that shows a single horizontal bar at the bottom of the screen. A dropdown on the left
 * selects the compartment to show, the controls of that compartment are laid out horizontally next to it, followed by
 * the toolbar when [showToolbar] is set.
 *
 * Objects are added and removed like with [GUI], parameter values are persisted and saved in the same way.
 *
 * @param appearance the base color of the bar; [GUIAppearance.barWidth] is not used
 * @param controlWidth the width of every control in pixels
 */
open class MiniGUI(
    appearance: GUIAppearance = GUIAppearance(),
    defaultStyles: List<StyleSheet> = defaultStyles(),
    controlWidth: Int = 150
) : GUI(appearance, defaultStyles) {
    init {
        showCompartmentHeaders = false
        layout = miniGUILayout
        styles = miniGUIStyles(appearance, controlWidth)
    }
}

/**
 * The layout of [MiniGUI]: an empty area that takes up the screen above a horizontal bar holding the compartment
 * selector, the compartments and the toolbar.
 */
val miniGUILayout: GUILayout = {
    body.div("mini-fullscreen") {
        div("mini-spacer") {}
        div("mini-bar") {
            compartmentSelector()
            sidebar()
            if (gui.showToolbar) {
                toolbar()
            }
        }
    }
}

/**
 * The style sheets for [miniGUILayout]. The compartments are laid out as rows of controls of [controlWidth] pixels.
 *
 * @param appearance the base color of the bar
 * @param controlWidth the width of every control in pixels
 */
fun miniGUIStyles(appearance: GUIAppearance = GUIAppearance(), controlWidth: Int = 150): List<StyleSheet> = listOf(
    styleSheet(has class_ "mini-fullscreen") {
        this.width = length { 100.percent }
        this.height = length { 100.percent }
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Column
    },

    styleSheet(has class_ "mini-spacer") {
        this.width = length { 100.percent }
        this.height = length { 0 }
        this.flexGrow = flex { 1 }
    },

    styleSheet(has class_ "mini-bar") {
        this.width = length { 100.percent }
        this.height = LinearDimension.Auto
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Row
        this.columnGap = length { 10 }
        // no horizontal padding, it would add to the 100% width; the outer children have margins instead.
        // the controls keep the vertical margins of the default styles, these are equal for all single-line
        // controls, which keeps them aligned
        this.paddingTop = length { 3 }
        this.paddingBottom = length { 3 }
        this.background = color { appearance.baseColor }
    },

    styleSheet(has class_ "compartment-selector") {
        this.width = length { controlWidth }
        this.marginLeft = length { 10 }
        this.flexShrink = flex { 0 }
    },

    // holds the compartments, takes up the space between the compartment selector and the toolbar
    styleSheet(has class_ "sidebar") {
        this.width = length { 0 }
        this.height = LinearDimension.Auto
        this.flexGrow = flex { 1 }
    },

    styleSheet(has class_ "compartment") {
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Row
        this.columnGap = length { 10 }
        this.height = LinearDimension.Auto

        descendant(
            has type listOf(
                "colorpicker-button",
                "slider",
                "button",
                "textfield",
                "toggle",
                "dropdown-button"
            )
        ) {
            this.width = length { controlWidth }
            this.flexShrink = flex { 0 }
        }

        descendant(has type "xy-pad") {
            this.width = length { 80 }
            this.height = length { 80 }
            this.flexShrink = flex { 0 }
        }

        descendant(
            has type listOf(
                "sequence-editor",
                "sliders-vector2",
                "sliders-vector3",
                "sliders-vector4"
            )
        ) {
            this.width = length { controlWidth }
            this.height = length { 80 }
            this.flexShrink = flex { 0 }
        }
    },

    styleSheet(has class_ "toolbar") {
        this.height = LinearDimension.Auto
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Row
        this.columnGap = length { 5 }
        this.marginRight = length { 10 }
        // only the compartments give way when the bar is too narrow
        this.flexShrink = flex { 0 }

        child(has type "button") {
            marginLeft = length { 0 }
            marginRight = length { 0 }
            flexShrink = flex { 0 }
        }
    },

    styleSheet(has class_ "collapsed") {
        this.display = Display.NONE
    },

    styleSheet(has class_ "randomize-strong") {
        color = color { ColorRGBa.PINK }

        and(has state "hover") {
            color = color { ColorRGBa.BLACK }
            background = color { ColorRGBa.PINK }
        }
    }
)
