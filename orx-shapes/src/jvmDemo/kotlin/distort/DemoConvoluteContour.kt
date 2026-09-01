package distort

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.parameters.description
import org.openrndr.extra.shapes.distort.convolute
import org.openrndr.extra.shapes.distort.distort
import org.openrndr.extra.shapes.distort.distortUniform
import org.openrndr.extra.shapes.primitives.regularStar
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.shape.Ellipse
import org.openrndr.shape.Rectangle
import kotlin.math.cos


import kotlin.math.exp

/**
 * Creates a 1D convolution kernel that amplifies curvature-scale detail on a
 * discretely sampled closed contour, using a Difference-of-Gaussians (DoG)
 * band-pass filter.
 *
 * kernel = identity + amplification * (gaussian(sigmaNarrow) - gaussian(sigmaWide))
 *
 * Circularly convolving a contour's x/y coordinate sequence with this kernel
 * pushes each point along the DoG "detail" signal, amplifying curvature at the
 * spatial scale defined by sigmaNarrow/sigmaWide while leaving the low-frequency
 * shape and position unchanged (the kernel sums to 1).
 *
 * @param size          Kernel length. Should be odd so it's centered on a sample;
 *                      bumped up by one if even.
 * @param sigmaNarrow   Std dev of the finer-scale Gaussian.
 * @param sigmaWide     Std dev of the coarser-scale Gaussian. Should be > sigmaNarrow
 *                      for a classic DoG band-pass shape.
 * @param amplification Strength of enhancement (alpha). 0 = identity (no change),
 *                      >0 amplifies curvature at the selected scale, <0 flattens it.
 * @return DoubleArray of length `size`, centered at index size / 2.
 */
fun createCurvatureAmplificationKernel(
    size: Int,
    sigmaNarrow: Double,
    sigmaWide: Double,
    amplification: Double
): DoubleArray {
    require(sigmaNarrow > 0.0) { "sigmaNarrow must be positive" }
    require(sigmaWide > 0.0) { "sigmaWide must be positive" }

    val n = if (size % 2 == 0) size + 1 else size
    val center = n / 2

    fun gaussianKernel(sigma: Double): DoubleArray {
        val g = DoubleArray(n)
        var sum = 0.0
        for (i in 0 until n) {
            val x = (i - center).toDouble()
            val v = exp(-(x * x) / (2.0 * sigma * sigma))
            g[i] = v
            sum += v
        }
        for (i in 0 until n) g[i] /= sum // normalize: pure low-pass, preserves DC
        return g
    }

    val gNarrow = gaussianKernel(sigmaNarrow)
    val gWide = gaussianKernel(sigmaWide)

    val kernel = DoubleArray(n)
    for (i in 0 until n) {
        kernel[i] = amplification * (gNarrow[i] - gWide[i])
    }
    kernel[center] += 1.0 // add identity so overall position/shape is preserved

    return kernel
}

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val c = regularStar(4, 100.0, 300.0, drawer.bounds.center).rectified()
            //val c = Rectangle.fromCenter(drawer.bounds.center, 200.0, 200.0).contour.rectified()

            //val c = Ellipse(drawer.bounds.center, 200.0, 100.0).contour.rectified()
            extend {
                var p = c.position(0.0)
                val w = 0.15

                //val kernel = DoubleArray(49) { 1.0 / 49.0}
                //val kernel = doubleArrayOf(-w,1.0 + 2*w, -w)


                val sn = 0.001 + 2.0  * mouse.position.x / width
                val kernel = createCurvatureAmplificationKernel(19, sn, 2.0, 0.5)

                println(kernel.joinToString(", "))
                var conved = c.convolute(sampleDistance = 5.0, kernel = kernel)

                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                drawer.contour(conved)
                drawer.circles(conved.segments.map { it.start }, 5.0)

                for (i in 0 until 10) {
                    conved = conved.rectified().convolute(sampleDistance = 5.0, kernel = kernel)


                }
                drawer.contour(conved)

            }
        }
    }
}