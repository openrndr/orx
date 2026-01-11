package org.openrndr.extra.meshgenerators

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.shape.Path3D
import org.openrndr.shape.Shape
import org.openrndr.shape.Triangle

/**
 * Writes two triangles to [writer] representing
 * the quad formed by four vertices.
 *
 * @param v00 vertex (0, 0)
 * @param v01 vertex (0, 1)
 * @param v10 vertex (1, 0)
 * @param v11 vertex (1, 1)
 * @param faceNormal the face normal
 * @param writer the vertex writer function
 */
internal fun quadToTris(
    v00: Vector3,
    v01: Vector3,
    v10: Vector3,
    v11: Vector3,
    faceNormal: Vector3,
    writer: VertexWriter
) {
    writeTri(v11, v01, v00, Vector2.ZERO, Vector2.ZERO, Vector2.ZERO, faceNormal, writer)
    writeTri(v00, v10, v11, Vector2.ZERO, Vector2.ZERO, Vector2.ZERO, faceNormal, writer)
}

/**
 * Converts a quadrilateral defined by its vertices and normals into two triangles
 * and writes the resulting data using the provided vertex writer function.
 *
 * @param v00 the vertex at the bottom-left corner of the quadrilateral
 * @param v01 the vertex at the top-left corner of the quadrilateral
 * @param v10 the vertex at the bottom-right corner of the quadrilateral
 * @param v11 the vertex at the top-right corner of the quadrilateral
 * @param n00 the normal vector at the bottom-left corner of the quadrilateral
 * @param n01 the normal vector at the top-left corner of the quadrilateral
 * @param n10 the normal vector at the bottom-right corner of the quadrilateral
 * @param n11 the normal vector at the top-right corner of the quadrilateral
 * @param writer the function responsible for writing vertex data
 */
internal fun quadToTris(
    v00: Vector3,
    v01: Vector3,
    v10: Vector3,
    v11: Vector3,
    n00: Vector3,
    n01: Vector3,
    n10: Vector3,
    n11: Vector3,
    writer: VertexWriter
) {
    writeTri(
        v11, v01, v00,
        Vector2.ZERO, Vector2.ZERO, Vector2.ZERO,
        n11, n01, n00,
        writer)

    writeTri(
        v00, v10, v11,
        Vector2.ZERO, Vector2.ZERO, Vector2.ZERO,
        n00, n10, n11,
        writer)
}

/**
 * Writes a triangle to [writer].
 *
 * @param v0 vertex 0
 * @param v1 vertex 1
 * @param v2 vertex 2
 * @param tc0 texture coordinate 0
 * @param tc1 texture coordinate 1
 * @param tc2 texture coordinate 2
 * @param faceNormal the face normal
 * @param writer the vertex writer function
 */
internal fun writeTri(
    v0: Vector3, v1: Vector3, v2: Vector3,
    tc0: Vector2, tc1: Vector2, tc2: Vector2,
    faceNormal: Vector3,
    writer: VertexWriter
) {
    writer(v0, faceNormal, tc0)
    writer(v1, faceNormal, tc1)
    writer(v2, faceNormal, tc2)
}

/**
 * Writes a triangle to [writer].
 *
 * @param v0 vertex 0
 * @param v1 vertex 1
 * @param v2 vertex 2
 * @param tc0 texture coordinate 0
 * @param tc1 texture coordinate 1
 * @param tc2 texture coordinate 2
 * @param n0 normal 0
 * @param n1 normal 1
 * @param n2 normal 2
 * @param writer the vertex writer function
 */
internal fun writeTri(
    v0: Vector3, v1: Vector3, v2: Vector3,
    tc0: Vector2, tc1: Vector2, tc2: Vector2,
    n0: Vector3, n1: Vector3, n2: Vector3,
    writer: VertexWriter
) {
    writer(v0, n0, tc0)
    writer(v1, n1, tc1)
    writer(v2, n2, tc2)
}

/**
 * Writes a list of triangles transformed by the [frame]
 * transformation matrix into [writer].
 *
 * @param triangulation the list of triangles to write
 * @param frame a transformation matrix to apply to each triangle
 * @param flipNormals generates inside-out geometry if true
 * @param writer the vertex writer function
 */
internal fun triangulationWithFrame(
    triangulation: List<Triangle>,
    frame: Matrix44,
    flipNormals: Boolean = true,
    writer: VertexWriter
) {
    val normalFrame = normalMatrix(frame)
    val normalScale = if (!flipNormals) -1.0 else 1.0
    val normal = ((normalFrame * Vector4(0.0, 0.0, -normalScale, 0.0)).xyz)
    for (triangle in triangulation) {
        val t = if (!flipNormals) triangle else Triangle(triangle.x3, triangle.x2, triangle.x1)
        writer((frame * t.x1.xy01).xyz, normal, Vector2.ZERO)
        writer((frame * t.x2.xy01).xyz, normal, Vector2.ZERO)
        writer((frame * t.x3.xy01).xyz, normal, Vector2.ZERO)
    }
}

/**
 * Adds caps to an extruded shape
 *
 * @param linearShape the cross-section of the mesh
 * @param path the 3D path
 * @param startCap adds a start cap if set to true
 * @param endCap adds an end cap if set to true
 * @param frames a list of matrices holding the transformation matrices along
 * the path
 * @param writer the vertex writer function
 */
internal fun extrudeCaps(
    linearShape: Shape,
    path: Path3D,
    startCap: Boolean,
    endCap: Boolean,
    frames: List<Matrix44>,
    writer: VertexWriter
) {
    if ((startCap || endCap) && !path.closed) {
        val capTriangles = linearShape.triangulation
        if (startCap) {
            triangulationWithFrame(capTriangles, frames.first(), false, writer)
        }
        if (endCap) {
            triangulationWithFrame(capTriangles, frames.last(), true, writer)
        }
    }
}
