import org.openrndr.application

import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fft.FFT
import org.openrndr.extra.noise.scatter
import org.openrndr.extra.shapes.hobbycurve.hobbyCurve
import org.openrndr.math.Vector2
import org.openrndr.extra.shapes.splines.catmullRom
import org.openrndr.extra.shapes.splines.toContour
import org.openrndr.math.smoothstep
import org.openrndr.math.transforms.buildTransform
import kotlin.math.*
import kotlin.random.Random

/**
 * Demonstration of using FFT to filter a two-dimensional shape. Mouse xy-position is mapped
 * to lowpass and highpass settings of the filter.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val fftSize = 512
            val fft = FFT(fftSize)
            fun List<Vector2>.toFloatArrays(x: FloatArray, y: FloatArray) {
                for ((index, segment) in this.withIndex()) {
                    x[index] = segment.x.toFloat()
                    y[index] = segment.y.toFloat()
                }
            }

            fun vectorsFromFloatArrays(x: FloatArray, y: FloatArray): List<Vector2> {
                val n = x.size
                val result = mutableListOf<Vector2>()
                for (i in 0 until n) {
                    result.add(Vector2(x[i].toDouble(), y[i].toDouble()))
                }
                return result
            }

            fun lp(t: Double, c: Double): Double {
                return smoothstep(c, c - 0.1, t)
            }

            fun hp(t: Double, c: Double): Double {
                return smoothstep(c, c + 0.1, t)
            }

            val c = hobbyCurve(
                drawer.bounds.scatter(40.0, distanceToEdge = 100.0, random = Random(0)),
                true
            ).transform(buildTransform { translate(-drawer.bounds.center) })

            val x = FloatArray(fftSize)
            val y = FloatArray(fftSize)

            val xFiltered = FloatArray(fftSize)
            val yFiltered = FloatArray(fftSize)

            extend {
                c.equidistantPositions(fftSize).take(fftSize).toFloatArrays(x, y)

                // process x-component
                fft.forward(x)
                val xpower = fft.magnitudeSum()

                val lpc = mouse.position.x / width
                val hpc = mouse.position.y / height

                for (i in 1..fftSize / 2) {
                    val t = i.toDouble() / (fftSize / 2 - 1)
                    val f = max(lp(t, lpc), hp(t, hpc))
                    fft.scaleBand(i, f.toFloat())
                }
                val xfpower = fft.magnitudeSum().coerceAtLeast(1.0)

                fft.scaleAll((xpower / xfpower).toFloat())
                fft.inverse(xFiltered)

                // process y-component
                fft.forward(y)
                val ypower = fft.magnitudeSum()

                for (i in 1..fftSize / 2) {
                    val t = i.toDouble() / (fftSize / 2 - 1)
                    val f = max(lp(t, lpc), hp(t, hpc))
                    fft.scaleBand(i, f.toFloat())
                }
                val yfpower = fft.magnitudeSum().coerceAtLeast(1.0)

                fft.scaleAll((ypower / yfpower).toFloat())
                fft.inverse(yFiltered)

                val cr = vectorsFromFloatArrays(xFiltered, yFiltered).catmullRom(closed = true).toContour()
                //val cr = ShapeContour.fromPoints(vectorsFromFloatArrays(xr, yr), closed=true)

                val recenteredShape = cr.transform(buildTransform {
                    translate(drawer.bounds.center)
                })
                drawer.fill = null
                drawer.stroke = ColorRGBa.WHITE
                drawer.contour(recenteredShape)
            }
        }
    }
}