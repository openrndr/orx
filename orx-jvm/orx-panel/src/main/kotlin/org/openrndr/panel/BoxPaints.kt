package org.openrndr.panel

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.panel.elements.Element
import org.openrndr.panel.elements.cornerRadius
import org.openrndr.panel.style.*
import org.openrndr.shape.Rectangle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** the most color stops of a gradient */
private const val maxStops = 16

/**
 * The paints of the boxes of elements that are drawn under them: their shadows, see [boxShadow], and the gradients of
 * their backgrounds, see [backgroundImage]. A shade style computes them in the coordinates of the element, so that they
 * follow its transform, within the rounded boxes that clip it.
 */
internal class BoxPaints(private val roundedClips: RoundedClips) {
    private val style = shadeStyle {
        fragmentPreamble = clipCoverageGlsl + "\n" + """
            float roundedBoxDistance(vec2 p, vec4 box, float r) {
                vec2 halfSize = box.zw * 0.5;
                vec2 q = abs(p - (box.xy + halfSize)) - halfSize + vec2(r);
                return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
            }
            float erfApprox(float x) {
                float s = sign(x);
                float a = abs(x);
                float t = 1.0 / (1.0 + 0.3275911 * a);
                float y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-a * a);
                return s * y;
            }
            vec3 srgbToLinear(vec3 c) {
                return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(vec3(0.04045), c));
            }
        """.trimIndent()
        fragmentTransform = """
            // the position in the coordinates of the element, y down
            vec2 p = p_rect.xy + c_boundsPosition.xy * p_rect.zw;
            // the coverage of the box of the element
            float boxDistance = roundedBoxDistance(p, p_box, p_radius);
            float boxCoverage = clamp(0.5 - boxDistance / max(fwidth(boxDistance), 1E-4), 0.0, 1.0);
            vec4 color = vec4(0.0);
            if (p_mode == 2 || p_mode == 3) {
                // a shadow: a rounded box blurred with a gaussian, outside the box of the element, or inside it for an
                // inset shadow, where it is the part of the box outside the blurred box
                float d = roundedBoxDistance(p, p_shadowBox, p_shadowRadius);
                float alpha = p_sigma > 0.0 ? 0.5 * (1.0 - erfApprox(d / (1.41421356 * p_sigma))) : clamp(0.5 - d / max(fwidth(d), 1E-4), 0.0, 1.0);
                float coverage = p_mode == 2 ? alpha * (1.0 - boxCoverage) : (1.0 - alpha) * boxCoverage;
                color = vec4(p_shadowColor.rgb, p_shadowColor.a * coverage);
            } else {
                float t;
                if (p_mode == 0) {
                    vec2 axis = p_end - p_start;
                    t = dot(p - p_start, axis) / max(dot(axis, axis), 1E-9);
                } else {
                    t = length((p - p_start) / max(p_end, vec2(1E-9)));
                }
                if (p_repeating == 1) {
                    float first = p_offsets[0];
                    float last = p_offsets[p_count - 1];
                    float span = max(last - first, 1E-6);
                    t = first + mod(t - first, span);
                }
                // the stops, interpolated in premultiplied sRGB, as CSS does
                vec4 c = p_colors[0];
                for (int i = 1; i < p_count; i++) {
                    float a = p_offsets[i - 1];
                    float b = p_offsets[i];
                    if (t > a) {
                        float f = b > a ? clamp((t - a) / (b - a), 0.0, 1.0) : 1.0;
                        vec4 ca = vec4(p_colors[i - 1].rgb * p_colors[i - 1].a, p_colors[i - 1].a);
                        vec4 cb = vec4(p_colors[i].rgb * p_colors[i].a, p_colors[i].a);
                        vec4 m = mix(ca, cb, f);
                        c = vec4(m.a > 0.0 ? m.rgb / m.a : vec3(0.0), m.a);
                    }
                }
                color = vec4(c.rgb, c.a * boxCoverage);
            }
            x_fill = vec4(srgbToLinear(color.rgb), color.a * clipCoverage(vec2(c_screenPosition.x, c_screenSize.y - c_screenPosition.y)));
            x_stroke.a = 0.0;
        """.trimIndent()
        parameter("rect", Vector4.ZERO)
        parameter("box", Vector4.ZERO)
        parameter("radius", 0.0)
        parameter("mode", 0)
        parameter("start", Vector2.ZERO)
        parameter("end", Vector2.ONE)
        parameter("repeating", 0)
        parameter("colors", Array(maxStops) { Vector4.ZERO })
        parameter("offsets", DoubleArray(maxStops))
        parameter("count", 2)
        parameter("shadowBox", Vector4.ZERO)
        parameter("shadowRadius", 0.0)
        parameter("sigma", 0.0)
        parameter("shadowColor", Vector4.ZERO)
        parameter("clipBoxes", Array(maxRoundedClips) { Vector4.ZERO })
        parameter("clipRadii", Array(maxRoundedClips) { Vector2.ZERO })
        parameter("clipInverses", Array(maxRoundedClips) { org.openrndr.math.Matrix44.IDENTITY })
        parameter("clipCount", 0)
    }

