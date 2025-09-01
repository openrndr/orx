package org.openrndr.extra.processing

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.SegmentType
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import processing.core.PShape

/**
 * Appends a vertex to the current shape using a 2D vector.
 *
 * @param v the [Vector2] instance containing the x and y coordinates of the vertex to be added.
 *          The coordinates are converted to Float for use in the shape.
 */
fun PShape.vertex(v: Vector2) {
    vertex(v.x.toFloat(), v.y.toFloat())
}

/**
 * Adds a quadratic Bezier vertex to the shape. The method specifies control and anchor points
 * for the curve using [Vector2] instances.
 *
 * @param v2 The control point of the quadratic Bezier curve, used to define its curvature.
 * @param v3 The anchor point of the quadratic Bezier curve, which is the endpoint of the curve.
 */
fun PShape.quadraticVertex(v2: Vector2, v3: Vector2) {
    quadraticVertex(
        v2.x.toFloat(), v2.y.toFloat(),
        v3.x.toFloat(), v3.y.toFloat()
    )
}

fun PShape.fill(c: ColorRGBa) {
    fill(c.r.toFloat() * 255.0f, c.g.toFloat() * 255.0f, c.b.toFloat() * 255.0f, c.alpha.toFloat() * 255.0f)
}

fun PShape.stroke(c: ColorRGBa) {
    stroke(c.r.toFloat() * 255.0f, c.g.toFloat() * 255.0f, c.b.toFloat() * 255.0f, c.alpha.toFloat() * 255.0f)
}

/**
 * Adds a cubic Bézier curve vertex to the current shape. The curve is defined
 * by two control points and an end point.
 *
 * @param v2 The first control point that influences the direction and shape of the curve.
 * @param v3 The second control point that affects the curvature of the Bézier curve.
 * @param v4 The end point where the Bézier curve terminates.
 */
fun PShape.bezierVertex(v2: Vector2, v3: Vector2, v4: Vector2) {
    bezierVertex(
        v2.x.toFloat(), v2.y.toFloat(),
        v3.x.toFloat(), v3.y.toFloat(),
        v4.x.toFloat(), v4.y.toFloat()
    )
}

/**
 * Combines a list of [Shape] objects into a single [PShape] object of type `GROUP`
 * by adding each shape as a child.
 *
 * Each [Shape] in the input list is converted to a [PShape], and these are added as
 * children to a parent [PShape] of type `GROUP`. This allows for easy grouping and management
 * of multiple [Shape] objects while using the Processing library.
 *
 * @param shapes the list of [Shape] objects to combine. Each shape is converted and added
 *               as a child to the resulting [PShape] object.
 * @return a [PShape] object of type `GROUP` containing the provided shapes as children.
 */
fun PShape(shapes: List<Shape>): PShape {
    val ps = PShape(PShape.GROUP)
    for (shape in shapes) {
        ps.addChild(PShape(shape))
    }
    return ps
}

/**
 * Converts a given [Shape] instance into a [PShape] object. If the shape contains a single
 * contour, it is directly converted. Otherwise, the method constructs a complex [PShape]
 * with paths and contours, mapping each segment type appropriately.
 *
 * @param shape the [Shape] instance to be converted into a [PShape]. The shape may consist
 *              of one or more contours, each containing multiple segments.
 * @return a [PShape] object representing the input shape. The resulting [PShape]
 *         will contain vertices, contours, and paths corresponding to the geometry of the input.
 */
fun PShape(shape: Shape): PShape {
    if (shape.contours.size == 1) {
        return PShape(shape.contours[0])
    } else {
        val ps = PShape(PShape.PATH)
        ps.beginShape()
        for (contour in shape.contours) {
            ps.beginContour()
            ps.vertex(contour.segments[0].start)
            for (segment in contour.segments) {
                when (segment.type) {
                    SegmentType.LINEAR -> ps.vertex(segment.end)
                    SegmentType.QUADRATIC -> ps.quadraticVertex(segment.control[0], segment.end)
                    SegmentType.CUBIC -> ps.bezierVertex(segment.control[0], segment.control[1], segment.end)
                }
            }
            ps.endContour()
        }
        ps.endShape(PShape.CLOSE)
        return ps
    }
}

/**
 * Converts a given [ShapeContour] into a [PShape] object. The method translates contour
 * segments (linear, quadratic, cubic) into corresponding vertices and shapes suitable
 * for use with the Processing library.
 *
 * @param contour the [ShapeContour] to convert into a [PShape]. It may consist of multiple
 * segments of varying types and can be open or closed.
 * @return a [PShape] object representing the given [ShapeContour].
 */
fun PShape(contour: ShapeContour): PShape {
    val ps = PShape(PShape.PATH)
    if (!contour.empty) {
        ps.beginShape()
        val start = contour.segments[0].start
        ps.vertex(start)
        for ((index, segment) in contour.segments.withIndex()) {
            when (segment.type) {
                SegmentType.LINEAR -> {
                    if (!(contour.closed && index == contour.segments.size - 1)) {
                        ps.vertex(segment.end)
                    }
                }
                SegmentType.QUADRATIC -> ps.quadraticVertex(segment.control[0], segment.end)
                SegmentType.CUBIC -> {
                    ps.bezierVertex(segment.control[0], segment.control[1], segment.end)
                }
            }
        }
        ps.endShape(if (contour.closed) PShape.CLOSE else PShape.OPEN)
    }
    return ps
}


