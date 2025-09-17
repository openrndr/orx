import org.openrndr.application
import org.openrndr.extra.noise.fbm
import org.openrndr.extra.noise.perlin2D

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            extend {
                perlin2D.fbm()

            }
        }
    }
}