package org.openrndr.extra.mesh.dcel.modify

import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.query.vertexNeighbors
import org.openrndr.math.Vector3

fun Dcel.vertexSetRelax(vertexIds: Set<Int>) {
    val newPositions = mutableMapOf<Int, Vector3>()
    val newPositions2 = mutableMapOf<Int, Vector3>()

    fun laplacian(v: Int, vertexPosition:(Int)->Vector3, p: Vector3): Vector3 {
        var sum = Vector3.ZERO
        val neighbors = vertexNeighbors(v)
        for (n in neighbors) {
            sum += vertexPosition(n)
        }
        return sum / (neighbors.size.toDouble()) - p
    }
    val gamma = 0.5
    val mu = -0.53
    for (v in vertexIds) {
        newPositions[v] = vertices[v].position +
                laplacian(v,
                    {vertices[it].position},
                    vertices[v].position) * gamma
    }
    for (v in vertexIds) {
        newPositions2[v] = newPositions[v]!! +
                laplacian(v, { newPositions2[it] ?: vertices[it].position},
                    newPositions[v]!!) * mu
    }


    for ((v, newPosition) in newPositions2) {
        vertices[v].position = newPosition
    }
}