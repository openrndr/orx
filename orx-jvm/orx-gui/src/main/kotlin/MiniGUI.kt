package org.openrndr.extra.gui

import org.openrndr.panel.elements.div
import org.openrndr.panel.style.*

/**
 * A compact version of [GUI] that shows a single horizontal bar at the bottom of the screen. A dropdown on the left
 * selects the compartment to show, the controls of that compartment are laid out horizontally next to it, followed by
 * the toolbar when [showToolbar] is set. When the controls do not fit, they scroll horizontally, with the wheel, a
 * trackpad or the scrollbar, which shows while scrolling and fades out after it.
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
 * The styles of [miniGUILayout], as CSS. The compartments are laid out as rows of controls of `--mini-control-width`;
 * the colors are those of the theme of the GUI, see [guiThemeCss].
 */
val miniGUIStylesCss = """
    .mini-fullscreen { width: 100%; height: 100%; display: flex; flex-direction: column }

    .mini-spacer { width: 100%; height: 0; flex-grow: 1 }

    /* no horizontal padding, it would add to the 100% width; the outer children have margins instead. the controls
       keep the vertical margins of the default styles, these are equal for all single-line controls, which keeps them
       aligned */
    .mini-bar {
        width: 100%; height: auto; display: flex; flex-direction: row; column-gap: 10px;
        padding-top: 3px; padding-bottom: 3px; background-color: var(--gui-base);
    }

    .compartment-selector { width: var(--mini-control-width); margin-left: 10px; flex-shrink: 0 }

    /* holds the compartments, takes up the space between the compartment selector and the toolbar; it scrolls
       horizontally when the controls do not fit, with scrollbars that show while scrolling, and room for the scrollbar
       below the controls */
    .sidebar {
        width: 0; height: auto; flex-grow: 1; overflow: scroll; scrollbar-visibility: auto; padding-bottom: 5px;
    }

    .compartment { display: flex; flex-direction: row; column-gap: 10px; height: auto }
    .compartment :is(colorpicker-button, slider, button, textfield, toggle, dropdown-button) {
        width: var(--mini-control-width); flex-shrink: 0;
    }
    .compartment xy-pad { width: 80px; height: 80px; flex-shrink: 0 }
    .compartment :is(sequence-editor, sliders-vector2, sliders-vector3, sliders-vector4) {
        width: var(--mini-control-width); height: 80px; flex-shrink: 0;
    }

    /* only the compartments give way when the bar is too narrow */
    .toolbar { height: auto; display: flex; flex-direction: row; column-gap: 5px; margin-right: 10px; flex-shrink: 0 }
    .toolbar > button { margin-left: 0; margin-right: 0; flex-shrink: 0 }

    .collapsed { display: none }

    .randomize-strong { color: pink }
    .randomize-strong:hover { color: black; background-color: pink }
""".trimIndent()

/**
 * The style sheets for [miniGUILayout]: the theme of [appearance], see [guiThemeCss], with controls of [controlWidth]
 * pixels, and the style sheets of [miniGUIStylesCss].
 *
 * @param appearance the base color of the bar
 * @param controlWidth the width of every control in pixels
 */
fun miniGUIStyles(appearance: GUIAppearance = GUIAppearance(), controlWidth: Int = 150): List<StyleSheet> =
    listOf(styleSheet(":root") { css(guiThemeCss(appearance) + "\n--mini-control-width: ${controlWidth}px;") }) +
            styleSheets(miniGUIStylesCss)
