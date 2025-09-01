package org.openrndr.extra.svg

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
    process(composition.root) { stage ->
        if (stage == VisitStage.PRE) {

            val styleSerialized = this.style.serialize(this.parent?.style)

            when (this) {
                is GroupNode -> {
                    val attributes = listOf(svgId, styleSerialized, svgAttributes)
                        .filter(String::isNotEmpty)
                        .joinToString(" ")
                    sb.append("<g${" $attributes"}>\n")
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
                        sb.append("<text $svgId $svgAttributes>$escapedText</text>")
                    } else {
                        sb.append("<defs>")
                        sb.append("<path id=\"text$textPathID\" d=\"${contour.toSvg()}\"/>")
                        sb.append("</defs>")
                        sb.append("<text $styleSerialized><textPath href=\"#text$textPathID\">$escapedText</textPath></text>")
                        textPathID++
                    }
                }
                is ImageNode -> {
                    val dataUrl = this.image.toDataUrl()
                    sb.append("""<image xlink:href="$dataUrl" height="${this.image.height}" width="${this.image.width}"/>""")
                }
            }
        } else {
            if (this is GroupNode) {
                sb.append("</g>\n")
            }
        }
    }
    sb.append("</svg>")
    return sb.toString()
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