package org.openrndr.extra.objloader

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.max
import kotlin.math.min

class Polygon(
    val positions: Array<Vector3> = emptyArray(),
    val normals: Array<Vector3> = emptyArray(),
    val textureCoords: Array<Vector2> = emptyArray()
) {
    fun transform(t: Matrix44): Polygon {
        return Polygon(positions.map { (t * it.xyz1).div }.toTypedArray(), normals, textureCoords)
    }
}

class Box(val corner: Vector3, val width: Double, val height: Double, val depth: Double)

fun bounds(polygons: List<Polygon>): Box {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY

    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    var maxZ = Double.NEGATIVE_INFINITY

    polygons.forEach {
        it.positions.forEach {
            minX = min(minX, it.x)
            minY = min(minY, it.y)
            minZ = min(minZ, it.z)

            maxX = max(maxX, it.x)
            maxY = max(maxY, it.y)
            maxZ = max(maxZ, it.z)
        }
    }
    return Box(Vector3(minX, minY, minZ), maxX - minX, maxY - minY, maxZ - minZ)
}
