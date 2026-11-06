package org.openrndr.extra.svg

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ImageFileFormat
import org.jsoup.nodes.*
import org.openrndr.extra.composition.*
import org.openrndr.extra.composition.TextNode
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.math.Matrix44

import java.io.*

/**
 * Saves the vector composition to a file in SVG format.
 *
 * Only files with the ".svg" extension are supported. Writing to files with unsupported extensions
 * will result in an exception.
 *
 * @param file the file to which the composition should be saved. The file must have an ".svg" extension.
 * @throws IllegalArgumentException if the file does not have the ".svg" extension.
 */
fun Composition.saveToFile(file: File) {
    if (file.extension == "svg") {
        val svg = writeSVG(this)
        file.writeText(svg)
    } else {
        throw IllegalArgumentException("can only write svg files, the extension '${file.extension}' is not supported")
    }
}

/**
 * Converts a [Composition] into its equivalent SVG representation.
 *
 * This method serializes the vector composition, including its structure, style,
 * and attributes, into an SVG string. The resulting SVG can be used for rendering
 * or exporting the composition to an SVG file.
 *
 * The method internally utilizes the [writeSVG] function for the conversion process.
 *
 * @receiver The [Composition] instance to be converted into SVG.
 * @return A [String] representing the composition in SVG format.
 */
fun Composition.toSVG() = writeSVG(this)

private val CompositionNode.svgId: String
    get() = when (val tempId = id) {
        "" -> ""
        null -> ""
        else -> "id=\"$tempId\""
    }

private val CompositionNode.svgAttributes: String
    get() {
        return attributes.map {
            if (it.value != null && it.value != "") {
                "${it.key}=\"${Entities.escape(it.value ?: "")}\""
            } else {
                it.key
            }
        }.joinToString(" ")
    }

private fun Styleable.serialize(parentStyleable: Styleable? = null): String {
    val sb = StringBuilder()

    /**
     * true when the value of [key] does not need to be written: an inherited property with the value of the parent,
     * the effective style of the parent node, or a property that is not inherited with its initial value
     */
    fun redundant(key: AttributeOrPropertyKey): Boolean {
        if (parentStyleable == null) return false
        val value = this.properties[key].toString()
        return if (isInheritedProperty(key)) value == (parentStyleable[key] ?: initialValue(key)).toString()
        else value == initialValue(key).toString()
    }

    // properties that are not presentation attributes are written in a style attribute
    val cssOnly = setOf(
        AttributeOrPropertyKey.MIX_BLEND_MODE, AttributeOrPropertyKey.ISOLATION, AttributeOrPropertyKey.FILTER,
        AttributeOrPropertyKey.WHITE_SPACE, AttributeOrPropertyKey.LINE_HEIGHT,
        AttributeOrPropertyKey.FONT_FEATURE_SETTINGS, AttributeOrPropertyKey.FONT_VARIATION_SETTINGS
    )
    val css = this.properties.filter { (key, value) ->
        key in cssOnly && value.toString().isNotEmpty() &&
                !redundant(key)
    }
    if (css.isNotEmpty()) {
        sb.append("style=\"${css.entries.joinToString(";") { (key, value) -> "$key:$value" }}\" ")
    }
    val filtered = this.properties.filter {
        it.key != AttributeOrPropertyKey.SHADESTYLE && it.key !in cssOnly
    }
    // Inheritance can't be checked without a parentStyleable
    when (parentStyleable) {
        null -> filtered.forEach { (t, u) ->
            if (u.toString().isNotEmpty()) {
                sb.append("$t=\"${u.toString()}\" ")
            }
        }
        else -> filtered.forEach { (t, u) ->
            if (u.toString().isNotEmpty() && !redundant(t)) {
                sb.append("$t=\"${u.toString()}\" ")
            }
        }

    }

    return sb.trim().toString()
}

/**
 * Generates an SVG string representation of a given vector composition.
 *
 * @param composition the vector composition to be converted to an SVG string
 * @param topLevelId an optional ID for the top-level SVG element, defaults to "openrndr-svg"
 * @return a string containing the SVG representation of the composition
 */
