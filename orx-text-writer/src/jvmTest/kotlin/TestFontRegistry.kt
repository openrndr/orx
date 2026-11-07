import org.openrndr.extra.textwriter.FontFace
import org.openrndr.extra.textwriter.FontRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestFontRegistry {
    @Test
    fun resolvesByFamilyWeightAndStyle() {
        val fonts = FontRegistry().apply {
            fallback = "fallback.ttf"
            register("Sans", "sans.ttf")
            register("Sans", "sans-medium.ttf", weight = 500)
            register("Sans", "sans-bold.ttf", weight = 700)
            register("Sans", "sans-italic.ttf", italic = true)
        }
        assertEquals("sans.ttf", fonts.resolve(listOf("sans")))
        assertEquals("sans-bold.ttf", fonts.resolve(listOf("Sans"), 800))
        assertEquals("sans-medium.ttf", fonts.resolve(listOf("Sans"), 550))
        assertEquals("sans-italic.ttf", fonts.resolve(listOf("Sans"), 700, italic = true))
        assertEquals(listOf("sans.ttf", "fallback.ttf"), fonts.candidates(listOf("Unknown", "'Sans'")))
        // a later registration of the same face replaces it
        fonts.register("Sans", "sans-2.ttf")
        assertEquals("sans-2.ttf", fonts.resolve(listOf("Sans")))
    }

    @Test
    fun fallsBackOnTheParent() {
        val parent = FontRegistry().apply {
            fallback = "fallback.ttf"
            register("Shared", "shared.ttf")
        }
        val child = FontRegistry(parent).apply { register("Own", "own.ttf") }
        assertEquals(listOf("own.ttf", "shared.ttf", "fallback.ttf"), child.candidates(listOf("Own", "Shared")))
        assertTrue(child.isRegistered("shared"))
        assertFalse(parent.isRegistered("Own"))
        // faces of documents refer to registered families with local()
        val faces = listOf(FontFace("Doc", listOf("local(Shared)", "doc.ttf")))
        assertEquals(listOf("shared.ttf", "doc.ttf", "fallback.ttf"), child.candidates(listOf("Doc"), faces = faces))
        child.fallback = "own-fallback.ttf"
        assertEquals(listOf("own-fallback.ttf"), child.candidates(listOf("Unknown")))
    }
}
