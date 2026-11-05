package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.AspectRatio
import org.openrndr.extra.composition.ClipPath
import org.openrndr.extra.composition.GroupNode
import org.openrndr.extra.composition.Length
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.composition.ViewBox
import org.openrndr.extra.shadestyles.presets.ColorStop
import org.openrndr.extra.shadestyles.presets.GradientSpread
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * The linearGradient and radialGradient elements of SVG documents, as [Paint.Gradient]s
 */
internal object SVGGradients {
    private val current = ThreadLocal<Map<String, Paint.Server>>()
    private val currentClips = ThreadLocal<Map<String, ClipPath.Shapes>>()

    /**
     * runs [block] with [gradients], gradients and patterns, available to [lookup], and [clips] to [lookupClip]
     */
    fun <T> using(
        gradients: Map<String, Paint.Server>,
        clips: Map<String, ClipPath.Shapes> = emptyMap(),
        block: () -> T
    ): T {
        val previous = current.get()
        val previousClips = currentClips.get()
        current.set(gradients)
        currentClips.set(clips)
        try {
            return block()
        } finally {
            current.set(previous)
            currentClips.set(previousClips)
        }
    }

    /** the clip path with [id] of the document that is being parsed */
    fun lookupClip(id: String): ClipPath.Shapes? = currentClips.get()?.get(id).also {
        if (it == null) logger.warn { "clip-path refers to '#$id', which is not a clip path in the document" }
    }

    /**
     * The clip paths in the document with [root], by their id, with their elements. Their content is empty, it is
     * added when the elements are converted.
     */
    fun parseClipPaths(root: Element): Map<String, Pair<ClipPath.Shapes, Element>> =
        root.allElements.filter { it.tagName() == "clipPath" && it.id().isNotEmpty() }.associate { element ->
            val units = if (element.attr("clipPathUnits").trim() == "objectBoundingBox") PaintUnits.BOUNDS else PaintUnits.OBJECT
            element.id() to Pair(ClipPath.Shapes(GroupNode(), units, element.id()), element)
        }

    /** the gradient or pattern with [id] of the document that is being parsed */
    fun lookup(id: String): Paint.Server? = current.get()?.get(id).also {
        if (it == null) logger.warn { "paint refers to '#$id', which is not a gradient or pattern in the document" }
    }

    /**
     * The patterns in the document with [root], by their id, with the element that holds their content: their own
     * element, or the first pattern they refer to with content. The content of the patterns is empty, it is added
     * when the elements are converted.
     */
    fun parsePatterns(root: Element): Map<String, Pair<Paint.Pattern, Element?>> {
        val elements = root.allElements.filter { it.tagName() == "pattern" && it.id().isNotEmpty() }
            .associateBy { it.id() }
        val viewport = viewport(root)
        return elements.mapValues { (_, element) -> pattern(element, elements, viewport) }
    }

    private fun pattern(element: Element, elements: Map<String, Element>, viewport: Vector2): Pair<Paint.Pattern, Element?> {
        val chain = chain(element, elements)
        fun attr(name: String): String? = chain.firstNotNullOfOrNull { e -> e.attr(name).trim().takeIf { it.isNotEmpty() } }
        fun units(name: String, default: PaintUnits) = when (attr(name)) {
            "userSpaceOnUse" -> PaintUnits.OBJECT
            "objectBoundingBox" -> PaintUnits.BOUNDS
            else -> default
        }
        val units = units("patternUnits", PaintUnits.BOUNDS)
        val contentUnits = units("patternContentUnits", PaintUnits.OBJECT)
        fun coordinate(name: String, size: Double) = coordinate(attr(name) ?: "0", units, size)
        val tile = Rectangle(
            coordinate("x", viewport.x), coordinate("y", viewport.y),
            coordinate("width", viewport.x), coordinate("height", viewport.y)
        )
        val transform = chain.firstOrNull { it.attr("patternTransform").isNotBlank() }
            ?.let { SVGParse.transform(it, "patternTransform").value } ?: Matrix44.IDENTITY
        val viewBox = chain.firstOrNull { it.attr("viewBox").isNotBlank() }
            ?.let { (SVGParse.viewBox(it) as? ViewBox.Value)?.value }
        val aspectRatio = chain.firstOrNull { it.attr("preserveAspectRatio").isNotBlank() }
            ?.let { SVGParse.preserveAspectRatio(it) } ?: AspectRatio.DEFAULT
        val content = chain.firstOrNull { e -> e.children().any { it.tagName() !in NON_CONTENT_TAGS } }
        val pattern = Paint.Pattern(
            GroupNode(), tile, units, contentUnits, viewBox, aspectRatio, transform, element.id()
        )
        return pattern to content
    }

    private val NON_CONTENT_TAGS = setOf("desc", "title", "metadata")

    /** the gradients in the document with [root], by their id */
    fun parse(root: Element): Map<String, Paint.Gradient> {
        val elements = root.allElements.filter { it.tagName() in GRADIENT_TAGS && it.id().isNotEmpty() }
            .associateBy { it.id() }
        val viewport = viewport(root)
        return elements.mapNotNull { (id, element) ->
            gradient(element, elements, viewport)?.let { id to it }
        }.toMap()
    }

    private val GRADIENT_TAGS = setOf("linearGradient", "radialGradient")

