package org.openrndr.extras.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Shape
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeneratorBuffer {
    class VertexData(val position: Vector3, val normal: Vector3, val texCoord: Vector2)

    var data = mutableListOf<VertexData>()

    fun write(position: Vector3, normal: Vector3, texCoord: Vector2) {
        data.add(VertexData(position, normal, texCoord))
    }

    fun concat(other: GeneratorBuffer) {
        data.addAll(other.data)
    }

    fun transform(m: Matrix44) {
        data = data.map {
            VertexData((m * (it.position.xyz1)).xyz, (m * (it.normal.xyz0)).xyz, it.texCoord)
        }.toMutableList()
    }

    fun toByteBuffer(): ByteBuffer {
        val bb = ByteBuffer.allocateDirect(data.size * (3 * 4 + 3 * 4 + 2 * 4))
        bb.order(ByteOrder.nativeOrder())
        bb.rewind()
        for (d in data) {
            bb.putFloat(d.position.x.toFloat())
            bb.putFloat(d.position.y.toFloat())
            bb.putFloat(d.position.z.toFloat())

            bb.putFloat(d.normal.x.toFloat())
            bb.putFloat(d.normal.y.toFloat())
            bb.putFloat(d.normal.z.toFloat())

            bb.putFloat(d.texCoord.x.toFloat())
            bb.putFloat(d.texCoord.y.toFloat())
        }
        return bb
    }
}

fun GeneratorBuffer.sphere(sides: Int, segments: Int, radius: Double, invert: Boolean = false) {
    generateSphere(sides, segments, radius, invert, this::write)
}

fun GeneratorBuffer.hemisphere(sides: Int, segments: Int, radius: Double, invert: Boolean = false) {
    generateHemisphere(sides, segments, radius, invert, this::write)
}

enum class GridCoordinates {
    INDEX,
    UNIPOLAR,
    BIPOLAR,
}

fun GeneratorBuffer.grid(width: Int, height: Int, coordinates: GridCoordinates = GridCoordinates.BIPOLAR, builder: GeneratorBuffer.(u: Double, v: Double) -> Unit) {
    for (v in 0 until height) {
        for (u in 0 until width) {
            group {
                when (coordinates) {
                    GridCoordinates.INDEX -> this.builder(u * 1.0, v * 1.0)
                    GridCoordinates.BIPOLAR -> this.builder(2 * u / (width - 1.0) - 1,
                            2 * v / (height - 1.0) - 1)
                    GridCoordinates.UNIPOLAR -> this.builder(u / (width - 1.0), v / (height - 1.0))
                }
            }
        }
    }
}

fun GeneratorBuffer.twist(degreesPerUnit: Double, start: Double, axis: Vector3 = Vector3.UNIT_Y) {
    data = data.map {
        val p = it.position.projectedOn(axis)
        val t = if (axis.x != 0.0) p.x / axis.x else if (axis.y != 0.0) p.y / axis.y else if (axis.z != 0.0) p.z / axis.z else
            throw IllegalArgumentException("0 axis")
        val r = Matrix44.rotate(axis, t * degreesPerUnit)
        GeneratorBuffer.VertexData((r * it.position.xyz1).xyz, (r * it.normal.xyz0).xyz, it.texCoord)
    }.toMutableList()
}

fun GeneratorBuffer.grid(width: Int, height: Int, depth: Int, coordinates: GridCoordinates = GridCoordinates.BIPOLAR, builder: GeneratorBuffer.(u: Double, v: Double, w: Double) -> Unit) {
    for (w in 0 until depth) {
        for (v in 0 until height) {
            for (u in 0 until width) {
                group {
                    when (coordinates) {
                        GridCoordinates.INDEX -> this.builder(u * 1.0, v * 1.0, w * 1.0)
                        GridCoordinates.BIPOLAR -> this.builder(2 * u / (width - 1.0) - 1,
                                2 * v / (height - 1.0) - 1, 2 * w / (depth - 1.0) - 1)
                        GridCoordinates.UNIPOLAR -> this.builder(u / (width - 1.0), v / (height - 1.0), w / (depth - 1.0))
                    }
                }
            }
        }
    }
}

fun GeneratorBuffer.box(width: Double, height: Double, depth: Double, widthSegments: Int = 1, heightSegments: Int = 1, depthSegments: Int = 1, invert: Boolean = false) {
    generateBox(width, height, depth, widthSegments, heightSegments, depthSegments, invert, this::write)
}

fun GeneratorBuffer.cylinder(sides: Int, segments: Int, radius: Double, length: Double, invert: Boolean = false) {
    generateCylinder(sides, segments, radius, length, invert, this::write)
}

fun GeneratorBuffer.taperedCylinder(sides: Int, segments: Int, startRadius: Double, endRadius: Double, length: Double, invert: Boolean = false) {
    generateTaperedCylinder(sides, segments, startRadius, endRadius, length, invert, this::write)
}

fun GeneratorBuffer.cap(sides: Int, radius: Double, enveloppe: List<Vector2>) {
    generateCap(sides, radius, enveloppe, this::write)
}

fun GeneratorBuffer.revolve(sides:Int, length:Double, enveloppe: List<Vector2>) {
    generateRevolve(sides, length, enveloppe, this::write)
}

fun GeneratorBuffer.extrudeShape( baseTriangles:List<Vector2>, contours:List<List<Vector2>>, length: Double, scale: Double = 1.0, distanceTolerance: Double = 0.5) {
    extrudeShape(baseTriangles, contours, -length / 2.0, length / 2.0, scale, scale, true, true, distanceTolerance, false, this::write)
}

fun GeneratorBuffer.extrudeShape(shape: Shape, length: Double, scale: Double = 1.0, distanceTolerance: Double = 0.5) {
    extrudeShape(shape, -length / 2.0, length / 2.0, scale, scale, true, true, distanceTolerance, false, this::write)
}

fun GeneratorBuffer.extrudeShapes(shapes: List<Shape>, length: Double, scale: Double = 1.0, distanceTolerance: Double = 0.5) {
    extrudeShapes(shapes, -length / 2.0, length / 2.0, scale, scale, true, true, distanceTolerance, false, this::write)
}



fun meshGenerator(builder: GeneratorBuffer.() -> Unit): VertexBuffer {
    val gb = GeneratorBuffer()
    gb.builder()

    val vb = vertexBuffer(vertexFormat {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, gb.data.size)

    val bb = gb.toByteBuffer()
    bb.rewind()
    vb.write(bb)
    return vb
}

fun generator(builder: GeneratorBuffer.() -> Unit): GeneratorBuffer {
    val gb = GeneratorBuffer()
    gb.builder()
    return gb
}

fun GeneratorBuffer.group(builder: GeneratorBuffer.() -> Unit) {
    val gb = GeneratorBuffer()
    gb.builder()
    this.concat(gb)
}

fun main(args: Array<String>) {
    val gb = generator {
        box(20.0, 20.0, 20.0)
        group {
            box(40.0, 40.0, 40.0)
            transform(transform {
                translate(0.0, 20.0, 0.0)
            })
        }
    }
}