package patterns

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.NAVY
import org.openrndr.extra.color.presets.PEACH_PUFF
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.patterns.pattern

/**
 * Demonstrates the use of the `xorMod2` variant of the `pattern` shade style;
 * an algorithmic and intricate pattern.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend(Camera2D())
        extend {
            drawer.shadeStyle = pattern {
                backgroundColor = ColorRGBa.NAVY
                foregroundColor = ColorRGBa.PEACH_PUFF
                patternUnits = FillUnits.WORLD
                parameter("time", seconds * 0.1)
                scale = 1.0
                xorMod2 {
                    patternMod = 13
                    patternOffset = (seconds * 1).toInt()
                    patternMask = 1
                }
            }
            drawer.rectangle(drawer.bounds.scaledBy(100.0))
        }
    }
}