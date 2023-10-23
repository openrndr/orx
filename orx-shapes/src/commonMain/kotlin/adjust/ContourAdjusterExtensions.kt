package org.openrndr.extra.shapes.adjust

/*
A collection of extension functions for ContourAdjuster. It is encouraged to keep the ContourAdjuster class at a minimum
size by adding extension functions here.
 */

/**
 * Apply a sub to the subject contour
 */
fun ContourAdjuster.sub(t0: Double, t1: Double, updateSelection: Boolean = true) {
    val oldSegments = contour.segments
    contour = contour.sub(t0, t1)
    val newSegments = contour.segments

    // TODO: this update of the selections is not right
    if (updateSelection && oldSegments.size != newSegments.size) {
        selectEdges()
        selectVertices()
    }
}