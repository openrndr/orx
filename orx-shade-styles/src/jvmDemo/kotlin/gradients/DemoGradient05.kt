package gradients

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.color.presets.BLUE_STEEL
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.gradients.gradient

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            extend {
                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.BLACK
                    stops[1.0] = ColorRGBa.WHITE

                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REPEAT
                    //levelWarpFunction = """float levelWarp(vec2 p, float level) { return level + cos(p.x*0.01 + level)*0.1; } """

                    //quantization = 3
//                    stellar {
//                        radius = drawer.bounds.width/8.0
//                        center = drawer.bounds.position(0.5, 0.0)
//                        sides = 6
//                        sharpness = 0.5
//                        rotation = seconds * 36.0
//                    }
                    conic {
                        angle = 360.0 * 8.0
                        center = drawer.bounds.position(0.5, 0.5)
                    }
                }

                drawer.rectangle(drawer.bounds)

            }
        }
    }
}