import org.openrndr.application

/**
 * Demonstrates how to use a ColorBrewer2 palette.
 * Finds the first available palette with 5 colors,
 * then draws concentric circles filled with those colors.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val palette = colorBrewer2Palettes(numberOfColors = 5).first().colors
        extend {
            palette.forEachIndexed { i, color ->
                drawer.fill = color
                drawer.circle(drawer.bounds.center, 300.0 - i * 40.0)
            }
        }
    }
}
