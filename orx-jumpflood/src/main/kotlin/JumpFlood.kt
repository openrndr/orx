package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.filter.filterShaderFromUrl
import org.openrndr.math.Matrix44
import org.openrndr.resourceUrl

class EncodePoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/encode-points.frag")))
class JumpFlood : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/jumpflood.frag"))) {
    var maxSteps: Int by parameters
    var step: Int by parameters
}

class PixelDistance : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/pixel-distance.frag")))
class ContourPoints : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/contour-points.frag")))
class Threshold : Filter(filterShaderFromUrl(resourceUrl("/shaders/gl3/threshold.frag")))

val encodePoints by lazy { EncodePoints() }
val jumpFlood by lazy { JumpFlood() }
val pixelDistance by lazy { PixelDistance() }
val contourPoints by lazy { ContourPoints() }
val threshold by lazy { Threshold() }

/** [points] is square and power of 2 */
fun jumpFlood(points: ColorBuffer, coordinates: List<ColorBuffer>) {
    encodePoints.apply(points, coordinates[0])
    val exp = Math.ceil(Math.log(points.width.toDouble()) / Math.log(2.0)).toInt()
    for (i in 0 until exp) {
        jumpFlood.step = i
        jumpFlood.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
    }
}

fun jumpFlood(drawer: Drawer, points: ColorBuffer): ColorBuffer {
    val dimension = Math.max(points.width, points.height)
    val exp = Math.ceil(Math.log(dimension.toDouble()) / Math.log(2.0)).toInt()
    val squareDim = Math.pow(2.0, exp.toDouble()).toInt()

    val rt = renderTarget(squareDim, squareDim) {
        colorBuffer()
    }

    val coordinates =
            listOf(colorBuffer(squareDim, squareDim, type = ColorType.FLOAT32),
                    colorBuffer(squareDim, squareDim, type = ColorType.FLOAT32))

    drawer.isolatedWithTarget(rt) {
        drawer.ortho(rt)
        drawer.view = Matrix44.IDENTITY
        drawer.model = Matrix44.IDENTITY
        drawer.image(points)
    }


    jumpFlood(rt.colorBuffer(0), coordinates)

//    encodePoints.apply(rt.colorBuffer(0), coordinates[0])
//
//
//    for (i in 0 until exp) {
//        jumpFlood.step = i
//        jumpFlood.apply(coordinates[i % 2], coordinates[(i + 1) % 2])
//
//    }

    val final = renderTarget(points.width, points.height) {
        colorBuffer(type = ColorType.FLOAT32)
    }

    pixelDistance.apply(coordinates[exp % 2], coordinates[exp % 2])

    drawer.isolatedWithTarget(final) {
        drawer.ortho(final)
        drawer.view = Matrix44.IDENTITY
        drawer.model = Matrix44.IDENTITY
        drawer.image(coordinates[exp % 2])
    }

    coordinates.forEach { it.destroy() }

    rt.colorBuffer(0).destroy()
    rt.detachColorBuffers()
    rt.destroy()

    val fcb = final.colorBuffer(0)
    final.detachColorBuffers()
    final.destroy()
    return fcb
}