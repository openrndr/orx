package image

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.extra.shadestyles.fills.SpreadMethod
import org.openrndr.extra.shadestyles.fills.image.imageFill
import org.openrndr.math.transforms.transform
import kotlin.math.cos

fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        var img = loadImage("demo-data/images/image-001.png")
        extend {
            for (i in 0 until 10) {
                drawer.shadeStyle = imageFill {
                    image = img
                    fillTransform = transform {
                        translate(0.5, 0.5)
                        rotate( cos(i * 0.5 + seconds*10.0) *10.0 )
                        scale(1.0 - i * 0.05)
                        translate(-0.5, -0.5)
                    }
                }
                //drawer.stroke = null
                drawer.circle(drawer.bounds.center, 360.0 - i * 18.0)
            }
        }
    }
}

