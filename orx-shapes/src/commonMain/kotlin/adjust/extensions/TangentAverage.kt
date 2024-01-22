package org.openrndr.extra.shapes.adjust.extensions

import org.openrndr.extra.shapes.adjust.ContourAdjusterVertex
import org.openrndr.extra.shapes.vertex.ContourVertex

fun ContourVertex.tangentsAveraged(): ContourVertex {
    if (contour.empty || tangentIn == null || tangentOut == null) return withoutAdjustments()

    val sum = (tangentIn!! - tangentOut!!).normalized
    val lengthIn = tangentIn!!.length
    val lengthOut = tangentOut!!.length

    val positionIn = position + sum * lengthIn
    val positionOut = position - sum * lengthOut

    return controlOutMovedBy(positionOut - controlOut!!).controlInMovedBy(positionIn - controlIn!!)
}

/**
 * Average the in and out tangents
 */
fun ContourAdjusterVertex.averageTangents() = wrap { tangentsAveraged() }
