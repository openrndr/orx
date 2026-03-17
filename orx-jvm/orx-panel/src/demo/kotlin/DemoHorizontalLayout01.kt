import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.h1
import org.openrndr.panel.elements.style
import org.openrndr.panel.style.*

/**
 * Demonstrates how to create a `styleSheet` using `Display.FLEX` and `FlexDirection.Row`
 * to create a horizontal layout featuring 10 clickable buttons with various colors.
 *
 * The `controlManager { }` DSL includes `styleSheet { }`, which uses a syntax inspired
 * by CSS, and `layout { }`, which is structured similarly to HTML.
 */
fun main() = application {
    configure {
        width = 720
        height = 200
        windowResizable = true
    }
    program {
        val cm = controlManager {
            styleSheet(has class_ "horizontal") {
                paddingLeft = length { 10 }
                paddingTop = length { 10 }

                // ----------------------------------------------
                // The next two lines produce a horizontal layout
                // ----------------------------------------------
                display = Display.FLEX
                flexDirection = FlexDirection.Row
                columnGap = length { 5 }
                width = length { 100.percent }

                child(has type "button") {
                    margins(length { 0 })

                    and(has state "hover") {
                        background = color(true) { ColorRGBa.BLACK }
                        borderColor = color { ColorRGBa.WHITE }
                        borderWidth = length { 2 }
                    }
                }
            }


            styleSheet(has type "h1") {
                marginTop = length { 10 }
                marginLeft = length { 7 }
                marginBottom = length { 10 }
            }

            layout {
                val header = h1 { "click a button..." }

                div("horizontal") {
                    // A bunch of names for generating buttons
                    listOf(
                        "load", "save", "redo", "stretch", "bounce",
                        "twist", "swim", "roll", "fly", "dance"
                    ).forEachIndexed { i, word ->

                        // A fun way of generating a set of colors
                        // of similar brightness:
                        // Grab a point on the surface of a sphere
                        // and treat its coordinates as an RGB color.
                        val pos = Vector3.fromSpherical(
                            Spherical(i * 19.0, i * 17.0, 0.4)
                        )

                        button {
                            label = word
                            style {
                                // Use color {} to convert a ColorRGBa
                                // color (the standard color datatype)
                                // into "CSS" format:
                                background = color { ColorRGBa.fromVector(pos + 0.4) }
                            }


                            // When the button is clicked, replace
                            // the header text with the button's label
                            events.clicked.listen {
                                header.replaceText(it.source.label)
                            }
                        }
                    }
                }
            }
        }
        extend(cm)
        extend {
            drawer.clear(ColorRGBa(0.2, 0.18, 0.16, 1.0, Linearity.SRGB))
        }
    }
}