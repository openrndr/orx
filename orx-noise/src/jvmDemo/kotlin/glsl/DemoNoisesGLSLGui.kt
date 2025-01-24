package glsl

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
        configure {
            width = 720
            height = 360
        }
    }
    program {
        val noises = listOf(
            HashNoise(), SpeckleNoise(), CellNoise(),
            ValueNoise(), SimplexNoise3D(), WorleyNoise()
        )

        val img = colorBuffer((width - 200) / noises.size, 460)

        val gui = GUI()
        noises.forEach { gui.add(it) }
        extend(gui)

        extend {
            noises.forEachIndexed { i, noise ->
                noise.apply(emptyArray(), img)
                drawer.image(img, 200.0 + i * img.width, 20.0)
            }
        }
    }
}