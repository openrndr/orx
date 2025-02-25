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
                    stops[0.0] = ColorRGBa.BLUE_STEEL
                    stops[0.75] = ColorRGBa.WHITE
                    stops[0.8] = ColorRGBa.BLACK

                    quantization = 10
                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REFLECT
                    levelWarpFunction = """float levelWarp(vec2 p, float level) { return level + cos(p.x*0.01 + level)*0.1; } """

                    stellar {
                        radius = drawer.bounds.width/4.0
                        center = drawer.bounds.position(0.5, 0.0)
                        sides = 6
                        sharpness = 0.5
                        rotation = seconds * 36.0
                    }
                }

                drawer.rectangle(drawer.bounds)
                drawer.shadeStyle = gradient<ColorRGBa> {
                    stops[0.0] = ColorRGBa.BLUE_STEEL
                    stops[0.75] = ColorRGBa.WHITE
                    stops[0.8] = ColorRGBa.BLACK

                    quantization = 10
                    fillUnits = FillUnits.WORLD
                    spreadMethod = SpreadMethod.REFLECT
                    levelWarpFunction = """float levelWarp(vec2 p, float level) { return level + 0.1 + cos(p.x*0.01 + level)*0.1; } """

                    stellar {
                        radius = drawer.bounds.width/4.0
                        center = drawer.bounds.position(0.5, 0.0)
                        sides = 6
                        sharpness = 0.5
                        rotation = seconds * 36.0
                    }
                }
                drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 196.0)
                for (i in 0 until 5) {
                    drawer.text("Gradient", 0.0, 128.0 + i * drawer.height / 5.0)
                }
            }
        }
    }
}