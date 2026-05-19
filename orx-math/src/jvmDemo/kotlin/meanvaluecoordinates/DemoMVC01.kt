package meanvaluecoordinates

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.math.meanvaluecoordinates.findMVCWeights
import org.openrndr.extra.noise.uniform
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

fun weightedSum(colors: List<ColorRGBa>, weights: DoubleArray): ColorRGBa {

    var r = 0.0
    var g = 0.0
    var b = 0.0
    var a = 0.0
    for (i in colors.indices) {
        val c = colors[i].toLinear()
        r += c.r * weights[i]
        g += c.g * weights[i]
        b += c.b * weights[i]
        a += c.alpha * weights[i]
    }
    return ColorRGBa(r, g, b, a)

}

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val p = Circle(drawer.bounds.center, 30.0).contour.adaptivePositions(20.0)

            val colors = p.map { ColorRGBa.RED.shiftHue<OKHSV>(Double.uniform(-180.0, 180.0))}
            //val colors = p.mapIndexed { index, _ -> ColorRGBa.PINK.shiftHue<OKHSV>(index * 360.0 / p.size)}

            extend {

                for (y in 0 until 720 step 5) {
                    for  (x in 0 until 720 step 5) {
                        val weights = findMVCWeights(p, Vector2(x.toDouble(), y.toDouble()))
                        drawer.fill = weightedSum(colors, weights)
                        drawer.stroke = null
                        drawer.circle(x.toDouble(), y.toDouble(), 5.0)
                        //drawer.point(x.toDouble(), y.toDouble())
                    }
                }

            }
        }
    }
}