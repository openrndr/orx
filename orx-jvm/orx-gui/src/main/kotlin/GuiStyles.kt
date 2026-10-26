package org.openrndr.extra.gui

import org.openrndr.color.ColorRGBa
import org.openrndr.panel.style.*
import org.openrndr.panel.style.Display

/**
 * The style sheets for the elements of a [GUI], in addition to the default control styles.
 *
 * The selectors refer to the element classes used by the building blocks of [GUILayoutScope]:
 * `fullscreen`, `container`, `toolbar`, `collapse-border`, `sidebar`, `compartment`, `full-canvas`, and the
 * `collapsed` and `randomize-strong` state classes. A custom [GUILayout] can use these classes on its own elements,
 * and custom style sheets can be appended to (or replace) the list returned here, see [GUI.styles].
 *
 * @param appearance the base color and bar width to derive the styles from
 */
fun guiStyles(appearance: GUIAppearance = GUIAppearance()): List<StyleSheet> = listOf(
    styleSheet(has class_ "fullscreen") {
        this.width = length { 100.percent }
        this.height = length { 100.percent }
        this.flexDirection = FlexDirection.Row
        this.display = Display.FLEX
    },

    styleSheet(has class_ "full-canvas") {
        this.background = color { ColorRGBa.RED }
        this.flexShrink = flex { 1 }
        this.flexGrow = flex { 1 }
        this.height = length { 100.percent }
        this.width = length { 100 }
    },

    styleSheet(has class_ "container") {
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Column
        this.width = length { appearance.barWidth }
        this.height = length { 100.percent }
    },

    styleSheet(has class_ "collapse-border") {
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Column
        this.height = length { 5 }
        this.flexShrink = flex { 0 }
        this.width = length { 100.percent }
        this.background = color { appearance.baseColor.shade(0.9) }

        and(has state "hover") {
            this.background = color { appearance.baseColor.shade(1.1) }
        }
    },

    styleSheet(has class_ "toolbar") {
        this.height = LinearDimension.Auto
        this.width = length { 100.percent }
        // the sidebar gives way when the column overflows
        this.flexShrink = flex { 0 }
        this.display = Display.FLEX
        this.flexDirection = FlexDirection.Row
        this.background = color { appearance.baseColor }
        this.columnGap = length { 5 }

        child(has type "button") {
            marginLeft = length { 0 }
            marginRight = length { 0 }
            this.flexGrow = flex { 1 }
        }
    },

    styleSheet(has class_ "collapsed") {
        this.display = Display.NONE
    },

    styleSheet(has class_ "compartment") {
        this.paddingBottom = length { 20 }
    },

    styleSheet(has class_ "sidebar") {
        this.width = length { appearance.barWidth }
        this.paddingBottom = length { 20 }
        this.paddingTop = length { 10 }
        this.paddingLeft = length { 10 }
        this.paddingRight = length { 10 }
        this.marginRight = length { 2 }
        this.height = length { 100.percent }
        this.background = color { appearance.baseColor }
        this.overflow = Overflow.Scroll

        //<editor-fold desc="1) setup control style">
        descendant(has type "colorpicker-button") {
            this.width = length { appearance.barWidth - 25 }
        }

        descendant(has type "slider") {
            this.width = length { appearance.barWidth - 25 }
        }

        descendant(has type "button") {
            this.width = length { appearance.barWidth - 25 }
        }

        descendant(has type "textfield") {
            this.width = length { appearance.barWidth - 25 }
        }

        descendant(has type "toggle") {
            this.width = length { appearance.barWidth - 25 }
        }

        descendant(has type "xy-pad") {
            this.width = length { appearance.barWidth - 25 }
            this.height = length { appearance.barWidth - 25 }
        }

        descendant(
            has type listOf(
                "sequence-editor",
                "sliders-vector2",
                "sliders-vector3",
                "sliders-vector4"
            )
        ) {
            this.width = length { appearance.barWidth - 25 }
            this.height = length { 100 }
        }
        //</editor-fold>
    },

    styleSheet(has class_ "randomize-strong") {
        color = color { ColorRGBa.PINK }

        and(has state "hover") {
            color = color { ColorRGBa.BLACK }
            background = color { ColorRGBa.PINK }
        }
    },

    styleSheet(has type "dropdown-button") {
        this.width = 175.px
    }
)
