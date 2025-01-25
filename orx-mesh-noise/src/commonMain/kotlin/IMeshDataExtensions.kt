package org.openrndr.extra.mesh.noise

import org.openrndr.extra.mesh.*
import org.openrndr.extra.noise.fhash1D
import org.openrndr.extra.noise.hammersley.hammersley2D
import org.openrndr.extra.noise.rsequence.rSeq2D
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.random.Random

/**
 * Represents a type alias for a function that processes data associated with a polygon and vertex data,
 * producing a result of type [T].
 *
 * This function takes the following parameters:
 * - `polygon`: An instance of [IIndexedPolygon] representing the polygon in 3D space.
 * - `vertexData`: An instance of [IVertexData] containing the associated vertex attributes,
 *   such as positions, texture coordinates, normals, etc.
 * - `barycentric`: A [Barycentric] representation defining the barycentric coordinates for interpolation.
 */
typealias SampleFunction<T> = (polygon: IIndexedPolygon, vertexData: IVertexData, barycentric: Barycentric) -> T

/**
 * A lambda function that computes the 3D position within an indexed polygon given vertex data
 * and barycentric coordinates.
 *
 * This function utilizes the `position` method of an `IIndexedPolygon` instance to calculate
 * a specific point in the 3D space based on the barycentric weights and vertex positions.
 *
 * @property positionSampleFunction A functional instance of type `SampleFunction` where:
 * - `t` is the `IIndexedPolygon`, representing the polygon.
 * - `v` is the `IVertexData`, representing the vertex attributes.
 * - `b` is the barycentric coordinate used for interpolation.
 * The lambda computes the resulting position by invoking `t.position(v, b)`.
 */
val positionSampleFunction: SampleFunction<Vector3> =
    { t: IIndexedPolygon, v: IVertexData, b: Barycentric -> t.position(v, b) }

/**
 * A lambda function that extracts a [Point] from a given [IIndexedPolygon], [IVertexData], and [Barycentric] coordinate.
 *
 * This function utilizes the `point` method of [IIndexedPolygon] to compute the [Point] in 3D space.
 *
 * @property t An instance of [IIndexedPolygon] representing the polygon from which the point is derived.
 * @property v An instance of [IVertexData] representing the vertex data associated with the polygon.
 * @property b An instance of [Barycentric] representing the barycentric coordinates used to interpolate the point.
 * @return A [Point] representing the interpolated 3D position and attributes at the specified barycentric coordinate.
 */
val pointSampleFunction: SampleFunction<Point> = { t: IIndexedPolygon, v: IVertexData, b: Barycentric -> t.point(v, b) }

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
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { Vector2(random.nextDouble(), random.nextDouble()) },
        identityWeightFunction,
        positionSampleFunction
    )
}

/**
 * Generates a uniformly distributed set of points over the surface of the mesh.
 *
 * The method creates a specific number of points distributed across the mesh,
 * based on the given count, using random values to determine their positions.
 *
 * @param count The number of points to generate and distribute across the mesh.
 * @param random The random number generator used for creating the distribution of points (default: [Random.Default]).
 * @return A list of [Point] instances representing the uniformly distributed points across the mesh.
 */
fun IMeshData.uniformPoints(count: Int, random: Random = Random.Default): List<Point> {
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { Vector2(random.nextDouble(), random.nextDouble()) },
        identityWeightFunction,
        pointSampleFunction
    )
}

/**
 * Generates a list of points within the mesh using a non-uniform distribution.
 * The points are sampled according to a provided weight function, where each point
 * is influenced by the weights of the vertices of the triangulated polygons.
 *
 * @param count the number of points to generate within the mesh
 * @param random the random number generator used for sampling; defaults to [Random.Default]
 * @param weightFunction a function that calculates the weight for a given vertex of a polygon
 * @return a list of [Vector3] instances representing the sampled points within the mesh
 */
fun IMeshData.nonuniform(count: Int, random: Random = Random.Default, weightFunction: WeightFunction): List<Vector3> {
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { Vector2(random.nextDouble(), random.nextDouble()) },
        weightFunction,
        positionSampleFunction
    )
}

/**
 * Generates a list of non-uniformly distributed points on the mesh based on a specified weight function.
 *
 * @param count The number of points to generate.
 * @param random A [Random] instance used to generate random values for the distribution. Defaults to [Random.Default].
 * @param weightFunction A function used to calculate weighting for vertices in a triangle, affecting the distribution of points.
 * @return A list of [Point] instances representing non-uniformly distributed points on the mesh.
 */
fun IMeshData.nonuniformPoints(
    count: Int,
    random: Random = Random.Default,
    weightFunction: WeightFunction
): List<Point> {
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { Vector2(random.nextDouble(), random.nextDouble()) },
        weightFunction,
        pointSampleFunction
    )
}

/**
 * Generates a list of 3D points distributed on a mesh surface using a nonuniform sampling strategy
 * with Hammersley sequence and a custom weight function.
 *
 * The Hammersley sequence is used to generate an initial set of 2D points that are mapped to the
 * surface of the mesh based on the provided weight function. Random numbers are also utilized
 * to introduce variability in the sampling process.
 *
 * @param count The number of points to sample on the mesh surface.
 * @param random An optional random number generator used for shuffling the Hammersley points
 * and generating random values for the sampling. Defaults to `Random.Default`.
 * @param weightFunction A function that determines the weight of individual components of the mesh,
 * influencing the distribution of generated points.
 * @return A list of 3D points (`Vector3`) distributed across the mesh surface based on the provided
 * weight function and sampling method.
 */
