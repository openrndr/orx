import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.findGroups
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.svg.loadSVG
import org.openrndr.shape.contains

/**
 * Demonstrates loading an SVG document with `loadSVG` and drawing it with `drawer.composition`.
 *
 * The document has basic shapes, paths with curves and arcs, a path with a hole (fill-rule evenodd) and nested
 * groups with transforms. Loading it gives a composition: a tree of groups and shapes with their styles. Hover over
 * a shape to outline it: the shapes of a composition can be found and used as regular OPENRNDR shapes.
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/shapes.svg")
        val shapes = composition.root.findShapes()
        println("${shapes.size} shapes in ${composition.root.findGroups().size} groups")

        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)

            // the shape under the mouse, in the coordinates of the document, which match the window here
            val hovered = shapes.lastOrNull { it.effectiveShape.contains(mouse.position) }
            if (hovered != null) {
                drawer.fill = null
                drawer.stroke = ColorRGBa.fromHex("#ff2266")
                drawer.strokeWeight = 3.0
                drawer.shape(hovered.effectiveShape)
            }
        }
    }
}
