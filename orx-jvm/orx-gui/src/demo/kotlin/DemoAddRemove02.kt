import org.openrndr.KEY_DELETE
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter1to1
import org.openrndr.extra.color.presets.DARK_SEA_GREEN
import org.openrndr.extra.color.presets.SEA_GREEN
import org.openrndr.extra.fx.Post
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.fx.dither.ADither
import org.openrndr.extra.fx.dither.CMYKHalftone
import org.openrndr.extra.fx.edges.EdgesWork
import org.openrndr.extra.fx.grain.FilmGrain
import org.openrndr.extra.gui.GUI
import org.openrndr.shape.Circle

/**
 * Advanced demonstration on how to add and remove filters from a collection and from a GUI.
 *
 * The program draws 10 circles and then applies the Post() filter.
 * Initially, the `filters` collection to apply is empty.
 *
 * To add filters, press one of the "bgaphcde" keys in the keyboard.
 *
 * Press the DELETE key to remove the most recently added filter.
 *
 * Pressing the "s" key shuffles the order in which the filters are applied.
 * This will rarely produce interesting visual effects, but it reveals that
 * the filter's order of application can be modified after creation.
 * Note that the order in the gui will not change.
 */
fun main() = application {
    configure {
        width = 1024
        height = 1024
    }
    program {
        val filters = mutableListOf<Filter1to1>()

        val gui = GUI()

        extend(gui)
        val post = extend(Post()) {
            post { input, output ->
                // See https://github.com/openrndr/orx/tree/master/orx-fx#post-extension
                // We can `apply` multiple filters. The first one must read from `input`
                // and the last one must write into `output`.
                filters.forEachIndexed { index, filter ->
                    val from = if (index == 0) input else intermediate[index]
                    val to = if (index == filters.lastIndex) output else intermediate[index + 1]
                    filter.apply(from, to)
                }
            }
            enabled = false
        }
        extend {
            // Draw something
            drawer.clear(ColorRGBa.SEA_GREEN)
            repeat(10) {
                val t = it / 9.0
                drawer.stroke = null
                drawer.fill = ColorRGBa.PINK.mix(ColorRGBa.DARK_SEA_GREEN, t)
                drawer.circle(
                    Circle(drawer.bounds.center, 300.0).contour.position(t),
                    200.0 - t * 150.0
                )
            }
        }
        keyboard.keyDown.listen {
            if (it.name in "bgaphcde") {
                val newFilter = when (it.name) {
                    "b" -> ApproximateGaussianBlur()
                    "g" -> FilmGrain()
                    "a" -> ChromaticAberration()
                    "p" -> Perturb()
                    "h" -> CMYKHalftone()
                    "c" -> ColorCorrection()
                    "d" -> ADither()
                    else -> EdgesWork()
                }
                // Add the new filter to the gui and to our collection used for post-processing.
                gui.add(newFilter)
                filters.add(newFilter)
                post.enabled = true
            }
            if (it.name == "s") {
                filters.shuffle()
            }
            if (it.key == KEY_DELETE && filters.isNotEmpty()) {
                // Remove the last filter from the gui and from our collection used for post-processing.
                gui.remove(filters.last())
                filters.removeAt(filters.lastIndex)
                // If the filters list is empty, disable post-processing; otherwise nothing is visible.
                post.enabled = filters.isNotEmpty()
            }
        }
    }
}