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

/**
 * A class that provides a simple Domain Specific Language
 * to construct and deform triangle-based 3D meshes.
 *
 */
class TriangleMeshBuilder {
    var color = ColorRGBa.WHITE

    var transform = Matrix44.IDENTITY
        set(value) {
            field = value
            normalTransform = normalMatrix(value)
        }

    var normalTransform: Matrix44 = Matrix44.IDENTITY
        private set

    private val transformStack = ArrayDeque<Matrix44>()

    /**
     * Applies a three-dimensional translation to the [transform] matrix.
     * Affects meshes added afterward.
     */
    fun translate(x: Double, y: Double, z: Double) {
        transform *= buildTransform {
            translate(x, y, z)
        }
    }

    /**
     * Applies a three-dimensional translation to the [transform] matrix.
     * Affects meshes added afterward.
     */
    fun translate(translation: Vector3) {
        transform *= buildTransform {
            translate(translation)
        }
    }

    /**
     * Applies a rotation over an arbitrary axis to the [transform] matrix.
     * Affects meshes added afterward.
     * @param axis the axis to rotate over, will be normalized
     * @param degrees the rotation in degrees
     */
    fun rotate(axis: Vector3, degrees: Double) {
        transform *= buildTransform {
            rotate(axis, degrees)
        }
    }

    /**
     * Push the active [transform] matrix on the transform state stack.
     */
    fun pushTransform() {
        transformStack.push(transform)
    }

    /**
     * Pop the active [transform] matrix from the transform state stack.
     */
    fun popTransform() {
        transform = transformStack.pop()
    }

    /**
     * Pushes the [transform] matrix, calls [function] and pops.
     * @param function the function that is called in the isolation
     */
    fun isolated(function: TriangleMeshBuilder.() -> Unit) {
        pushTransform()
        function()
        popTransform()
    }

    /**
     * A container class for vertex [position], [normal], [texCoord] and
     * [color].
     */
    class VertexData(
        val position: Vector3,
        val normal: Vector3,
        val texCoord: Vector2,
        val color: ColorRGBa
    ) {
        /**
         * Return a new vertex with the position transformed with [transform]
         * and the normal transformed with [normalTransform]. Used to
         * translate, rotate or scale vertices.
         */
        fun transform(
            transform: Matrix44,
            normalTransform: Matrix44
        ) = VertexData(
            (transform * position.xyz1).xyz,
            (normalTransform * normal.xyz0).xyz,
            texCoord,
            color
        )
    }

    /**
     * Vertex storage
     */
    var data = mutableListOf<VertexData>()

    /**
     * Write new vertex data into [data]. The current [color] is used for the
     * vertex.
     */
    fun write(position: Vector3, normal: Vector3, texCoord: Vector2) {
        data.add(
            VertexData(position, normal, texCoord, color).transform(
                transform,
                normalTransform
            )
        )
    }

    /**
     * Append [other] data into [data], combining the two meshes.
     */
    fun concat(other: TriangleMeshBuilder) {
        data.addAll(other.data)
    }

    /**
     * Returns a [MPPBuffer] representation of [data] used for rendering.
     */
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

/**
 * Add a sphere mesh
 *
 * @param sides The number of steps around its axis.
 * @param segments The number of steps from pole to pole.
 * @param radius The radius of the sphere.
 * @param flipNormals Create an inside-out shape if true.
 */
fun TriangleMeshBuilder.sphere(
    sides: Int,
    segments: Int,
    radius: Double,
    flipNormals: Boolean = false
) {
    generateSphere(sides, segments, radius, flipNormals, this::write)
}

/**
 * Add a hemisphere
 *
 * @param sides The number of steps around its axis.
 * @param segments The number of steps from pole to pole.
 * @param radius The radius of the sphere.
 * @param flipNormals Create an inside-out shape if true.
 */
fun TriangleMeshBuilder.hemisphere(
    sides: Int,
    segments: Int,
    radius: Double,
    flipNormals: Boolean = false
) {
    generateHemisphere(sides, segments, radius, flipNormals, this::write)
}

/**
 * Used by the [grid] methods. Specifies how the UV or UVW
 * coordinates the user function receives are scaled.
 */
enum class GridCoordinates {
    /**
     * The coordinates are the cell location index as Double.
     */
    INDEX,

    /**
     * The coordinates with the cell's location are normalized
     * to the 0.0 ~ 1.0 range.
     */
    UNIPOLAR,

