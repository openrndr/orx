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
 * @param closed true when the ts go round the whole of a closed path, from 0 up to or including 1; the frames then meet
 * at the start, see [frames]. Defaults to whether the path is closed.
 * @return a list of 4x4 transformation matrices representing the frames at the specified path positions.
 */
fun Path3D.frames(
    ascendingTs: List<Double>,
    up0: Vector3,
    analyticalDirections: Boolean,
    closed: Boolean = this.closed
): List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0, closed)
}

/**
 * Computes a list of frame transformation matrices along a 3D rectified path using parallel transport.
 *
 * @param ascendingTs a list of increasing parameter values that define positions along the path.
 * @param up0 the initial up vector, which determines the initial orientation of the frames.
 * @param analyticalDirections whether to calculate direction vectors analytically;
 *                              if false, the directions are estimated from the positions.
 * @param closed true when the ts go round the whole of a closed path, from 0 up to or including 1; the frames then meet
 * at the start, see [frames]. Defaults to whether the path is closed.
 * @return a list of 4x4 transformation matrices representing frames at the specified positions on the path.
 */
fun RectifiedPath3D.frames(
    ascendingTs: List<Double>,
    up0: Vector3,
    analyticalDirections: Boolean = true,
    closed: Boolean = path.closed
): List<Matrix44> {
    val positions = ascendingTs.map { this.position(it) }
    val directions = if (analyticalDirections) ascendingTs.map { this.direction(it) } else emptyList()

    return frames(positions, directions, up0, closed)
}