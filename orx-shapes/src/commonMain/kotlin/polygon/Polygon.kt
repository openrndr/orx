package org.openrndr.extra.shapes.polygon

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmInline

@JvmInline
value class Polygon2D(val points: List<Vector2>): List<Vector2> by points

@JvmInline
value class Polygon3D(val points: List<Vector3>): List<Vector3> by points