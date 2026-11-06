package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.composition.FilterColorSpace
import org.openrndr.extra.composition.FilterEffect
import org.openrndr.extra.composition.FilterInput
import org.openrndr.extra.composition.FilterPrimitive
import org.openrndr.extra.composition.FilterPrimitive.TransferFunction
import org.openrndr.extra.composition.FilterReference
import org.openrndr.extra.composition.FilterSubregion
import org.openrndr.extra.composition.GroupNode
import org.openrndr.extra.composition.MixBlendMode
import org.openrndr.extra.composition.Paint
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

private val logger = KotlinLogging.logger {}

/**
 * The filter elements of SVG documents, as [FilterEffect]s, and the filter property with references to them and the
 * filter functions of CSS
 */
internal object SVGFilters {
    private val current = ThreadLocal<Map<String, FilterEffect>>()

    /** the feImage primitives of the document that is being parsed, with their references, see [images] */
    private val currentImages = ThreadLocal<MutableList<Pair<FilterPrimitive.Image, String>>>()

    /** the feImage primitives of the filters of the last document that was parsed, their content is added later */
    val images: List<Pair<FilterPrimitive.Image, String>> get() = currentImages.get() ?: emptyList()

    /** runs [block] with [filters] available to [filter] */
    fun <T> using(filters: Map<String, FilterEffect>, block: () -> T): T {
        val previous = current.get()
        current.set(filters)
        try {
            return block()
        } finally {
            current.set(previous)
        }
    }

    /** the filter elements in the document with [root], by their id */
    fun parse(root: Element): Map<String, FilterEffect> {
        currentImages.set(mutableListOf())
        return root.allElements.filter { it.tagName() == "filter" && it.id().isNotEmpty() }.associate { it.id() to effect(it) }
    }

    /** a property or attribute of [element], the style attribute first */
    private fun property(element: Element, name: String): String? =
        SVGStyleSheet.declarations(element.attr("style")).lastOrNull { it.first == name }?.second
            ?: element.attr(name).trim().takeIf { it.isNotEmpty() }

    private fun numbers(value: String): List<Double> =
        value.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.mapNotNull { it.toDoubleOrNull() }

    /** one or two numbers, the second is the first when it is not given */
    private fun pair(value: String, default: Double): Vector2 {
        val n = numbers(value)
        return Vector2(n.getOrElse(0) { default }, n.getOrElse(1) { n.getOrElse(0) { default } })
    }

    private fun input(value: String): FilterInput = when (val v = value.trim()) {
        "" -> FilterInput.Previous
        "SourceGraphic" -> FilterInput.SourceGraphic
        "SourceAlpha" -> FilterInput.SourceAlpha
        "BackgroundImage", "BackgroundAlpha", "FillPaint", "StrokePaint" -> FilterInput.Unsupported(v)
        else -> FilterInput.Result(v)
    }

    /** the flood color and opacity of a primitive */
    private fun flood(element: Element): ColorRGBa {
        val color = (property(element, "flood-color")?.let { SVGParse.color(it) } as? Paint.RGB)?.value ?: ColorRGBa.BLACK
        val opacity = property(element, "flood-opacity")?.let { SVGParse.number(it).value } ?: 1.0
        return color.opacify(opacity)
    }

    private fun effect(element: Element): FilterEffect {
        val regionUnits = if (element.attr("filterUnits").trim() == "userSpaceOnUse") PaintUnits.OBJECT else PaintUnits.BOUNDS
        val primitiveUnits = if (element.attr("primitiveUnits").trim() == "objectBoundingBox") PaintUnits.BOUNDS else PaintUnits.OBJECT
        fun region(name: String, default: String, axis: SVGParse.LengthAxis): Double {
            val value = element.attr(name).trim().ifEmpty { default }
            return if (regionUnits == PaintUnits.BOUNDS) {
                if (value.endsWith("%")) (value.dropLast(1).toDoubleOrNull() ?: 0.0) / 100.0 else value.toDoubleOrNull() ?: 0.0
            } else {
                SVGParse.lengthValue(value, element, axis) ?: 0.0
            }
        }
        val region = Rectangle(
            region("x", "-10%", SVGParse.LengthAxis.X), region("y", "-10%", SVGParse.LengthAxis.Y),
            region("width", "120%", SVGParse.LengthAxis.X), region("height", "120%", SVGParse.LengthAxis.Y)
        )
        val primitives = element.children().mapNotNull { child -> primitive(child, element, primitiveUnits) }
        return FilterEffect(primitives, region, regionUnits, primitiveUnits, element.id())
    }

