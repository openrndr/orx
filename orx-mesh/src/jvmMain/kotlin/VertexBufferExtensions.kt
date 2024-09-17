package org.openrndr.extra.mesh

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.extra.mesh.Polygon
import org.openrndr.extra.mesh.objVertexFormat
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder


private fun ByteBuffer.getVector3(): Vector3 {
    val x = getFloat()
    val y = getFloat()
    val z = getFloat()
    return Vector3(x.toDouble(), y.toDouble(), z.toDouble())
}

private fun ByteBuffer.getVector2(): Vector2 {
    val x = getFloat()
    val y = getFloat()
    return Vector2(x.toDouble(), y.toDouble())
}

private fun ByteBuffer.getColorRGBa(): ColorRGBa {
    val r = getFloat()
    val g = getFloat()
    val b = getFloat()
    val a = getFloat()
    return ColorRGBa(r.toDouble(), g.toDouble(), b.toDouble(), a.toDouble())
}

/**
    Convert vertex buffer contents to a list of [Polygon] instances
 */
fun VertexBuffer.toPolygons(vertexCount: Int = this.vertexCount): List<Polygon> {
    require(vertexFormat == objVertexFormat)
    val triangleCount = vertexCount / 3
    val buffer = ByteBuffer.allocateDirect(this.vertexCount * vertexFormat.size)
    buffer.order(ByteOrder.nativeOrder())

    val polygons = mutableListOf<Polygon>()
    for (t in 0 until triangleCount) {
        val positions = mutableListOf<Vector3>()
        val normals = mutableListOf<Vector3>()
        val textureCoordinates = mutableListOf<Vector2>()
        val colors = mutableListOf<ColorRGBa>()

        for (v in 0 until 3) {
            positions.add(buffer.getVector3())
            normals.add(buffer.getVector3())
            textureCoordinates.add(buffer.getVector2())
            colors.add(buffer.getColorRGBa())
        }
        polygons.add(Polygon(positions, normals, textureCoordinates, colors))
    }
    return polygons
}