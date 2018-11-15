package org.openrndr.extras.meshgenerators

import org.openrndr.draw.BufferWriter
import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.mod
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape
import org.openrndr.shape.triangulate

/**
 * Vertex writer function interface
 */
typealias VertexWriter = (position: Vector3, normal: Vector3, texCoord: Vector2) -> Unit

/**
 * create a [VertexWriter] that writes into a [java.nio.ByteBuffer] through [BufferWriter]
 */
fun bufferWriter(bw: BufferWriter): VertexWriter {
    return { p, n, t ->
        bw.write(p)
        bw.write(n)
        bw.write(t)
    }
}

/**
 * creates a [VertexBuffer] that is suited for holding meshes
 */
fun meshVertexBuffer(size: Int): VertexBuffer {
    return vertexBuffer(vertexFormat {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, size)
}

/**
 * extrudes a [shape] by triangulating it and creating sides and cap geometry
 * @sample sample
 */
fun extrudeShape(shape: Shape, front: Double, back: Double, distanceTolerance: Double = 0.5, writer: VertexWriter) {
    val baseTriangles = triangulate(shape, distanceTolerance)
    val depth = back - front
    val normal = Vector3(0.0, 0.0, depth).normalized
    val negativeNormal = normal * -1.0

    baseTriangles.forEach {
        writer(it.vector3(z = front), normal, Vector2.ZERO)
    }
    baseTriangles.forEach {
        writer(it.vector3(z = back), negativeNormal, Vector2.ZERO)
    }

    shape.contours.forEach {
        val points = it.adaptivePositions(distanceTolerance)

        val normals = (0 until points.size).map {
            (points[mod(it+1, points.size)]-points[mod(it-1, points.size)]).safeNormalized
        }

        val forward = Vector3(0.0, 0.0, depth)
        val base = Vector3(0.0, 0.0, front)

        (points zip normals).zipWithNext().forEach { (left, right) ->
            val lnormal = left.second.perpendicular.vector3()
            val rnormal = right.second.perpendicular.vector3()

            writer(left.first.vector3() + base, lnormal, Vector2.ZERO)
            writer(right.first.vector3() + base, rnormal, Vector2.ZERO)
            writer(right.first.vector3() + base + forward, rnormal, Vector2.ZERO)

            writer(right.first.vector3() + base + forward, rnormal, Vector2.ZERO)
            writer(left.first.vector3() + base + forward, lnormal, Vector2.ZERO)
            writer(left.first.vector3() + base, lnormal, Vector2.ZERO)
        }
    }
}

private val Vector2.safeNormalized: Vector2
    get() {

        return if (length > 0.0001) {
            normalized
        } else {
            Vector2.ZERO
        }

    }

/**
 * @suppress
 */
private fun sample() {
    val shape = Circle(100.0, 100.0, 200.0).shape
    val vbo = meshVertexBuffer(400)

    val vertexCount = vbo.put {
        extrudeShape(shape, 0.0, 10.0, 0.05, bufferWriter(this))
    }
}