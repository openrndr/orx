package org.openrndr.extra.svg
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.composition.*
import org.openrndr.shape.Rectangle
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Load a [Composition] from a filename, url or svg string
 * @param fileOrUrlOrSvg a filename, a url or an svg document
 */
fun loadSVG(fileOrUrlOrSvg: String): Composition {
    return if (fileOrUrlOrSvg.endsWith(".svg")) {
        try {
            val url = URL(fileOrUrlOrSvg)
            parseSVG(url.readText(), url.toURI())
        } catch (e: MalformedURLException) {
            val file = File(fileOrUrlOrSvg)
            parseSVG(file.readText(), file.absoluteFile.toURI())
        }
    } else {
        parseSVG(fileOrUrlOrSvg)
    }
}

/**
 * Load a [Composition] from a file, url or svg string
 * @param file a filename, a url or an svg document
 */
fun loadSVG(file: File): Composition {
    return parseSVG(file.readText(), file.absoluteFile.toURI())
}

/**
 * Parses an SVG document and creates a [Composition]
 * @param svgString xml-like svg document
 * @param base the location of the document, relative references to images are resolved against it; when null they
 * are relative to the working directory
 */
fun parseSVG(svgString: String, base: URI? = null): Composition {
    val document = SVGLoader().loadSVG(svgString, base)
    return document.composition()
}

internal class SVGDocument(
    private val root: SVGSVGElement,
    val namespaces: Map<String, String>,
    private val base: URI? = null
) {
    fun composition(): Composition = Composition(
        convertElement(root),
        root.bounds

    ).apply {
        namespaces.putAll(this@SVGDocument.namespaces)
        this.documentStyle = this@SVGDocument.root.documentStyle
    }

    private fun convertElement(svgElem: SVGElement): CompositionNode = when (svgElem) {
        is SVGGroup -> GroupNode().apply {
            this.id = svgElem.id
            svgElem.elements.mapTo(children) { convertElement(it).also { x -> x.parent = this@apply } }
        }
        is SVGPath -> {
            ShapeNode(svgElem.shape()).apply {
                style = svgElem.style
                this.id = svgElem.id
            }
        }
        is SVGImage -> {
            imageNode(svgElem).apply {
                style = svgElem.style
                this.id = svgElem.id
            }
        }
    }.apply {
        transform = svgElem.style.transform.value
    }

    /**
     * The location of an image reference: data urls and absolute references as they are, relative references
     * resolved against the location of the document
     */
    private fun resolve(href: String): String {
        val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(href)
        if (base == null || hasScheme || File(href).isAbsolute) {
            return href
        }
        return try {
            // a URI with only a path encodes the characters that are not allowed in URIs, like spaces
            val resolved = base.resolve(URI(null, null, href, null))
            if (resolved.scheme == "file") File(resolved).path else resolved.toString()
        } catch (e: Exception) {
            href
        }
    }

    /**
     * An [ImageNode] for an image element, or an empty [GroupNode] when the image is not rendered: when it has no
     * reference, can not be loaded or its viewport is empty
     */
    private fun imageNode(svgImage: SVGImage): CompositionNode {
        val href = svgImage.href
        if (href == null) {
            logger.warn { "image without href" }
            return GroupNode()
        }
        val image = try {
            loadImage(resolve(href))
        } catch (e: Throwable) {
            logger.warn { "could not load image '${href.take(80)}': ${e.message}" }
            return GroupNode()
        }

        // a missing width or height is the size of the image, keeping its aspect ratio when only one is given
        val imageWidth = image.width.toDouble()
        val imageHeight = image.height.toDouble()
        val width = svgImage.width ?: svgImage.height?.let { it * imageWidth / imageHeight } ?: imageWidth
        val height = svgImage.height ?: svgImage.width?.let { it * imageHeight / imageWidth } ?: imageHeight
        if (width <= 0.0 || height <= 0.0) {
            return GroupNode()
        }
        return fitImage(image, Rectangle(svgImage.x, svgImage.y, width, height), svgImage.aspectRatio)
    }
}

/**
 * Places [image] in [viewport] following [aspectRatio], like SVG's preserveAspectRatio: stretched to the viewport
 * with [Align.NONE], scaled to fit inside it with [MeetOrSlice.MEET], or scaled to cover it and cropped to it with
 * [MeetOrSlice.SLICE], aligned as given.
 */
internal fun fitImage(image: ColorBuffer, viewport: Rectangle, aspectRatio: AspectRatio): ImageNode {
    if (aspectRatio.align == Align.NONE) {
        return ImageNode(image, viewport.x, viewport.y, viewport.width, viewport.height)
    }
    val name = aspectRatio.align.name
    fun factor(axis: String) = when {
        name.contains("${axis}_MIN") -> 0.0
        name.contains("${axis}_MAX") -> 1.0
        else -> 0.5
    }
    val alignX = factor("X")
    val alignY = factor("Y")
    val imageWidth = image.width.toDouble()
    val imageHeight = image.height.toDouble()

    return when (aspectRatio.meetOrSlice) {
        MeetOrSlice.MEET -> {
            val scale = min(viewport.width / imageWidth, viewport.height / imageHeight)
            val width = imageWidth * scale
            val height = imageHeight * scale
            ImageNode(
                image,
                viewport.x + (viewport.width - width) * alignX,
                viewport.y + (viewport.height - height) * alignY,
                width,
                height
            )
        }

        MeetOrSlice.SLICE -> {
            // the part of the image that covers the viewport
            val scale = max(viewport.width / imageWidth, viewport.height / imageHeight)
            val sourceWidth = viewport.width / scale
            val sourceHeight = viewport.height / scale
            val source = Rectangle(
                (imageWidth - sourceWidth) * alignX,
                (imageHeight - sourceHeight) * alignY,
                sourceWidth,
                sourceHeight
            )
            ImageNode(image, viewport.x, viewport.y, viewport.width, viewport.height, source)
        }
    }
}

internal class SVGLoader {
    fun loadSVG(svg: String, base: URI? = null): SVGDocument {
        val doc = Jsoup.parse(svg, "", Parser.xmlParser())
        val root = doc.select(Tag.SVG).first() ?: error("no root")
        val namespaces = root.attributes().filter { it.key.startsWith("xmlns") }.associate {
            Pair(it.key, it.value)
        }
        val rootGroup = SVGSVGElement(root)
        return SVGDocument(rootGroup, namespaces, base)
    }

//    private fun handleImage(group: SVGGroup, e: Element) {
//        val width = e.attr(Attr.WIDTH).toDoubleOrNull()
//        val height = e.attr(Attr.HEIGHT).toDoubleOrNull()
//        val x = e.attr("x").toDoubleOrNull()
//        val y = e.attr("y").toDoubleOrNull()
//        val imageData = e.attr("xlink:href")
//        val image = ColorBuffer.fromUrl(imageData)
//        val imageNode = ImageNode(image, width ?: image.width.toDouble(), height ?: image.height.toDouble())
//        val image = SVGImage(imageData, x, y, width, height)
//        image.parseTransform(e)
//        group.elements.add(image)
//    }

//    private fun handleImage(group: SVGGroup, e: Element) {
//        val width = e.attr("width").toDouble()
//        val height = e.attr("height").toDouble()
//        val url = e.attr("xlink:href")
//        val image = SVGImage(url).apply {
//            id = e.id()
//            parseTransform(e)
//        }
//        image.id = e.id()
//    }
}