    private fun primitive(element: Element, filter: Element, units: PaintUnits): FilterPrimitive? {
        val input = input(element.attr("in"))
        val input2 = input(element.attr("in2"))
        val primitive = when (element.tagName()) {
            "feGaussianBlur" -> FilterPrimitive.GaussianBlur(input, pair(element.attr("stdDeviation"), 0.0))
            "feOffset" -> FilterPrimitive.Offset(input, element.attr("dx").toDoubleOrNull() ?: 0.0, element.attr("dy").toDoubleOrNull() ?: 0.0)
            "feFlood" -> FilterPrimitive.Flood(flood(element))
            "feMerge" -> FilterPrimitive.Merge(element.children().filter { it.tagName() == "feMergeNode" }.map { input(it.attr("in")) })
            "feComposite" -> FilterPrimitive.Composite(
                input, input2,
                FilterPrimitive.CompositeOperator.entries.firstOrNull { it.name.lowercase() == element.attr("operator").trim() }
                    ?: FilterPrimitive.CompositeOperator.OVER,
                element.attr("k1").toDoubleOrNull() ?: 0.0, element.attr("k2").toDoubleOrNull() ?: 0.0,
                element.attr("k3").toDoubleOrNull() ?: 0.0, element.attr("k4").toDoubleOrNull() ?: 0.0
            )
            "feBlend" -> FilterPrimitive.Blend(input, input2, MixBlendMode.of(element.attr("mode").ifBlank { "normal" }) ?: MixBlendMode.NORMAL)
            "feColorMatrix" -> {
                val values = numbers(element.attr("values"))
                FilterPrimitive.ColorMatrix(input, when (element.attr("type").trim()) {
                    "saturate" -> FilterPrimitive.ColorMatrix.saturate(values.firstOrNull() ?: 1.0)
                    "hueRotate" -> FilterPrimitive.ColorMatrix.hueRotate(values.firstOrNull() ?: 0.0)
                    "luminanceToAlpha" -> FilterPrimitive.ColorMatrix.luminanceToAlpha
                    else -> values.takeIf { it.size == 20 } ?: FilterPrimitive.ColorMatrix.saturate(1.0)
                })
            }
            "feComponentTransfer" -> {
                fun function(tag: String): TransferFunction {
                    val f = element.children().lastOrNull { it.tagName() == tag } ?: return TransferFunction.Identity
                    fun number(name: String, default: Double) = f.attr(name).toDoubleOrNull() ?: default
                    return when (f.attr("type").trim()) {
                        "table" -> TransferFunction.Table(numbers(f.attr("tableValues")))
                        "discrete" -> TransferFunction.Discrete(numbers(f.attr("tableValues")))
                        "linear" -> TransferFunction.Linear(number("slope", 1.0), number("intercept", 0.0))
                        "gamma" -> TransferFunction.Gamma(number("amplitude", 1.0), number("exponent", 1.0), number("offset", 0.0))
                        else -> TransferFunction.Identity
                    }
                }
                FilterPrimitive.ComponentTransfer(input, function("feFuncR"), function("feFuncG"), function("feFuncB"), function("feFuncA"))
            }
            "feMorphology" -> FilterPrimitive.Morphology(input, element.attr("operator").trim() == "dilate", pair(element.attr("radius"), 0.0))
            "feDropShadow" -> FilterPrimitive.DropShadow(
                input, element.attr("dx").toDoubleOrNull() ?: 2.0, element.attr("dy").toDoubleOrNull() ?: 2.0,
                pair(element.attr("stdDeviation"), 2.0), flood(element)
            )
            "feTurbulence" -> FilterPrimitive.Turbulence(
                pair(element.attr("baseFrequency"), 0.0), element.attr("numOctaves").toIntOrNull() ?: 1,
                element.attr("seed").toDoubleOrNull() ?: 0.0, element.attr("type").trim() == "fractalNoise"
            )
            "feDisplacementMap" -> {
                fun channel(name: String) = FilterPrimitive.Channel.entries.firstOrNull { it.name == element.attr(name).trim() }
                    ?: FilterPrimitive.Channel.A
                FilterPrimitive.DisplacementMap(input, input2, element.attr("scale").toDoubleOrNull() ?: 0.0,
                    channel("xChannelSelector"), channel("yChannelSelector"))
            }
            "feTile" -> FilterPrimitive.Tile(input)
            "feImage" -> {
                val href = listOf("href", "xlink:href").firstNotNullOfOrNull { name -> element.attr(name).trim().takeIf { it.isNotEmpty() } }
                FilterPrimitive.Image(GroupNode(), aspectRatio = SVGParse.preserveAspectRatio(element), href = href).also { image ->
                    // the content is added when the document is converted
                    if (href != null) currentImages.get()?.add(image to href)
                }
            }
            else -> {
                logger.warn { "filter primitive ${element.tagName()} is not supported, its input is passed on" }
                FilterPrimitive.ColorMatrix(input, FilterPrimitive.ColorMatrix.saturate(1.0))
            }
        }
        primitive.result = element.attr("result").trim().takeIf { it.isNotEmpty() }
        // the subregion, in the units of the primitives: fractions of the bounds, or lengths in user space
        fun component(name: String, axis: SVGParse.LengthAxis): Double? {
            val value = element.attr(name).trim().takeIf { it.isNotEmpty() } ?: return null
            return if (units == PaintUnits.BOUNDS) {
                if (value.endsWith("%")) value.dropLast(1).toDoubleOrNull()?.div(100.0) else value.toDoubleOrNull()
            } else SVGParse.lengthValue(value, element, axis)
        }
        val x = component("x", SVGParse.LengthAxis.X)
        val y = component("y", SVGParse.LengthAxis.Y)
        val width = component("width", SVGParse.LengthAxis.X)
        val height = component("height", SVGParse.LengthAxis.Y)
        if (x != null || y != null || width != null || height != null) primitive.subregion = FilterSubregion(x, y, width, height)
        // color-interpolation-filters is inherited from the filter element and the elements it is in
        val space = property(element, "color-interpolation-filters")?.takeIf { it != "auto" && it != "inherit" }
            ?: generateSequence(filter) { it.parent() }.firstNotNullOfOrNull { e ->
                property(e, "color-interpolation-filters")?.takeIf { it != "auto" && it != "inherit" }
            }
        primitive.colorSpace = if (space == "sRGB") FilterColorSpace.SRGB else FilterColorSpace.LINEAR_RGB
        return primitive
    }

