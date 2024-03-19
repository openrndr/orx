package org.openrndr.extra.shapes.frames

import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D

fun Path3D.frames(ascendingTs: List<Double>, up0: Vector3, analyticalDirections: Boolean) : List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0)
}

fun RectifiedPath3D.frames(ascendingTs: List<Double>, up0: Vector3, analyticalDirections: Boolean = true) : List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0)
}