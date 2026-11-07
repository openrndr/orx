import org.openrndr.extra.style.CssMediaEnvironment
import org.openrndr.extra.style.cssMediaMatches
import org.openrndr.extra.style.parseCssRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestMedia {
    private val wide = CssMediaEnvironment(1200.0, 800.0, "dark")
    private val narrow = CssMediaEnvironment(500.0, 900.0, "light")

    @Test
    fun mediaQueries() {
        assertTrue(cssMediaMatches("(min-width: 800px)", wide))
        assertFalse(cssMediaMatches("(min-width: 800px)", narrow))
        assertTrue(cssMediaMatches("screen and (max-width: 600px)", narrow))
        assertFalse(cssMediaMatches("print and (max-width: 600px)", narrow))
        assertTrue(cssMediaMatches("(orientation: portrait)", narrow))
        assertTrue(cssMediaMatches("(prefers-color-scheme: dark)", wide))
        assertTrue(cssMediaMatches("(width >= 1000px)", wide))
        assertTrue(cssMediaMatches("(min-aspect-ratio: 4/3)", wide))
        assertTrue(cssMediaMatches("not (min-width: 800px)", narrow))
        assertTrue(cssMediaMatches("(min-width: 2000px), (prefers-color-scheme: light)", narrow))
        assertFalse(cssMediaMatches("(hover: hover)", wide))
    }

    @Test
    fun rulesInMediaRules() {
        val rules = parseCssRules("""
            a { color: red }
            @media (min-width: 600px) {
                b { color: blue }
                @media (prefers-color-scheme: dark) { c { color: green } }
            }
        """)
        assertEquals(listOf(null, "(min-width: 600px)", "(min-width: 600px) and (prefers-color-scheme: dark)"), rules.map { it.media })
        assertEquals(listOf("a", "b", "c"), rules.map { it.selectors.single() })
    }
}
