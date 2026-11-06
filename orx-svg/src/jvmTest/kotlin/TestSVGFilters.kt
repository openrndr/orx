import org.openrndr.extra.composition.*
import org.openrndr.extra.shadestyles.presets.PaintUnits
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.writeSVG
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGFilters {
    private val c = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="200" height="100" color-interpolation-filters="sRGB">
            <defs>
                <filter id="shadow" x="-20%" width="140%" primitiveUnits="objectBoundingBox">
                    <feGaussianBlur in="SourceAlpha" stdDeviation="0.1 0.2" result="b"/>
                    <feOffset in="b" dx="0.05" dy="0.1" result="o"/>
                    <feFlood flood-color="#ff0000" flood-opacity="0.5"/>
                    <feComposite in2="o" operator="in"/>
                    <feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge>
                </filter>
                <filter id="space" filterUnits="userSpaceOnUse" x="0" y="0" width="50" height="60" color-interpolation-filters="linearRGB">
                    <feTurbulence type="fractalNoise" baseFrequency="0.1 0.2" numOctaves="3" seed="4"/>
                    <feDisplacementMap in="SourceGraphic" scale="10" xChannelSelector="R" yChannelSelector="B"/>
                    <feComponentTransfer><feFuncR type="table" tableValues="0 1"/><feFuncA type="gamma" exponent="2"/></feComponentTransfer>
                    <feColorMatrix type="hueRotate" values="90"/>
                    <feMorphology operator="dilate" radius="2"/>
                    <feBlend in2="BackgroundImage" mode="screen"/>
                    <feImage href="#x"/>
                </filter>
            </defs>
            <rect id="a" width="10" height="10" filter="url(#shadow)"/>
            <rect id="b" width="10" height="10" style="filter: url(#space) blur(2px) drop-shadow(1px 2px 4px #00ff00) grayscale(50%)"/>
            <rect id="c" width="10" height="10" filter="url(#missing)"/>
        </svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }

    @Test
    fun parsesFilterElements() {
        val effect = c.shape("a").style.filter.value.single()
        assertEquals("shadow", effect.id)
        assertEquals(Rectangle(-0.2, -0.1, 1.4, 1.2), effect.region)
        assertEquals(PaintUnits.BOUNDS, effect.regionUnits)
        assertEquals(PaintUnits.BOUNDS, effect.primitiveUnits)
        val p = effect.primitives
        assertEquals(5, p.size)
        val blur = assertIs<FilterPrimitive.GaussianBlur>(p[0])
        assertIs<FilterInput.SourceAlpha>(blur.input)
        assertEquals(Vector2(0.1, 0.2), blur.stdDeviation)
        assertEquals("b", blur.result)
        assertEquals("b", (assertIs<FilterPrimitive.Offset>(p[1]).input as FilterInput.Result).name)
        assertEquals(0.5, assertIs<FilterPrimitive.Flood>(p[2]).color.alpha, 1E-9)
        val composite = assertIs<FilterPrimitive.Composite>(p[3])
        assertIs<FilterInput.Previous>(composite.input)
        assertEquals(FilterPrimitive.CompositeOperator.IN, composite.operator)
        val merge = assertIs<FilterPrimitive.Merge>(p[4])
        assertIs<FilterInput.Previous>(merge.inputs[0])
        assertIs<FilterInput.SourceGraphic>(merge.inputs[1])
        // color-interpolation-filters is inherited from the document
        assertTrue(p.all { it.colorSpace == FilterColorSpace.SRGB })
    }

    @Test
    fun parsesPrimitives() {
        val effects = c.shape("b").style.filter.value
        assertEquals(2, effects.size)
        val space = effects[0]
        assertEquals(Rectangle(0.0, 0.0, 50.0, 60.0), space.region)
        assertEquals(PaintUnits.OBJECT, space.regionUnits)
        val p = space.primitives
        assertTrue(p.all { it.colorSpace == FilterColorSpace.LINEAR_RGB })
        val turbulence = assertIs<FilterPrimitive.Turbulence>(p[0])
        assertEquals(Vector2(0.1, 0.2), turbulence.baseFrequency)
        assertEquals(3, turbulence.octaves)
        assertTrue(turbulence.fractalNoise)
        val displacement = assertIs<FilterPrimitive.DisplacementMap>(p[1])
        assertEquals(FilterPrimitive.Channel.R, displacement.xChannel)
        assertEquals(FilterPrimitive.Channel.B, displacement.yChannel)
        assertEquals(10.0, displacement.scale)
        val transfer = assertIs<FilterPrimitive.ComponentTransfer>(p[2])
        assertEquals(0.25, transfer.red.apply(0.25), 1E-9)
        assertEquals(0.25, transfer.alpha.apply(0.5), 1E-9)
        assertIs<FilterPrimitive.TransferFunction.Identity>(transfer.green)
        assertEquals(20, assertIs<FilterPrimitive.ColorMatrix>(p[3]).values.size)
        assertEquals(Vector2(2.0, 2.0), assertIs<FilterPrimitive.Morphology>(p[4]).radius)
        assertIs<FilterInput.Unsupported>(assertIs<FilterPrimitive.Blend>(p[5]).input2)
        assertIs<FilterPrimitive.Image>(p[6])
    }

    @Test
    fun parsesFilterFunctions() {
        val functions = c.shape("b").style.filter.value[1]
        // filter functions are applied everywhere, in sRGB
        assertNull(functions.region)
        assertTrue(functions.primitives.all { it.colorSpace == FilterColorSpace.SRGB })
        assertEquals(Vector2(2.0, 2.0), assertIs<FilterPrimitive.GaussianBlur>(functions.primitives[0]).stdDeviation)
        val shadow = assertIs<FilterPrimitive.DropShadow>(functions.primitives[1])
        assertEquals(1.0, shadow.dx)
        assertEquals(2.0, shadow.dy)
        assertEquals(Vector2(2.0, 2.0), shadow.stdDeviation)
        assertEquals(1.0, shadow.color.g, 1E-9)
        assertIs<FilterPrimitive.ColorMatrix>(functions.primitives[2])
    }

    @Test
    fun hidesElementsWithMissingFilters() {
        val effect = c.shape("c").style.filter.value.single()
        assertEquals(0.0, assertIs<FilterPrimitive.Flood>(effect.primitives.single()).color.alpha)
    }

    @Test
    fun writesFilters() {
        val written = writeSVG(c)
        assertTrue("<filter id=\"shadow\"" in written)
        assertTrue("filter:url(#shadow)" in written)
        val reread = parseSVG(written)
        val p = reread.findShapes().first { it.id == "a" }.style.filter.value.single().primitives
        assertEquals(listOf("GaussianBlur", "Offset", "Flood", "Composite", "Merge"), p.map { it::class.simpleName })
        assertEquals("b", p[0].result)
        assertEquals(FilterColorSpace.SRGB, p[0].colorSpace)
        val b = reread.findShapes().first { it.id == "b" }.style.filter.value
        assertEquals(2, b.size)
        assertIs<FilterPrimitive.DropShadow>(b[1].primitives[1])
    }
}
