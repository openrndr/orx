import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.fluidsim.FluidSolver
import org.openrndr.extra.fluidsim.UpdateFluidCallback

fun main() = application {
    configure {
        width = 800
        height = 800
    }

    program {

        val fluidSolver = FluidSolver(width, height)
        fluidSolver.params.apply {
            densityDissipation = 1.0
            velocityDissipation = 0.99
            vorticity = 0.5
            timestep = 0.1
        }

        fun resetFluidTexture() {
            fluidSolver.updateFluidColorBuffer(drawer) {
                shadeStyle = shadeStyle {
                    fragmentTransform = """
                    x_fill.rgb = vec3(step(0.5, c_boundsPosition.y));
                """.trimIndent()
                }
                rectangle(fluidSolver.fluidBounds)
            }
        }

        resetFluidTexture()

        keyboard.keyDown.listen {
            if (it.name == "c") {
                resetFluidTexture()
            }
        }

        // add fluid velocity with a custom shader
        fluidSolver.updateCallback = object : UpdateFluidCallback {
            override fun invoke(solver: FluidSolver) {

                fluidSolver.updateFluidVelocityBuffer(drawer) {
                    shadeStyle = shadeStyle {
                        fragmentTransform = """
                            vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                            float mag = 1 - smoothstep(0.0, 0.05, abs(0.5 - pos.y));
                            vec2 oldVel = texture(p_velocity, pos).rg;
                            vec2 newVel = vec2(0.0, 10 * mag * sin(7 * 2 * 3.14159265359 * pos.x)); 
                            x_fill.rg = mix(oldVel, newVel, mag);
                            
                        """.trimIndent()
                        parameter("velocity", fluidSolver.velocity.src())
                        parameter("time", (seconds % 5.0) / 5.0)
                    }
                    rectangle(fluidSolver.fluidBounds)
                }
            }
        }

        extend {
            fluidSolver.update(drawer)
            drawer.image(fluidSolver.density.src())
        }
    }
}