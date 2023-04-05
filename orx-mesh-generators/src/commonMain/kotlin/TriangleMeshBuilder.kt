package org.openrndr.extra.meshgenerators

import org.openrndr.collections.pop
import org.openrndr.collections.push
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform
import org.openrndr.math.transforms.normalMatrix
import org.openrndr.math.transforms.rotate
import org.openrndr.shape.Shape
import org.openrndr.utils.buffer.MPPBuffer

class TriangleMeshBuilder {
    var transform = Matrix44.IDENTITY
        set(value) {
            field = value
            normalTransform = normalMatrix(value)
        }

    var color = ColorRGBa.WHITE

    var normalTransform: Matrix44 = Matrix44.IDENTITY
        private set

    private val transformStack = ArrayDeque<Matrix44>()

    fun translate(x: Double, y: Double, z: Double) {
        transform *= buildTransform {
            translate(x, y, z)
        }
    }

    fun rotate(axis: Vector3, degrees: Double) {
        transform *= buildTransform {
            rotate(axis, degrees)
        }
    }


    fun pushTransform() {
        transformStack.push(transform)
    }

    fun popTransform() {
        transform = transformStack.pop()
    }

    fun isolated(builder: TriangleMeshBuilder.() -> Unit) {
        pushTransform()
        builder()
        popTransform()
    }

    class VertexData(val position: Vector3, val normal: Vector3, val texCoord: Vector2, val color: ColorRGBa) {
        fun transform(transform: Matrix44, normalTransform: Matrix44): VertexData {
            return VertexData((transform * position.xyz1).xyz, (normalTransform * normal.xyz0).xyz, texCoord, color)
        }
    }

    var data = mutableListOf<VertexData>()

    fun write(position: Vector3, normal: Vector3, texCoord: Vector2) {
        data.add(VertexData(position, normal, texCoord, color).transform(transform, normalTransform))
    }

    fun concat(other: TriangleMeshBuilder) {
        data.addAll(other.data)
    }

    fun toByteBuffer(): MPPBuffer {
        //val bb = ByteBuffer.allocateDirect(data.size * (3 * 4 + 3 * 4 + 2 * 4 + 4 * 4))
        val bb = MPPBuffer.allocate(data.size * (3 * 4 + 3 * 4 + 2 * 4 + 4 * 4))

        //bb.order(ByteOrder.nativeOrder())
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

            bb.putFloat(d.color.r.toFloat())
            bb.putFloat(d.color.g.toFloat())
            bb.putFloat(d.color.b.toFloat())
            bb.putFloat(d.color.alpha.toFloat())
        }
        bb.rewind()
        return bb
    }
}

fun TriangleMeshBuilder.sphere(sides: Int, segments: Int, radius: Double, invert: Boolean = false) {
    generateSphere(sides, segments, radius, invert, this::write)
}

fun TriangleMeshBuilder.hemisphere(sides: Int, segments: Int, radius: Double, invert: Boolean = false) {
    generateHemisphere(sides, segments, radius, invert, this::write)
}

enum class GridCoordinates {
    INDEX,
    UNIPOLAR,
    BIPOLAR,
}

/**
 * Create a 2D grid of [width] x [height] elements.
 * The [builder] function will get called with the x and y
 * coordinates of each grid cell. The u and v values passed to the
 * [builder] function will be scaled depending on [coordinates]:
 * - [GridCoordinates.INDEX] to get the column and row values as Double.
 * - [GridCoordinates.BIPOLAR] to get values between -1.0 and 1.0
 * - [GridCoordinates.UNIPOLAR] to get values between 0.0 and 1.0
 */
fun TriangleMeshBuilder.grid(
    width: Int,
    height: Int,
    coordinates: GridCoordinates = GridCoordinates.BIPOLAR,
    builder: TriangleMeshBuilder.(u: Double, v: Double) -> Unit
) {
    for (v in 0 until height) {
        for (u in 0 until width) {
            group {
                when (coordinates) {
                    GridCoordinates.INDEX -> this.builder(u * 1.0, v * 1.0)
                    GridCoordinates.BIPOLAR -> this.builder(
                        2 * u / (width - 1.0) - 1,
                        2 * v / (height - 1.0) - 1
                    )

                    GridCoordinates.UNIPOLAR -> this.builder(u / (width - 1.0), v / (height - 1.0))
                }
            }
        }
    }
}

fun TriangleMeshBuilder.twist(degreesPerUnit: Double, start: Double, axis: Vector3 = Vector3.UNIT_Y) {
    data = data.map {
        val p = it.position.projectedOn(axis)
        val t = when {
            axis.x != 0.0 -> p.x / axis.x
            axis.y != 0.0 -> p.y / axis.y
            axis.z != 0.0 -> p.z / axis.z
            else -> throw IllegalArgumentException("0 axis")
        }
        val r = Matrix44.rotate(axis, t * degreesPerUnit)
        TriangleMeshBuilder.VertexData((r * it.position.xyz1).xyz, (r * it.normal.xyz0).xyz, it.texCoord, this@twist.color)
    }.toMutableList()
}

/**
 * Create a 3D grid of [width] x [height] x [depth] elements.
 * The [builder] function will get called with the x and y
 * coordinates of each grid cell. The u and v values passed to the
 * [builder] function will be scaled depending on [coordinates]:
 * - [GridCoordinates.INDEX] to get the XYZ cell index values as Double.
 * - [GridCoordinates.BIPOLAR] to get values between -1.0 and 1.0
 * - [GridCoordinates.UNIPOLAR] to get values between 0.0 and 1.0
 */