fun IMeshData.nonuniformHammersley(
    count: Int,
    shuffle: Boolean = true,
    random: Random = Random.Default,
    weightFunction: WeightFunction
): List<Vector3> {
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { hammersley2D(it, count) }.let { if (shuffle) it.shuffled(random) else it },
        weightFunction,
        positionSampleFunction
    )
}

/**
 * Generates a non-uniform sequence of 3D points based on the provided weight function, using
 * a combination of randomized values and the R2 low-discrepancy quasirandom sequence.
 *
 * @param count The number of points to generate.
 * @param shuffle Determines whether the resulting R2 sequence should be shuffled. Default is true.
 * @param random The source of randomness used for shuffling and random value generation. Default is [Random.Default].
 * @param weightFunction A function that computes the weight for a given vertex data and indexed polygon. This influences
 *                       the distribution of points across the mesh.
 * @return A list of [Vector3] objects representing the generated sequence of 3D points.
 */
fun IMeshData.nonuniformRSeq(
    count: Int,
    shuffle: Boolean = true,
    random: Random = Random.Default,
    weightFunction: WeightFunction
): List<Vector3> {
    return nonuniformEx(
        (0 until count).map { random.nextDouble() },
        (0 until count).map { rSeq2D(it) }.let { if (shuffle) it.shuffled(random) else it },
        weightFunction,
        positionSampleFunction
    )
}

/**
 * Generates a list of samples by distributing random points across the mesh
 * using weighted areas of triangles and a provided sample function.
 *
 * @param T The type of the sample produced by the sample function.
 * @param randomsUnsorted A list of random values in the range [0, 1), which are used to
 *        distribute points across the mesh based on weighted triangle areas.
 * @param randomPoints A list of random 2D points in barycentric coordinates, used to
 *        determine the position of generated samples within a triangle.
 * @param weightFunction A function that calculates weights for vertices in a triangle,
 *        modifying the distribution of random points based on these weights.
 * @param sampleFunction A function that generates a sample of type [T] given a triangle
 *        (as an [IIndexedPolygon]), associated vertex data, and barycentric coordinates.
 * @return A list of samples of type [T], generated by applying the sample function
 *         to distributed random points across the mesh.
 */
fun <T> IMeshData.nonuniformEx(
    randomValuesUnsorted: List<Double>,
    randomPoints: List<Vector2>,
    weightFunction: WeightFunction,
    sampleFunction: (IIndexedPolygon, IVertexData, Barycentric) -> T
): List<T> {
    val triangulated = triangulate()
    val defaultWeights = doubleArrayOf(1.0, 1.0, 1.0)
    val result = ArrayList<T>(randomPoints.size)

    // Helper function to calculate triangle weights
    fun calculateWeights(triangle: IIndexedPolygon): DoubleArray =
        if (weightFunction !== identityWeightFunction)
            doubleArrayOf(
                weightFunction(vertexData, triangle, 0),
                weightFunction(vertexData, triangle, 1),
                weightFunction(vertexData, triangle, 2)
            )
        else defaultWeights

    // Helper function to calculate total area
    fun calculateTotalArea(): Double =
        triangulated.polygons.sumOf { triangle ->
            val weights = calculateWeights(triangle)
            triangle.area(vertexData) * weights.sum()
        }

    val totalArea = calculateTotalArea()
    val randomValues = randomValuesUnsorted.sorted().map { it * totalArea }

    var sum = 0.0
    var idx = 0

    // Iterate through each triangle in the triangulated mesh
    for (triangle in triangulated.polygons) {
        val weights = calculateWeights(triangle)
        sum += triangle.area(vertexData) * weights.sum()

        // Distribute random points across the triangle
        while (idx <= randomValues.lastIndex && sum > randomValues[idx]) {
            val barycentricCoords = if (weightFunction !== identityWeightFunction) {
                weightBarycentric(
                    uniformBarycentric(randomPoints[idx].x, randomPoints[idx].y),
                    weights[0], weights[1], weights[2]
                )
            } else {
                uniformBarycentric(randomPoints[idx].x, randomPoints[idx].y)
            }
            result.add(sampleFunction(triangle, vertexData, barycentricCoords))
            idx++
        }
    }

    return result
}

/**
 * Generate points on the surface described by the mesh data
 */
fun IMeshData.hash(count: Int, seed: Int, x: Int): List<Vector3> {
    val randoms = (0 until count).map {
        fhash1D(seed, x + it)
    }
    val randomPoints = (0 until count).map {
        val x = x + it
        val u = fhash1D(seed xor 0x7f7f7f7f, x)
        val v = fhash1D(seed xor 0x7f7f7f7f, u.toRawBits().toInt() - x)
        Vector2(u, v)
    }

    return nonuniformEx(
        randoms, randomPoints, identityWeightFunction, positionSampleFunction
    )
}