fun writeSVG(
    composition: Composition,
    topLevelId: String = "openrndr-svg"
): String {
    val sb = StringBuilder()
    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")

    val defaultNamespaces = mapOf(
        "xmlns" to "http://www.w3.org/2000/svg",
        "xmlns:xlink" to "http://www.w3.org/1999/xlink"
    )

    val namespaces = (defaultNamespaces + composition.namespaces).map { (k, v) ->
        "$k=\"$v\""
    }.joinToString(" ")

    val styleSer = composition.style.serialize()
    val docStyleSer = composition.documentStyle.serialize()

    sb.append("<svg version=\"1.2\" baseProfile=\"tiny\" id=\"$topLevelId\" $namespaces $styleSer $docStyleSer>")
    val ids = WriterIds()
    sb.appendFontFaces(composition)
    sb.appendGradients(composition)
    sb.appendPatterns(composition, ids)
    sb.appendClipPaths(composition, ids)
    sb.appendMarkers(composition, ids)
    sb.appendMasks(composition, ids)
    sb.appendFilters(composition)

    sb.appendNodes(composition.root, ids)
    sb.append("</svg>")
    return sb.toString()
}

/** the title and desc elements of the title and description of this node, see [loadSVG] */
private val CompositionNode.metadata: String
    get() = listOfNotNull(
        (userData["title"] as? String)?.let { "<title>${Entities.escape(it)}</title>" },
        (userData["desc"] as? String)?.let { "<desc>${Entities.escape(it)}</desc>" }
    ).joinToString("")

/**
 * The text runs in this group and its descendants, null when the group holds anything else
 */
private fun GroupNode.textRuns(): List<TextNode>? = children.flatMap {
    when (it) {
        is TextNode -> listOf(it)
        is GroupNode -> it.textRuns() ?: return null
        else -> return null
    }
}

/**
 * Does this group hold only text, like the text elements that are loaded? Text along a contour is only written as a
 * group when all runs share the contour and only the first run has a start offset.
 */
private fun GroupNode.isText(): Boolean {
    val runs = textRuns()
    if (runs.isNullOrEmpty()) return false
    val contour = runs.first().contour ?: return runs.all { it.contour == null }
    return runs.all { it.contour === contour } && runs.drop(1).all { it.x == null }
}

/**
 * Writes a text group as a text or tspan [element], with a tspan for every nested group and for every run that has
 * a position or a style of its own, so that runs without a position continue after the previous run.
 * @param pathId the id of the path that the text is placed along, its contents are written in a textPath element
 */
private fun StringBuilder.appendText(node: CompositionNode, element: String, pathId: String? = null) {
    val styleSerialized = node.style.serialize(node.parent?.effectiveStyle)
    when (node) {
        is GroupNode -> {
            val attributes = listOf(node.svgId, styleSerialized, node.svgAttributes)
                .filter(String::isNotEmpty)
                .joinToString(" ")
            append("<$element${if (attributes.isEmpty()) "" else " $attributes"}>")
            append(node.metadata)
            if (pathId != null) {
                val startOffset = node.textRuns()?.first()?.x?.takeIf { it != 0.0 }
                    ?.let { "startOffset=\"$it\"" } ?: ""
                // a single group in the text is written as the textPath element, like the loaded textPath elements,
                // properties like text-anchor apply to the textPath element but not to a tspan in it
                val single = node.children.singleOrNull() as? GroupNode
                val pathAttributes = listOf(
                    "href=\"#$pathId\"", startOffset,
                    single?.svgId ?: "", single?.style?.serialize(node.effectiveStyle) ?: "", single?.svgAttributes ?: ""
                ).filter(String::isNotEmpty).joinToString(" ")
                append("<textPath $pathAttributes>")
                (single?.children ?: node.children).forEach { appendText(it, "tspan") }
                append("</textPath>")
            } else {
                node.children.forEach { appendText(it, "tspan") }
            }
            append("</$element>")
        }

        is TextNode -> {
            val escapedText = Entities.escape(node.text)
            // the x of a run along a contour is the start offset, written with the textPath element
            val position = listOfNotNull(
                node.x?.takeIf { node.contour == null }?.let { "x=\"$it\"" },
                node.y?.takeIf { node.contour == null }?.let { "y=\"$it\"" },
                node.dx.takeIf { it != 0.0 }?.let { "dx=\"$it\"" },
                node.dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" },
                node.rotate.takeIf { it != 0.0 }?.let { "rotate=\"$it\"" }
            )
            val attributes = (listOf(node.svgId, styleSerialized, node.svgAttributes) + position)
                .filter(String::isNotEmpty)
                .joinToString(" ")
            if (attributes.isEmpty()) {
                append(escapedText)
            } else {
                append("<tspan $attributes>$escapedText</tspan>")
            }
        }

        else -> {}
    }
}