/**
 * Converts a [PShape] of type `PATH` into a [ShapeContour].
 *
 * This function processes the vertices and vertex codes of the `PShape` to construct a
 * corresponding [ShapeContour]. The function supports vertex types including `VERTEX`,
 * `BEZIER_VERTEX`, and `QUADRATIC_VERTEX`. Other vertex codes will result in an error.
 * The contour will reflect whether the `PShape` is closed or open.
 *
 * @return A [ShapeContour] that represents the geometry of the given `PShape.PATH`.
 * @throws IllegalArgumentException if the `PShape` is not of type `PATH`.
 * @throws IllegalStateException for unsupported vertex codes.
 */
fun PShape.pathToShapeContours(): List<ShapeContour> {
    require(family == PShape.PATH) {
        "can only convert PShape.PATH to ShapeContour"
    }
    if (vertexCodeCount == 0) {
        val vertices = mutableListOf<Vector2>()
        for (i in 0 until vertexCount) {
            val pv = getVertex(i)
            vertices.add(pv.toVector2())
        }
        val contour = ShapeContour.fromPoints(vertices, isClosed)
        return listOf(contour)
    } else {
        val result = mutableListOf<ShapeContour>()

        var segments = mutableListOf<Segment2D>()
        var vertexIndex = 0
        var vertex:Vector2? = null

        for (i in 0 until vertexCodeCount) {
            val code = vertexCodes[i]
            when (code) {
                PShape.VERTEX -> {
                    val pv = getVertex(vertexIndex).toVector2()
                    vertexIndex++
                    if (vertex != null) {
                        segments.add(Segment2D(vertex, pv))
                    }
                    vertex = pv
                }
                PShape.BEZIER_VERTEX -> {
                    val c0 = getVertex(vertexIndex).toVector2(); vertexIndex++
                    val c1 = getVertex(vertexIndex).toVector2(); vertexIndex++
                    val pv = getVertex(vertexIndex).toVector2(); vertexIndex++
                   segments.add(Segment2D(vertex ?: error("no vertex set"), c0, c1, pv))
                    vertex = pv
                }
                PShape.QUADRATIC_VERTEX -> {
                    val c0 = getVertex(vertexIndex).toVector2(); vertexIndex++
                    val pv = getVertex(vertexIndex).toVector2(); vertexIndex++
                    segments.add(Segment2D(vertex ?: error("no vertex set"), c0,  pv))
                    vertex = pv
                }
                PShape.BREAK -> {
                    segments.add(Segment2D(vertex ?: error("no vertex set"), segments.first().start))
                    result.add(ShapeContour(segments, closed = isClosed))
                    segments = mutableListOf()
                    vertex = getVertex(vertexIndex).toVector2()
                }
                else -> error("unsupported code $code")
            }
        }
        if (isClosed && segments.last().end.distanceTo(segments.first().start) > 1E-6) {
            segments.add(Segment2D(segments.last().end, segments.first().start))
        }

        if (segments.isNotEmpty()) {
            result.add(ShapeContour(segments, closed = isClosed))
        }
        return result
    }
}

/**
 * Converts this [PShape] instance into a list of [ShapeContour] objects.
 *
 * This function processes the shape based on its family type:
 * - If the shape is a `GROUP`, it recursively converts its children into contours.
 * - If the shape is a `PATH`, it converts it directly to a single [ShapeContour].
 * - If the shape is `GEOMETRY`, it constructs contours based on vertex information.
 *
 * Unsupported shape families will throw an error.
 *
 * @return A list of [ShapeContour] objects representing the contours of this [PShape].
 */
fun PShape.toShapeContours(): List<ShapeContour> {
    return when (this.family) {
        PShape.GROUP -> {
            children.flatMap { it.toShapeContours() }
        }

        PShape.PATH -> {
            pathToShapeContours()
        }

        PShape.GEOMETRY -> {
            val contourPoints = mutableListOf<MutableList<Vector2>>()
            //https://github.com/processing/processing4/blob/d35f4de58936d41946d253f37986127fd100654c/core/src/processing/core/PShape.java#L1772

            var codeIndex = 0
            var activeContour = mutableListOf<Vector2>()
            for (i in 0 until vertexCount) {
                if (vertexCodes[codeIndex++] == PShape.BREAK) {
                    contourPoints.add(activeContour)
                    activeContour = mutableListOf()
                    codeIndex++
                }
            }
            if (activeContour.isNotEmpty()) {
                contourPoints.add(activeContour)
            }
            contourPoints.map { ShapeContour.fromPoints(it, false) }
        }

        else -> error("unsupported shape family: ${this.family}")
    }
}

/**
 * Converts this [PShape] instance into a [Shape] instance.
 *
 * This function processes the contours of the [PShape] and transforms them into
 * the corresponding contours of a [Shape] object.
 *
 * @return A [Shape] object representing the converted [PShape].
 */
fun PShape.toShape(): Shape {
    return Shape(toShapeContours())
}