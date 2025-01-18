package org.openrndr.extra.mesh.noise

import org.openrndr.extra.mesh.IIndexedPolygon
import org.openrndr.extra.mesh.IMeshData
import org.openrndr.extra.mesh.IVertexData
import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.noise.uhash11
import org.openrndr.extra.noise.uhash1D
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
 * Generate a non-uniformly distributed barycentric coordinate
 * @param random a random number generator
 */
fun nonUniformBarycentric(weight0: Double, weight1: Double, weight2: Double, random: Random = Random.Default): Vector3 {
    val b = uniformBarycentric()
    var b0 = b.x / weight0
    var b1 = b.y / weight1
    var b2 = b.z / weight2
    val totalWeight = b0 + b1 + b2
    b0 /= totalWeight
    b1 /= totalWeight
    b2 /= totalWeight
    return Vector3(b0, b1, b2)
}

/**
 * Generate a uniformly distributed barycentric coordinate
 * @param random a random number generator
 */
fun hashBarycentric(seed: Int, x: Int): Vector3 {
    val u = fhash1D(seed, x)
    val v = fhash1D(seed, u.toRawBits().toInt() - x)


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
    require(positions.size == 3) { "polygon must be a triangle" }

    val x = vertexData.positions.slice(positions)
    val b = uniformBarycentric(random)
    return x[0] * b.x + x[1] * b.y + x[2] * b.z
}

/**
 * Computes a point within a triangle defined by the current indexed polygon. The point is determined
 * through non-uniform barycentric coordinates, which are influenced by the specified weights.
 *
 * @param vertexData the vertex data containing positions and other attributes
 * @param weight0 the weight associated with the first vertex of the triangle
 * @param weight1 the weight associated with the second vertex of the triangle
 * @param weight2 the weight associated with the third vertex of the triangle
 * @param random an optional random number generator used for generating the barycentric coordinates
 * @return a 3D vector representing a point within the triangle specified by the barycentric coordinates
 */
fun IIndexedPolygon.nonUniform(
    vertexData: IVertexData,
    weight0: Double,
    weight1: Double,
    weight2: Double,
    random: Random = Random.Default
): Vector3 {
    require(positions.size == 3) { "polygon must be a triangle" }

    val x = vertexData.positions.slice(positions)
    val b = nonUniformBarycentric(weight0, weight1, weight2, random)
    return x[0] * b.x + x[1] * b.y + x[2] * b.z
}

/**
 * Generate a uniformly distributed point that lies inside this [IIndexedPolygon]
 * @param vertexData vertex data used to resolve positions
 * @param random a random number generator
 */
fun IIndexedPolygon.hash(vertexData: IVertexData, seed: Int, x: Int): Vector3 {
    require(positions.size == 3) { "polygon must be a triangle" }

    val s = vertexData.positions.slice(positions)
    val b = hashBarycentric(seed, x)
    return s[0] * b.x + s[1] * b.y + s[2] * b.z
}

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

/**
 * Computes the weighted area of a triangular polygon by scaling its area with the average of the given weights.
 *
 * @param vertexData the vertex data containing position information of the polygon vertices
 * @param weight0 the weight associated with the first vertex of the polygon
 * @param weight1 the weight associated with the second vertex of the polygon
 * @param weight2 the weight associated with the third vertex of the polygon
 * @return the weighted area of the triangular polygon
 */
internal fun IIndexedPolygon.weightedArea(
    vertexData: IVertexData,
    weight0: Double,
    weight1: Double,
    weight2: Double
): Double {
    return area(vertexData) * (weight0 + weight1 + weight2) / 3.0
}

/**
 * Generates a list of uniformly distributed points on the surface of the given mesh.
 *
 * The method uses triangulation and computes areas of triangular polygons to ensure
 * uniform distribution of points across the surface.
 *
 * @param count the number of points to generate
 * @param random a random number generator instance, defaulting to [Random.Default]
 * @return a list of [Vector3] points uniformly distributed across the mesh surface
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

/**
 * Generate points on the surface described by the mesh data
 */
fun IMeshData.hash(count: Int, seed: Int, x: Int): List<Vector3> {
    val triangulated = triangulate()
    val result = mutableListOf<Vector3>()
    val totalArea = triangulated.polygons.sumOf { it.area(vertexData) }
    val randoms = (0 until count).map {
        Pair(x + it, fhash1D(seed, x + it) * totalArea)
    }.sortedBy { it.second }

    var idx = 0
    var sum = 0.0
    for (t in triangulated.polygons) {
        sum += t.area(vertexData)
        while (idx <= randoms.lastIndex && sum > randoms[idx].second) {
            result.add(t.hash(vertexData, seed xor 0x7f7f7f, randoms[idx].first))
            idx++
        }
    }
    return result
}