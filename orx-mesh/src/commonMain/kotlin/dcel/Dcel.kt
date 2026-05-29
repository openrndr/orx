package org.openrndr.extra.mesh.dcel

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
class HalfEdge(
    var face: Int,
    var vertex: Int,
    var nextEdge: Int,
    var prevEdge: Int,
    var otherEdge: Int,
    var attributes: IntArray
)

@Serializable
class Face(
    var edge: Int,
    var holeEdges: IntArray = IntArray(0),
)

@Serializable
class Vertex(var position: Vector3, var edge: Int)

@JvmInline
value class FaceList(val faceIds: List<Int>): List<Int> by faceIds

@JvmInline
value class FaceSet(val faceIds: Set<Int>): Set<Int> by faceIds

@JvmInline
value class EdgeList(val edgeIds: List<Int>): List<Int> by edgeIds

@JvmInline
value class EdgeSet(val edgeIds: Set<Int>): Set<Int> by edgeIds

@JvmInline
value class VertexList(val vertexIds: List<Int>): List<Int> by vertexIds

@JvmInline
value class VertexSet(val vertexIds: Set<Int>): Set<Int> by vertexIds


enum class DCELAttributes(val index: Int) {
    COLOR(0),
    TEXTURE_COORDINATE(1),
    NORMAL(2),
    TANGENT(3),
    BITANGENT(4)
}

@Serializable
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