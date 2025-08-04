import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.math.Spherical
import org.openrndr.math.Vector3
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.button
import org.openrndr.panel.elements.div
import org.openrndr.panel.elements.h1
import org.openrndr.panel.style.*

fun main() = application {
    program {
        val cm = controlManager {
            styleSheet(has class_ "horizontal") {
                paddingLeft = 10.px
                paddingTop = 10.px

                // ----------------------------------------------
                // The next two lines produce a horizontal layout
                // ----------------------------------------------
                display = Display.FLEX
                flexDirection = FlexDirection.Row
                width = 100.percent
            }

            styleSheet(has type "h1") {
                marginTop = 10.px
                marginLeft = 7.px
                marginBottom = 10.px
            }

            layout {
                val header = h1 { "click a button..." }

                div("horizontal") {
                    // A bunch of names for generating buttons
                    listOf(
                        "load", "save", "redo", "stretch", "bounce",
                        "twist", "swim", "roll", "fly", "dance"
                    )
                        .forEachIndexed { i, word ->

                            // A fun way of generating a set of colors
                            // of similar brightness:
                            // Grab a point on the surface of a sphere
                            // and treat its coordinates as an rgb color.
                            val pos = Vector3.fromSpherical(
                                Spherical(i * 19.0, i * 17.0, 0.4)
                            )
                            val rgb = ColorRGBa.fromVector(pos + 0.4)

                            button {
                                label = word
                                style = styleSheet {
                                    // Use Color.RGBa() to convert a ColorRGBa
                                    // color (the standard color datatype)
                                    // into "CSS" format:
                                    background = Color.RGBa(rgb)
                                }

                                // When the button is clicked replace
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