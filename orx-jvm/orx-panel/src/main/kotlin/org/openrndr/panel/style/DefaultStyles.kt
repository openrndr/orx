package org.openrndr.panel.style

import org.openrndr.color.ColorRGBa

fun defaultStyles(
        controlBackground: ColorRGBa = ColorRGBa(0.5, 0.5, 0.5),
        controlHoverBackground: ColorRGBa = controlBackground.shade(1.5),
        controlTextColor: Color = Color.RGBa(ColorRGBa.WHITE.shade(0.8)),
        controlActiveColor : Color = Color.RGBa(ColorRGBa.fromHex(0xf88379 )),
        controlFontSize: Double = 14.0
) = listOf(
        styleSheet(has type "item") {
            display = Display.NONE
        },

        styleSheet(has type "textfield") {
            width = 100.percent
            height = 64.px
            and(has state "active") {
                color = controlActiveColor
            }
        },

        styleSheet(has type "dropdown-button") {
            width = LinearDimension.Auto
            height = 32.px
            background = Color.RGBa(controlBackground)
            marginLeft = 5.px
            marginRight = 5.px
            marginTop = 5.px
            marginBottom = 5.px
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
            height = 32.px
            background = Color.RGBa(controlBackground)
            marginLeft = 5.px
            marginRight = 5.px
            marginTop = 5.px
            marginBottom = 5.px

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
            marginTop = 5.px
            marginBottom = 5.px
        },

        styleSheet(has type "body") {
            fontSize = 18.px
            fontFamily = "default"
        },

        styleSheet(has type "slider") {
            height = 32.px
            width = 100.percent
            marginTop = 5.px
            marginBottom = 5.px
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
            marginTop = 5.px
            marginBottom = 15.px
            marginLeft = 5.px
            marginRight = 5.px
        },

        styleSheet(has type listOf(
                "sequence-editor",
                "sliders-vector2",
                "sliders-vector3",
                "sliders-vector4"
        )) {
            height = 60.px
            width = 100.percent
            marginTop = 5.px
            marginBottom = 15.px
            marginLeft = 5.px
            marginRight = 5.px
            color = controlTextColor
            and(has state "active") {
                color = controlActiveColor
            }
        },

        styleSheet(has type "colorpicker") {
            height = 80.px
            width = 100.percent
            marginTop = 5.px
            marginBottom = 15.px
            marginLeft = 5.px
            marginRight = 5.px
        },

        styleSheet(has type "xy-pad") {
            display = Display.BLOCK
            background = Color.RGBa(ColorRGBa.GRAY)
            width = 175.px
            height = 175.px
            marginLeft = 5.px
            marginRight = 5.px
            marginTop = 5.px
            marginBottom = 25.px
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
            height = 32.px
            width = LinearDimension.Auto
            marginTop = 5.px
            marginBottom = 5.px
            marginLeft = 5.px
            marginRight = 5.px
            fontSize = controlFontSize.px
            color = controlTextColor
            and(has state "active") {
                color = controlActiveColor
            }
        },

        styleSheet(has type "h1") {
            fontSize = 24.px
            width = 100.percent
            height = LinearDimension.Auto
            display = Display.BLOCK
            paddingTop = 1.0.px
        },

        styleSheet(has type "h2") {
            fontSize = 20.px
            width = 100.percent
            height = LinearDimension.Auto
            display = Display.BLOCK
            paddingTop = 1.0.px
        },

        styleSheet(has type "h3") {
            fontSize = 16.px
            width = 100.percent
            height = LinearDimension.Auto
            display = Display.BLOCK
            paddingTop = 1.0.px
        },

        styleSheet(has type "p") {
            fontSize = 16.px
            width = 100.percent
            height = LinearDimension.Auto
            display = Display.BLOCK
            paddingTop = 1.0.px
        },
        styleSheet(has type "button") {
            display = Display.BLOCK
            background = color { controlBackground }
            width = LinearDimension.Auto
            height = 32.px
            paddingTop = 5.px
            paddingBottom = 5.px
            paddingLeft = 5.px
            paddingRight = 5.px

            textVerticalAlign = TextAlign.Value(0.5)
            textHorizontalAlign = TextAlign.Value(0.5)

            marginLeft = 5.px
            marginRight = 5.px
            marginTop = 5.px
            marginBottom = 5.px
            fontSize = controlFontSize.px

            and(has state "active") {
                display = Display.BLOCK
                background = controlActiveColor
            }
            and(has state "hover") {
                display = Display.BLOCK
                background = Color.RGBa(controlHoverBackground)
            }
        }
)