    /** an amount of a filter function: a number or a percentage */
    private fun amount(value: String?, default: Double): Double {
        val v = value?.trim()?.takeIf { it.isNotEmpty() } ?: return default
        return if (v.endsWith("%")) (v.dropLast(1).toDoubleOrNull() ?: 0.0) / 100.0 else v.toDoubleOrNull() ?: default
    }

    /** the primitives of a filter function of CSS, they compute in sRGB */
    private fun function(name: String, arguments: String, element: Element?): FilterPrimitive? {
        val a = arguments.trim()
        fun matrix(values: List<Double>) = FilterPrimitive.ColorMatrix(values = values)
        fun linear(slope: Double, intercept: Double) = TransferFunction.Linear(slope, intercept)
        val primitive = when (name) {
            "blur" -> FilterPrimitive.GaussianBlur(stdDeviation = (SVGParse.lengthValue(a.ifEmpty { "0" }, element) ?: 0.0).let { Vector2(it, it) })
            "brightness" -> amount(a, 1.0).let { FilterPrimitive.ComponentTransfer(red = linear(it, 0.0), green = linear(it, 0.0), blue = linear(it, 0.0)) }
            "contrast" -> amount(a, 1.0).let { val f = linear(it, 0.5 - 0.5 * it); FilterPrimitive.ComponentTransfer(red = f, green = f, blue = f) }
            "grayscale" -> (1.0 - amount(a, 1.0).coerceIn(0.0, 1.0)).let { g ->
                matrix(listOf(
                    0.2126 + 0.7874 * g, 0.7152 - 0.7152 * g, 0.0722 - 0.0722 * g, 0.0, 0.0,
                    0.2126 - 0.2126 * g, 0.7152 + 0.2848 * g, 0.0722 - 0.0722 * g, 0.0, 0.0,
                    0.2126 - 0.2126 * g, 0.7152 - 0.7152 * g, 0.0722 + 0.9278 * g, 0.0, 0.0,
                    0.0, 0.0, 0.0, 1.0, 0.0
                ))
            }
            "sepia" -> (1.0 - amount(a, 1.0).coerceIn(0.0, 1.0)).let { s ->
                matrix(listOf(
                    0.393 + 0.607 * s, 0.769 - 0.769 * s, 0.189 - 0.189 * s, 0.0, 0.0,
                    0.349 - 0.349 * s, 0.686 + 0.314 * s, 0.168 - 0.168 * s, 0.0, 0.0,
                    0.272 - 0.272 * s, 0.534 - 0.534 * s, 0.131 + 0.869 * s, 0.0, 0.0,
                    0.0, 0.0, 0.0, 1.0, 0.0
                ))
            }
            "saturate" -> matrix(FilterPrimitive.ColorMatrix.saturate(amount(a, 1.0)))
            "hue-rotate" -> matrix(FilterPrimitive.ColorMatrix.hueRotate(SVGParse.angle(a.ifEmpty { "0" }) ?: 0.0))
            "invert" -> amount(a, 1.0).coerceIn(0.0, 1.0).let {
                val f = TransferFunction.Table(listOf(it, 1.0 - it))
                FilterPrimitive.ComponentTransfer(red = f, green = f, blue = f)
            }
            "opacity" -> FilterPrimitive.ComponentTransfer(alpha = TransferFunction.Table(listOf(0.0, amount(a, 1.0).coerceIn(0.0, 1.0))))
            "drop-shadow" -> {
                // a color and two or three lengths, in any order of color and lengths
                val parts = Regex("[a-z]+\\([^)]*\\)|#[0-9a-fA-F]+|\\S+").findAll(a).map { it.value }.toList()
                val lengths = parts.mapNotNull { SVGParse.lengthValue(it, element) }
                val color = parts.firstOrNull { SVGParse.lengthValue(it, element) == null }
                    ?.let { (SVGParse.color(it) as? Paint.RGB)?.value } ?: ColorRGBa.BLACK
                val blur = lengths.getOrElse(2) { 0.0 } / 2.0
                FilterPrimitive.DropShadow(dx = lengths.getOrElse(0) { 0.0 }, dy = lengths.getOrElse(1) { 0.0 }, stdDeviation = Vector2(blur, blur), color = color)
            }
            else -> {
                logger.warn { "filter function $name is not supported" }
                null
            }
        }
        return primitive?.also { it.colorSpace = FilterColorSpace.SRGB }
    }

