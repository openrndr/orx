package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.style.toCssColor

private const val defaultFontSize = 14.0

/**
 * The theme of the default styles, the custom properties that [defaultStylesCss] uses, as declarations for `:root`.
 * Override them in a later style sheet, like `:root { --control-background: #335 }`, or make a theme with
 * [panelTheme].
 */
fun panelThemeCss(
    controlBackground: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5),
    controlHoverBackground: ColorRGBa = controlBackground.shade(1.5),
    controlTextColor: ColorRGBa = ColorRGBa.WHITE.shade(0.8),
    controlActiveColor: ColorRGBa = ColorRGBa.fromHex(0xf88379),
    controlFontSize: Double = defaultFontSize,
    focusColor: ColorRGBa = ColorRGBa.fromHex("#5ab0ff"),
    menuBackground: ColorRGBa = ColorRGBa.fromHex("#262626").opacify(0.97),
    menuBorder: ColorRGBa = ColorRGBa.WHITE.opacify(0.2),
    padBackground: ColorRGBa = ColorRGBa.GRAY,
    padHoverBackground: ColorRGBa = ColorRGBa.GRAY.shade(1.5)
): String = """
    --control-background: ${controlBackground.toCssColor()};
    --control-hover-background: ${controlHoverBackground.toCssColor()};
    --control-sunken-background: ${controlBackground.shade(0.6).toCssColor()};
    --control-sunken-hover-background: ${controlBackground.shade(0.8).toCssColor()};
    --control-text: ${controlTextColor.toCssColor()};
    --control-selected-text: ${ColorRGBa.WHITE.toCssColor()};
    --control-active: ${controlActiveColor.toCssColor()};
    --focus-color: ${focusColor.toCssColor()};
    --menu-background: ${menuBackground.toCssColor()};
    --menu-border: ${menuBorder.toCssColor()};
    --pad-background: ${padBackground.toCssColor()};
    --pad-hover-background: ${padHoverBackground.toCssColor()};
    --control-font-size: ${controlFontSize}px;
    --control-height: calc(var(--control-font-size) * 32 / 14);
    --control-margin: calc(var(--control-font-size) * 5 / 14);
    --control-margin-wide: calc(var(--control-font-size) * 15 / 14);
    --transition: 0.12s;
""".trimIndent()

/** the theme of the default styles, see [panelThemeCss], as a style sheet for `:root` */
fun panelTheme(css: String = panelThemeCss()): StyleSheet = styleSheet(":root") { css(css) }

/**
 * The default styles of the controls of panels, as CSS. Their colors and sizes are the custom properties of the
 * theme, see [panelThemeCss].
 */
