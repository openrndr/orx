package org.openrndr.extra.textoncontour

import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.extra.shapes.pose.PosePath3D
import org.openrndr.extra.shapes.pose.pose
import org.openrndr.extra.shapes.rectify.RectifiedPath3D
import org.openrndr.extra.shapes.rectify.rectified
import org.openrndr.extra.textwriter.ContourOverflow
import org.openrndr.extra.textwriter.ContourTextLayout
import org.openrndr.extra.textwriter.GlyphPivot
import org.openrndr.extra.textwriter.TextBaseline
import org.openrndr.extra.textwriter.TextSpan
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.layoutTextOnContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Path3D

/**
 * The baseline for text along a 3D [path]: the 3D counterpart of [org.openrndr.extra.textwriter.ContourBaseline].
 *
 * The text stands in the frames of [PosePath3D] along the path, which start with their y-axis along [up] and turn as
 * little as possible, so the text does not twist. A glyph's x runs along the path, its ascenders point along the y-axis
 * of the frame of the path (up, as far as the path allows), and its face looks along the x-axis of that frame, the
 * z-axis x × y of the glyph. [offset] moves the baseline towards the ascenders. Distances are measured along the path.
 *
 * @param path the rectified path
 * @param up the up of the first frame, see [PosePath3D]
 * @param offset the distance of the baseline from the path, towards the ascenders
 */
class PathBaseline3D(val path: RectifiedPath3D, up: Vector3 = Vector3.UNIT_Y, val offset: Double = 0.0) : TextBaseline {
    constructor(path: Path3D, up: Vector3 = Vector3.UNIT_Y, offset: Double = 0.0) : this(path.rectified(), up, offset)

    private val poses = path.pose(up)

    override val length: Double = path.path.length

    override val closed: Boolean get() = path.path.closed

    /**
     * The frame at [distance] along the path that places the outline of a glyph: its x-axis along the path, its y-axis
     * away from the ascenders, the down of the frame of the path, and its z-axis x × y
     */
    override fun pose(distance: Double): Matrix44 {
        val t = if (length > 0.0) (distance / length).coerceIn(0.0, 1.0) else 0.0
        val m = poses.pose(t)
        val along = Vector3(m.c2r0, m.c2r1, m.c2r2)
        val up = Vector3(m.c1r0, m.c1r1, m.c1r2)
        val down = -up
        val origin = Vector3(m.c3r0, m.c3r1, m.c3r2) + up * offset
        return Matrix44.fromColumnVectors(along.xyz0, down.xyz0, along.cross(down).xyz0, origin.xyz1)
    }
}

/**
 * Places [spans] on a single line along a 3D [path]: the 3D counterpart of [layoutTextOnContour], which it uses with a
 * [PathBaseline3D]. Every glyph of the result has a transform that places its outline, in its own x and y, in 3D.
 *
 * @param up the up of the first frame of the path, see [PathBaseline3D]
 * @param offset the distance of the baseline from the path, towards the ascenders
 * @param style the style that is cascaded into the style of every span
 * @param startDistance the distance along the path at which the text is anchored
 * @param anchor the point of the text at [startDistance], 0.0 for the start, 0.5 for the middle, 1.0 for the end
 * @param overflow what happens to glyphs beyond the start or the end of the path
 * @param pivot the point of every glyph that is placed on the path
 * @param shifts the shift of every span along and across the path, or an empty list
 */
fun layoutTextOnPath3D(
    spans: List<TextSpan>,
    path: RectifiedPath3D,
    up: Vector3 = Vector3.UNIT_Y,
    offset: Double = 0.0,
    style: TextStyle = TextStyle.Defaults,
    startDistance: Double = 0.0,
    anchor: Double = 0.0,
    overflow: ContourOverflow = ContourOverflow.WRAP,
    pivot: GlyphPivot = GlyphPivot.START,
    shifts: List<Vector2> = emptyList(),
    shaper: TextShapingDriver = TextShapingDriver.instance
): ContourTextLayout = layoutTextOnContour(
    spans, PathBaseline3D(path, up, offset), style, startDistance, anchor, overflow, pivot, shifts, shaper
)

/** Places [spans] on a single line along a 3D [path], see the [RectifiedPath3D] overload */
fun layoutTextOnPath3D(
    spans: List<TextSpan>,
    path: Path3D,
    up: Vector3 = Vector3.UNIT_Y,
    offset: Double = 0.0,
    style: TextStyle = TextStyle.Defaults,
    startDistance: Double = 0.0,
    anchor: Double = 0.0,
    overflow: ContourOverflow = ContourOverflow.WRAP,
    pivot: GlyphPivot = GlyphPivot.START,
    shifts: List<Vector2> = emptyList(),
    shaper: TextShapingDriver = TextShapingDriver.instance
): ContourTextLayout = layoutTextOnPath3D(
    spans, path.rectified(), up, offset, style, startDistance, anchor, overflow, pivot, shifts, shaper
)
