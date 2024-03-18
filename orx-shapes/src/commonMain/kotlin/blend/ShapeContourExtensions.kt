package org.openrndr.extra.shapes.blend

import org.openrndr.shape.ShapeContour

/**
 * Mix between two [ShapeContour] instances
 *
 * @param other other [ShapeContour] to mix with
 * @param factor the blend factor between 0.0 and 1.0
 * @see ContourBlend
 */
fun ShapeContour.mix(other: ShapeContour, factor: Double): ShapeContour {
    return ContourBlend(this, other).mix(factor)
}

fun ShapeContour.mix(other: ShapeContour, factor: (Double) -> Double): ShapeContour {
    return ContourBlend(this, other).mix(factor)
}

/**
 * Create a [ContourBlend] instance for blending between this and [other]
 * @see ContourBlend
 */
fun ShapeContour.blend(other: ShapeContour) : ContourBlend {
    return ContourBlend(this, other)
}