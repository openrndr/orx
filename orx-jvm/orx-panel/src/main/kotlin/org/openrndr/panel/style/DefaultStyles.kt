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
