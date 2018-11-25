package org.openrndr.extras.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Vector3

fun boxMesh(width: Double = 1.0, height: Double = 1.0, depth: Double = 1.0,
            widthSegments: Int = 1, heightSegments: Int = 1, depthSegments: Int = 1,
            invert: Boolean = false): VertexBuffer {
    val vb = meshVertexBuffer(widthSegments * heightSegments * 6 * 2 +
            widthSegments * depthSegments * 6 * 2 +
            heightSegments * depthSegments * 6 * 2)
    vb.put {
        generateBox(width, height, depth,
                widthSegments, heightSegments, depthSegments,
                invert, bufferWriter(this))
    }
    return vb
}

fun generateBox(width: Double = 1.0, height: Double = 1.0, depth: Double = 1.0,
                widthSegments: Int = 1, heightSegments: Int = 1, depthSegments: Int = 1,
                invert: Boolean = false,
                writer: VertexWriter) {

    val sign = if (invert) -1.0 else 1.0
    // +x -- ZY
    generatePlane(Vector3(width / 2.0 * sign, 0.0, 0.0),
            Vector3.UNIT_Z, Vector3.UNIT_Y, Vector3.UNIT_X,
            depth, height,
            depthSegments, heightSegments, writer)

    // -x -- ZY
    generatePlane(Vector3(-width / 2.0 * sign, 0.0, 0.0),
            Vector3.UNIT_Z, Vector3.UNIT_Y, -Vector3.UNIT_X,
            depth, height,
            depthSegments, heightSegments, writer)

    // +y -- XZ
    generatePlane(Vector3(0.0, height / 2.0 * sign, 0.0),
            Vector3.UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y,
            width, depth,
            widthSegments, depthSegments, writer)

    // -y -- XZ
    generatePlane(Vector3(0.0, -height / 2.0 * sign, 0.0),
            Vector3.UNIT_X, Vector3.UNIT_Z, -Vector3.UNIT_Y,
            width, depth,
            widthSegments, depthSegments, writer)

    // +z -- XY
    generatePlane(Vector3(0.0, 0.0, depth / 2.0 * sign),
            Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z,
            width, height,
            widthSegments, heightSegments, writer)

    // -z -- XY
    generatePlane(Vector3(0.0, 0.0, -depth / 2.0 * sign),
            Vector3.UNIT_X, Vector3.UNIT_Y, -Vector3.UNIT_Z,
            width, height,
            widthSegments, heightSegments, writer)
}