private enum class VisitStage {
    PRE,
    POST
}

private fun process(compositionNode: CompositionNode, visitor: CompositionNode.(stage: VisitStage) -> Unit) {
    compositionNode.visitor(VisitStage.PRE)
    if (compositionNode is GroupNode) {
        compositionNode.children.forEach { process(it, visitor) }
    }
    compositionNode.visitor(VisitStage.POST)
}
/**
 * The nodes of [root] and the nodes of the patterns, clip paths, masks and markers that they refer to, which are
 * written in defs elements and can refer to gradients, patterns, clip paths, masks and markers themselves
 */
private fun allNodes(root: CompositionNode): List<CompositionNode> {
    val nodes = mutableListOf<CompositionNode>()
    val visited = mutableSetOf<GroupNode>()
    fun visit(node: CompositionNode) {
        process(node) { stage ->
            if (stage == VisitStage.PRE) {
                nodes.add(this)
                val referenced = listOfNotNull(
                    (style.fill as? Paint.Pattern)?.content, (style.stroke as? Paint.Pattern)?.content,
                    (style.clipPath as? ClipPath.Shapes)?.content, style.mask.value?.content,
                    style.markerStart.value?.content, style.markerMid.value?.content, style.markerEnd.value?.content
                )
                for (content in referenced) if (visited.add(content)) visit(content)
            }
        }
    }
    visit(root)
    return nodes
}

/** the gradients that the fills and strokes of the nodes of [composition] use, by their id */
private fun gradients(composition: Composition): Map<String, Paint.Gradient> {
    val gradients = LinkedHashMap<String, Paint.Gradient>()
    for (node in allNodes(composition.root)) {
        for (paint in listOf(node.style.fill, node.style.stroke)) {
            if (paint is Paint.Gradient) gradients.getOrPut(paint.svgId) { paint }
        }
    }
    return gradients
}

/** writes a defs element with the gradients that [composition] uses, when it uses any */
private fun StringBuilder.appendGradients(composition: Composition) {
    val gradients = gradients(composition)
    if (gradients.isEmpty()) return
    append("<defs>")
    for ((id, gradient) in gradients) {
        val units = if (gradient.units == PaintUnits.OBJECT) "userSpaceOnUse" else "objectBoundingBox"
        val spread = gradient.spread.name.lowercase()
        val t = gradient.transform
        val transform = if (t == Matrix44.IDENTITY) "" else
            " gradientTransform=\"matrix(${t.c0r0} ${t.c0r1} ${t.c1r0} ${t.c1r1} ${t.c3r0} ${t.c3r1})\""
        val tag = when (gradient) {
            is Paint.LinearGradient -> {
                append("<linearGradient id=\"$id\" x1=\"${gradient.start.x}\" y1=\"${gradient.start.y}\" ")
                append("x2=\"${gradient.end.x}\" y2=\"${gradient.end.y}\"")
                "linearGradient"
            }
            is Paint.RadialGradient -> {
                append("<radialGradient id=\"$id\" cx=\"${gradient.center.x}\" cy=\"${gradient.center.y}\" ")
                append("r=\"${gradient.radius}\" fx=\"${gradient.focal.x}\" fy=\"${gradient.focal.y}\" ")
                append("fr=\"${gradient.focalRadius}\"")
                "radialGradient"
            }
        }
        val interpolation = if (gradient.interpolation == org.openrndr.extra.shadestyles.presets.GradientInterpolation.LINEAR_RGB)
            " color-interpolation=\"linearRGB\"" else ""
        append(" gradientUnits=\"$units\" spreadMethod=\"$spread\"$transform$interpolation>")
        for (stop in gradient.stops) {
            val color = Paint.RGB(stop.color.copy(alpha = 1.0)).toString()
            append("<stop offset=\"${stop.offset}\" stop-color=\"$color\" stop-opacity=\"${stop.color.alpha}\"/>")
        }
        append("</$tag>")
    }
    append("</defs>\n")
}

/** counters for the ids that the writer generates */
private class WriterIds {
    var textPath = 0
}

