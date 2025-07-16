import org.openrndr.application
import org.openrndr.extra.palette.PaletteStudio

/**
 * Demonstrates how to access palette colors using PaletteStudio.
 * A new random palette is loaded every 60 animation frames.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val paletteStudio = PaletteStudio()

        // Choose a specific initial palette
        paletteStudio.select(11)

        extend {
            if(frameCount % 60 == 50) {
                paletteStudio.randomPalette()
            }
            drawer.clear(paletteStudio.background)

            paletteStudio.colors2.forEachIndexed { i, color ->
                drawer.fill = color
                drawer.circle(drawer.bounds.center, 300.0 - i * 40.0)
            }
        }
    }
}