    private fun Rectangle.vector() = Vector4(x, y, width, height)

    private fun ColorRGBa.vector(): Vector4 = toSRGB().let { Vector4(it.r, it.g, it.b, it.alpha) }

    /** draws [rect] with the style, in the coordinates of the element */
    private fun Drawer.paint(rect: Rectangle) {
        style.parameter("rect", rect.vector())
        roundedClips.applyTo(style)
        isolated {
            shadeStyle = style
            fill = ColorRGBa.WHITE
            stroke = null
            rectangle(rect)
        }
    }

    /** draws the shadows and the background image of [element], with the drawer at the position of the element */
    fun draw(drawer: Drawer, element: Element) {
        val cs = element.computedStyle
        val shadows = (cs.boxShadow as? BoxShadow)?.shadows ?: emptyList()
        val image = cs.backgroundImage
        if (shadows.isEmpty() && image !is BackgroundImage.Linear && image !is BackgroundImage.Radial) return
        val box = Rectangle(0.0, 0.0, element.layout.screenWidth, element.layout.screenHeight)
        val radius = element.cornerRadius
        style.parameter("box", box.vector())
        style.parameter("radius", radius)

        // the outer shadows, the first on top
        for (shadow in shadows.reversed().filter { !it.inset }) {
            val shadowBox = Rectangle(box.x + shadow.offset.x - shadow.spread, box.y + shadow.offset.y - shadow.spread,
                box.width + 2.0 * shadow.spread, box.height + 2.0 * shadow.spread)
            if (shadowBox.width <= 0.0 || shadowBox.height <= 0.0) continue
            val sigma = shadow.blur / 2.0
            val extent = 3.0 * sigma + 1.0
            style.parameter("mode", 2)
            style.parameter("shadowBox", shadowBox.vector())
            style.parameter("shadowRadius", if (radius > 0.0) max(0.0, radius + shadow.spread) else 0.0)
            style.parameter("sigma", sigma)
            style.parameter("shadowColor", (shadow.color ?: (cs.color as? Color.RGBa)?.color ?: ColorRGBa.BLACK).vector())
            drawer.paint(Rectangle(shadowBox.x - extent, shadowBox.y - extent, shadowBox.width + 2.0 * extent, shadowBox.height + 2.0 * extent))
        }

        // the gradient, in the box
        if (image is BackgroundImage.Linear || image is BackgroundImage.Radial) drawGradient(drawer, image, box)
    }