/** writes [root] and the nodes in it */
private fun StringBuilder.appendNodes(root: CompositionNode, ids: WriterIds) {
    val sb = this
    // groups written as text elements, their contents are written with them
    val textGroups = mutableSetOf<CompositionNode>()
    process(root) { stage ->
        if (generateSequence(this) { it.parent }.any { it in textGroups && it != this }) {
            return@process
        }
        if (stage == VisitStage.PRE) {

            val styleSerialized = this.style.serialize(this.parent?.effectiveStyle)

            when (this) {
                is GroupNode -> {
                    if (isText() && (parent as? GroupNode)?.isText() != true) {
                        textGroups.add(this)
                        val contour = textRuns()?.firstOrNull()?.contour
                        if (contour == null) {
                            sb.appendText(this, "text")
                        } else {
                            // the runs are placed along the contour, the first run starts at the start offset
                            sb.append("<defs><path id=\"text${ids.textPath}\" d=\"${contour.toSvg()}\"/></defs>")
                            sb.appendText(this, "text", "text${ids.textPath}")
                            ids.textPath++
                        }
                        sb.append("\n")
                    } else {
                        val attributes = listOf(svgId, styleSerialized, svgAttributes)
                            .filter(String::isNotEmpty)
                            .joinToString(" ")
                        sb.append("<g${" $attributes"}>$metadata\n")
                    }
                }
                is ShapeNode -> {
                    val pathAttribute = "d=\"${shape.toSvg()}\""

                    val attributes = listOf(
                        svgId,
                        styleSerialized,
                        svgAttributes,
                        pathAttribute
                    )
                        .filter(String::isNotEmpty)
                        .joinToString(" ")

                    val metadata = metadata
                    sb.append(if (metadata.isEmpty()) "<path $attributes/>\n" else "<path $attributes>$metadata</path>\n")
                }

                is TextNode -> {
                    val contour = this.contour
                    val escapedText = Entities.escape(this.text)
                    if (contour == null) {
                        // text without a position continues the previous text, written as a separate text element
                        // it starts at the origin
                        val position = listOfNotNull(
                            x?.let { "x=\"$it\"" },
                            y?.let { "y=\"$it\"" },
                            dx.takeIf { it != 0.0 }?.let { "dx=\"$it\"" },
                            dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" },
                            rotate.takeIf { it != 0.0 }?.let { "rotate=\"$it\"" }
                        )
                        val attributes = (listOf(svgId, styleSerialized, svgAttributes) + position)
                            .filter(String::isNotEmpty)
                            .joinToString(" ")
                        sb.append("<text $attributes>$escapedText</text>\n")
                    } else {
                        sb.append("<defs>")
                        sb.append("<path id=\"text${ids.textPath}\" d=\"${contour.toSvg()}\"/>")
                        sb.append("</defs>")
                        // dx moves the text along the path, dy moves it across the path
                        val attributes = listOf(
                            svgId, styleSerialized, svgAttributes,
                            dx.takeIf { it != 0.0 }?.let { "dx=\"$it\"" } ?: "",
                            dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" } ?: ""
                        ).filter(String::isNotEmpty).joinToString(" ")
                        val startOffset = x?.takeIf { it != 0.0 }?.let { " startOffset=\"$it\"" } ?: ""
                        sb.append("<text $attributes><textPath href=\"#text${ids.textPath}\"$startOffset>$escapedText</textPath></text>\n")
                        ids.textPath++
                    }
                }
                is ImageNode -> {
                    // PNG keeps the colors and the transparency of the image, JPEG does not
                    val dataUrl = this.image.toDataUrl(ImageFileFormat.PNG)
                    val attributes = listOf(svgId, styleSerialized, svgAttributes)
                        .filter(String::isNotEmpty)
                        .joinToString(" ")
                    val source = this.source
                    if (source == null) {
                        sb.append("""<image $attributes xlink:href="$dataUrl" x="$x" y="$y" width="$width" height="$height" preserveAspectRatio="none"/>""")
                    } else {
                        // the viewBox of a nested viewport selects the part of the image that is shown
                        sb.append("""<svg $attributes x="$x" y="$y" width="$width" height="$height" viewBox="${source.x} ${source.y} ${source.width} ${source.height}" preserveAspectRatio="none">""")
                        sb.append("""<image xlink:href="$dataUrl" width="${image.width}" height="${image.height}"/></svg>""")
                    }
                    sb.append("\n")
                }
            }
        } else {
            if (this is GroupNode && this !in textGroups) {
                sb.append("</g>\n")
            }
        }
    }
}

