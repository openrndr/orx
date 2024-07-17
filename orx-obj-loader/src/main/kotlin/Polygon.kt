package org.openrndr.extra.objloader

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.max
import kotlin.math.min

/**
 * A 3D Polygon
 *
 * @property positions Vertex 3D positions
 * @property normals Vertex 3D normals
 * @property textureCoords Vertex 2D texture coordinates
 * @constructor Create empty 3D Polygon
 */
class Polygon(
    val positions: Array<Vector3> = emptyArray(),
    val normals: Array<Vector3> = emptyArray(),
    val textureCoords: Array<Vector2> = emptyArray()
) {
    fun transform(t: Matrix44): Polygon {
        return Polygon(positions.map { (t * it.xyz1).div }.toTypedArray(), normals, textureCoords)
    }
}

/**
 * A 3D Box defined by an anchor point ([corner]), [width], [height] and [depth].
 */
class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double)

/**
 * Calculates the 3D bounding box of a list of [Polygon].
 */
fun bounds(polygons: List<Polygon>): Box {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY

    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    polygons.forEach {
        it.positions.forEach { pos ->
            minX = min(minX, pos.x)
            minY = min(minY, pos.y)
            minZ = min(minZ, pos.z)

            maxX = max(maxX, pos.x)
            maxY = max(maxY, pos.y)
            maxZ = max(maxZ, pos.z)
        }
    }
    return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
}
