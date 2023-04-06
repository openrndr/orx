package org.openrndr.extra.meshgenerators

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
 * Creates a [VertexBuffer] that is suited for holding meshes.
 * Each vertex contains:
 * - `position` (vec3)
 * - `normal` (vec3)
 * - `textureCoordinate` (vec2)
 */
fun meshVertexBuffer(size: Int): VertexBuffer {
    return vertexBuffer(vertexFormat {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, size)
}

/**
 * Creates a [VertexBuffer] that is suited for holding meshes.
 * Each vertex contains:
 * - `position` (vec3)
 * - `normal` (vec3)
 * - `textureCoordinate` (vec2)
 * - `color` (vec4)
 */
fun meshVertexBufferWithColor(size: Int): VertexBuffer {
    return vertexBuffer(vertexFormat {
        position(3)
        normal(3)
        textureCoordinate(2)
        color(4)
    }, size)
}


@Deprecated("binary compatibility only")
fun extrudeShape(shape: Shape, front: Double, back: Double, distanceTolerance: Double = 0.5, writer: VertexWriter) {
    extrudeShape(shape, front, back, distanceTolerance = distanceTolerance, flipNormals = false, writer = writer)
}


/**
 * Extrudes a [Shape] from its triangulations
 *
 * @param baseTriangles triangle vertices for the caps
 * @param contours contour vertices for the sides
 * @param front the `z` position of the front
 * @param back the `z` position of the back
 * @param frontScale scale factor for the front cap
 * @param backScale scale factor for the back cap
 * @param frontCap add a front cap if true
 * @param backCap add a back cap if true
 * @param sides add the sides if true
 * @param writer the vertex writer function
 */

fun extrudeShape(baseTriangles: List<Vector2>,
                 contours: List<List<Vector2>>,
                 front: Double,
                 back: Double,
                 frontScale: Double = 1.0,
                 backScale: Double = 1.0,
                 frontCap: Boolean = true,
                 backCap: Boolean = true,
                 sides: Boolean = true,
                 flipNormals: Boolean = false,
                 writer: VertexWriter) {

    val depth = back - front
    val flip = if (flipNormals) 1.0 else -1.0

    run {
        val normal = Vector3(0.0, 0.0, depth).normalized * flip
        val negativeNormal = normal * -1.0

        if (frontCap) {
            baseTriangles.reversed().forEach {
                writer((it * frontScale).vector3(z = front), normal, Vector2.ZERO)
            }
        }
        if (backCap) {
            baseTriangles.forEach {
                writer((it * backScale).vector3(z = back), negativeNormal, Vector2.ZERO)
            }
        }
    }

    if (sides) {
        contours.forEach {
            val points = it

            val normals = (points.indices).map { index ->
                (points[mod(index + 1, points.size)] - points[mod(index - 1, points.size)]).safeNormalized * -flip
            }
            val forward = Vector3(0.0, 0.0, depth)
            val base = Vector3(0.0, 0.0, front)

            var offset = 0.0
            (points zip normals).zipWithNext().forEach { (left, right) ->

                val width = right.first.distanceTo(left.first)

                val frontRight = (right.first * frontScale).xy0 + base
                val frontLeft = (left.first * frontScale).xy0 + base


                val backRight = (right.first * backScale).xy0 + base + forward
                val backLeft = (left.first * backScale).xy0 + base + forward

                val height = frontRight.distanceTo(backRight)


                val backRightUV = Vector2(offset + width, 0.0)
                val backLeftUV = Vector2(offset, 0.0)

                val frontLeftUV = Vector2(offset, height)
                val frontRightUV = Vector2(offset + width, height)


                val lnormal = (frontLeft - backLeft).normalized.cross(left.second.xy0)
                val rnormal = (frontRight - backRight).normalized.cross(right.second.xy0)

                writer(frontLeft, lnormal, frontLeftUV)
                writer(frontRight, rnormal, frontRightUV)
                writer(backRight, rnormal, backRightUV)

                writer(backRight, rnormal, backRightUV)
                writer(backLeft, lnormal, backLeftUV)
                writer(frontLeft, lnormal, frontLeftUV)

                offset += width
            }
        }
    }
}


/**
 * extrudes a [shape] by triangulating it and creating side- and cap geometry
 */
fun extrudeShape(shape: Shape,
                 front: Double,
                 back: Double,
                 frontScale: Double = 1.0,
                 backScale: Double = 1.0,
                 frontCap: Boolean = true,
                 backCap: Boolean = true,
                 sides: Boolean = true,
                 distanceTolerance: Double = 0.5,
                 flipNormals: Boolean = false, writer: VertexWriter) {
    val baseTriangles = triangulate(shape, distanceTolerance)
    val points = shape.contours.map { it.adaptivePositions(distanceTolerance) }

    extrudeShape(
            baseTriangles = baseTriangles,
            contours = points,
            front = front,
            back = back,
            frontScale = frontScale,
            backScale = backScale,
            frontCap = frontCap,
            backCap = backCap,
            sides = sides,
            flipNormals = flipNormals,
            writer = writer
    )
}

fun extrudeShapes(shapes: List<Shape>,
                  front: Double,
                  back: Double,
                  frontScale: Double = 1.0,
                  backScale: Double = 1.0,
                  frontCap: Boolean = true,
                  backCap: Boolean = true,
                  sides: Boolean = true,
                  distanceTolerance: Double = 0.5,
                  flipNormals: Boolean = false, writer: VertexWriter) {
    shapes.forEach {
        extrudeShape(
                shape = it,
                front = front,
                back = back,
                frontScale = frontScale,
                backScale = backScale,
                frontCap = frontCap,
                backCap = backCap,
                sides = sides,
                distanceTolerance = distanceTolerance,
                flipNormals = flipNormals,
                writer = writer
        )
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
