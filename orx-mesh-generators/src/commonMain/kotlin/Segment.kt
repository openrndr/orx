package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment

/**
 * Converts a [Segment] into a [VertexBuffer]. It takes
 * [samples] samples of the Segment. The thickness can
 * be constant: `segment.toMesh(50) { 10.0 }`
 * or variable: `segment.toMesh(50) { t -> 10.0 * t }`
 * or even:     `segment.toMesh(30) { t -> cos(t * 3.14159) * 10 + 5 }`
 */
fun Segment.toMesh(
    samples: Int,
    thickness: (Double) -> Double = { 5.0 }
): VertexBuffer {
    val vb = vertexBuffer(vertexFormat {
        position(3)
        textureCoordinate(2)
    }, samples * 2)

    vb.put {
        repeat(samples) {
            val t = it / (samples - 1.0)
            val pos = position(t)
            val n = normal(t) * 0.5
            write((pos + n * thickness(t)).xy0)
            write(Vector2(t, 0.0))
            write((pos - n * thickness(t)).xy0)
            write(Vector2(t, 1.0))
        }
    }
    return vb
}
