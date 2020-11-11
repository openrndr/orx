
import org.openrndr.application
import org.openrndr.color.ColorHSLa
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fluidsim.FluidSolver
import org.openrndr.extra.fluidsim.UpdateFluidCallback
import org.openrndr.math.Vector2

fun main() = application {

    configure {
        width = 920
        height = 920
    }

    program {

        val fluidSolver = FluidSolver(width, height)

        // set initial fluid parameters
        fluidSolver.params.apply {
            densityDissipation = 0.99
            velocityDissipation = 0.99
            vorticity = 0.99
            timestep = 0.125
        }

        fluidSolver.density.fill(ColorRGBa.WHITE)

        var mousePosition: Vector2? = null
        var mouseDrag: Vector2? = null

        mouse.dragged.listen {
            mousePosition = it.position
            mouseDrag = it.dragDisplacement
        }
        mouse.clicked.listen {
            mousePosition = it.position
            mouseDrag = Vector2(0.0, -3.0)
        }

        // add fluid velocity and color when the mouse is moved and clicked:
        fluidSolver.updateCallback = object : UpdateFluidCallback {
            override fun invoke(solver: FluidSolver) {
                if (mousePosition != null && mouseDrag != null) {

                    val scaledPosition = mousePosition!! / fluidSolver.fluidBounds.dimensions
                    val scaledVelocity = mouseDrag!! * Vector2(150.0, -150.0)
                    fluidSolver.addVelocity(drawer, scaledPosition, scaledVelocity, 20.0, 1, 0.5)

                    val color = ColorHSLa(360 * (seconds % 5), 0.8, 0.5).toRGBa()
                    fluidSolver.addFluidColor(drawer, scaledPosition, color, 20.0)

                    mousePosition = null
                    mouseDrag = null
                }
            }
        }

        extend {
            fluidSolver.update(drawer)
            drawer.image(fluidSolver.density.src())
        }
    }
}