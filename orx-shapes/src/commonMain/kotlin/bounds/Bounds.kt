package org.openrndr.extra.shapes.bounds

import org.openrndr.shape.*
import kotlin.jvm.JvmName

/**
 * Evaluates the bounds around all [ShapeContour] instances in the [Iterable]
 */

val Iterable<ShapeContour>.bounds : Rectangle
    @JvmName("shapeContourBounds")
    get() = map {
        it.bounds
    }.bounds

/**
 * Evaluates the bounds around all [Shape] instances in the [Iterable]
 */
val Iterable<Shape>.bounds : Rectangle
    @JvmName("shapeBounds")
    get() = map {
        it.bounds
    }.bounds


/**
 * Evaluates the bounds around all [Segment] instances in the [Iterable]
 */
val Iterable<Segment>.bounds : Rectangle
    @JvmName("segmentBounds")
    get() = map {
        it.bounds
    }.bounds