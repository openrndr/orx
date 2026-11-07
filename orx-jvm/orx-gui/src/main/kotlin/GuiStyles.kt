package org.openrndr.extra.gui

import org.openrndr.extra.style.toCssColor
import org.openrndr.panel.style.*

/**
 * The theme of the GUI, the custom properties that [guiStylesCss] and [miniGUIStylesCss] use, as declarations for
 * `:root`: the base color of the bars and their shades, and the width of the bar. Override them in a later style
 * sheet, like `:root { --gui-base: #223 }`.
 */
fun guiThemeCss(appearance: GUIAppearance = GUIAppearance()): String = """
    --gui-base: ${appearance.baseColor.toCssColor()};
    --gui-base-dark: ${appearance.baseColor.shade(0.9).toCssColor()};
    --gui-base-light: ${appearance.baseColor.shade(1.1).toCssColor()};
    --gui-base-opaque: ${appearance.baseColor.opacify(1.0 / appearance.baseColor.alpha).toCssColor()};
    --bar-width: ${appearance.barWidth}px;
    --gui-control-width: calc(var(--bar-width) - 25px);
""".trimIndent()

/**
 * The styles of the elements of a [GUI], as CSS, in addition to the default control styles. The colors and sizes are
 * the custom properties of the theme, see [guiThemeCss].
 *
 * The selectors refer to the element classes used by the building blocks of [GUILayoutScope]:
 * `fullscreen`, `container`, `toolbar`, `collapse-border`, `sidebar`, `compartment-section`, `compartment-header`,
 * `compartment`, `nested`, `nested-header`, `nested-content`, `full-canvas`, and the
 * `collapsed` and `randomize-strong` state classes. A custom [GUILayout] can use these classes on its own elements,
 * and custom style sheets can be appended to (or replace) the list returned by [guiStyles], see [GUI.styles].
 */
val guiStylesCss = """
    .fullscreen { width: 100%; height: 100%; flex-direction: row; display: flex }

    .full-canvas { background-color: red; flex-shrink: 1; flex-grow: 1; height: 100%; width: 100px }

    .container { display: flex; flex-direction: column; width: var(--bar-width); height: 100% }

    .collapse-border {
        display: flex; flex-direction: column; height: 5px; flex-shrink: 0; width: 100%;
        background-color: var(--gui-base-dark);
    }
    .collapse-border:hover { background-color: var(--gui-base-light) }

    /* the sidebar gives way when the column overflows */
    .toolbar {
        height: auto; width: 100%; flex-shrink: 0; display: flex; flex-direction: row;
        background-color: var(--gui-base); column-gap: 5px;
    }
    .toolbar > button { margin-left: 0; margin-right: 0; flex-grow: 1 }

    .collapsed { display: none }

    .compartment { padding-bottom: 20px }

    /* the header of a compartment stays at the top of the sidebar while the compartment scrolls by, over the controls
       that scroll under it */
    .compartment-header { position: sticky; background-color: var(--gui-base-opaque) }

    .nested-header { font-size: 13px; padding-top: 8px; padding-bottom: 2px }

    /* the controls are 25 pixels narrower than the bar and have margins of 5 pixels, they fit the content exactly; the
       sidebar only scrolls vertically, with scrollbars that show while scrolling and fade out after it, in the space at
       the right of the controls */
    .sidebar {
        width: var(--bar-width); height: 100%;
        padding: 10px 5px 20px 10px; margin-right: 2px;
        background-color: var(--gui-base);
        overflow: scroll; overflow-x: hidden; scrollbar-visibility: auto;
    }
    .sidebar :is(colorpicker-button, slider, button, textfield, toggle) { width: var(--gui-control-width) }
    .sidebar xy-pad { width: var(--gui-control-width); height: var(--gui-control-width) }
    .sidebar :is(sequence-editor, sliders-vector2, sliders-vector3, sliders-vector4) {
        width: var(--gui-control-width); height: 100px;
    }

    .randomize-strong { color: pink }
    .randomize-strong:hover { color: black; background-color: pink }

    dropdown-button { width: 175px }
""".trimIndent()

/**
 * The style sheets for the elements of a [GUI], in addition to the default control styles: the theme of [appearance],
 * see [guiThemeCss], and the style sheets of [guiStylesCss]. Custom style sheets can be appended to (or replace) the
 * list, see [GUI.styles].
 *
 * @param appearance the base color and bar width to derive the styles from
 */
fun guiStyles(appearance: GUIAppearance = GUIAppearance()): List<StyleSheet> =
    listOf(styleSheet(":root") { css(guiThemeCss(appearance)) }) + styleSheets(guiStylesCss)
