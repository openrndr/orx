import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSVGFontFaces {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100">
            <style>
                /* @font-face { font-family: "Commented"; src: url(x.ttf); } */
                @font-face { font-family: "Doc"; src: url("fonts/doc.ttf") format("truetype"); }
                @font-face {
                    font-family: Doc; font-weight: 300 700; font-style: italic;
                    src: url(doc.svg#f) format("svg"), local("Doc Italic"), url('data:font/ttf;base64,AAAA') format('truetype');
                }
                @font-face { font-family: "Bold"; font-weight: bold; src: url(https://example.com/bold.woff2) format("woff2"); }
                text { font-family: Doc; }
            </style>
            <text>a</text>
        </svg>""",
        File("/documents/drawing.svg").toURI()
    )

    private val faces = (c.root as GroupNode).fontFaces

    @Test
    fun parsesFontFaces() {
        assertEquals(listOf("Doc", "Doc", "Bold"), faces.map { it.family })
        // relative sources are resolved against the location of the document
        assertEquals(listOf(File("/documents/fonts/doc.ttf").path), faces[0].sources)
        assertEquals(400..400, faces[0].weight)
        // fonts in SVG format are left out, local fonts are kept, data urls are passed on as they are
        assertEquals(listOf("local(\"Doc Italic\")", "data:font/ttf;base64,AAAA"), faces[1].sources)
        assertEquals(300..700, faces[1].weight)
        assertTrue(faces[1].italic)
        assertEquals(listOf("https://example.com/bold.woff2"), faces[2].sources)
        assertEquals(700..700, faces[2].weight)
    }

    @Test
    fun resolvesFacesBeforeRegisteredFonts() {
        CompositionFonts.register("Doc Italic", "registered-italic.ttf")
        CompositionFonts.register("Registered", "registered.ttf")
        val regular = CompositionFonts.candidates(listOf("Doc", "Registered"), 400, false, faces)
        assertEquals(listOf(File("/documents/fonts/doc.ttf").path, "registered.ttf"), regular.dropLast(1))
        // the italic face matches by style and weight range, local() refers to a registered family
        val italic = CompositionFonts.candidates(listOf("Doc"), 600, true, faces)
        assertEquals(listOf("registered-italic.ttf", "data:font/ttf;base64,AAAA"), italic.dropLast(1))
        // families that are neither faces nor registered are skipped
        assertEquals(listOf("registered.ttf"), CompositionFonts.candidates(listOf("Unknown", "Registered")).dropLast(1))
    }

    @Test
    fun writesFontFaces() {
        val written = writeSVG(c)
        assertTrue("@font-face { font-family: \"Bold\"; src: url(\"https://example.com/bold.woff2\"); font-weight: 700;" in written)
        val reread = parseSVG(written)
        assertEquals(faces.map { it.sources }, (reread.root as GroupNode).fontFaces.map { it.sources })
        assertEquals(faces.map { it.weight }, (reread.root as GroupNode).fontFaces.map { it.weight })
    }
}