/** the patterns that the fills and strokes of [root] and the nodes in it use, also in the content of patterns */
private fun patterns(root: CompositionNode): Map<String, Paint.Pattern> {
    val patterns = LinkedHashMap<String, Paint.Pattern>()
    for (node in allNodes(root)) {
        for (paint in listOf(node.style.fill, node.style.stroke)) {
            if (paint is Paint.Pattern) patterns.getOrPut(paint.svgId) { paint }
        }
    }
    return patterns
}

private fun StringBuilder.appendPatterns(composition: Composition, ids: WriterIds) {
    val patterns = patterns(composition.root)
    if (patterns.isEmpty()) return
    append("<defs>")
    for ((id, pattern) in patterns) {
        fun units(units: PaintUnits) = if (units == PaintUnits.OBJECT) "userSpaceOnUse" else "objectBoundingBox"
        val t = pattern.transform
        val transform = if (t == Matrix44.IDENTITY) "" else
            " patternTransform=\"matrix(${t.c0r0} ${t.c0r1} ${t.c1r0} ${t.c1r1} ${t.c3r0} ${t.c3r1})\""
        val viewBox = pattern.viewBox?.let {
            " viewBox=\"${it.x} ${it.y} ${it.width} ${it.height}\" preserveAspectRatio=\"${pattern.aspectRatio}\""
        } ?: ""
        append("<pattern id=\"$id\" x=\"${pattern.tile.x}\" y=\"${pattern.tile.y}\" ")
        append("width=\"${pattern.tile.width}\" height=\"${pattern.tile.height}\" ")
        append("patternUnits=\"${units(pattern.units)}\" patternContentUnits=\"${units(pattern.contentUnits)}\"")
        append("$transform$viewBox>")
        appendNodes(pattern.content, ids)
        append("</pattern>")
    }
    append("</defs>\n")
}

/** writes a defs element with the clip paths that the nodes of [composition] use, when they use any */
private fun StringBuilder.appendClipPaths(composition: Composition, ids: WriterIds) {
    val clips = LinkedHashMap<String, ClipPath.Shapes>()
    for (node in allNodes(composition.root)) {
        (node.style.clipPath as? ClipPath.Shapes)?.let { clips.getOrPut(it.svgId) { it } }
    }
    if (clips.isEmpty()) return
    append("<defs>")
    for ((id, clip) in clips) {
        val units = if (clip.units == PaintUnits.BOUNDS) "objectBoundingBox" else "userSpaceOnUse"
        append("<clipPath id=\"$id\" clipPathUnits=\"$units\">")
        // a clip path holds shapes and text, not groups
        for (child in clip.content.children) appendNodes(child, ids)
        append("</clipPath>")
    }
    append("</defs>\n")
}

/** writes a style element with the @font-face rules of the font faces of the root of [composition] */
private fun StringBuilder.appendFontFaces(composition: Composition) {
    val faces = (composition.root as? GroupNode)?.fontFaces ?: return
    if (faces.isEmpty()) return
    append("<style>")
    for (face in faces) {
        val sources = face.sources.joinToString(", ") { if (it.startsWith("local(")) it else "url(\"$it\")" }
        val weight = if (face.weight.first == face.weight.last) "${face.weight.first}" else "${face.weight.first} ${face.weight.last}"
        append("@font-face { font-family: \"${face.family}\"; src: $sources; font-weight: $weight; ")
        append("font-style: ${if (face.italic) "italic" else "normal"}; }")
    }
    append("</style>\n")
}

/** the value of an input of a filter primitive */
private fun FilterInput.svg(): String? = when (this) {
    FilterInput.Previous -> null
    FilterInput.SourceGraphic -> "SourceGraphic"
    FilterInput.SourceAlpha -> "SourceAlpha"
    is FilterInput.Result -> name
    is FilterInput.Unsupported -> name
}

