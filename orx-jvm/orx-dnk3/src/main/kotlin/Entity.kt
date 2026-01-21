package org.openrndr.extra.dnk3

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.IndexBuffer
import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Matrix44
import org.openrndr.math.transforms.perspective
import org.openrndr.shape.Path3D

class Geometry(val vertexBuffers: List<VertexBuffer>,
               val indexBuffer: IndexBuffer?,
               val primitive: DrawPrimitive,
               val offset: Int,
               val vertexCount: Int) {

    override fun toString(): String {
        return "Geometry(vertexBuffers: $vertexBuffers, indexBuffers: $indexBuffer, primitive: $primitive, offset: $offset, vertexCount: $vertexCount)"
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + primitive.ordinal.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + vertexCount.hashCode()
        return result
    }
}

val DummyGeometry = Geometry(emptyList(), null, DrawPrimitive.TRIANGLES, 0, 0)

sealed class Entity {
    var userData: Any? = null
    var update: (() -> Unit)? = null
}
class MeshPrimitive(var geometry: Geometry, var material: Material) {
    override fun toString(): String {
        return "MeshPrimitive(geometry: $geometry, material: $material)"
    }

    override fun hashCode(): Int {
        var result = geometry.hashCode()
        result = 31 * result + material.hashCode()
        return result
    }
}

class MeshPrimitiveInstance(val primitive: MeshPrimitive, val instances: Int, val attributes: List<VertexBuffer>)

class PathMesh(var paths: MutableList<Path3D>, var material: Material, var weight: Double) : Entity() {
    override fun toString(): String {
        return "PathMesh(paths=$paths)"
    }

    override fun hashCode(): Int {
        return paths.hashCode()
    }
}

abstract class MeshBase(var primitives: List<MeshPrimitive>) : Entity()
class Mesh(primitives: List<MeshPrimitive>) : MeshBase(primitives) {
    override fun toString(): String {
        return "Mesh(primitives: $primitives)"
    }

    override fun hashCode(): Int {
        return primitives.hashCode()
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


data class Fog(var color: ColorRGBa = ColorRGBa.WHITE, var end: Double = 100.0) : Entity()

abstract class Light : Entity() {
    var color: ColorRGBa = ColorRGBa.WHITE
}

abstract class Camera : Entity() {
    abstract val projectionMatrix: Matrix44
    abstract val viewMatrix: Matrix44
}

abstract class CubemapProbe : Entity() {
    open val projectionMatrix: Matrix44
        get() {
            return perspective(90.0, 1.0, 0.1, 150.0)
        }
    var dirty = true
}

class IrradianceProbe : CubemapProbe() {
    override fun hashCode(): Int {
        return true.hashCode()
    }
}