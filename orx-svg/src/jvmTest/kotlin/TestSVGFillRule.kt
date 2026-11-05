import org.openrndr.extra.composition.FillRule
import org.openrndr.extra.composition.fillRule
import org.openrndr.extra.composition.findShapes
import org.openrndr.extra.svg.parseSVG
import org.openrndr.extra.svg.toSVG
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestSVGFillRule {
    private val svg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
            <path id="default" d="M 0 0 h 10 v 10 h -10 z"/>
            <path id="evenodd" d="M 0 0 h 10 v 10 h -10 z" fill-rule="evenodd"/>
            <path id="styled" d="M 0 0 h 10 v 10 h -10 z" style="fill-rule: evenodd"/>
            <g fill-rule="evenodd">
                <path id="inherited" d="M 0 0 h 10 v 10 h -10 z"/>
                <path id="overridden" d="M 0 0 h 10 v 10 h -10 z" fill-rule="nonzero"/>
            </g>
        </svg>
    """.trimIndent()

    private val composition = parseSVG(svg)
    private fun rule(id: String) = composition.findShapes().first { it.id == id }.effectiveStyle.fillRule

    @Test
    fun parsesFillRules() {
        assertEquals(FillRule.NonZero, rule("default"))
        assertEquals(FillRule.EvenOdd, rule("evenodd"))
        assertEquals(FillRule.EvenOdd, rule("styled"))
    }

    @Test
    fun fillRulesAreInherited() {
        assertEquals(FillRule.EvenOdd, rule("inherited"))
        assertEquals(FillRule.NonZero, rule("overridden"))
    }

    @Test
    fun writesFillRules() {
        val written = composition.toSVG()
        assertTrue("fill-rule=\"evenodd\"" in written)
        val reread = parseSVG(written)
        assertEquals(FillRule.EvenOdd, reread.findShapes().first { it.id == "evenodd" }.effectiveStyle.fillRule)
        assertEquals(FillRule.NonZero, reread.findShapes().first { it.id == "default" }.effectiveStyle.fillRule)
    }
}
