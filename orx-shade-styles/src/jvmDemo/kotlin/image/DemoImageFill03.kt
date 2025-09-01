package image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.image.imageFill

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        var img = loadImage("demo-data/images/image-001.png")
        extend {
            drawer.shadeStyle = imageFill {
                image = img
                parameter("time", seconds)
                domainWarpFunction = """vec2 if_domainWarp(vec2 p) { return p + vec2(cos(p.y * 20.0 + p_time), sin(p.x * 20.0 + p_time)) * 0.1; }"""
                spreadMethodX = SpreadMethod.REFLECT
                spreadMethodY = SpreadMethod.REFLECT
            }
            drawer.circle(drawer.bounds.center, 360.0)
        }
    }
}

