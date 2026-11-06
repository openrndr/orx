import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.loadSVG
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGCrossFile {
    private val directory = createTempDirectory("svg-cross-file").toFile()

    private fun write(name: String, content: String) = File(directory, name).also {
        it.parentFile.mkdirs()
        it.writeText(content)
    }

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    private val sprites = """<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
        <style>@font-face { font-family: "Sprite"; src: url("sprite.ttf"); }</style>
        <defs>
            <linearGradient id="g"><stop offset="0" stop-color="red"/><stop offset="1" stop-color="blue"/></linearGradient>
            <symbol id="icon" viewBox="0 0 10 10"><rect id="inSymbol" width="10" height="10"/></symbol>
        </defs>
        <rect id="filled" width="20" height="20" fill="url(#g)"/>
        <view id="corner" viewBox="0 0 50 50" preserveAspectRatio="none"/>
    </svg>"""

    @Test
    fun usesElementsOfOtherDocuments() {
        write("sprites.svg", sprites)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
            <use href="sprites.svg#icon" x="10" y="20" width="100" height="100"/>
            <use href="sprites.svg#filled" x="200"/>
            <use href="sprites.svg#missing"/>
            <use href="missing.svg#icon"/>
        </svg>"""))
        val symbol = c.shape("inSymbol").effectiveShape.bounds
        assertEquals(Rectangle(10.0, 20.0, 100.0, 100.0), symbol)
        val filled = c.shape("filled")
        assertEquals(200.0, filled.effectiveShape.bounds.x, 1E-9)
        // the gradient of the other document
        assertIs<Paint.Gradient>(filled.effectiveStyle.fill)
        // the fonts of the other document are available to its elements
        val fonts = generateSequence(filled as CompositionNode) { it.parent }.filterIsInstance<GroupNode>().flatMap { it.fontFaces }
        assertEquals(listOf("Sprite"), fonts.map { it.family }.toList())
    }

    @Test
    fun selectsViews() {
        write("sprites.svg", sprites)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
            <image href="sprites.svg#corner" width="100" height="200"/>
            <image href="sprites.svg#svgView(viewBox(0,0,20,20))" x="200" width="100" height="100"/>
        </svg>"""))
        // the view box of the view, stretched by its aspect ratio
        val (corner, view) = c.findShapes().filter { it.id == "filled" }
        assertEquals(Rectangle(0.0, 0.0, 40.0, 80.0), corner.effectiveShape.bounds)
        assertEquals(Rectangle(200.0, 0.0, 100.0, 100.0), view.effectiveShape.bounds)

        val viewed = loadSVG(File(directory, "sprites.svg").path + "#corner")
        assertEquals(Rectangle(0.0, 0.0, 50.0, 50.0), viewed.documentStyle.viewBox.value)
        assertEquals(Align.NONE, viewed.documentStyle.preserveAspectRatio.align)
    }

    @Test
    fun readsImagesAndTilesOfFilters() {
        write("sprites.svg", sprites)
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">
            <defs>
                <circle id="dot" r="5"/>
                <filter id="f" primitiveUnits="objectBoundingBox">
                    <feImage href="#dot" x="0.1" y="20%" width="0.5" height="0.5" result="dot"/>
                    <feTile in="dot"/>
                    <feImage href="sprites.svg" preserveAspectRatio="none"/>
                </filter>
            </defs>
            <rect id="r" width="10" height="10" filter="url(#f)"/>
        </svg>"""))
        val p = c.shape("r").style.filter.value.single().primitives
        val element = assertIs<FilterPrimitive.Image>(p[0])
        assertEquals(listOf("dot"), element.content.findShapes().map { it.id })
        assertNull(element.size)
        val sub = element.subregion!!
        assertEquals(listOf(0.1, 0.2, 0.5, 0.5), listOf(sub.x, sub.y, sub.width, sub.height))
        assertIs<FilterInput.Result>(assertIs<FilterPrimitive.Tile>(p[1]).input)
        val document = assertIs<FilterPrimitive.Image>(p[2])
        assertEquals(Vector2(100.0, 100.0), document.size)
        assertEquals(Align.NONE, document.aspectRatio.align)
        assertTrue(document.content.findShapes().any { it.id == "filled" })
    }

    @Test
    fun importsStyleSheets() {
        write("css/base.css", """@charset "utf-8"; .a { fill: #00ff00; } @font-face { font-family: "Imported"; src: url("fonts/f.ttf"); }""")
        write("css/theme.css", """@import "base.css"; .b { fill: #0000ff; }""")
        val c = loadSVG(write("main.svg", """<svg xmlns="http://www.w3.org/2000/svg">
            <style>@import url(css/theme.css); .c { fill: #ff0000; }</style>
            <rect id="a" class="a" width="1" height="1"/><rect id="b" class="b" width="1" height="1"/><rect id="c" class="c" width="1" height="1"/>
        </svg>"""))
        assertEquals(1.0, (c.shape("a").style.fill as Paint.RGB).value.g, 1E-9)
        assertEquals(1.0, (c.shape("b").style.fill as Paint.RGB).value.b, 1E-9)
        assertEquals(1.0, (c.shape("c").style.fill as Paint.RGB).value.r, 1E-9)
        // references in imported style sheets are relative to them
        val face = (c.root as GroupNode).fontFaces.single()
        assertTrue(face.sources.single().endsWith("css/fonts/f.ttf"), face.sources.single())
    }
}
