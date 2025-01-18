package org.openrndr.extra.shapes.frames

import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D

/**
 * Generates a list of frame transformation matrices along a 3D path using parallel transport.
 *
 * @param ascendingTs a list of increasing parameter values that define positions along the path.
 * @param up0 the initial up vector, used to determine the orientation of frames; must not have zero or NaN length.
 * @param analyticalDirections a flag indicating whether to use analytically calculated directions along the path.
 * @return a list of 4x4 transformation matrices representing the frames at the specified path positions.
 */
fun Path3D.frames(ascendingTs: List<Double>, up0: Vector3, analyticalDirections: Boolean) : List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0)
}

/**
 * Computes a list of frame transformation matrices along a 3D rectified path using parallel transport.
 *
 * @param ascendingTs a list of increasing parameter values that define positions along the path.
 * @param up0 the initial up vector, which determines the initial orientation of the frames.
 * @param analyticalDirections whether to calculate direction vectors analytically;
 *                              if false, this will use an empty list as directions.
 * @return a list of 4x4 transformation matrices representing frames at the specified positions on the path.
 */
fun RectifiedPath3D.frames(ascendingTs: List<Double>, up0: Vector3, analyticalDirections: Boolean = true) : List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0)
}