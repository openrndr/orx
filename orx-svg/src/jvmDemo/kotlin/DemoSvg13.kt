import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates nested svg elements, units, color interpolation and CSS transforms:
 *
 * - top left: a nested svg whose view box is fitted to its viewport with preserveAspectRatio="xMidYMid slice" and
 *   clipped, next to one with overflow="visible"
 * - middle: lengths in percentages of the viewport, in em of the font size, and in mm, in and pt
 * - bottom left: a gradient interpolated in sRGB, and the same gradient with color-interpolation="linearRGB"
 * - right: transforms with CSS units and functions, in style attributes and a style sheet, around a transform-origin
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/structure.svg")
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
        }
    }
}
