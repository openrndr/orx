package org.openrndr.extra.meshgenerators

import org.openrndr.extra.shapes.bezierpatches.BezierVolume
import org.openrndr.shape.Box

/**
 * Deforms the vertices written so far into [volume], by their places in the [basis] box: the CPU form of
 * [org.openrndr.extra.shapes.bezierpatches.bezierVolumeShadeStyle], and the counterpart of
 * [org.openrndr.extra.shapes.distort.envelope] for meshes. Positions go to the point of the volume at their u, v, w in
 * the box, normals are mapped by the inverse transpose of the Jacobian of that mapping and normalized, so lighting
 * follows the deformed surface. Triangles keep their vertices, so a mesh needs enough of them to bend smoothly.
 */
fun TriangleMeshBuilder.envelope(basis: Box, volume: BezierVolume) {
    data = data.map { vertex ->
        val (position, normal) = volume.deform(basis, vertex.position, vertex.normal)
        TriangleMeshBuilder.VertexData(position, normal, vertex.texCoord, vertex.color)
    }.toMutableList()
}
