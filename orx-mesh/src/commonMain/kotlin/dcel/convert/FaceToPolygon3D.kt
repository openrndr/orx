package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.shapes.polygon.Polygon2D
import org.openrndr.extra.shapes.polygon.Polygon3D

fun Dcel.faceToPolygon3D(faceId: Int): Polygon3D {
    val vertices = verticesForFace(faceId)
    return Polygon3D(vertices.map {
        this.vertices[it].position
    })
}

/**
 * Converts a face from the DCEL structure into a 2D polygon representation.
 *
 * @param faceId the identifier of the face within the DCEL to be converted.
 * @return a [Polygon2D] object representing the 2D polygon corresponding to the specified face.
 */
fun Dcel.faceToPolygon2D(faceId: Int): Polygon2D {
    val vertices = verticesForFace(faceId)
    return Polygon2D(vertices.map {
        this.vertices[it].position.xy
    })
}