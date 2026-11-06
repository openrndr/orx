package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa

private const val defaultFontSize = 14.0

fun defaultStyles(
    controlBackground: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5),
    controlHoverBackground: ColorRGBa = controlBackground.shade(1.5),
    controlTextColor: Color = Color.RGBa(ColorRGBa.WHITE.shade(0.8)),
    controlActiveColor: Color = Color.RGBa(ColorRGBa.fromHex(0xf88379)),
    controlFontSize: Double = defaultFontSize
) = listOf(
    styleSheet(has type "item") {
        display = Display.NONE
    },

    // controls change their colors smoothly when they are hovered and pressed
    styleSheet(has type listOf("button", "dropdown-button", "colorpicker-button", "envelope-button", "xy-pad", "item")) {
        transitionDuration = 0.12
    },

    styleSheet(has type "number-field") {
        width = 100.percent
        height = (controlFontSize * 32.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        paddingLeft = 8.px
        paddingRight = 8.px
        fontSize = controlFontSize.px
        background = Color.RGBa(controlBackground.shade(0.6))
        color = controlTextColor
        borderRadius = 4.px
        transitionDuration = 0.12
        and(has state "hover") {
            background = Color.RGBa(controlBackground.shade(0.8))
        }
    },

    styleSheet(has type "tab-bar") {
        display = Display.FLEX
        flexDirection = FlexDirection.Row
        columnGap = 2.px
    },

    styleSheet(has type "tab") {
        paddingLeft = 12.px
        paddingRight = 12.px
        paddingTop = 6.px
        paddingBottom = 6.px
        fontSize = controlFontSize.px
        color = controlTextColor
        background = Color.RGBa(controlBackground.shade(0.6))
        transitionDuration = 0.12
        and(has state "hover") {
            background = Color.RGBa(controlBackground.shade(0.8))
        }
        and(has state "selected") {
            background = Color.RGBa(controlBackground)
            color = Color.RGBa(ColorRGBa.WHITE)
        }
    },

    styleSheet(has type "tab-page") {
        padding(5.px)
    },

    styleSheet(has type "context-menu") {
        width = 180.px
        padding(4.px)
        background = Color.RGBa(ColorRGBa.fromHex("#262626").opacify(0.97))
        borderWidth = 1.px
        borderColor = Color.RGBa(ColorRGBa.WHITE.opacify(0.2))
        borderRadius = 6.px
    },

    styleSheet(has type "menu-item") {
        paddingLeft = 8.px
        paddingRight = 8.px
        paddingTop = 4.px
        paddingBottom = 4.px
        fontSize = controlFontSize.px
        color = controlTextColor
        borderRadius = 4.px
        and(has state "hover") {
            background = Color.RGBa(controlBackground)
            color = Color.RGBa(ColorRGBa.WHITE)
        }
    },

    // a ring around the control that the keyboard moved the focus to
    styleSheet(has state "focus-visible") {
        outlineWidth = length { 2 }
        outlineColor = color { ColorRGBa.fromHex("#5ab0ff") }
    },

    styleSheet(has type "textfield") {
        width = 100.percent
        height = length { auto }
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginLeft = length { 5 }
        marginRight = length { 5 }
        borderWidth = length { 1.0 }

        child(has type "label") {
            width = 100.percent
            height = length { controlFontSize * 20.0 / defaultFontSize }
            color = color { controlTextColor }
            fontSize = controlFontSize.px
            and(has state "active") {
                color = color { controlActiveColor }
            }
        }
        child(has type "text-input") {
            width = 100.percent
            height = (controlFontSize * 24.0 / defaultFontSize).px
            borderColor = color { controlTextColor }
            paddingLeft = length { 5 }
            paddingRight = length { 5 }
            fontSize = controlFontSize.px
            and(has state "active") {
                color = color { controlActiveColor }
            }
        }
    },

    styleSheet(has type "dropdown-button") {
        width = LinearDimension.Auto
        height = (controlFontSize * 32.0 / defaultFontSize).px
        background = Color.RGBa(controlBackground)
        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        fontSize = controlFontSize.px

        and(has state "hover") {
            background = Color.RGBa(controlHoverBackground)
        }

        descendant(has type "button") {
            width = 100.percent
            height = 24.px
            marginBottom = 0.px
            marginTop = 0.px
            marginLeft = 0.px
            marginRight = 0.px
        }
    },

    styleSheet(has type "colorpicker-button") {
        width = 100.px
        height = (controlFontSize * 32.0 / defaultFontSize).px
        background = Color.RGBa(controlBackground)
        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        fontSize = controlFontSize.px

        and(has state "hover") {
            background = Color.RGBa(controlHoverBackground)
        }
    },

    styleSheet(has type "envelope-button") {
        width = 100.px
        height = 40.px
        background = Color.RGBa(controlBackground)
        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
    },

    styleSheet(has type "body") {
        fontSize = (controlFontSize * 18.0 / defaultFontSize).px
        fontFamily = "default"
    },

    styleSheet(has type "slider") {
        height = (controlFontSize * 32.0 / defaultFontSize).px
        width = 100.percent
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px
        fontSize = controlFontSize.px
        color = controlTextColor

        and(has state "active") {
            color = controlActiveColor
        }
    },

    styleSheet(has type "envelope-editor") {
        height = 60.px
        width = 100.percent
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 15.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px
    },

    styleSheet(
        has type listOf(
            "sequence-editor",
            "sliders-vector2",
            "sliders-vector3",
            "sliders-vector4"
        )
    ) {
        height = 60.px
        width = 100.percent
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 15.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px
        color = controlTextColor
        fontSize = controlFontSize.px
        and(has state "active") {
            color = controlActiveColor
        }
    },

    styleSheet(has type "colorpicker") {
        height = 160.px
        width = 100.percent
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 15.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px

        and(has state "active") {
            cursor = Cursor.Crosshair
        }
    },

    styleSheet(has type "text-input") {
        and(has state "active") {
            cursor = Cursor.Text
        }
    },

    styleSheet(has type "xy-pad") {
        display = Display.BLOCK
        background = Color.RGBa(ColorRGBa.GRAY)
        width = 175.px
        height = 175.px
        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 25.0 / defaultFontSize).px
        fontFamily = "default"
        color = controlTextColor

        and(has state "hover") {
            display = Display.BLOCK
            background = Color.RGBa(ColorRGBa.GRAY.shade(1.5))
        }
    },

    styleSheet(has type "overlay") {
        zIndex = ZIndex.Value(1)
    },

    styleSheet(has type "toggle") {
        height = (controlFontSize * 32.0 / defaultFontSize).px
        width = LinearDimension.Auto
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        marginLeft = 5.px
        marginRight = 5.px
        fontSize = controlFontSize.px
        color = controlTextColor
        and(has state "active") {
            color = controlActiveColor
        }
    },

    styleSheet(has type "h1") {
        fontSize = (controlFontSize * 24.0 / defaultFontSize).px
        width = 100.percent
        height = LinearDimension.Auto
        display = Display.BLOCK
        paddingTop = 1.0.px
    },

    styleSheet(has type "h2") {
        fontSize = (controlFontSize * 20.0 / defaultFontSize).px
        width = 100.percent
        height = LinearDimension.Auto
        display = Display.BLOCK
        paddingTop = 1.0.px
    },

    styleSheet(has type "h3") {
        fontSize = (controlFontSize * 16.0 / defaultFontSize).px
        width = 100.percent
        height = LinearDimension.Auto
        display = Display.BLOCK
        paddingTop = 1.0.px
    },

    styleSheet(has type "p") {
        fontSize = (controlFontSize * 16.0 / defaultFontSize).px
        width = 100.percent
        height = LinearDimension.Auto
        display = Display.BLOCK
        paddingTop = 1.0.px
    },
    styleSheet(has type "button") {
        display = Display.BLOCK
        background = color { controlBackground }
        width = LinearDimension.Auto
        height = (controlFontSize * 32.0 / defaultFontSize).px
        paddingTop = 5.px
        paddingBottom = 5.px
        paddingLeft = 5.px
        paddingRight = 5.px

        textVerticalAlign = TextAlign.Value(0.5)
        textHorizontalAlign = TextAlign.Value(0.5)

        marginLeft = 5.px
        marginRight = 5.px
        marginTop = (controlFontSize * 5.0 / defaultFontSize).px
        marginBottom = (controlFontSize * 5.0 / defaultFontSize).px
        fontSize = controlFontSize.px

        and(has state "active") {
            display = Display.BLOCK
            background = controlActiveColor
        }
        and(has state "hover") {
            display = Display.BLOCK
            background = Color.RGBa(controlHoverBackground)
        }
        and(has state "selected") {
            display = Display.BLOCK
            background = controlActiveColor
        }
    }
)
