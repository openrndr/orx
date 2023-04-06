package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Vector3

/**
 * Returns a Box mesh
 *
 * @param width the width of the box
 * @param height the height of the box
 * @param depth the depth of the box
 * @param widthSegments the number of segments along the x-axis
 * @param heightSegments the number of segments along the z-axis
 * @param depthSegments the number of segments along the y-axis
 * @param flipNormals generates inside-out geometry if true
 * @return A vertex buffer containing the triangles to render the 3D shape
 */
fun boxMesh(
    width: Double = 1.0,
    height: Double = 1.0,
    depth: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
    flipNormals: Boolean = false
): VertexBuffer {
    val vb = meshVertexBuffer(
        widthSegments * heightSegments * 6 * 2 +
                widthSegments * depthSegments * 6 * 2 +
                heightSegments * depthSegments * 6 * 2
    )
    vb.put {
        generateBox(
            width, height, depth,
            widthSegments, heightSegments, depthSegments,
            flipNormals, bufferWriter(this)
        )
    }
    return vb
}

/**
 * Generate a cylinder along the z-axis
 * @param width the width of the box
 * @param height the height of the box
 * @param depth the depth of the box
 * @param widthSegments the number of segments along the x-axis
 * @param heightSegments the number of segments along the z-axis
 * @param depthSegments the number of segments along the y-axis
 * @param flipNormals generates inside-out geometry if true
 * @param writer the vertex writer function
 */

fun generateBox(
    width: Double = 1.0,
    height: Double = 1.0,
    depth: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
    flipNormals: Boolean = false,
    writer: VertexWriter
) {

    val sign = if (flipNormals) -1.0 else 1.0
    // +x -- ZY
    generatePlane(
        Vector3(width / 2.0 * sign, 0.0, 0.0),
        Vector3.UNIT_Z, Vector3.UNIT_Y, Vector3.UNIT_X,
        -depth, -height,
        depthSegments, heightSegments, writer
    )

    // -x -- ZY
    generatePlane(
        Vector3(-width / 2.0 * sign, 0.0, 0.0),
        Vector3.UNIT_Z, Vector3.UNIT_Y, -Vector3.UNIT_X,
        -depth, height,
        depthSegments, heightSegments, writer
    )

    // +y -- XZ
    generatePlane(
        Vector3(0.0, height / 2.0 * sign, 0.0),
        Vector3.UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y,
        width, depth,
        widthSegments, depthSegments, writer
    )

    // -y -- XZ
    generatePlane(
        Vector3(0.0, -height / 2.0 * sign, 0.0),
        Vector3.UNIT_X, Vector3.UNIT_Z, -Vector3.UNIT_Y,
        width, -depth,
        widthSegments, depthSegments, writer
    )

    // +z -- XY
    generatePlane(
        Vector3(0.0, 0.0, depth / 2.0 * sign),
        Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z,
        -width, height,
        widthSegments, heightSegments, writer
    )

    // -z -- XY
    generatePlane(
        Vector3(0.0, 0.0, -depth / 2.0 * sign),
        Vector3.UNIT_X, Vector3.UNIT_Y, -Vector3.UNIT_Z,
        width, height,
        widthSegments, heightSegments, writer
    )
}