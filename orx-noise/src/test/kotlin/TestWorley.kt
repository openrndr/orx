import org.openrndr.application
import org.openrndr.draw.renderTarget
import org.openrndr.extra.noise.filters.CellNoise
import org.openrndr.extra.noise.filters.WorleyNoise
import  org.openrndr.extra.gui.*

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        val gui = GUI()

        val worleyNoise = WorleyNoise()
        val target = renderTarget(width, height) {
            colorBuffer()
        }

        gui.add(worleyNoise)

        extend(gui)

        extend {
            worleyNoise.apply(target.colorBuffer(0), target.colorBuffer(0))
            drawer.image(target.colorBuffer(0))
        }
    }
}