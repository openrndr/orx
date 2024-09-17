package org.openrndr.extra.mesh.noise

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IMeshData
import org.openrndr.extra.mesh.IVertexData
import org.openrndr.math.Vector3
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generate a uniformly distributed barycentric coordinate
 * @param random a random number generator
 */
fun uniformBarycentric(random: Random = Random.Default): Vector3 {
    val u = random.nextDouble()
    val v = random.nextDouble()
    val su0 = sqrt(u)
    val b0 = 1.0 - su0
    val b1 = v * su0
    return Vector3(b0, b1, 1.0 - b0 - b1)
}

/**
 * Generate a uniformly distributed point that lies inside this [IIndexedPolygon]
 * @param vertexData vertex data used to resolve positions
 * @param random a random number generator
 */
fun IIndexedPolygon.uniform(vertexData: IVertexData, random: Random = Random.Default): Vector3 {
    require(positions.size == 3) { "polygon must be a triangle"}

    val x = vertexData.positions.slice(positions)
    val b = uniformBarycentric(random)
    return x[0] * b.x + x[1] * b.y + x[2] * b.z
}

internal fun IIndexedPolygon.area(vertexData: IVertexData): Double {
    require(positions.size == 3) { "polygon must be a triangle"}
    val x = vertexData.positions.slice(positions)
    val u = x[1] - x[0]
    val v = x[2] - x[0]
    return u.areaBetween(v) / 2.0
}

/**
 * Generate points on the surface described by the mesh data
 */
fun IMeshData.uniform(count: Int, random: Random = Random.Default): List<Vector3> {
    val triangulated = triangulate()
    val result = mutableListOf<Vector3>()
    val totalArea = triangulated.polygons.sumOf { it.area(vertexData) }
    val randoms = (0 until count).map {
        random.nextDouble(totalArea)
    }.sorted()

    var idx = 0
    var sum = 0.0
    for (t in triangulated.polygons) {
        sum += t.area(vertexData)
        while (idx <= randoms.lastIndex && sum > randoms[idx]) {
            result.add(t.uniform(vertexData, random))
            idx++
        }
    }
    return result
}