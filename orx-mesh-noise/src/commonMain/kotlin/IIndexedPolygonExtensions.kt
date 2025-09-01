package org.openrndr.extra.mesh.noise

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IVertexData


/**
 * Calculates the area of the triangular polygon.
 *
 * The method assumes that the polygon is a triangle and computes its area
 * using the cross product formula. The computed area is a positive value as it
 * represents the absolute area of the triangle.
 *
 * @param vertexData the vertex data containing positional information of the polygon vertices
 * @return the area of the triangle as a Double
 * @throws IllegalArgumentException if the polygon is not a triangle (i.e., does not have exactly 3 vertices)
 */
internal fun IIndexedPolygon.area(vertexData: IVertexData): Double {
    require(positions.size == 3) { "polygon must be a triangle" }
    val x = vertexData.positions.slice(positions)
    val u = x[1] - x[0]
    val v = x[2] - x[0]
    return u.areaBetween(v) / 2.0
}
