import org.openrndr.application
import org.openrndr.draw.renderTarget
import  org.openrndr.extra.gui.*
import org.openrndr.extra.noise.filters.SimplexNoise3D

fun main() = application {
    configure {
        width = 1000
        height = 1000
    }

    program {
        val gui = GUI()

        val simplexNoise3D = SimplexNoise3D()
        val target = renderTarget(width, height) {
            colorBuffer()
        }

        gui.add(simplexNoise3D)

        extend(gui)

        extend {
            simplexNoise3D.apply(target.colorBuffer(0), target.colorBuffer(0))
            drawer.image(target.colorBuffer(0))
        }
    }
}