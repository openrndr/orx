package org.openrndr.extra.svg

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

    val filtered = this.properties.filter {
        it.key != AttributeOrPropertyKey.SHADESTYLE
    }
    // Inheritance can't be checked without a parentStyleable
    when (parentStyleable) {
        null -> filtered.forEach { (t, u) ->
            if (u.toString().isNotEmpty()) {
                sb.append("$t=\"${u.toString()}\" ")
            }
        }
        else -> filtered.forEach { (t, u) ->
            if (u.toString().isNotEmpty() && !this.isInherited(parentStyleable, t)) {
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
    sb.appendGradients(composition)
    sb.appendPatterns(composition, ids)

    sb.appendNodes(composition.root, ids)
    sb.append("</svg>")
    return sb.toString()
}

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
    val styleSerialized = node.style.serialize(node.parent?.style)
    when (node) {
        is GroupNode -> {
            val attributes = listOf(node.svgId, styleSerialized, node.svgAttributes)
                .filter(String::isNotEmpty)
                .joinToString(" ")
            append("<$element${if (attributes.isEmpty()) "" else " $attributes"}>")
            if (pathId != null) {
                val startOffset = node.textRuns()?.first()?.x?.takeIf { it != 0.0 }
                    ?.let { "startOffset=\"$it\"" } ?: ""
                // a single group in the text is written as the textPath element, like the loaded textPath elements,
                // properties like text-anchor apply to the textPath element but not to a tspan in it
                val single = node.children.singleOrNull() as? GroupNode
                val pathAttributes = listOf(
                    "href=\"#$pathId\"", startOffset,
                    single?.svgId ?: "", single?.style?.serialize(node.style) ?: "", single?.svgAttributes ?: ""
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
                node.dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" }
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
/** the gradients that the fills and strokes of the nodes of [composition] use, by their id */
private fun gradients(composition: Composition): Map<String, Paint.Gradient> {
    val gradients = LinkedHashMap<String, Paint.Gradient>()
    process(composition.root) { stage ->
        if (stage == VisitStage.PRE) {
            for (paint in listOf(style.fill, style.stroke)) {
                if (paint is Paint.Gradient) gradients.getOrPut(paint.svgId) { paint }
            }
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
        append(" gradientUnits=\"$units\" spreadMethod=\"$spread\"$transform>")
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

            val styleSerialized = this.style.serialize(this.parent?.style)

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
                        sb.append("<g${" $attributes"}>\n")
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

                    sb.append("<path $attributes/>\n")
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
                            dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" }
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
    fun collect(node: CompositionNode) {
        process(node) { stage ->
            if (stage == VisitStage.PRE) {
                for (paint in listOf(style.fill, style.stroke)) {
                    if (paint is Paint.Pattern && paint.svgId !in patterns) {
                        patterns[paint.svgId] = paint
                        collect(paint.content)
                    }
                }
            }
        }
    }
    collect(root)
    return patterns
}

/** writes a defs element with the patterns that [composition] uses, when it uses any */
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
