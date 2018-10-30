package studio.rndnr.packture

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBufferShadow
import org.openrndr.shape.IntRectangle

class IntegralImage(val width: Int, val height: Int, val integral: LongArray) {

    internal val maximum: Long
        get() = integral[integral.size - 1]

    companion object {
        fun fromColorBufferShadow(shadow: ColorBufferShadow, sampler: (ColorRGBa) -> Long = { (it.r * 255.0).toLong() }): IntegralImage {
            val integral = LongArray(shadow.colorBuffer.width * shadow.colorBuffer.height)

            val width = shadow.colorBuffer.width
            val height = shadow.colorBuffer.height

            for (i in integral.indices) {
                integral[i] = 0
            }

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val i = sampler(shadow.read(x, y))

                    var i10: Long = 0
                    if (x > 0)
                        i10 = integral[x - 1 + y * width]

                    var i01: Long = 0
                    if (y > 0) {
                        i01 = integral[x + (y - 1) * width]
                    }

                    var i11: Long = 0
                    if (y > 0 && x > 0) {
                        i11 = integral[x - 1 + (y - 1) * width]
                    }

                    integral[y * width + x] = i + i10 + i01 - i11
                }
            }
            return IntegralImage(width, height, integral)
        }
    }


    private fun clip(x: Int, left: Int, right: Int): Int {
        return Math.min(right, Math.max(left, x))
    }

    fun sum(area: IntRectangle): Long {
        return sum(area.x, area.y, area.x + area.width - 1, area.y + area.height - 1)
    }

    private fun sum(left: Int, top: Int, right: Int, bottom: Int): Long {
        var left = left
        var top = top
        var right = right
        var bottom = bottom
        top = clip(top, 0, height - 1)
        bottom = clip(bottom, 0, height - 1)

        left = clip(left, 0, width - 1)
        right = clip(right, 0, width - 1)

        val a = integral[left + top * width]
        val b = integral[right + top * width]
        val c = integral[right + bottom * width]
        val d = integral[left + bottom * width]

        return a + c - b - d
    }

    private fun average(left: Int, top: Int, right: Int, bottom: Int): Double {
        val area = ((right - left) * (bottom - top)).toDouble()
        return sum(left, top, right, bottom) / area
    }
}