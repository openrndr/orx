import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.filters.*

/**
 * Render existing GLSL noise algorithms side by side.
 * Use the GUI to explore the effects.
 */
fun main() = application {
    configure {
        width = 200 * 6 + 200
        height = 500
    }
    program {
        val noises = listOf(
            HashNoise(), SpeckleNoise(), CellNoise(),
            ValueNoise(), SimplexNoise3D(), WorleyNoise()
        )

        val img = colorBuffer(200, 460)

        val gui = GUI()
        noises.forEach { gui.add(it) }
        extend(gui)

        extend {
            noises.forEachIndexed { i, noise ->
                noise.apply(img, img)
                drawer.image(img, 200.0 + i * 200.0, 20.0)
            }
        }
    }
}