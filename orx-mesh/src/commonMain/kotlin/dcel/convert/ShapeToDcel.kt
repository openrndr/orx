package org.openrndr.extra.mesh.dcel.convert

import org.openrndr.extra.mesh.IndexedPolygon
import org.openrndr.extra.mesh.MeshData
import org.openrndr.extra.mesh.VertexData
import org.openrndr.extra.mesh.dcel.Dcel
import org.openrndr.extra.mesh.dcel.Face
import org.openrndr.extra.mesh.dcel.HalfEdge
import org.openrndr.extra.mesh.dcel.Vertex
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Shape
import org.openrndr.shape.Winding
import org.openrndr.shape.triangulate

fun shapeToDcel(shape: Shape, distanceTolerance: Double): Dcel {
    val baseTriangles = triangulate(shape, distanceTolerance)

    val positions = baseTriangles.map { Vector3(it.x, it.y, 0.0) }
    val polygons = (0 until positions.size / 3).map {
        IndexedPolygon(
            positions = listOf(it * 3, it * 3 + 1, it * 3 + 2),
            textureCoords = emptyList(),
            colors = emptyList(),
            normals = emptyList(),
            tangents = emptyList(),
            bitangents = emptyList()
        )
    }

    val meshData = MeshData(VertexData(positions = positions), polygons)
    return meshData.toDcel()
}

fun polygonToDcel(outer: List<Vector2>, holes: List<List<Vector2>>): Dcel {
    val dcel = Dcel()
    val allPoints = outer + holes.flatten()

    dcel.vertices.addAll(allPoints.mapIndexed { index, it -> Vertex(it.xy0, index) })


    val loopEdges = mutableListOf<Int>()
    for (loop in listOf(outer) + holes) {
        val loopStart = dcel.halfEdges.size

        loopEdges.add(loopStart)
        for (i in 0 until loop.size) {
            dcel.halfEdges.add(
                HalfEdge(
                    0,
                    loopStart + i,
                    loopStart + (i + 1).mod(loop.size),
                    loopStart + (i + loop.size - 1).mod(loop.size),
                    -1,
                    IntArray(0)
                )
            )
        }

    }
    dcel.faces.add(Face(loopEdges.first(), loopEdges.drop(1).toIntArray()))

    return dcel
}

fun shapeToDcelNoTriangulation(shape: Shape, distanceTolerance: Double): Dcel {
    val compounds = shape.splitCompounds()

    val outer = shape.outline.sampleLinear(distanceTolerance).segments.map { it.start }
    val holes = shape.contours
        .filter { it.winding == Winding.COUNTER_CLOCKWISE }
        .map { it.sampleLinear(distanceTolerance).segments.map { it.start } }

    return polygonToDcel(outer, holes)
}