package org.openrndr.extra.mesh.dcel

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import org.openrndr.events.Event

@Serializable
class HalfEdge(
    var face: Int,
    var vertex: Int,
    var nextEdge: Int,
    var prevEdge: Int,
    var otherEdge: Int,
    var attributes: IntArray
) {
    fun copy(): HalfEdge = HalfEdge(face, vertex, nextEdge, prevEdge, otherEdge, attributes.copyOf())
}

@Serializable
class Face(
    var edge: Int,
    var holeEdges: IntArray = IntArray(0),
) {
    fun copy(): Face = Face(edge, holeEdges.copyOf())
}




@Serializable
class Vertex(var position: Vector3, var edge: Int) {
    fun copy(): Vertex = Vertex(position, edge)
}



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

    fun copy(): Dcel = Dcel().also {
        it.halfEdges.addAll(halfEdges.map { it.copy() })
        it.faces.addAll(faces.map { it.copy() })
        it.vertices.addAll(vertices.map { it.copy() })
        it.colors.addAll(colors)
        it.textureCoordinates.addAll(textureCoordinates)
        it.normals.addAll(normals)
        it.tangents.addAll(tangents)
        it.bitangents.addAll(bitangents)
    }

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

class DcelEvents {
    val vertexAdded = Event<Int>("vertex-added")
    val edgeAdded = Event<Int>("edge-added")
    val faceAdded = Event<Int>("face-added")
    val vertexRemoved = Event<Int>("vertex-removed")
    val edgeRemoved = Event<Int>("edge-removed")
    val faceRemoved = Event<Int>("face-removed")
    val vertexModified = Event<Int>("vertex-modified")
    val edgeModified = Event<Int>("edge-modified")
    val faceModified = Event<Int>("face-modified")

    companion object {
        val DEFAULT = DcelEvents()
    }
}