/** writes a defs element with the filters that the nodes of [composition] use, when they use any */
private fun StringBuilder.appendFilters(composition: Composition) {
    val filters = LinkedHashMap<String, FilterEffect>()
    for (node in allNodes(composition.root)) {
        for (effect in node.style.filter.value) filters.getOrPut(effect.svgId) { effect }
    }
    if (filters.isEmpty()) return
    fun n(v: Double) = v.toString()
    fun color(c: ColorRGBa) = "flood-color=\"${Paint.RGB(c.copy(alpha = 1.0))}\" flood-opacity=\"${c.alpha}\""
    append("<defs>")
    for ((id, effect) in filters) {
        val regionUnits = if (effect.regionUnits == PaintUnits.BOUNDS) "objectBoundingBox" else "userSpaceOnUse"
        val primitiveUnits = if (effect.primitiveUnits == PaintUnits.BOUNDS) "objectBoundingBox" else "userSpaceOnUse"
        // a filter without a region is applied everywhere, a region far beyond what is drawn stands in for it
        val r = effect.region ?: if (effect.regionUnits == PaintUnits.BOUNDS) org.openrndr.shape.Rectangle(-1E4, -1E4, 2E4, 2E4)
            else org.openrndr.shape.Rectangle(-1E7, -1E7, 2E7, 2E7)
        append("<filter id=\"$id\" filterUnits=\"$regionUnits\" primitiveUnits=\"$primitiveUnits\" ")
        append("x=\"${r.x}\" y=\"${r.y}\" width=\"${r.width}\" height=\"${r.height}\">")
        for (p in effect.primitives) {
            val common = listOfNotNull(
                p.inputs.getOrNull(0)?.svg()?.let { "in=\"$it\"" }.takeIf { p !is FilterPrimitive.Merge },
                p.inputs.getOrNull(1)?.svg()?.let { "in2=\"$it\"" }.takeIf { p !is FilterPrimitive.Merge },
                p.result?.let { "result=\"$it\"" },
                if (p.colorSpace == FilterColorSpace.SRGB) "color-interpolation-filters=\"sRGB\"" else null
            ).joinToString(" ")
            fun element(tag: String, attributes: String, children: String = "") =
                if (children.isEmpty()) append("<$tag $common $attributes/>") else append("<$tag $common $attributes>$children</$tag>")
            when (p) {
                is FilterPrimitive.GaussianBlur -> element("feGaussianBlur", "stdDeviation=\"${n(p.stdDeviation.x)} ${n(p.stdDeviation.y)}\"")
                is FilterPrimitive.Offset -> element("feOffset", "dx=\"${n(p.dx)}\" dy=\"${n(p.dy)}\"")
                is FilterPrimitive.Flood -> element("feFlood", color(p.color))
                is FilterPrimitive.Merge -> element("feMerge", "", p.inputs.joinToString("") { i ->
                    i.svg()?.let { "<feMergeNode in=\"$it\"/>" } ?: "<feMergeNode/>"
                })
                is FilterPrimitive.Composite -> element("feComposite", "operator=\"${p.operator.name.lowercase()}\" " +
                        "k1=\"${n(p.k1)}\" k2=\"${n(p.k2)}\" k3=\"${n(p.k3)}\" k4=\"${n(p.k4)}\"")
                is FilterPrimitive.Blend -> element("feBlend", "mode=\"${p.mode}\"")
                is FilterPrimitive.ColorMatrix -> element("feColorMatrix", "type=\"matrix\" values=\"${p.values.joinToString(" ")}\"")
                is FilterPrimitive.ComponentTransfer -> {
                    fun function(tag: String, f: FilterPrimitive.TransferFunction) = when (f) {
                        FilterPrimitive.TransferFunction.Identity -> ""
                        is FilterPrimitive.TransferFunction.Table -> "<$tag type=\"table\" tableValues=\"${f.values.joinToString(" ")}\"/>"
                        is FilterPrimitive.TransferFunction.Discrete -> "<$tag type=\"discrete\" tableValues=\"${f.values.joinToString(" ")}\"/>"
                        is FilterPrimitive.TransferFunction.Linear -> "<$tag type=\"linear\" slope=\"${f.slope}\" intercept=\"${f.intercept}\"/>"
                        is FilterPrimitive.TransferFunction.Gamma -> "<$tag type=\"gamma\" amplitude=\"${f.amplitude}\" exponent=\"${f.exponent}\" offset=\"${f.offset}\"/>"
                    }
                    val children = function("feFuncR", p.red) + function("feFuncG", p.green) + function("feFuncB", p.blue) + function("feFuncA", p.alpha)
                    if (children.isEmpty()) element("feComponentTransfer", "") else element("feComponentTransfer", "", children)
                }
                is FilterPrimitive.Morphology -> element("feMorphology", "operator=\"${if (p.dilate) "dilate" else "erode"}\" radius=\"${n(p.radius.x)} ${n(p.radius.y)}\"")
                is FilterPrimitive.DropShadow -> element("feDropShadow", "dx=\"${n(p.dx)}\" dy=\"${n(p.dy)}\" " +
                        "stdDeviation=\"${n(p.stdDeviation.x)} ${n(p.stdDeviation.y)}\" ${color(p.color)}")
                is FilterPrimitive.Turbulence -> element("feTurbulence", "type=\"${if (p.fractalNoise) "fractalNoise" else "turbulence"}\" " +
                        "baseFrequency=\"${n(p.baseFrequency.x)} ${n(p.baseFrequency.y)}\" numOctaves=\"${p.octaves}\" seed=\"${n(p.seed)}\"")
                is FilterPrimitive.DisplacementMap -> element("feDisplacementMap", "scale=\"${n(p.scale)}\" " +
                        "xChannelSelector=\"${p.xChannel}\" yChannelSelector=\"${p.yChannel}\"")
            }
        }
        append("</filter>")
    }
    append("</defs>\n")
}

