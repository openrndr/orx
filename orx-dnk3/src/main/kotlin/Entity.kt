package org.openrndr.extra.dnk3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Matrix44


class Geometry(val vertexBuffers: List<VertexBuffer>,
               val indexBuffer: IndexBuffer?,
               val primitive: DrawPrimitive,
               val offset: Int,
               val vertexCount: Int) {

    override fun toString(): String {
        return "Geometry(vertexBuffers: $vertexBuffers, indexBuffers: $indexBuffer, primitive: $primitive, offset: $offset, vertexCount: $vertexCount)"
    }

}

val DummyGeometry = Geometry(emptyList(), null, DrawPrimitive.TRIANGLES, 0, 0)

sealed class Entity

class MeshPrimitive(var geometry: Geometry, var material: Material) {

    override fun toString(): String {
        return "MeshPrimitive(geometry: $geometry, material: $material)"
    }

}

class MeshPrimitiveInstance(val primitive: MeshPrimitive, val instances: Int, val attributes: List<VertexBuffer>)

abstract class MeshBase(var primitives: List<MeshPrimitive>) : Entity()
class Mesh(primitives: List<MeshPrimitive>) : MeshBase(primitives) {
    override fun toString(): String {
        return "Mesh(primitives: $primitives)"
    }
}

class SkinnedMesh(primitives: List<MeshPrimitive>,
                  val joints: List<SceneNode>,
                  val skeleton: SceneNode,
                  val inverseBindMatrices: List<Matrix44>
) : MeshBase(primitives)

class InstancedMesh(primitives: List<MeshPrimitive>,
                    var instances: Int,
                    var attributes: List<VertexBuffer>) : MeshBase(primitives)


class Fog : Entity() {
    var color: ColorRGBa = ColorRGBa.WHITE
    var end: Double = 100.0
}

abstract class Light : Entity() {
    var color: ColorRGBa = ColorRGBa.WHITE
}

