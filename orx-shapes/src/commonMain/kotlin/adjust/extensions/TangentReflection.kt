package org.openrndr.extra.shapes.adjust.extensions

import org.openrndr.extra.shapes.adjust.ContourAdjusterVertex
import org.openrndr.extra.shapes.vertex.ContourVertex
import org.openrndr.math.transforms.buildTransform

fun ContourVertex.tangentInReflectedToOut(tangentScale: Double = 1.0): ContourVertex {
    if (contour.empty || tangentIn == null || tangentOut == null) return withoutAdjustments()
    return controlOutMovedBy(position - tangentIn!! * tangentScale - controlOut!!)
}

fun ContourVertex.tangentOutReflectedToIn(tangentScale: Double = 1.0): ContourVertex {
    if (contour.empty || tangentIn == null || tangentOut == null) return withoutAdjustments()
    return controlInMovedBy(position - tangentOut!! * tangentScale - controlIn!!)
}

fun ContourVertex.switchedTangents(preserveLength: Boolean = false): ContourVertex {
    if (contour.empty || tangentIn == null || tangentOut == null) return withoutAdjustments()

    val sIn = if (preserveLength) tangentIn!!.length / tangentOut!!.length else 1.0
    val sOut = if (preserveLength) 1.0 / sIn else 1.0

    val newControlIn = position + tangentOut!! * sIn
    val newControlOut = position + tangentIn!! * sOut

    return transformTangents(
        buildTransform { translate(newControlIn - controlIn!!) },
        buildTransform { translate(newControlOut - controlOut!!) })
}

/**
 * Switch in and out tangents
 */
fun ContourAdjusterVertex.switchTangents(preserveLength: Boolean = false) = wrap { switchedTangents(preserveLength) }


fun ContourAdjusterVertex.reflectTangentInToOut(tangentScale: Double) = wrap { tangentInReflectedToOut(tangentScale) }

fun ContourAdjusterVertex.reflectTangentOutToIn(tangentScale: Double) = wrap { tangentOutReflectedToIn(tangentScale) }
