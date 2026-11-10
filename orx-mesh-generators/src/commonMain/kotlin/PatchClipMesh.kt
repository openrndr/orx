package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.bezierpatches.BezierPatch3D
import org.openrndr.extra.shapes.bezierpatches.PatchClip
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape

/**
 * Writes the part of [patch] inside [clip]'s shape: the surface, or with a [thickness] a solid between the surface and
 * its offset along the normal, see [PatchClip.vertices]. Make the [PatchClip] once and write it again for a patch that
 * changes.
 */
fun TriangleMeshBuilder.patchClip(clip: PatchClip, patch: BezierPatch3D, thickness: Double = 0.0) {
    for (v in clip.vertices(patch, thickness)) write(v.position, v.normal, v.texCoord)
}

/**
 * Writes the part of [patch] inside [shape], by the place of the shape in [basis]: the surface, or with a [thickness]
 * a solid between the surface and its offset along the normal, see [PatchClip].
 */
fun TriangleMeshBuilder.patchClip(
    patch: BezierPatch3D,
    shape: Shape,
    basis: Rectangle,
    gridSize: Int = 32,
    thickness: Double = 0.0,
    distanceTolerance: Double = 0.5
) = patchClip(PatchClip(shape, basis, gridSize, distanceTolerance), patch, thickness)