    /**
     * Draws the inset shadows of [element] over its background, after the element drew itself, with the drawer at the
     * position of the element
     */
    fun drawOver(drawer: Drawer, element: Element) {
        val cs = element.computedStyle
        val shadows = (cs.boxShadow as? BoxShadow)?.shadows?.filter { it.inset } ?: return
        if (shadows.isEmpty()) return
        val box = Rectangle(0.0, 0.0, element.layout.screenWidth, element.layout.screenHeight)
        val radius = element.cornerRadius
        style.parameter("box", box.vector())
        style.parameter("radius", radius)
        for (shadow in shadows.reversed()) {
            // the box that casts the shadow inside: offset, and shrunk by the spread
            val shadowBox = Rectangle(box.x + shadow.offset.x + shadow.spread, box.y + shadow.offset.y + shadow.spread,
                box.width - 2.0 * shadow.spread, box.height - 2.0 * shadow.spread)
            style.parameter("mode", 3)
            style.parameter("shadowBox", shadowBox.vector())
            style.parameter("shadowRadius", if (radius > 0.0) max(0.0, radius - shadow.spread) else 0.0)
            style.parameter("sigma", shadow.blur / 2.0)
            style.parameter("shadowColor", (shadow.color ?: (cs.color as? Color.RGBa)?.color ?: ColorRGBa.BLACK).vector())
            drawer.paint(box)
        }
    }

    /** draws the gradient [image] in [box], the box of the element */
    private fun drawGradient(drawer: Drawer, image: BackgroundImage, box: Rectangle) {
        val stops = when (image) {
            is BackgroundImage.Linear -> image.stops
            is BackgroundImage.Radial -> image.stops
            else -> return
        }
        val length: Double
        when (image) {
            is BackgroundImage.Linear -> {
                // the direction of the gradient line, and its length, the corners are at its ends
                val angle = image.corner?.let { c ->
                    // towards a corner: perpendicular to the diagonal between the other two corners
                    Math.toDegrees(kotlin.math.atan2(c.x * box.height, -c.y * box.width))
                } ?: image.angle
                val radians = Math.toRadians(angle)
                val direction = Vector2(sin(radians), -cos(radians))
                length = abs(box.width * sin(radians)) + abs(box.height * cos(radians))
                val center = box.center
                style.parameter("mode", 0)
                style.parameter("start", center - direction * (length / 2.0))
                style.parameter("end", center + direction * (length / 2.0))
            }
            is BackgroundImage.Radial -> {
                val center = Vector2(box.x + image.center.x * box.width, box.y + image.center.y * box.height)
                val dx = max(center.x - box.x, box.x + box.width - center.x)
                val dy = max(center.y - box.y, box.y + box.height - center.y)
                // to the farthest corner: a circle through it, or an ellipse with the proportions of the box
                val radii = if (image.circle) Vector2(sqrt(dx * dx + dy * dy), sqrt(dx * dx + dy * dy)) else Vector2(dx, dy) * sqrt(2.0)
                length = radii.x
                style.parameter("mode", 1)
                style.parameter("start", center)
                style.parameter("end", radii)
            }
            else -> return
        }
        // the positions of the stops, as fractions of the gradient line; stops without positions are spread evenly
        val offsets = DoubleArray(stops.size) { Double.NaN }
        for ((i, stop) in stops.withIndex()) {
            offsets[i] = when (val p = stop.position) {
                is LinearDimension.Percent -> p.value / 100.0
                is LinearDimension.PX -> p.value / length
                else -> Double.NaN
            }
        }
        if (offsets[0].isNaN()) offsets[0] = 0.0
        if (offsets.last().isNaN()) offsets[offsets.lastIndex] = 1.0
        var i = 1
        while (i < offsets.size) {
            if (offsets[i].isNaN()) {
                val start = i - 1
                var end = i
                while (offsets[end].isNaN()) end++
                for (k in start + 1 until end) offsets[k] = offsets[start] + (offsets[end] - offsets[start]) * (k - start) / (end - start)
                i = end
            }
            i++
        }
        // a position before an earlier one is that earlier one
        for (k in 1 until offsets.size) offsets[k] = max(offsets[k], offsets[k - 1])
        val count = minOf(stops.size, maxStops)
        style.parameter("colors", Array(maxStops) { k -> stops.getOrNull(minOf(k, count - 1))!!.color.vector() })
        style.parameter("offsets", DoubleArray(maxStops) { k -> offsets[minOf(k, count - 1)] })
        style.parameter("count", count)
        style.parameter("repeating", if ((image as? BackgroundImage.Linear)?.repeating == true || (image as? BackgroundImage.Radial)?.repeating == true) 1 else 0)
        drawer.paint(box)
    }
}
