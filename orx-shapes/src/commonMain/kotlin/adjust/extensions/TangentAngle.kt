package org.openrndr.extra.shapes.adjust.extensions

import org.openrndr.extra.shapes.adjust.ContourAdjusterVertex
import org.openrndr.extra.shapes.vertex.ContourVertex
import kotlin.math.acos

val ContourVertex.angleBetweenTangents: Double
    get() {
        return if (tangentIn != null && tangentOut != null) {
            acos(tangentIn!!.normalized.dot(tangentOut!!.normalized))
        } else {
            0.0
        }
    }

val ContourAdjusterVertex.angleBetweenTangents: Double
    get() {
        return ContourVertex(contourAdjuster.contour, segmentIndex()).angleBetweenTangents
    }