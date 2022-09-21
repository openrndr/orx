import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.noise.filters.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.sin

/**
 * Render existing GLSL noise algorithms side by side.
 * Re-use the same color buffer for the rendering.
 * Not all noise properties are used. Explore each noise class
 * to find out more adjustable properties.
 * The noise color can be set using a `color` or a `gain` property.
 */
fun main() = application {
    program {
        val noises = listOf(
            HashNoise(), SpeckleNoise(), CellNoise(),
            ValueNoise(), SimplexNoise3D(), WorleyNoise()
        )

        val img = colorBuffer(width / noises.size, height * 8 / 10)

        extend {
            val seed = seconds * 0.1
            val scale = 1.0 + sin(seed) * 0.5
            noises.forEach { noise ->
                when (noise) {
                    is HashNoise -> {
                        noise.seed = seed
                        noise.gain = Vector4(0.5 + sin(seconds) * 0.5)
                        noise.monochrome = frameCount % 100 < 50
                    }
                    is SpeckleNoise -> {
                        noise.seed = seed
                        noise.density = 0.1 + sin(seconds) * 0.1
                    }
                    is CellNoise -> {
                        noise.seed = Vector2(seed)
                        noise.scale = Vector2(scale)
                    }
                    is ValueNoise -> {
                        noise.seed = Vector2(seed)
                        noise.scale = Vector2(scale)
                        noise.gain = Vector4(0.5)
                    }
                    is SimplexNoise3D -> {
                        noise.seed = Vector3(seed)
                        noise.scale = Vector3(scale)
                    }
                    is WorleyNoise -> {
                        noise.scale = scale
                        noise.offset = Vector2(seed)
                    }
                }
                noise.apply(img, img)
                drawer.image(img)
                drawer.translate(
                    width / noises.size * 1.0,
                    height * 2 / 10 / (noises.size - 1.0)
                )
            }
        }
    }
}