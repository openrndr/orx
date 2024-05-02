package org.openrndr.extra.shapes.path3d

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.*

private val Segment2D.vertices: List<Vector2>
    get() {
        return when (type) {
            SegmentType.LINEAR -> listOf(start, end)
            SegmentType.QUADRATIC -> listOf(start, control[0], end)
            SegmentType.CUBIC -> listOf(start, control[0], control[1], end)
        }
    }

/**
 * Projects [Path3D] to [ShapeContour]
 * @param projection The projection matrix to use
 * @param view The view matrix to use
 * @param width The viewport width
 * @param height The viewport height
 * @since orx 0.4.5
 */

fun Path3D.projectToContour(projection: Matrix44, view: Matrix44, width: Int, height: Int): ShapeContour {
    val ps = segments.map { it.projectToSegment2D(projection, view, width, height) }
    val ok = (ps.asSequence().flatMap { it.vertices }.all { it.x == it.x && it.y == it.y })

    if (ok)
        return ShapeContour(ps, closed)
    else
        return ShapeContour.EMPTY
}


/**
 * Projects [Segment3D] to [Segment2D]
 * @param projection The projection matrix to use
 * @param view The view matrix to use
 * @param width The viewport width
 * @param height The viewport height
 * @since orx 0.4.5
 */

fun Segment3D.projectToSegment2D(projection: Matrix44, view: Matrix44, width: Int, height: Int): Segment2D {
    fun Vector3.project(): Vector2 {
        val p3 = org.openrndr.math.transforms.project(this, projection, view, width, height)
        return p3.xy
    }

    return when (control.size) {
        0 -> Segment2D(start.project(), end.project())
        1 -> Segment2D(start.project(), control[0].project(), end.project())
        2 -> Segment2D(start.project(), control[0].project(), control[1].project(), end.project())
        else -> error("not supported")
    }
}

/**
 * Convert [ShapeContour] to [Path3D]
 * @since orx 0.4.5
 */
fun ShapeContour.toPath3D(): Path3D = Path3D.fromSegments(segments.map { it.toSegment3D() }, closed)

/**
 * Convert [Segment2D to [Segment3D]
 * @since orx 0.4.5
 */
fun Segment2D.toSegment3D(): Segment3D = Segment3D(start.xy0, control.map { c -> c.xy0 }, end.xy0)