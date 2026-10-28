package org.openrndr.extra.svg

import org.openrndr.draw.ImageFileFormat
import org.jsoup.nodes.*
import org.openrndr.extra.composition.*
import org.openrndr.extra.composition.TextNode

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

    var textPathID = 0
    // groups written as text elements, their contents are written with them
    val textGroups = mutableSetOf<CompositionNode>()
    process(composition.root) { stage ->
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
                            sb.append("<defs><path id=\"text$textPathID\" d=\"${contour.toSvg()}\"/></defs>")
                            sb.appendText(this, "text", "text$textPathID")
                            textPathID++
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
                        sb.append("<path id=\"text$textPathID\" d=\"${contour.toSvg()}\"/>")
                        sb.append("</defs>")
                        // dx moves the text along the path, dy moves it across the path
                        val attributes = listOf(
                            svgId, styleSerialized, svgAttributes,
                            dx.takeIf { it != 0.0 }?.let { "dx=\"$it\"" } ?: "",
                            dy.takeIf { it != 0.0 }?.let { "dy=\"$it\"" } ?: ""
                        ).filter(String::isNotEmpty).joinToString(" ")
                        val startOffset = x?.takeIf { it != 0.0 }?.let { " startOffset=\"$it\"" } ?: ""
                        sb.append("<text $attributes><textPath href=\"#text$textPathID\"$startOffset>$escapedText</textPath></text>\n")
                        textPathID++
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