fun TriangleMeshBuilder.grid(
    width: Int,
    height: Int,
    depth: Int,
    coordinates: GridCoordinates = GridCoordinates.BIPOLAR,
    builder: TriangleMeshBuilder.(u: Double, v: Double, w: Double) -> Unit
) {
    for (w in 0 until depth) {
        for (v in 0 until height) {
            for (u in 0 until width) {
                group {
                    when (coordinates) {
                        GridCoordinates.INDEX -> this.builder(u * 1.0, v * 1.0, w * 1.0)
                        GridCoordinates.BIPOLAR -> this.builder(
                            2 * u / (width - 1.0) - 1,
                            2 * v / (height - 1.0) - 1, 2 * w / (depth - 1.0) - 1
                        )

                        GridCoordinates.UNIPOLAR -> this.builder(
                            u / (width - 1.0),
                            v / (height - 1.0),
                            w / (depth - 1.0)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generate a box of [width], [height] and [depth] dimensions.
 * Specify the number of segments with [widthSegments], [heightSegments] and
 * [depthSegments]. Use [invert] for an inside-out shape.
 */
fun TriangleMeshBuilder.box(
    width: Double,
    height: Double,
    depth: Double,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
    invert: Boolean = false
) {
    generateBox(width, height, depth, widthSegments, heightSegments, depthSegments, invert, this::write)
}

fun TriangleMeshBuilder.cylinder(
    sides: Int,
    segments: Int,
    radius: Double,
    length: Double,
    invert: Boolean = false,
    center: Boolean = false
) {
    generateCylinder(sides, segments, radius, length, invert, center, this::write)
}

fun TriangleMeshBuilder.dodecahedron(radius: Double) {
    generateDodecahedron(radius, this::write)
}

fun TriangleMeshBuilder.taperedCylinder(
    sides: Int,
    segments: Int,
    startRadius: Double,
    endRadius: Double,
    length: Double,
    invert: Boolean = false,
    center: Boolean = false
) {
    generateTaperedCylinder(sides, segments, startRadius, endRadius, length, invert, center, this::write)
}

fun TriangleMeshBuilder.cap(sides: Int, radius: Double, envelope: List<Vector2>) {
    generateCap(sides, radius, envelope, this::write)
}

fun TriangleMeshBuilder.revolve(sides: Int, length: Double, envelope: List<Vector2>) {
    generateRevolve(sides, length, envelope, this::write)
}

fun TriangleMeshBuilder.plane(
    center: Vector3, right: Vector3, forward: Vector3, up: Vector3, width: Double = 1.0, height: Double = 1.0,
    widthSegments: Int = 1, heightSegments: Int = 1
) =
    generatePlane(center, right, forward, up, width, height, widthSegments, heightSegments, this::write)


fun TriangleMeshBuilder.extrudeShape(
    baseTriangles: List<Vector2>,
    contours: List<List<Vector2>>,
    length: Double,
    scale: Double = 1.0,
    frontCap: Boolean = true,
    backCap: Boolean = true,
    sides: Boolean = true
) {
    extrudeShape(
        baseTriangles = baseTriangles,
        contours = contours,
        front = -length / 2.0,
        back = length / 2.0,
        frontScale = scale,
        backScale = scale,
        frontCap = frontCap,
        backCap = backCap,
        sides = sides,
        flipNormals = false,
        writer = this::write
    )
}

fun TriangleMeshBuilder.extrudeShape(
    shape: Shape,
    length: Double,
    scale: Double = 1.0,
    frontCap: Boolean = true,
    backCap: Boolean = true,
    sides: Boolean = true,
    distanceTolerance: Double = 0.5
) {
    extrudeShape(
        shape = shape,
        front = -length / 2.0,
        back = length / 2.0,
        frontScale = scale,
        backScale = scale,
        frontCap = frontCap,
        backCap = backCap,
        sides = sides,
        distanceTolerance = distanceTolerance,
        flipNormals = false,
        writer = this::write
    )
}

fun TriangleMeshBuilder.extrudeShapes(
    shapes: List<Shape>,
    length: Double,
    scale: Double = 1.0,
    distanceTolerance: Double = 0.5
) {
    extrudeShapes(
        shapes = shapes,
        front = -length / 2.0,
        back = length / 2.0,
        frontScale = scale,
        backScale = scale,
        frontCap = true,
        backCap = true,
        sides = true,
        distanceTolerance = distanceTolerance,
        flipNormals = false,
        writer = this::write
    )
}

fun buildTriangleMesh(vertexBuffer: VertexBuffer? = null, builder: TriangleMeshBuilder.() -> Unit): VertexBuffer {
    val gb = TriangleMeshBuilder()
    gb.builder()

    val vb = vertexBuffer ?: meshVertexBufferWithColor(gb.data.size)

    val bb = gb.toByteBuffer()
    bb.rewind()
    vb.write(bb)
    return vb
}

fun generator(builder: TriangleMeshBuilder.() -> Unit): TriangleMeshBuilder {
    val gb = TriangleMeshBuilder()
    gb.builder()
    return gb
}

fun TriangleMeshBuilder.group(builder: TriangleMeshBuilder.() -> Unit) {
    val gb = TriangleMeshBuilder()
    gb.builder()
    this.concat(gb)
}
