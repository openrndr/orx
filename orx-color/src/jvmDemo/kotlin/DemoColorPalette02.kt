import org.openrndr.application
import org.openrndr.extra.color.palettes.analogous
import org.openrndr.extra.color.spaces.HSLuv
import org.openrndr.extra.color.spaces.RGB

/**
 * By default, generated palettes contain colors of varying hue
 * but similar brightness and saturation.
 * Here we alter the brightness of each color using .shade() for
 * an increased dynamic range.
 */
fun main() = application {
    program {
        val count = 8
        val palette = RGB.PINK.analogous<HSLuv>(360.0, count).mapIndexed { i, c ->
            c.shade((i + 1.0) / count)
        }.reversed()
        extend {
            drawer.stroke = null

            palette.forEachIndexed { i, c ->
                drawer.fill = c
                drawer.rectangle(
                    0.0, i * height / count.toDouble(),
                    width.toDouble(), height / count.toDouble()
                )
            }

        }
    }
}