package org.openrndr.extra.mesh.dcel.navigate

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.math.Vector3

val HalfEdge.isBoundary: Boolean
    get() = otherEdge == -1

context(dcel: Dcel)
val HalfEdge.length: Double
    get() {
        if (vertex == -1) return 0.0
        if (nextEdge == -1) return 0.0

        val pos = dcel.vertices[this.vertex].position
        val nextPos = dcel.vertices[dcel.halfEdges[this.nextEdge].vertex].position
        return (nextPos.distanceTo(pos))
    }

context(dcel: Dcel)
val HalfEdge.start: Vector3
    get() = dcel.vertices[this.vertex].position

context(dcel: Dcel)
val HalfEdge.end: Vector3
    get() = dcel.vertices[dcel.halfEdges[this.nextEdge].vertex].position

context(dcel: Dcel)
val HalfEdge.direction: Vector3
    get() = (end - start).normalized

context(dcel: Dcel)
val HalfEdge.realFace: Face
    get() = dcel.faces[this.face]