val defaultStylesCss = """
    item { display: none }

    /* a long list that builds the rows in view, see VirtualList */
    virtual-list { display: block; overflow: scroll; overflow-x: hidden }

    /* trees and tables */
    tree-view { display: block; height: 100% }
    tree-view > virtual-list { height: 100% }
    tree-row { width: 100%; height: 100%; color: var(--control-text); font-size: var(--control-font-size) }
    tree-row:hover { background-color: rgb(255 255 255 / 0.06) }
    tree-row:selected { background-color: var(--control-background); color: var(--control-selected-text) }
    data-table { display: flex; flex-direction: column; align-items: stretch; height: 100% }
    data-header { display: flex; flex-direction: row; flex-shrink: 0; height: 28px; background-color: var(--control-sunken-background) }
    data-header-cell { height: 100%; flex-shrink: 0; color: var(--control-text); font-size: var(--control-font-size) }
    data-header-cell:hover { background-color: var(--control-sunken-hover-background) }
    data-table > virtual-list { flex: 1; flex-basis: 0 }
    data-row { width: 100%; height: 100%; color: var(--control-text); font-size: var(--control-font-size) }
    .virtual-row:nth-child(even) > data-row { background-color: rgb(255 255 255 / 0.03) }
    data-row:hover { background-color: rgb(255 255 255 / 0.06) }
    data-row:selected { background-color: var(--control-background); color: var(--control-selected-text) }

    /* drag and drop */
    :dragging { opacity: 0.5 }
    tree-row:drag-over { background-color: rgb(90 176 255 / 0.25) }
    .virtual-marker { width: 100%; height: 2px; background-color: var(--focus-color) }

    /* menus, the menu bar and the command palette */
    menu-separator { width: 100%; height: 9px }
    context-menu menu-item { width: 100% }
    menu-bar { display: flex; flex-direction: row; width: 100%; height: auto; background-color: var(--control-sunken-background) }
    menu-title { padding: 6px 12px; font-size: var(--control-font-size); color: var(--control-text); width: auto }
    menu-title:hover, menu-title:active { background-color: var(--control-sunken-hover-background); color: var(--control-selected-text) }
    .command-palette { width: 480px }
    .command-palette text-input { width: 100%; height: 32px; padding-left: 8px; font-size: var(--control-font-size); background-color: rgb(0 0 0 / 0.25); color: var(--control-selected-text); margin-bottom: 8px }
    .command-palette virtual-list { height: 300px }
    command-row { width: 100%; height: 100%; color: var(--control-text); font-size: var(--control-font-size); border-radius: 4px }
    command-row:hover { background-color: rgb(255 255 255 / 0.06) }
    command-row:selected { background-color: var(--control-background); color: var(--control-selected-text) }

    /* split panes, dialogs and toasts */
    split-pane { display: flex; flex-direction: row; align-items: stretch; width: 100%; height: 100% }
    split-pane[data-vertical] { flex-direction: column }
    .split-first { flex-grow: 0; flex-shrink: 0; overflow: hidden }
    .split-second { flex-grow: 1; flex-shrink: 1; flex-basis: 0; overflow: hidden }
    .splitter { width: 6px; flex-shrink: 0; background-color: rgb(255 255 255 / 0.08); cursor: col-resize }
    split-pane[data-vertical] > .splitter { width: auto; height: 6px; cursor: row-resize }
    .splitter:hover { background-color: var(--focus-color) }
    .dialog-backdrop {
        position: fixed; inset: 0; z-index: 1000; display: flex; flex-direction: row;
        justify-content: center; align-items: center; background-color: rgb(0 0 0 / 0.45);
    }
    .dialog {
        min-width: 240px; max-width: 480px; padding: 16px; border-radius: 10px; background-color: #26292f; color: var(--control-text);
        box-shadow: 0 12px 40px rgb(0 0 0 / 0.5); animation: dialog-in 150ms ease-out;
    }
    @keyframes dialog-in { from { opacity: 0; transform: scale(0.96) } to { opacity: 1; transform: scale(1) } }
    .toasts {
        position: fixed; right: 16px; bottom: 16px; z-index: 1001; display: flex; flex-direction: column;
        align-items: end; row-gap: 8px;
    }
    .toast > * { width: auto; white-space: nowrap }
    .toast {
        padding: 10px 14px; border-radius: 8px; background-color: #26292f; color: var(--control-text);
        box-shadow: 0 6px 18px rgb(0 0 0 / 0.4); animation: toast-in 200ms ease-out;
    }
    .toast.leaving { animation: toast-out 300ms ease-in forwards }
    @keyframes toast-in { from { opacity: 0; transform: translateY(16px) } to { opacity: 1; transform: translateY(0) } }
    @keyframes toast-out { to { opacity: 0; transform: translateY(16px) } }

    /* controls change their colors smoothly when they are hovered and pressed */
    :is(button, dropdown-button, colorpicker-button, envelope-button, xy-pad, item) { transition-duration: var(--transition) }

    number-field {
        width: 100%; height: var(--control-height);
        margin: var(--control-margin) 5px; padding-left: 8px; padding-right: 8px;
        font-size: var(--control-font-size);
        background-color: var(--control-sunken-background); color: var(--control-text);
        border-radius: 4px; transition-duration: var(--transition);
    }
    number-field:hover { background-color: var(--control-sunken-hover-background) }

    tab-bar { display: flex; flex-direction: row; column-gap: 2px }

    tab {
        padding: 6px 12px; font-size: var(--control-font-size);
        color: var(--control-text); background-color: var(--control-sunken-background);
        transition-duration: var(--transition);
    }
    tab:hover { background-color: var(--control-sunken-hover-background) }
    tab:selected { background-color: var(--control-background); color: var(--control-selected-text) }

    tab-page { padding: 5px }

    context-menu {
        width: 180px; padding: 4px;
        background-color: var(--menu-background); border-width: 1px; border-color: var(--menu-border);
        border-radius: 6px;
    }

    menu-item {
        padding: 4px 8px; font-size: var(--control-font-size); color: var(--control-text); border-radius: 4px;
    }
    menu-item:hover { background-color: var(--control-background); color: var(--control-selected-text) }

    /* a ring around the control that the keyboard moved the focus to */
    :focus-visible { outline-width: 2px; outline-color: var(--focus-color) }

    textfield {
        width: 100%; height: auto;
        margin: var(--control-margin) 5px; border-width: 1px;
    }
    textfield > label {
        width: 100%; height: calc(var(--control-font-size) * 20 / 14);
        color: var(--control-text); font-size: var(--control-font-size);
    }
    textfield > label:active { color: var(--control-active) }
    textfield > text-input {
        width: 100%; height: calc(var(--control-font-size) * 24 / 14);
        border-color: var(--control-text); padding-left: 5px; padding-right: 5px;
        font-size: var(--control-font-size);
    }
    textfield > text-input:active { color: var(--control-active) }

    dropdown-button {
        width: auto; height: var(--control-height); background-color: var(--control-background);
        margin: var(--control-margin) 5px; font-size: var(--control-font-size);
    }
    dropdown-button:hover { background-color: var(--control-hover-background) }
    dropdown-button button { width: 100%; height: 24px; margin: 0 }

    colorpicker-button {
        width: 100px; height: var(--control-height); background-color: var(--control-background);
        margin: var(--control-margin) 5px; font-size: var(--control-font-size);
    }
    colorpicker-button:hover { background-color: var(--control-hover-background) }

    envelope-button {
        width: 100px; height: 40px; background-color: var(--control-background);
        margin: var(--control-margin) 5px;
    }

    body { font-size: calc(var(--control-font-size) * 18 / 14); font-family: default }

    slider {
        height: var(--control-height); width: 100%;
        margin: var(--control-margin) 5px; font-size: var(--control-font-size); color: var(--control-text);
    }
    slider:active { color: var(--control-active) }

    range-slider {
        height: var(--control-height); width: 100%;
        margin: var(--control-margin) 5px; font-size: var(--control-font-size); color: var(--control-text);
    }
    range-slider:active { color: var(--control-active) }
    knob {
        width: 64px; height: 92px; margin: var(--control-margin) 5px;
        font-size: var(--control-font-size); color: var(--control-text);
    }
    knob:active { color: var(--control-active) }
    curve-editor {
        width: 100%; height: 160px; margin: var(--control-margin) 5px var(--control-margin-wide) 5px;
        background-color: var(--pad-background); font-size: var(--control-font-size); color: var(--control-text);
    }
    gradient-editor { width: 100%; margin: var(--control-margin) 5px var(--control-margin-wide) 5px }
    gradient-bar { width: 100%; height: 40px; margin-bottom: 6px; color: var(--control-text) }
    timeline {
        width: 100%; margin: var(--control-margin) 5px; background-color: var(--control-sunken-background);
        font-size: var(--control-font-size); color: var(--control-text);
    }

    /* docks */
    dock { width: 100%; height: 100% }
    dock-group { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100%; position: relative }
    dock-tab-bar { display: flex; flex-direction: row; flex-shrink: 0; column-gap: 1px; background-color: var(--control-sunken-background) }
    dock-tab {
        padding: 6px 12px; font-size: var(--control-font-size); color: var(--control-text); width: auto;
        transition-duration: var(--transition);
    }
    dock-tab:hover { background-color: var(--control-sunken-hover-background) }
    dock-tab:selected { background-color: var(--control-background); color: var(--control-selected-text) }
    dock-content { flex: 1; flex-basis: 0; overflow: scroll; overflow-x: hidden; padding: 5px }
    dock-zone { z-index: 2; background-color: rgb(90 176 255 / 0.2); border-width: 1px; border-color: var(--focus-color); border-radius: 4px }

    envelope-editor {
        height: 60px; width: 100%;
        margin: var(--control-margin) 5px var(--control-margin-wide) 5px;
    }

    :is(sequence-editor, sliders-vector2, sliders-vector3, sliders-vector4) {
        height: 60px; width: 100%;
        margin: var(--control-margin) 5px var(--control-margin-wide) 5px;
        color: var(--control-text); font-size: var(--control-font-size);
    }
    :is(sequence-editor, sliders-vector2, sliders-vector3, sliders-vector4):active { color: var(--control-active) }

    colorpicker {
        height: 160px; width: 100%;
        margin: var(--control-margin) 5px var(--control-margin-wide) 5px;
    }
    colorpicker:active { cursor: crosshair }

    text-input:active { cursor: text }

    xy-pad {
        display: block; background-color: var(--pad-background); width: 175px; height: 175px;
        margin: var(--control-margin) 5px calc(var(--control-font-size) * 25 / 14) 5px;
        font-family: default; color: var(--control-text);
    }
    xy-pad:hover { display: block; background-color: var(--pad-hover-background) }

    overlay { z-index: 1 }

    toggle {
        height: var(--control-height); width: auto;
        margin: var(--control-margin) 5px; font-size: var(--control-font-size); color: var(--control-text);
    }
    toggle:active { color: var(--control-active) }

    h1 { font-size: calc(var(--control-font-size) * 24 / 14); width: 100%; height: auto; display: block; padding-top: 1px }
    h2 { font-size: calc(var(--control-font-size) * 20 / 14); width: 100%; height: auto; display: block; padding-top: 1px }
    h3 { font-size: calc(var(--control-font-size) * 16 / 14); width: 100%; height: auto; display: block; padding-top: 1px }
    p { font-size: calc(var(--control-font-size) * 16 / 14); width: 100%; height: auto; display: block; padding-top: 1px }

    button {
        display: block; background-color: var(--control-background);
        width: auto; height: var(--control-height); padding: 5px;
        text-align: center; vertical-align: middle;
        margin: var(--control-margin) 5px; font-size: var(--control-font-size);
    }
    button:active { display: block; background-color: var(--control-active) }
    button:hover { display: block; background-color: var(--control-hover-background) }
    button:selected { display: block; background-color: var(--control-active) }
""".trimIndent()

/**
 * The default styles of the controls of panels: the theme of the colors and the font size that are given, see
 * [panelThemeCss], and the style sheets of [defaultStylesCss]
 */
fun defaultStyles(
    controlBackground: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5),
    controlHoverBackground: ColorRGBa = controlBackground.shade(1.5),
    controlTextColor: Color = Color.RGBa(ColorRGBa.WHITE.shade(0.8)),
    controlActiveColor: Color = Color.RGBa(ColorRGBa.fromHex(0xf88379)),
    controlFontSize: Double = defaultFontSize
): List<StyleSheet> = listOf(
    panelTheme(
        panelThemeCss(
            controlBackground, controlHoverBackground, (controlTextColor as Color.RGBa).color,
            (controlActiveColor as Color.RGBa).color, controlFontSize
        )
    )
) + styleSheets(defaultStylesCss)
