package matrix

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolated
import org.openrndr.drawImage
import org.openrndr.extra.math.matrix.Matrix
import org.openrndr.extra.math.matrix.plus
import org.openrndr.extra.math.matrix.svd
import kotlin.math.sin
import kotlin.math.sqrt

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            fun imageFromMatrix(m1: Matrix): ColorBuffer {
                return drawImage(m1.cols, m1.rows, 1.0) {
                    for (y in 0 until m1.rows) {
                        for (x in 0 until m1.cols) {
                            drawer.fill = ColorRGBa.WHITE.shade(m1[y, x])
                            drawer.point(x.toDouble() + 0.5, y.toDouble() + 0.5)
                        }
                    }
                }
            }

            val m = Matrix(128, 128)
            for (y in 0 until m.rows) {
                for (x in 0 until m.cols) {
                    val dx = x - m.cols.toDouble() / 2.0
                    val dy = y - m.rows.toDouble() / 2.0
                    val d = 0.5 * sqrt(dx * dx + dy * dy) + 0.001
                    val sc = (sin(d) / d) * 5.0
                    m[y, x] = sc
                }
            }
            val svd = m.svd()

            val images = mutableListOf<ColorBuffer>()
            val stages = mutableListOf<ColorBuffer>()
            var sum = Matrix.zerosLike(m)
            for (i in 0 until 5) {
                val m0 = svd.first[svd.first.allRows, i]
                val n0 = svd.third[svd.first.allRows, i].transposed()

                val m1 = m0 * n0 * svd.second[i, i]
                sum += m1

                val image = drawImage(m.cols, m.rows, 1.0) {
                    for (y in 0 until m1.rows) {
                        for (x in 0 until m1.cols) {
                            drawer.fill = ColorRGBa.WHITE.shade(m1[y, x])
                            drawer.point(x.toDouble(), y.toDouble())
                        }
                    }
                }

                val r = imageFromMatrix(sum)

                images.add(image)
                stages.add(r)
            }

            val og = imageFromMatrix(m)
            extend {
                drawer.image(og)
                drawer.translate(0.0, m.rows.toDouble())
                drawer.isolated {
                    for (image in images) {
                        drawer.image(image)
                        drawer.translate(m.cols.toDouble(), 0.0)
                    }
                }
                drawer.translate(0.0, m.rows.toDouble())
                drawer.isolated {
                    for (image in stages) {
                        drawer.image(image)
                        drawer.translate(m.cols.toDouble(), 0.0)
                    }
                }
            }
        }
    }
}