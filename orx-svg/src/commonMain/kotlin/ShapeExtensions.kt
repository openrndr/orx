package org.openrndr.extra.svg

import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

fun Shape.toSvg(): String {
    val sb = StringBuilder()
    contours.forEach {
        it.segments.forEachIndexed { index, segment ->
            if (index == 0) {
                sb.append("M ${segment.start.x} ${segment.start.y}")
            }
            sb.append(
                when (segment.control.size) {
                    1 -> "Q${segment.control[0].x} ${segment.control[0].y} ${segment.end.x} ${segment.end.y}"
                    2 -> "C${segment.control[0].x} ${segment.control[0].y} ${segment.control[1].x} ${segment.control[1].y} ${segment.end.x} ${segment.end.y}"
                    else -> "L${segment.end.x} ${segment.end.y}"
                }
            )
        }
        if (it.closed) {
            sb.append("z")
        }
    }
    return sb.toString()
}

fun ShapeContour.toSvg(): String {
    val sb = StringBuilder()
    segments.forEachIndexed { index, segment ->
        if (index == 0) {
            sb.append("M ${segment.start.x} ${segment.start.y}")
        }
        sb.append(
            when (segment.control.size) {
                1 -> "C${segment.control[0].x}, ${segment.control[0].y} ${segment.end.x} ${segment.end.y}"
                2 -> "C${segment.control[0].x}, ${segment.control[0].y} ${segment.control[1].x} ${segment.control[1].y} ${segment.end.x} ${segment.end.y}"
                else -> "L${segment.end.x} ${segment.end.y}"
            }
        )
    }
    if (closed) {
        sb.append("z")
    }
    return sb.toString()
}