    /**
     * The coordinates with the cell's location are normalized
     * to the -1.0 ~ 1.0 range.
     */
    BIPOLAR,
}

/**
 * Create a 2D grid of [width] x [height] 3D elements.
 * The [builder] function will get called with the `u` and `v`
 * coordinates of each grid cell, so you have an opportunity to add meshes
 * to the scene using those coordinates. The coordinate values will be scaled
 * according to [coordinates]. Use:
 * - [GridCoordinates.INDEX] to get UV cell indices as [Double]s.
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

                    GridCoordinates.UNIPOLAR -> this.builder(
                        u / (width - 1.0),
                        v / (height - 1.0)
                    )
                }
            }
        }
    }
}

/**
 * Create a 3D grid of [width] x [height] x [depth] 3D elements.
 * The [builder] function will get called with the `u`, `v` and `w`
 * coordinates of each grid cell, so you have an opportunity to add meshes
 * to the scene using those coordinates. The coordinate values will be scaled
 * according to [coordinates]. Use:
 * - [GridCoordinates.INDEX] to get the UVW cell indices as [Double]s.
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
                        GridCoordinates.INDEX -> this.builder(
                            u * 1.0,
                            v * 1.0,
                            w * 1.0
                        )

                        GridCoordinates.BIPOLAR -> this.builder(
                            2 * u / (width - 1.0) - 1,
                            2 * v / (height - 1.0) - 1,
                            2 * w / (depth - 1.0) - 1
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
 * Twists a 3D mesh around an axis that starts at [Vector3.ZERO] and ends
 * at [axis]. [degreesPerUnit] controls the amount of  twist. [start] is
 * currently unused.
 */
fun TriangleMeshBuilder.twist(
    degreesPerUnit: Double,
    start: Double,
    axis: Vector3 = Vector3.UNIT_Y
) {
    data = data.map {
        val p = it.position.projectedOn(axis)
        val t = when {
            axis.x != 0.0 -> p.x / axis.x
            axis.y != 0.0 -> p.y / axis.y
            axis.z != 0.0 -> p.z / axis.z
            else -> throw IllegalArgumentException("0 axis")
        }
        val r = Matrix44.rotate(axis, t * degreesPerUnit)
        TriangleMeshBuilder.VertexData(
            (r * it.position.xyz1).xyz,
            (r * it.normal.xyz0).xyz,
            it.texCoord,
            this@twist.color
        )
    }.toMutableList()
}

/**
 * Generate a box of size [width], [height] and [depth].
 * Specify the number of segments with [widthSegments], [heightSegments] and
 * [depthSegments]. Use [flipNormals] for an inside-out shape.
 */
fun TriangleMeshBuilder.box(
    width: Double,
    height: Double,
    depth: Double,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
    flipNormals: Boolean = false
) {
    generateBox(
        width,
        height,
        depth,
        widthSegments,
        heightSegments,
        depthSegments,
        flipNormals,
        this::write
    )
}

/**
 * Generate a cylinder
 *
 * @param sides the number of sides of the cylinder
 * @param segments the number of segments along the z-axis
 * @param radius the radius of the cylinder
 * @param length the length of the cylinder
 * @param flipNormals generates inside-out geometry if true
 * @param center center the cylinder on the z-plane
 */
fun TriangleMeshBuilder.cylinder(
    sides: Int,
    segments: Int,
    radius: Double,
    length: Double,
    flipNormals: Boolean = false,
    center: Boolean = false
) {
    generateCylinder(
        sides,
        segments,
        radius,
        length,
        flipNormals,
        center,
        this::write
    )
}

/**
 * Generate dodecahedron mesh
 *
 * @param radius the radius of the dodecahedron
 */
fun TriangleMeshBuilder.dodecahedron(radius: Double) {
    generateDodecahedron(radius, this::write)
}

/**
 * Generate a tapered cylinder along the z-axis
 *
 * @param sides the number of sides of the tapered cylinder
 * @param segments the number of segments along the z-axis
 * @param startRadius the start radius of the tapered cylinder
 * @param endRadius the end radius of the tapered cylinder
 * @param length the length of the tapered cylinder
 * @param flipNormals generates inside-out geometry if true
 * @param center centers the cylinder on the z-plane if true
 */
fun TriangleMeshBuilder.taperedCylinder(
    sides: Int,
    segments: Int,
    startRadius: Double,
    endRadius: Double,
    length: Double,
    flipNormals: Boolean = false,
    center: Boolean = false
) {
    generateTaperedCylinder(
        sides,
        segments,
        startRadius,
        endRadius,
        length,
        flipNormals,
        center,
        this::write
    )
}

/**
 * Generate a shape by rotating an envelope around a vertical axis.
 *
 * @param sides the angular resolution of the cap
 * @param radius the radius of the cap
 * @param envelope a list of points defining the profile of the cap.
 * The default envelope is a horizontal line which produces a flat round disk.
 * By providing a more complex envelope one can create curved shapes like a bowl.
 */
fun TriangleMeshBuilder.cap(
    sides: Int,
    radius: Double,
    envelope: List<Vector2>
) {
    generateCap(sides, radius, envelope, this::write)
}

/**
 * Generate a shape by rotating an envelope around a vertical axis.
 *
 * @param sides the angular resolution of the cap
 * @param length the length of the shape. A multiplier for the y component of the envelope
 * @param envelope a list of points defining the profile of the shape.
 * The default envelope is a vertical line which produces a hollow cylinder.
 */
fun TriangleMeshBuilder.revolve(
    sides: Int,
    length: Double,
    envelope: List<Vector2>
) {
    generateRevolve(sides, length, envelope, this::write)
}

/**
 * Generate plane centered at [center], using the [right], [forward] and [up]
 * vectors for its orientation.
 * [width] and [height] specify the dimensions of the plane.
 * [widthSegments] and [heightSegments] control the plane's number of
 * segments.
 */
fun TriangleMeshBuilder.plane(
    center: Vector3,
    right: Vector3,
    forward: Vector3,
    up: Vector3,
    width: Double = 1.0,
    height: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1
) {
    generatePlane(
        center,
        right,
        forward,
        up,
        width,
        height,
        widthSegments,
        heightSegments,
        this::write
    )
}

/**
 * Extrudes a [Shape] from its triangulations
 *
 * @param baseTriangles triangle vertices for the caps
 * @param contours contour vertices for the sides
 * @param length the length of the extrusion
 * @param scale scale factor for the caps
 * @param frontCap add a front cap if true
 * @param backCap add a back cap if true
 * @param sides add the sides if true
 */
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

/**
 * Extrudes a [Shape]
 *
 * @param shape the [Shape] to extrude
 * @param length length of the extrusion
 * @param scale scale factor of the caps
 * @param frontCap add a front cap if true
 * @param backCap add a back cap if true
 * @param sides add the sides if true
 * @param distanceTolerance controls how many segments will be created. Lower
 * values result in higher vertex counts.
 */
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

/**
 * Extrudes a list of [Shape]
 *
 * @param shapes The [Shape]s to extrude
 * @param length length of the extrusion
 * @param scale scale factor of the caps
 * @param distanceTolerance controls how many segments will be created. Lower
 * values result in higher vertex counts.
 */
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

/**
 * Creates a triangle mesh builder
 *
 * @param vertexBuffer The optional [VertexBuffer] into which to write data.
 * If not provided one is created.
 * @param builder A user function that adds 3D meshes to the [vertexBuffer]
 * @return The populated [VertexBuffer]
 */
fun buildTriangleMesh(
    vertexBuffer: VertexBuffer? = null,
    builder: TriangleMeshBuilder.() -> Unit
): VertexBuffer {
    val gb = TriangleMeshBuilder()
    gb.builder()

    val vb = vertexBuffer ?: meshVertexBufferWithColor(gb.data.size)

    val bb = gb.toByteBuffer()
    bb.rewind()
    vb.write(bb)
    return vb
}

//fun generator(
//    builder: TriangleMeshBuilder.() -> Unit
//): TriangleMeshBuilder {
//    val gb = TriangleMeshBuilder()
//    gb.builder()
//    return gb
//}

/**
 * Creates a group. Can be used to avoid leaking mesh properties like `color`
 * and `transform` into following meshes or groups.
 *
 * @param builder A user function that adds 3D meshes to the [vertexBuffer]
 * @see [TriangleMeshBuilder.isolated]
 */
fun TriangleMeshBuilder.group(
    builder: TriangleMeshBuilder.() -> Unit
) {
    val gb = TriangleMeshBuilder()
    gb.builder()
    this.concat(gb)
}
