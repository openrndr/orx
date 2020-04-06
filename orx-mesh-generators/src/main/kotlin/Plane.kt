package org.openrndr.extras.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

fun planeMesh(center: Vector3,
              right: Vector3,
              forward: Vector3,
              up: Vector3 = forward.cross(right).normalized,
              width: Double = 1.0, height: Double = 1.0,
              widthSegments: Int = 1, heightSegments: Int = 1): VertexBuffer {

    val vertexCount = (widthSegments * heightSegments) * 6
    val vb = meshVertexBuffer(vertexCount)
    vb.put {
        generatePlane(center, right, forward, up,
                width, height, widthSegments, heightSegments, bufferWriter(this))
    }
    return vb
}

/**
 * generates a finite plane with its center at (0,0,0) and spanning the xz-plane
 */
fun groundPlaneMesh(width: Double = 1.0,
                    height: Double = 1.0,
                    widthSegments: Int = 1,
                    heightSegments: Int = 1): VertexBuffer {
    return planeMesh(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y,
            width, height, widthSegments, heightSegments)
}

/**
 * generates a finite plane with its center at (0,0,0) and spanning the xy-plane
 */
fun wallPlaneMesh(width: Double = 1.0,
                    height: Double = 1.0,
                    widthSegments: Int = 1,
                    heightSegments: Int = 1): VertexBuffer {
    return planeMesh(Vector3.ZERO, Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z,
            width, height, widthSegments, heightSegments)
}


fun generatePlane(center: Vector3,
                  right: Vector3,
                  forward: Vector3,
                  up: Vector3 = forward.cross(right).normalized,

                  width: Double = 1.0, height: Double = 1.0,
                  widthSegments: Int = 1, heightSegments: Int = 2,
                  writer: VertexWriter) {

    val forwardStep = forward.normalized * (height / heightSegments)
    val rightStep = right.normalized * (width / widthSegments)

    val corner = center - forward.normalized *  (height*0.5) - right.normalized * (width * 0.5)

    val step = Vector2(1.0 / widthSegments, 1.0 / heightSegments)

    for (v in 0 until heightSegments) {
        for (u in 0 until widthSegments) {

            val uv00 = Vector2(u + 0.0, v + 0.0) * step
            val uv01 = Vector2(u + 0.0, v + 1.0) * step
            val uv10 = Vector2(u + 1.0, v + 0.0) * step
            val uv11 = Vector2(u + 1.0, v + 1.0) * step

            val c00 = corner + forwardStep * v.toDouble() + rightStep * u.toDouble()
            val c01 = corner + forwardStep * (v + 1).toDouble() + rightStep * u.toDouble()
            val c10 = corner + forwardStep * v.toDouble() + rightStep * (u + 1).toDouble()
            val c11 = corner + forwardStep * (v + 1).toDouble() + rightStep * (u + 1).toDouble()

            writer(c11, up, uv00)
            writer(c10, up, uv10)
            writer(c00, up, uv11)

            writer(c00, up, uv11)
            writer(c01, up, uv01)
            writer(c11, up, uv00)
        }
    }
}
