package org.openrndr.panel

import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.math.round

/** the most rounded boxes that clip at the same time, deeper ones only clip by their rectangles */
private const val maxRoundedClips = 4

/**
 * The rounded boxes that clip what is drawn, of the elements with a border radius that clip their content: a shade
 * style covers the pixels outside their corners, smoothly at the edges. The rectangles are clipped by scissoring.
 */
internal class RoundedClips {
    private val boxes = ArrayList<Rectangle>()
    private val radii = ArrayList<Double>()

    private val style = shadeStyle {
        fragmentTransform = """
            // the position in points, y down from the top
            vec2 p = vec2(c_screenPosition.x, c_screenSize.y - c_screenPosition.y);
            float coverage = 1.0;
            for (int i = 0; i < p_count; i++) {
                vec4 box = p_boxes[i];
                float r = p_radii[i].x;
                vec2 halfSize = box.zw * 0.5;
                vec2 q = abs(p - (box.xy + halfSize)) - halfSize + vec2(r);
                float d = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
                coverage *= clamp(0.5 - d / max(fwidth(d), 1E-4), 0.0, 1.0);
            }
            x_fill.a *= coverage;
            x_stroke.a *= coverage;
        """.trimIndent()
        parameter("boxes", Array(maxRoundedClips) { Vector4.ZERO })
        parameter("radii", Array(maxRoundedClips) { Vector2.ZERO })
        parameter("count", 0)
    }

    fun push(box: Rectangle, radius: Double) {
        boxes.add(box)
        radii.add(radius)
    }

    fun pop() {
        boxes.removeAt(boxes.lastIndex)
        radii.removeAt(radii.lastIndex)
    }

    /** the shade style that clips to the rounded boxes, null when there are none */
    fun style(): ShadeStyle? {
        if (boxes.isEmpty()) return null
        val count = minOf(boxes.size, maxRoundedClips)
        style.parameter("boxes", Array(maxRoundedClips) { i ->
            boxes.getOrNull(i)?.takeIf { i < count }?.let { Vector4(it.x, it.y, it.width, it.height) } ?: Vector4.ZERO
        })
        style.parameter("radii", Array(maxRoundedClips) { i -> Vector2(if (i < count) radii[i] else 0.0, 0.0) })
        style.parameter("count", count)
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
