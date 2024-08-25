package org.openrndr.extra.dnk3.tools

import org.openrndr.draw.*
import org.openrndr.extra.dnk3.Geometry
import org.openrndr.extra.dnk3.Mesh
import org.openrndr.extra.dnk3.MeshPrimitive
import org.openrndr.extra.dnk3.PBRMaterial
import java.nio.ByteBuffer
import java.nio.ByteOrder


private data class CollapseItem(val vertexFormats: List<VertexFormat>,
                                val drawPrimitive: DrawPrimitive,
                                val hasIndexBuffer: Boolean)

fun Mesh.collapse() {
    val grouped = primitives.groupBy {
        CollapseItem(it.geometry.vertexBuffers.map { it.vertexFormat }, it.geometry.primitive, it.geometry.indexBuffer != null)
    }

    grouped.map {
        val vertexCount = it.value.sumOf { primitive ->
            primitive.geometry.vertexCount
        }

        val indexCount = if (it.key.hasIndexBuffer)
            it.value.sumOf { primitive ->
                primitive.geometry.indexBuffer?.indexCount ?: 0
            }
        else 0

        val collapsedVertices = it.key.vertexFormats.map {
            vertexBuffer(it, vertexCount)
        } + vertexBuffer(vertexFormat { attribute("fragmentID", VertexElementType.INT16) }, vertexCount)


        val fragmentBuffer = ByteBuffer.allocateDirect(vertexCount * 2)
        fragmentBuffer.order(ByteOrder.nativeOrder())

        for (i in 0 until collapsedVertices.size) {
            var offset = 0
            for (fromPrimitive in it.value) {
                val fromBuffer = fromPrimitive.geometry.vertexBuffers[i]

                val copy = ByteBuffer.allocateDirect(fromBuffer.vertexCount * fromBuffer.vertexFormat.size)
                copy.order(ByteOrder.nativeOrder())
                fromBuffer.read(copy)
                copy.rewind()

                collapsedVertices[i].write(copy, offset)
                offset += copy.capacity()

                for (v in 0 until fromBuffer.vertexCount) {
                    fragmentBuffer.putShort(fromPrimitive.material.fragmentID.toShort())
                }
            }
        }

        val collapsedIndices = if (it.key.hasIndexBuffer) indexBuffer(indexCount, IndexType.INT32) else null

        if (it.key.hasIndexBuffer) {
            var offset = 0
            val result = ByteBuffer.allocateDirect(4 * indexCount)
            result.order(ByteOrder.nativeOrder())

            for (fromPrimitive in it.value) {
                val fromBuffer = fromPrimitive.geometry.indexBuffer!!
                when (fromBuffer.type) {
                    IndexType.INT16 -> {
                        val copy = ByteBuffer.allocateDirect(fromBuffer.indexCount * 2)
                        fromBuffer.read(copy)
                        copy.rewind()
                        for (i in 0 until fromBuffer.indexCount) {
                            val index = (copy.getShort().toInt() and 0xffff) + offset
                            result.putInt(index)
                        }
                    }
                    IndexType.INT32 -> {
                        val copy = ByteBuffer.allocateDirect(fromBuffer.indexCount * 4)
                        fromBuffer.read(copy)
                        copy.rewind()
                        for (i in 0 until fromBuffer.indexCount) {
                            val index = copy.getInt() + offset
                            result.putInt(index)
                        }
                    }
                }
                offset += fromPrimitive.geometry.vertexCount
            }
        }

        val collapsedGeometry = Geometry(collapsedVertices, collapsedIndices, it.key.drawPrimitive, 0, if (collapsedIndices == null)
            vertexCount else indexCount
        )

        MeshPrimitive(collapsedGeometry, PBRMaterial())
    }
}