    /** the size of the viewport of the document, which percentages of user space coordinates refer to */
    private fun viewport(root: Element): Vector2 {
        (SVGParse.viewBox(root) as? ViewBox.Value)?.let { return Vector2(it.value.width, it.value.height) }
        val bounds = SVGParse.bounds(root)
        return Vector2(
            (bounds.width as? Length.Pixels)?.value ?: 0.0,
            (bounds.height as? Length.Pixels)?.value ?: 0.0
        ).let { if (it.x > 0.0 && it.y > 0.0) it else Vector2(100.0, 100.0) }
    }

    /** the gradient elements that [element] refers to with href, starting with [element] */
    private fun chain(element: Element, elements: Map<String, Element>): List<Element> {
        val chain = mutableListOf(element)
        var next = href(element)
        while (next != null) {
            val target = elements[next] ?: break
            if (target in chain) {
                logger.warn { "gradient '${element.id()}' refers to itself" }
                break
            }
            chain.add(target)
            next = href(target)
        }
        return chain
    }

    private fun href(element: Element): String? = listOf("href", "xlink:href").firstNotNullOfOrNull {
        element.attr(it).trim().takeIf { value -> value.startsWith("#") }?.removePrefix("#")
    }

    private fun gradient(element: Element, elements: Map<String, Element>, viewport: Vector2): Paint.Gradient? {
        val chain = chain(element, elements)

        /** the attribute [name] of the gradient or the first gradient it refers to that has it */
        fun attr(name: String): String? = chain.firstNotNullOfOrNull { e -> e.attr(name).trim().takeIf { it.isNotEmpty() } }

        val units = when (attr("gradientUnits")) {
            "userSpaceOnUse" -> PaintUnits.OBJECT
            else -> PaintUnits.BOUNDS
        }
        val spread = when (attr("spreadMethod")) {
            "reflect" -> GradientSpread.REFLECT
            "repeat" -> GradientSpread.REPEAT
            else -> GradientSpread.PAD
        }
        val transform = chain.firstOrNull { it.attr("gradientTransform").isNotBlank() }
            ?.let { SVGParse.transform(it, "gradientTransform").value } ?: Matrix44.IDENTITY
        val stops = chain.firstNotNullOfOrNull { e -> e.children().filter { it.tagName() == "stop" }.takeIf { it.isNotEmpty() } }
            ?.map(::stop)?.let(::monotonic) ?: emptyList()

        val diagonal = sqrt((viewport.x * viewport.x + viewport.y * viewport.y) / 2.0)
        fun coordinate(name: String, default: String, size: Double): Double =
            coordinate(attr(name) ?: default, units, size)

        return when (element.tagName()) {
            "linearGradient" -> Paint.LinearGradient(
                start = Vector2(coordinate("x1", "0%", viewport.x), coordinate("y1", "0%", viewport.y)),
                end = Vector2(coordinate("x2", "100%", viewport.x), coordinate("y2", "0%", viewport.y)),
                stops = stops, units = units, spread = spread, transform = transform, id = element.id()
            )

            "radialGradient" -> {
                val center = Vector2(coordinate("cx", "50%", viewport.x), coordinate("cy", "50%", viewport.y))
                Paint.RadialGradient(
                    center = center,
                    radius = coordinate("r", "50%", diagonal),
                    focal = Vector2(
                        attr("fx")?.let { coordinate(it, units, viewport.x) } ?: center.x,
                        attr("fy")?.let { coordinate(it, units, viewport.y) } ?: center.y
                    ),
                    focalRadius = coordinate("fr", "0%", diagonal),
                    stops = stops, units = units, spread = spread, transform = transform, id = element.id()
                )
            }

            else -> null
        }
    }

    /** [stops] with offsets that are smaller than the offset of a previous stop set to that offset, as in SVG */
    private fun monotonic(stops: List<ColorStop>): List<ColorStop> {
        var last = 0.0
        return stops.map { stop -> stop.copy(offset = stop.offset.coerceAtLeast(last)).also { last = it.offset } }
    }

    /** a coordinate or length: a percentage of [size] in user space, of 1 in bounding box units */
    private fun coordinate(value: String, units: PaintUnits, size: Double): Double {
        val trimmed = value.trim()
        return if (trimmed.endsWith("%")) {
            val fraction = (trimmed.dropLast(1).toDoubleOrNull() ?: 0.0) / 100.0
            if (units == PaintUnits.BOUNDS) fraction else fraction * size
        } else {
            trimmed.toDoubleOrNull() ?: SVGParse.length(trimmed).value
        }
    }

    /** a stop element: its offset, stop-color and stop-opacity, given as attributes or in its style */
    private fun stop(element: Element): ColorStop {
        val properties = element.attr("style").split(";").mapNotNull {
            val parts = it.split(":").map(String::trim)
            if (parts.size >= 2) parts[0] to parts[1] else null
        }.toMap()

        fun property(name: String) = properties[name] ?: element.attr(name).trim().takeIf { it.isNotEmpty() }

        val offset = element.attr("offset").trim().let {
            if (it.endsWith("%")) (it.dropLast(1).toDoubleOrNull() ?: 0.0) / 100.0 else it.toDoubleOrNull() ?: 0.0
        }
        val color = when (val paint = property("stop-color")?.let { SVGParse.color(it) }) {
            is Paint.RGB -> paint.value
            else -> ColorRGBa.BLACK
        }
        val opacity = property("stop-opacity")?.toDoubleOrNull() ?: 1.0
        return ColorStop(offset.coerceIn(0.0, 1.0), color.copy(alpha = opacity.coerceIn(0.0, 1.0)))
    }
}
