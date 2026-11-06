package org.openrndr.extra.svg

import org.openrndr.shape.Shape

/**
 * Creates a [Shape] from SVG path data, the value of the d attribute of a path element, like
 * `svgShape("M 10 10 h 80 v 80 h -80 z")`. All commands are supported, absolute and relative: M, L, H, V, C, S, Q, T,
 * A and Z. Every subpath becomes a contour, closed when it ends with Z. Path data that is not valid is read up to
 * the first error, as SVG does.
 *
 * @param svgPath the path data
 */
fun svgShape(svgPath: String): Shape {
    if (svgPath.isBlank() || svgPath.trim() == "none") return Shape.EMPTY
    return SVGPath().also { it.commands += SVGParse.pathCommands(svgPath) }.shape()
}
