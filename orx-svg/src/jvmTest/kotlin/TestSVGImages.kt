import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.loadSVG
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.math.Vector2
import java.io.File
import java.util.Base64
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TestSVGImages {
    private val directory = createTempDirectory("svg-images").toFile()

    private fun write(name: String, content: String) = File(directory, name).also {
        it.parentFile.mkdirs()
        it.writeText(content)
    }

    private val icon = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 50" width="100" height="50">
        <rect id="body" width="100" height="50"/></svg>"""

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    @Test
    fun placesDocumentsInTheirViewport() {
        write("icon.svg", icon)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
            <image href="icon.svg" x="10" y="20" width="200" height="200"/>
        </svg>"""))
        val body = c.shape("body")
        // fitted into 200 by 200 by its view box, centered vertically
        val bounds = body.effectiveShape.bounds
        assertEquals(10.0, bounds.x, 1E-9)
        assertEquals(70.0, bounds.y, 1E-9)
        assertEquals(200.0, bounds.width, 1E-9)
        assertEquals(100.0, bounds.height, 1E-9)
        // clipped to the viewport of the image
        val clipped = generateSequence(body as CompositionNode) { it.parent }.mapNotNull { it.style.clipPath as? ClipPath.Shapes }.first()
        assertEquals(org.openrndr.shape.Rectangle(10.0, 20.0, 200.0, 200.0), clipped.content.findShapes().single().shape.bounds)
    }

    @Test
    fun usesTheIntrinsicSize() {
        write("icon.svg", icon)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg"><image href="icon.svg" width="300"/></svg>"""))
        val bounds = c.shape("body").effectiveShape.bounds
        assertEquals(Vector2(300.0, 150.0), bounds.dimensions)
    }

    @Test
    fun isolatesDocuments() {
        write("icon.svg", icon)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg">
            <g fill="red" stroke="red"><image href="icon.svg"/></g>
        </svg>"""))
        val style = c.shape("body").effectiveStyle
        val fill = (style.fill as Paint.RGB).value
        assertEquals(listOf(0.0, 0.0, 0.0), listOf(fill.r, fill.g, fill.b))
        assertIs<Paint.None>(style.stroke)
        // the reset styles are written, the document stays isolated
        val written = parseSVG(writeSVG(c))
        assertIs<Paint.None>(written.shape("body").effectiveStyle.stroke)
    }

    @Test
    fun readsDataUrls() {
        val encoded = java.net.URLEncoder.encode(icon, Charsets.UTF_8).replace("+", "%20")
        val base64 = Base64.getEncoder().encodeToString(icon.replace("body", "b64").toByteArray())
        val c = parseSVG("""<svg xmlns="http://www.w3.org/2000/svg">
            <image href="data:image/svg+xml,$encoded" width="100" height="50"/>
            <image href="data:image/svg+xml;base64,$base64" width="100" height="50"/>
        </svg>""")
        assertEquals(listOf("body", "b64"), c.findShapes().map { it.id })
    }

    @Test
    fun resolvesReferencesInDocumentsAndStopsCycles() {
        write("sub/inner.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10">
            <image href="deeper/leaf.svg"/><image href="../loop.svg"/></svg>""")
        write("sub/deeper/leaf.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect id="leaf" width="10" height="10"/></svg>""")
        write("loop.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect id="loop" width="1" height="1"/>
            <image href="sub/inner.svg"/></svg>""")
        val c = loadSVG(File(directory, "loop.svg"))
        // the reference of the inner document is relative to it, the loop is loaded once
        assertEquals(listOf("loop", "leaf"), c.findShapes().map { it.id })
    }
}
