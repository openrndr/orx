package org.openrndr.extra.shapes.ordering

import org.openrndr.math.Vector2
import org.openrndr.math.map
import org.openrndr.shape.Rectangle
import org.openrndr.shape.bounds
import kotlin.math.max
import kotlin.math.pow

enum class Axis2DPermutation {
    XY,
    YX,
}

fun List<Vector2>.mortonOrder(
    scale: Double = 1.0,
    permutation: Axis2DPermutation = Axis2DPermutation.XY,
    bits: Int = 16,
): List<Vector2> {
    val bounds = this.bounds
    val md = max(bounds.width, bounds.height) * scale
    val rbounds = Rectangle(bounds.corner.x, bounds.corner.y, md, md)
    val extend = 2.0.pow(bits.toDouble()) - 1.0
    val inputPoints = map {
        it.map(
            rbounds.position(0.0, 0.0),
            rbounds.position(1.0, 1.0),
            Vector2(0.0, 0.0),
            Vector2(extend, extend)
        )
    }
    val mortonCodes = when (bits) {
        5 -> when (permutation) {
            Axis2DPermutation.XY -> inputPoints.map { morton2dEncode5Bit(it.x.toUInt(), it.y.toUInt()) }
            Axis2DPermutation.YX -> inputPoints.map { morton2dEncode5Bit(it.y.toUInt(), it.x.toUInt()) }
        }
        16 -> when (permutation) {
            Axis2DPermutation.XY -> inputPoints.map { morton2dEncode16Bit(it.x.toUInt(), it.y.toUInt()) }
            Axis2DPermutation.YX -> inputPoints.map { morton2dEncode16Bit(it.y.toUInt(), it.x.toUInt()) }
        }
        else -> error("Only 5 and 16 bit modes are supported.")
    }
    return (this zip mortonCodes).sortedBy { it.second }.map { it.first }
}

fun List<Vector2>.hilbertOrder(
    scale: Double = 1.0,
    permutation: Axis2DPermutation = Axis2DPermutation.XY,
    bits: Int = 16,
): List<Vector2> {
    val bounds = this.bounds
    val md = max(bounds.width, bounds.height) * scale
    val rbounds = Rectangle(bounds.corner.x, bounds.corner.y, md, md)
    val extend = 2.0.pow(bits.toDouble()) - 1.0
    val inputPoints = map {
        it.map(
            rbounds.position(0.0, 0.0),
            rbounds.position(1.0, 1.0),
            Vector2(0.0, 0.0),
            Vector2(extend, extend)
        )
    }
    val hilbertCodes = when (bits) {
        5 -> when (permutation) {
            Axis2DPermutation.XY -> inputPoints.map { hilbert2dEncode5Bit(it.x.toUInt(), it.y.toUInt()) }
            Axis2DPermutation.YX -> inputPoints.map { hilbert2dEncode5Bit(it.y.toUInt(), it.x.toUInt()) }
        }
        16 -> when (permutation) {
            Axis2DPermutation.XY -> inputPoints.map { hilbert2dEncode16Bit(it.x.toUInt(), it.y.toUInt()) }
            Axis2DPermutation.YX -> inputPoints.map { hilbert2dEncode16Bit(it.y.toUInt(), it.x.toUInt()) }
        }
        else -> error("Only 5 and 16 bit modes are supported.")
    }
    return (this zip hilbertCodes).sortedBy { it.second }.map { it.first }
}