/** writes a defs element with the masks that the nodes of [composition] use, when they use any */
private fun StringBuilder.appendMasks(composition: Composition, ids: WriterIds) {
    val masks = LinkedHashMap<String, Mask>()
    for (node in allNodes(composition.root)) {
        node.style.mask.value?.let { masks.getOrPut(it.svgId) { it } }
    }
    if (masks.isEmpty()) return
    append("<defs>")
    for ((id, mask) in masks) {
        val contentUnits = if (mask.units == PaintUnits.BOUNDS) "objectBoundingBox" else "userSpaceOnUse"
        val regionUnits = if (mask.regionUnits == PaintUnits.BOUNDS) "objectBoundingBox" else "userSpaceOnUse"
        // a mask without a region is drawn everywhere, a region far beyond what is drawn stands in for it
        val r = mask.region ?: if (mask.regionUnits == PaintUnits.BOUNDS) org.openrndr.shape.Rectangle(-1E4, -1E4, 2E4, 2E4)
            else org.openrndr.shape.Rectangle(-1E7, -1E7, 2E7, 2E7)
        val type = if (mask.type == MaskType.ALPHA) " mask-type=\"alpha\"" else ""
        append("<mask id=\"$id\" maskUnits=\"$regionUnits\" maskContentUnits=\"$contentUnits\" ")
        append("x=\"${r.x}\" y=\"${r.y}\" width=\"${r.width}\" height=\"${r.height}\"$type>")
        for (child in mask.content.children) appendNodes(child, ids)
        append("</mask>")
    }
    append("</defs>\n")
}

/** writes a defs element with the markers that the nodes of [composition] use, when they use any */
private fun StringBuilder.appendMarkers(composition: Composition, ids: WriterIds) {
    val markers = LinkedHashMap<String, Marker>()
    for (node in allNodes(composition.root)) {
        for (reference in listOf(node.style.markerStart, node.style.markerMid, node.style.markerEnd)) {
            reference.value?.let { markers.getOrPut(it.svgId) { it } }
        }
    }
    if (markers.isEmpty()) return
    append("<defs>")
    for ((id, marker) in markers) {
        val orient = when (val o = marker.orient) {
            MarkerOrient.Auto -> "auto"
            MarkerOrient.AutoStartReverse -> "auto-start-reverse"
            is MarkerOrient.Angle -> "${o.degrees}"
        }
        val units = if (marker.strokeWidthUnits) "strokeWidth" else "userSpaceOnUse"
        val viewBox = marker.viewBox?.let {
            " viewBox=\"${it.x} ${it.y} ${it.width} ${it.height}\" preserveAspectRatio=\"${marker.aspectRatio}\""
        } ?: ""
        // the content is clipped to the viewport of the marker, the default of SVG
        val overflow = if (marker.content.style.clipPath is ClipPath.None) " overflow=\"visible\"" else ""
        append("<marker id=\"$id\" refX=\"${marker.reference.x}\" refY=\"${marker.reference.y}\" ")
        append("markerWidth=\"${marker.size.x}\" markerHeight=\"${marker.size.y}\" markerUnits=\"$units\" orient=\"$orient\"$viewBox$overflow>")
        for (child in marker.content.children) appendNodes(child, ids)
        append("</marker>")
    }
    append("</defs>\n")
}
