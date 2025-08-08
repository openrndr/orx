import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.palette.PaletteStudio

/**
 * Demonstrates how to create a design using PaletteStudio.
 * Press the `c` key to load a random palette.
 *
 */
fun main() = application {
    configure {
        title = "Palette"
        width = 720
        height = 720
    }
    program {
        val paletteStudio = PaletteStudio()

        val colors = mutableListOf<ColorRGBa>()

        fun fillColors() {
            for (n in 0..36) {
                when (n) {
                    12 -> paletteStudio.loadCollection(PaletteStudio.Collections.TWO)
                    24 -> paletteStudio.loadCollection(PaletteStudio.Collections.THREE)
                }

                val color = paletteStudio.colors.random()

                colors.add(color)
            }
        }

        keyboard.keyDown.listen {
            if (it.name == "c") {
                colors.clear()
                fillColors()
            }
        }

        fillColors()

        extend {
            drawer.clear(paletteStudio.background)

            val size = 120.0
            val radius = size / 2.0

            for (x in 0 until 6) {
                for (y in 0 until 6) {
                    val index = x + y * 6
                    val color = colors[index]
                    val x = size * x
                    val y = size * y

                    drawer.fill = color
                    drawer.stroke = color

                    if (index <= 11 || index > 23) {
                        drawer.circle(x + radius, y + radius, radius)
                    } else {
                        drawer.rectangle(x, y, size, size)
                    }
                }
            }
        }
    }
}