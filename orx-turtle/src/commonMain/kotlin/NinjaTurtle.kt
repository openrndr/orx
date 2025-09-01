package org.openrndr.extra.turtle

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour

fun Turtle.contour(contour: ShapeContour, alignTangent: Boolean = true) {
    if (!contour.empty) {
        val align = segment(contour.segments.first(), alignTangent)
        for (segment in contour.segments.drop(1)) {
            segment(segment, alignTangent = false, externalAlignTransform = align)
        }
    }
}

fun Turtle.segment(
    segment: Segment2D,
    alignTangent: Boolean = true,
    externalAlignTransform: Matrix44 = Matrix44.IDENTITY
): Matrix44 {
    var segment0 = segment.transform(buildTransform {
        translate(-segment.start)
    })

    var alignTransform = externalAlignTransform

    if (alignTangent) {
        val n = -segment0.normal(0.0)
        val t = n.perpendicular()
        val m = Matrix44.fromColumnVectors(t.xy00, n.xy00, Vector4.UNIT_Z, Vector4.UNIT_W)
        alignTransform = orientation * m.inversed
    }

    segment0 = segment0.transform(buildTransform {
        translate(position)
        multiply(alignTransform)
    })

    require(position.distanceTo(segment0.start) < 1E-5) {
        "Alignment error: Turtle position ${position} is not aligned with segment start ${segment0.start}. Distance: ${
            position.distanceTo(
                segment0.start
            )
        }"
    }
    cb.segment(segment0)
    orientation = cb.segments.last().pose(1.0).matrix33.matrix44

    if (!isPenDown) {
        cb.segments.removeLastOrNull()
    }
    return alignTransform
}