    /** the filter property: none, or references to filter elements and filter functions, applied in order */
    fun filter(value: String, element: Element?): FilterReference {
        if (value.trim() == "none") return FilterReference.None
        val effects = mutableListOf<FilterEffect>()
        val functions = mutableListOf<FilterPrimitive>()
        fun flush() {
            // filter functions are not clipped to a region
            if (functions.isNotEmpty()) effects.add(FilterEffect(functions.toList(), region = null))
            functions.clear()
        }
        for (match in Regex("([a-z-]+)\\(((?:[^()]|\\([^()]*\\))*)\\)").findAll(value)) {
            val name = match.groupValues[1]
            val arguments = match.groupValues[2]
            if (name == "url") {
                flush()
                val id = arguments.trim().trim('"', '\'').removePrefix("#")
                val effect = current.get()?.get(id)
                if (effect == null) {
                    // a reference to a filter that does not exist means the element is not drawn, as in browsers
                    logger.warn { "filter refers to '#$id', which is not a filter in the document" }
                    effects.add(FilterEffect(listOf(FilterPrimitive.Flood(ColorRGBa.TRANSPARENT)), region = null))
                } else {
                    effects.add(effect)
                }
            } else {
                function(name, arguments, element)?.let { functions.add(it) }
            }
        }
        flush()
        return if (effects.isEmpty()) FilterReference.None else FilterReference.Value(effects)
    }
}
