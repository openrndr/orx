package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.bounds
import kotlin.jvm.JvmInline

@JvmInline
value class Polygon2D(val points: List<Vector2>) : List<Vector2> by points

@JvmInline
value class Polygon3D(val points: List<Vector3>) : List<Vector3> by points


val Polygon2D.bounds get() = points.bounds

val Polygon3D.xy
    get() = Polygon2D(points.map { it.xy })

val Polygon2D.xy0
    get() = Polygon3D(points.map { it.xy0 })