package org.openrndr.extra.mesh.dcel

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class HalfEdge(
    var face: Int,
    var vertex: Int,
    var nextEdge: Int,
    var prevEdge: Int,
    var otherEdge: Int,
    var attributes: IntArray
)


class Face(
    var edge: Int,
    var holeEdges: IntArray = IntArray(0),
)

class Vertex(var position: Vector3, var edge: Int)


enum class DCELAttributes(val index: Int) {
    COLOR(0),
    TEXTURE_COORDINATE(1),
    NORMAL(2),
    TANGENT(3),
    BITANGENT(4)
}

class Dcel {
    var halfEdges = mutableListOf<HalfEdge>()
    var faces = mutableListOf<Face>()
    var vertices = mutableListOf<Vertex>()

    var colors = mutableListOf<ColorRGBa>()
    var textureCoordinates = mutableListOf<Vector2>()
    var normals = mutableListOf<Vector3>()
    var tangents = mutableListOf<Vector3>()
    var bitangents = mutableListOf<Vector3>()

}

class Point(
    val position: Vector3,
    val textureCoordinate: Vector2? = null,
    val color: ColorRGBa? = null,
    val normal: Vector3? = null,
    val tangent: Vector3? = null,
    val bitangent: Vector3? = null,
    val attributes: IntArray = IntArray(0)
)