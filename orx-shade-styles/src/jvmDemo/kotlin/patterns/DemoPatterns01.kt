package patterns

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Camera2D
import org.openrndr.extra.color.presets.NAVY
import org.openrndr.extra.imageFit.imageFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.patterns.pattern

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        extend(Camera2D())
        val image = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = pattern {
                backgroundColor = ColorRGBa.NAVY
                foregroundColor = ColorRGBa.WHITE
                patternUnits = FillUnits.WORLD
                parameter("time", seconds*0.1)
//                domainWarpFunction = """vec2 patternDomainWarp(vec2 uv) { return uv + vec2(cos(uv.y * 0.1 + p_time), sin(uv.x * 0.1 + p_time)) * 30.05; }"""
                scale = 0.4

                checkers {
                }
            }

            //drawer.rectangle(drawer.bounds)
            drawer.imageFit(image, drawer.bounds)

            drawer.shadeStyle = pattern {
                backgroundColor = ColorRGBa.NAVY
                foregroundColor = ColorRGBa.WHITE
                patternUnits = FillUnits.WORLD
                parameter("time", seconds)
                domainWarpFunction = """vec2 patternDomainWarp(vec2 uv) { return uv + vec2(cos(uv.y * 0.1 + p_time), sin(uv.x * 0.1 + p_time)) * 30.05; }"""
                scale = 0.2
                checkers {
                }
            }
            drawer.fontMap = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 196.0)
            drawer.text("Patterns", 10.0, height / 2.0)
            //drawer.circle(drawer.bounds.center, 300.0)
        }
    }
}