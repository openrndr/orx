package org.openrndr.extra.svg
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.composition.*
import org.openrndr.shape.Rectangle
import org.openrndr.shape.ShapeContour
import java.io.File
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Load a [Composition] from a filename, url or svg string
 * @param fileOrUrlOrSvg a filename, a url or an svg document. A fragment after a filename or url selects a view of
 * the document: the id of a view element, or svgView(viewBox(...);preserveAspectRatio(...)).
 */
fun loadSVG(fileOrUrlOrSvg: String): Composition {
    val path = fileOrUrlOrSvg.substringBefore('#')
    val fragment = fileOrUrlOrSvg.substringAfter('#', "").takeIf { it.isNotEmpty() }
    return if (path.endsWith(".svg")) {
        val (text, base) = try {
            val url = URL(path)
            url.readText() to url.toURI()
        } catch (e: MalformedURLException) {
            val file = File(path)
            file.readText() to file.absoluteFile.toURI()
        }
        val document = SVGLoader().loadSVG(text, base)
        val (viewBox, aspectRatio) = document.view(fragment)
        document.composition().also { composition ->
            viewBox?.let { composition.documentStyle.viewBox = ViewBox.Value(it) }
            aspectRatio?.let { composition.documentStyle.preserveAspectRatio = it }
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

/**
 * A copy of this style: the nodes of the elements of a document get their own styles, an element that is used more
 * than once, by use elements, becomes nodes with independent styles
 */
private fun Style.copy(): Style = Style().also { it.properties.putAll(properties) }

internal class SVGDocument(
    private val root: SVGSVGElement,
    val namespaces: Map<String, String>,
    private val base: URI? = null,
    private val contents: List<Pair<GroupNode, SVGGroup>> = emptyList(),
    private val filterImages: List<Pair<FilterPrimitive.Image, String>> = emptyList(),
    /** the rules for pointer states and the keyframes of the style sheets of the document */
    val dynamic: SVGStyleSheet.Dynamic = SVGStyleSheet.Dynamic(emptyList(), emptyMap())
) {
    /** the nodes of the elements of the document, an element that is used more than once has more nodes */
    val nodesByElement = java.util.IdentityHashMap<org.jsoup.nodes.Element, MutableList<CompositionNode>>()

    /** the root element of the document */
    val rootElement: org.jsoup.nodes.Element get() = root.element

    /** the location of the document, relative references are resolved against it */
    val documentBase: URI? get() = base

    /** the font faces of the document, known once it is converted */
    var fontFaces: List<FontFace> = emptyList()
        private set

    /** the documents that use elements refer to, converted once, by their location */
    private val externalDocuments = mutableMapOf<String, SVGDocument?>()

    /**
     * The document at [href], a file or URL relative to this document or a data URL, converted once so that the
     * content of its patterns, clip paths, markers and masks is known. Null when it can not be loaded, or when it is
     * being converted, when documents refer to each other.
     */
    private fun externalDocument(href: String): SVGDocument? {
        val location = if (href.startsWith("data:", ignoreCase = true)) href else resolve(href)
        return externalDocuments.getOrPut(location) {
            if (location in documentsInProgress.get()) {
                logger.warn { "use refers to '${href.take(80)}', which is being loaded, it refers to itself" }
                return null
            }
            loadDocument(location)?.also { it.composition() }
        }
    }

    /** the document at [location] parsed, null when it can not be loaded */
    private fun loadDocument(location: String): SVGDocument? = try {
        val (text, documentBase) = readSvgDocument(location, base)
        // the viewport of this document is used again when its elements are converted
        val viewport = SVGParse.viewport.get()
        try {
            SVGLoader().loadSVG(text, documentBase)
        } finally {
            SVGParse.viewport.set(viewport)
        }
    } catch (e: Throwable) {
        logger.warn { "could not load '${location.take(80)}': ${e.message}" }
        null
    }

    /**
     * The view of this document that [fragment] selects: the id of a view element, or svgView(viewBox(...);
     * preserveAspectRatio(...)). Null components are those of the document.
     */
    fun view(fragment: String?): Pair<Rectangle?, AspectRatio?> {
        if (fragment.isNullOrBlank()) return Pair(null, null)
        val svgView = Regex("^svgView\\((.*)\\)$").matchEntire(fragment.trim())
        val (viewBox, aspectRatio) = if (svgView != null) {
            val arguments = svgView.groupValues[1]
            Pair(
                Regex("viewBox\\(([^)]*)\\)").find(arguments)?.groupValues?.get(1),
                Regex("preserveAspectRatio\\(([^)]*)\\)").find(arguments)?.groupValues?.get(1)
            )
        } else {
            val view = root.element.getElementsByTag("view").firstOrNull { it.id() == fragment }
                ?: return Pair(null, null).also { logger.warn { "'#$fragment' is not a view of the document" } }
            Pair(view.attr("viewBox").takeIf { it.isNotBlank() }, view.attr("preserveAspectRatio").takeIf { it.isNotBlank() })
        }
        val rectangle = viewBox?.split(Regex("[\\s,]+"))?.mapNotNull { it.toDoubleOrNull() }?.takeIf { it.size == 4 }
            ?.let { Rectangle(it[0], it[1], it[2], it[3]) }
        val ratio = aspectRatio?.let { value ->
            SVGParse.preserveAspectRatio(org.jsoup.nodes.Element("view").attr("preserveAspectRatio", value))
        }
        return Pair(rectangle, ratio)
    }

    /** the nodes with properties that are not inherited but have the value inherit, see [resolveInherit] */
    private val inherits = java.util.IdentityHashMap<CompositionNode, Set<AttributeOrPropertyKey>>()

    /** gives the properties of nodes that have the value inherit the value of their parent, from the root down */
    private fun resolveInherit(node: CompositionNode) {
        inherits[node]?.let { keys ->
            val parentStyle = node.parent?.effectiveStyle
            for (key in keys) node.style[key] = parentStyle?.get(key) ?: initialValue(key)
        }
        if (node is GroupNode) node.children.forEach(::resolveInherit)
    }

    /** the location of this document as images refer to it, null when it has no location */
    private val location: String? = base?.let { if (it.scheme == "file") File(it).path else it.toString() }

    fun composition(): Composition {
        // images in this document that refer to it, directly or through other documents, are not drawn
        val inProgress = documentsInProgress.get()
        val added = location != null && inProgress.add(location)
        try {
            return buildComposition().also { composition ->
                // the animations of the document change the nodes it is converted to
                val animation = SVGAnimation(this)
                if (animation.animated) (composition.root as? GroupNode)?.userData?.set("svg-animation", animation)
            }
        } finally {
            if (added) inProgress.remove(location)
        }
    }

    private fun buildComposition(): Composition = Composition(
        convertElement(root).also {
            // the content of patterns, clip paths, markers and masks is converted like the elements of the document
            for ((content, group) in contents) {
                // the clip of a marker to its viewport is kept, the clip path of a clipPath element applies to its
                // content, not to the element it clips
                val clip = content.style.clipPath.takeIf { group.element.tagName() == "marker" } ?: ClipPath.None
                content.style = group.style.copy()
                content.style.clipPath = clip
                group.elements.mapTo(content.children) { element ->
                    convertElement(element).also { node -> node.parent = content }
                }
            }
            if (inherits.isNotEmpty()) resolveInherit(it)
            // the content of the images of filters: elements of the document, or images and documents
            for ((primitive, href) in filterImages) {
                primitive.content.children.clear()
                val node = if (href.startsWith("#")) {
                    elementsById[href.removePrefix("#")]?.let { convertElement(it) }
                        ?.also { primitive.size = null }
                } else {
                    filterImageContent(href)?.let { (node, size) -> primitive.size = size; node }
                }
                node?.let {
                    it.parent = primitive.content
                    primitive.content.children.add(it)
                } ?: logger.warn { "feImage refers to '${href.take(80)}', which can not be loaded" }
            }
            // the fonts of the document are available to its text, and to the text in patterns, markers and masks
            val faces = SVGStyleSheet.fontFaces(root.element, base, ::resolve)
            fontFaces = faces
            if (faces.isNotEmpty()) {
                (it as? GroupNode)?.fontFaces = faces
                for ((content, _) in contents) content.fontFaces = faces
            }
        },
        root.bounds

    ).apply {
        namespaces.putAll(this@SVGDocument.namespaces)
        this.documentStyle = this@SVGDocument.root.documentStyle
    }

    private fun convertElement(svgElem: SVGElement): CompositionNode = when (svgElem) {
        // the contents of defs and symbol elements are not rendered where they are defined
        is SVGDefs, is SVGSymbol -> GroupNode().apply {
            this.id = svgElem.id
        }
        is SVGUse -> convertUse(svgElem)
        is SVGNestedSVG -> convertNestedSVG(svgElem)
        is SVGGroup -> GroupNode().apply {
            style = svgElem.style.copy()
            this.id = svgElem.id
            // an a element is a group that is a link
            if (svgElem.element.tagName() == "a") {
                link = listOf("href", "xlink:href").firstNotNullOfOrNull { name ->
                    svgElem.element.attr(name).trim().takeIf { it.isNotEmpty() }
                }
            }
            svgElem.elements.mapTo(children) { convertElement(it).also { x -> x.parent = this@apply } }
        }
        is SVGPath -> {
            ShapeNode(svgElem.shape()).apply {
                style = svgElem.style.copy()
                this.id = svgElem.id
            }
        }
        is SVGText -> convertText(svgElem, null).apply { textElement = true }
        is SVGTextRun -> svgElem.pieces.firstOrNull().let { TextNode(svgElem.text, null, it?.x, it?.y, it?.dx ?: 0.0, it?.dy ?: 0.0, it?.rotate ?: 0.0) }
        is SVGImage -> {
            imageNode(svgElem).apply {
                style = svgElem.style.copy()
                this.id = svgElem.id
            }
        }
        // the elements that animations set properties with are not in documents
        else -> GroupNode()
    }.apply {
        svgElem.sourceElement?.let { nodesByElement.getOrPut(it) { mutableListOf() }.add(this) }
        transform = svgElem.effectiveTransform()
        if (svgElem.inheritedProperties.isNotEmpty()) inherits[this] = svgElem.inheritedProperties
        svgElem.title?.let { userData["title"] = it }
        svgElem.description?.let { userData["desc"] = it }
    }

    /** the elements of the document by their id, including the elements in defs and symbol elements */
    private val elementsById: Map<String, SVGElement> by lazy {
        val elements = mutableMapOf<String, SVGElement>()
        fun collect(element: SVGElement) {
            if (element.id.isNotEmpty()) elements.putIfAbsent(element.id, element)
            when (element) {
                is SVGGroup -> element.elements.forEach(::collect)
                is SVGText -> element.elements.forEach(::collect)
                else -> {}
            }
        }
        collect(root)
        elements
    }

    /**
     * Converts a nested svg element: a group, clipped to the viewport unless its overflow is visible, with a group
     * that places and fits the content
     */
    private fun convertNestedSVG(svg: SVGNestedSVG): GroupNode = GroupNode().apply {
        style = svg.style.copy()
        this.id = svg.id
        if (svg.width <= 0.0 || svg.height <= 0.0) return@apply
        if (svg.clipped) {
            val clipContent = GroupNode()
            val rectangle = org.openrndr.shape.Rectangle(svg.x, svg.y, svg.width, svg.height)
            clipContent.children.add(ShapeNode(rectangle.shape).also { it.parent = clipContent })
            style.clipPath = ClipPath.Shapes(clipContent)
        }
        val content = GroupNode().also { group ->
            group.transform = org.openrndr.math.transforms.buildTransform { translate(svg.x, svg.y, 0.0) } *
                    (svg.viewBox?.let {
                        viewportTransform(it, org.openrndr.shape.Rectangle(0.0, 0.0, svg.width, svg.height), svg.aspectRatio)
                    } ?: org.openrndr.math.Matrix44.IDENTITY)
            group.parent = this
            svg.elements.mapTo(group.children) { convertElement(it).also { node -> node.parent = group } }
        }
        children.add(content)
    }

    /** the use elements that are being converted, a use element that refers to an element it is in is not drawn */
    private val usesInProgress = mutableSetOf<SVGUse>()

    /**
     * Converts a use element to a group with a copy of the element it refers to, moved by its x and y. A symbol is
     * fitted into the width and height of the use element, by default the size of the document.
     */
    /**
     * A copy of [target], an element of this document, for [use]: a symbol is fitted into [width] by [height] by its
     * view box, other elements are converted as they are
     */
    private fun useContent(target: SVGElement, width: Double, height: Double): CompositionNode =
        if (target is SVGSymbol) {
            GroupNode().also { symbol ->
                symbol.style = target.style.copy()
                symbol.transform = target.viewBox?.let {
                    viewportTransform(it, org.openrndr.shape.Rectangle(0.0, 0.0, width, height), target.aspectRatio)
                } ?: org.openrndr.math.Matrix44.IDENTITY
                target.elements.mapTo(symbol.children) { convertElement(it).also { node -> node.parent = symbol } }
            }
        } else {
            convertElement(target)
        }

    private fun convertUse(use: SVGUse): GroupNode = GroupNode().apply {
        style = use.style.copy()
        this.id = use.id
        // the element is in this document, or in the document the use element refers to
        val document = use.document?.let { externalDocument(it) ?: return@apply } ?: this@SVGDocument
        val target = use.href?.let { document.elementsById[it] }
        if (target == null) {
            logger.warn { "use refers to '${use.document ?: ""}#${use.href}', which is not an element in the document" }
            return@apply
        }
        if (use in usesInProgress) {
            logger.warn { "use refers to '#${use.href}', which contains it" }
            return@apply
        }
        usesInProgress.add(use)
        try {
            val content = document.useContent(target, use.width ?: root.bounds.dimensions.x, use.height ?: root.bounds.dimensions.y)
            // the text of an element of another document uses the fonts of that document
            if (document !== this@SVGDocument) this.fontFaces = document.fontFaces
            content.parent = this
            // the position moves the copy, after the transform of the use element
            val translated = GroupNode().also {
                it.transform = org.openrndr.math.transforms.buildTransform { translate(use.x, use.y, 0.0) }
                it.parent = this
                content.parent = it
                it.children.add(content)
            }
            children.add(translated)
        } finally {
            usesInProgress.remove(use)
        }
    }

    /** the paths of the document by their id, including the paths in defs elements */
    private val pathsById: Map<String, SVGPath> by lazy {
        val paths = mutableMapOf<String, SVGPath>()
        fun collect(element: SVGElement) {
            when (element) {
                is SVGPath -> if (element.id.isNotEmpty()) paths[element.id] = element
                is SVGGroup -> element.elements.forEach(::collect)
                else -> {}
            }
        }
        collect(root)
        paths
    }

    /** the contour of the path that [href] refers to, in the coordinates of the path element's parent */
    private fun pathContour(href: String): ShapeContour? {
        val path = pathsById[href.removePrefix("#")] ?: return null
        return path.shape().contours.firstOrNull()?.transform(path.style.transform.value)
    }

    /** the distance along the path where text starts: a length, or a percentage of the length of the path */
    private fun startOffset(value: String, length: Double, scale: Double): Double = when {
        value.isEmpty() -> 0.0
        value.endsWith("%") -> (value.dropLast(1).toDoubleOrNull() ?: 0.0) * length / 100.0
        else -> SVGParse.length(value).value * scale
    }

    /** the contour that the runs of a textPath element are placed along, the first run starts at [startOffset] */
    private class PathText(val contour: ShapeContour, var startOffset: Double?)

    /**
     * Converts a text, tspan or textPath element to a group of text runs. Runs of a textPath element, and of the
     * tspan elements in it, are placed along its path; a textPath element of which the path is not found is not
     * rendered.
     */
    private fun convertText(svgText: SVGText, pathText: PathText?): GroupNode = GroupNode().apply {
        style = svgText.style.copy()
        this.id = svgText.id
        val href = svgText.pathHref
        val path = if (href != null) {
            // the path of the path attribute, or the path the textPath element refers to; on the right side the text
            // is along the path reversed
            val contour = (svgText.pathData?.let { svgShape(it).contours.firstOrNull() } ?: pathContour(href))
                ?.let { if (svgText.rightSide) it.reversed else it }
            if (contour == null) {
                logger.warn { "textPath refers to '$href', which is not a path in the document" }
                return@apply
            }
            // a length along a path with a path length is in units of that length
            val scale = pathsById[href.removePrefix("#")]?.takeIf { svgText.pathData == null }?.style?.pathLength?.value?.takeIf { it > 0.0 }
                ?.let { contour.length / it } ?: 1.0
            PathText(contour, startOffset(svgText.startOffset, contour.length, scale))
        } else {
            pathText
        }
        for (element in svgText.elements) {
            val nodes = when (element) {
                is SVGText -> listOf(convertText(element, path))
                is SVGTextRun -> element.pieces.map { piece ->
                    if (path != null) {
                        // along a path the text starts at the start offset, x is a distance along the path
                        TextNode(piece.text, path.contour, path.startOffset ?: piece.x, null, piece.dx, piece.dy, piece.rotate)
                            .also { path.startOffset = null }
                    } else {
                        TextNode(piece.text, null, piece.x, piece.y, piece.dx, piece.dy, piece.rotate)
                    }
                }
                else -> continue
            }
            for (child in nodes) {
                child.parent = this
                children.add(child)
            }
        }
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
        if (isSvgReference(href)) {
            return svgImageNode(svgImage, href)
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

    /**
     * The document at [href] for an image: its root, isolated from the styles of this document, and its intrinsic
     * size, with the view box and aspect ratio of the view its fragment selects. Null when it can not be loaded, or
     * when it is being converted, when documents refer to each other.
     */
    private class ImageDocument(val root: CompositionNode, val size: org.openrndr.math.Vector2, val viewBox: Rectangle, val aspectRatio: AspectRatio?)

    private fun imageDocument(href: String): ImageDocument? {
        val isData = href.startsWith("data:", ignoreCase = true)
        val location = if (isData) href else resolve(href.substringBefore('#'))
        val fragment = if (isData) null else href.substringAfter('#', "").takeIf { it.isNotEmpty() }
        if (!isData && location in documentsInProgress.get()) {
            logger.warn { "image refers to '${href.take(80)}', which is being loaded, it refers to itself" }
            return null
        }
        val document = loadDocument(location) ?: return null
        val (viewBox, aspectRatio) = document.view(fragment)
        val composition = document.composition()

        // the intrinsic size of the document is its width and height, or the size of its view box
        val documentViewBox = composition.documentStyle.viewBox.value
        val dimensions = composition.bounds.dimensions
        val size = when {
            dimensions.x > 0.0 && dimensions.y > 0.0 -> dimensions
            documentViewBox != null && documentViewBox.width > 0.0 && documentViewBox.height > 0.0 -> documentViewBox.dimensions
            else -> org.openrndr.math.Vector2(300.0, 150.0)
        }
        val root = composition.root
        // the document does not inherit the properties of this document
        for (key in AttributeOrPropertyKey.entries) {
            if (isInheritedProperty(key) && root.style.properties[key] == null) {
                initialValue(key)?.let { root.style[key] = it }
            }
        }
        return ImageDocument(root, size, viewBox ?: documentViewBox ?: Rectangle(0.0, 0.0, size.x, size.y), aspectRatio)
    }

    /** [content] fitted into [viewport] from [viewBox] by [aspectRatio], clipped to the viewport */
    private fun fitted(content: CompositionNode, viewBox: Rectangle, viewport: Rectangle, aspectRatio: AspectRatio): GroupNode {
        val fit = GroupNode().also { group ->
            group.transform = viewportTransform(viewBox, viewport, aspectRatio)
            content.parent = group
            group.children.add(content)
        }
        return GroupNode().also { group ->
            val clip = GroupNode()
            clip.children.add(ShapeNode(viewport.shape).also { it.parent = clip })
            group.style.clipPath = ClipPath.Shapes(clip)
            fit.parent = group
            group.children.add(fit)
        }
    }

    /**
     * Converts an image element that refers to an SVG document: the document is loaded as a composition of its own,
     * fitted into the viewport of the image by its view box, or by its size when it has none, and clipped to the
     * viewport. As in browsers, the document does not inherit the styles of the document that refers to it. A
     * fragment selects a view of the document: the id of a view element or svgView(...).
     */
    private fun svgImageNode(svgImage: SVGImage, href: String): CompositionNode {
        val document = imageDocument(href) ?: return GroupNode()
        val size = document.size
        val width = svgImage.width ?: svgImage.height?.let { it * size.x / size.y } ?: size.x
        val height = svgImage.height ?: svgImage.width?.let { it * size.y / size.x } ?: size.y
        if (width <= 0.0 || height <= 0.0) return GroupNode()
        val viewport = Rectangle(svgImage.x, svgImage.y, width, height)
        // the aspect ratio of a view applies when the image element does not give one
        val aspectRatio = if (svgImage.element?.hasAttr("preserveAspectRatio") == true) svgImage.aspectRatio
            else document.aspectRatio ?: svgImage.aspectRatio
        val clipped = fitted(document.root, document.viewBox, viewport, aspectRatio)
        return GroupNode().also { outer ->
            clipped.parent = outer
            outer.children.add(clipped)
        }
    }

    /**
     * The content of an feImage that refers to an image or a document, in the coordinates of its size, and its size;
     * null when it can not be loaded
     */
    private fun filterImageContent(href: String): Pair<CompositionNode, org.openrndr.math.Vector2>? {
        if (isSvgReference(href)) {
            val document = imageDocument(href) ?: return null
            val size = document.size
            return Pair(fitted(document.root, document.viewBox, Rectangle(0.0, 0.0, size.x, size.y),
                document.aspectRatio ?: AspectRatio.DEFAULT), size)
        }
        val image = try {
            loadImage(resolve(href))
        } catch (e: Throwable) {
            return null
        }
        val size = org.openrndr.math.Vector2(image.width.toDouble(), image.height.toDouble())
        return Pair(ImageNode(image, 0.0, 0.0, size.x, size.y), size)
    }
}

/** the locations of the documents that are being loaded as images, a document that refers to itself is not loaded again */
private val documentsInProgress = ThreadLocal.withInitial { mutableSetOf<String>() }

/** true when [href] refers to an SVG document: an .svg or .svgz file or URL, or an SVG data URL */
private fun isSvgReference(href: String): Boolean {
    val h = href.trim()
    if (h.startsWith("data:", ignoreCase = true)) return h.substringAfter(':').startsWith("image/svg+xml", ignoreCase = true)
    val path = h.substringBefore('#').substringBefore('?').lowercase()
    return path.endsWith(".svg") || path.endsWith(".svgz")
}

/** the text of the SVG document at [location], a file, a URL or a data URL, and the location relative references in it are resolved against */
private fun readSvgDocument(location: String, base: URI?): Pair<String, URI?> {
    if (location.startsWith("data:", ignoreCase = true)) {
        val header = location.substringBefore(',')
        val data = location.substringAfter(',')
        val bytes = if (header.endsWith(";base64", ignoreCase = true)) {
            java.util.Base64.getMimeDecoder().decode(data)
        } else {
            java.net.URLDecoder.decode(data.replace("+", "%2B"), Charsets.UTF_8).toByteArray()
        }
        return Pair(gunzipIfNeeded(bytes).toString(Charsets.UTF_8), base)
    }
    val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(location) && !File(location).isAbsolute
    return if (hasScheme) {
        val uri = URI(location)
        Pair(gunzipIfNeeded(uri.toURL().readBytes()).toString(Charsets.UTF_8), uri)
    } else {
        val file = File(location)
        Pair(gunzipIfNeeded(file.readBytes()).toString(Charsets.UTF_8), file.absoluteFile.toURI())
    }
}

/** [bytes], decompressed when they are gzipped, as svgz documents are */
private fun gunzipIfNeeded(bytes: ByteArray): ByteArray =
    if (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
        java.util.zip.GZIPInputStream(bytes.inputStream()).readBytes()
    } else bytes

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
        // the rules of style elements are applied to the elements they select before the elements are read
        val dynamic = SVGStyleSheet.apply(root, base)
        // percentages of lengths refer to the size of the viewport of the document
        SVGParse.viewport.set(
            (SVGParse.viewBox(root) as? ViewBox.Value)?.value?.dimensions
                ?: SVGParse.bounds(root).dimensions.takeIf { it.x > 0.0 && it.y > 0.0 }
                ?: org.openrndr.math.Vector2(100.0, 100.0)
        )
        // gradients and patterns are collected first, elements can refer to them before they are defined
        val gradients = SVGGradients.parse(root)
        val patterns = SVGGradients.parsePatterns(root)
        val clips = SVGGradients.parseClipPaths(root)
        val markers = SVGGradients.parseMarkers(root)
        val masks = SVGGradients.parseMasks(root)
        val filters = SVGFilters.parse(root)
        val servers: Map<String, Paint.Server> = gradients + patterns.mapValues { it.value.first }
        return SVGFilters.using(filters) { SVGGradients.usingMasks(masks.mapValues { it.value.first }) { SVGGradients.usingMarkers(markers.mapValues { it.value.first }) { SVGGradients.using(servers, clips.mapValues { it.value.first }) {
            val rootGroup = SVGSVGElement(root)
            // the content of patterns and clip paths can refer to gradients, patterns and clip paths too
            val contents = patterns.values.mapNotNull { (pattern, element) ->
                element?.let { pattern.content to SVGGroup(it) }
            } + clips.values.map { (clip, element) -> clip.content to SVGGroup(element) } +
                    markers.values.map { (marker, element) -> marker.content to SVGGroup(element) } +
                    masks.values.map { (mask, element) -> mask.content to SVGGroup(element) }
            SVGDocument(rootGroup, namespaces, base, contents, SVGFilters.images.toList(), dynamic)
        } } } }
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