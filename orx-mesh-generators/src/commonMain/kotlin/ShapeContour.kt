package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.shape.ShapeContour

/**
 * Converts a [ShapeContour] into a [VertexBuffer]. It takes
 * [samples] samples of the Segment. The thickness can
 * be constant: `contour.toMesh(50) { 10.0 }`
 * or variable: `contour.toMesh(50) { t -> 10.0 * t }`
 * or even:     `contour.toMesh(30) { t -> cos(t * 3.14159) * 10 + 5`
 */
fun ShapeContour.toMesh(
    samples: Int,
    thickness: (Double) -> Double = { 5.0 }
): VertexBuffer {
    val actualSamples = samples + if (closed) 1 else 0
    val vb = vertexBuffer(vertexFormat {
        position(3)
        textureCoordinate(2)
    }, actualSamples * 2)

    vb.put {
        repeat(actualSamples) {
            val t = it / samples.toDouble()
            val t1 = t % 1.0
            val pos = position(t1)
            val n = normal(t1) * 0.5
            write((pos + n * thickness(t1)).xy0)
            write(Vector2(t, 0.0))
            write((pos - n * thickness(t1)).xy0)
            write(Vector2(t, 1.0))
        }
    }
    return vb
}
