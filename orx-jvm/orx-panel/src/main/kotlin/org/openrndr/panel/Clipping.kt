package org.openrndr.panel

import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.math.round

/** the most rounded boxes that clip at the same time, deeper ones only clip by their rectangles */
internal const val maxRoundedClips = 4

/**
 * GLSL: `clipCoverage(p)`, the coverage of the point [p] of the screen, in points with y down, by the rounded boxes of
 * the parameters of [RoundedClips.applyTo]
 */
internal val clipCoverageGlsl = """
    float clipCoverage(vec2 p) {
        float coverage = 1.0;
        for (int i = 0; i < p_clipCount; i++) {
            vec4 box = p_clipBoxes[i];
            float r = p_clipRadii[i].x;
            // the point in the coordinates of the box before its transform
            vec2 l = (p_clipInverses[i] * vec4(p, 0.0, 1.0)).xy;
            vec2 halfSize = box.zw * 0.5;
            vec2 q = abs(l - (box.xy + halfSize)) - halfSize + vec2(r);
            float d = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
            coverage *= clamp(0.5 - d / max(fwidth(d), 1E-4), 0.0, 1.0);
        }
        return coverage;
    }
""".trimIndent()

/**
 * The rounded boxes that clip what is drawn, of the elements with a border radius that clip their content, and the
 * boxes of transformed elements that clip their content: a shade style covers the pixels outside them, smoothly at the
 * edges. The rectangles of the window are clipped by scissoring.
 */
internal class RoundedClips {
    private val boxes = ArrayList<Rectangle>()
    private val radii = ArrayList<Double>()
    private val inverses = ArrayList<Matrix44>()

    private val style = shadeStyle {
        fragmentPreamble = clipCoverageGlsl
        fragmentTransform = """
            // the position in points, y down from the top
            float coverage = clipCoverage(vec2(c_screenPosition.x, c_screenSize.y - c_screenPosition.y));
            x_fill.a *= coverage;
            x_stroke.a *= coverage;
        """.trimIndent()
        parameter("clipBoxes", Array(maxRoundedClips) { Vector4.ZERO })
        parameter("clipRadii", Array(maxRoundedClips) { Vector2.ZERO })
        parameter("clipInverses", Array(maxRoundedClips) { Matrix44.IDENTITY })
        parameter("clipCount", 0)
    }

    /** sets the rounded boxes as the parameters clipBoxes, clipRadii and clipCount of [style], see [clipCoverageGlsl] */
    fun applyTo(style: ShadeStyle) {
        val count = minOf(boxes.size, maxRoundedClips)
        style.parameter("clipBoxes", Array(maxRoundedClips) { i ->
            boxes.getOrNull(i)?.takeIf { i < count }?.let { Vector4(it.x, it.y, it.width, it.height) } ?: Vector4.ZERO
        })
        style.parameter("clipRadii", Array(maxRoundedClips) { i -> Vector2(if (i < count) radii[i] else 0.0, 0.0) })
        style.parameter("clipInverses", Array(maxRoundedClips) { i -> if (i < count) inverses[i] else Matrix44.IDENTITY })
        style.parameter("clipCount", count)
    }

    /** clips by [box] with corners of [radius]; [transform] is the transform of the box, from its coordinates to the window */
    fun push(box: Rectangle, radius: Double, transform: Matrix44? = null) {
        boxes.add(box)
        radii.add(radius)
        inverses.add(transform?.inversed ?: Matrix44.IDENTITY)
    }

    fun pop() {
        boxes.removeAt(boxes.lastIndex)
        radii.removeAt(radii.lastIndex)
        inverses.removeAt(inverses.lastIndex)
    }

    /** the shade style that clips to the rounded boxes, null when there are none */
    fun style(): ShadeStyle? {
        if (boxes.isEmpty()) return null
        applyTo(style)
        return style
    }
}

/**
 * [rectangle] with its edges on the nearest pixels of a target with [contentScale], as scissoring truncates its
 * rectangle to pixels
 */
internal fun snapToPixels(rectangle: Rectangle, contentScale: Double): Rectangle {
    val x0 = round(rectangle.x * contentScale)
    val y0 = round(rectangle.y * contentScale)
    val x1 = round((rectangle.x + rectangle.width) * contentScale)
    val y1 = round((rectangle.y + rectangle.height) * contentScale)
    return Rectangle(x0 / contentScale, y0 / contentScale, (x1 - x0) / contentScale, (y1 - y0) / contentScale)
}

/** the part of [a] that is in [b], null when they do not overlap */
internal fun intersection(a: Rectangle, b: Rectangle): Rectangle? {
    val x0 = maxOf(a.x, b.x)
    val y0 = maxOf(a.y, b.y)
    val x1 = minOf(a.x + a.width, b.x + b.width)
    val y1 = minOf(a.y + a.height, b.y + b.height)
    return if (x1 > x0 && y1 > y0) Rectangle(x0, y0, x1 - x0, y1 - y0) else null
}
