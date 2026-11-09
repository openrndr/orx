import org.openrndr.draw.font.loadFace
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.extra.textwriter.GlyphMaps
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestGlyphMaps : AbstractApplicationTestFixture() {
    @Test
    fun transientGlyphsAreFoundAgainAfterTheMapIsCleared() {
        val face = loadFace("../demo-data/fonts/IBMPlexMono-Regular.ttf").withSize(16.0)
        GlyphMaps.setTransient(face)
        val budget = GlyphMaps.transientBudget
        try {
            val a = SlugTextDrawer()
            a.addText(face, "abc", Vector2.ZERO)
            val transient = GlyphMaps.transientOf(a.boundaryDrawer)
            val stable = GlyphMaps.of(a.boundaryDrawer)
            assertTrue(transient.glyphCount >= 3)
            val generation = transient.generation
            // a tiny budget clears the map the next time glyphs are added, by another drawer
            GlyphMaps.transientBudget = 1L
            val b = SlugTextDrawer()
            b.addText(face, "xyz", Vector2.ZERO)
            assertTrue(transient.generation > generation)
            // the first drawer finds its glyphs again when it draws: the indices of its commands are those of its
            // glyphs in the cleared map
            a.draw(program.drawer)
            val expected = "abc".map { transient.getBoundaryForGlyphIndex(face, face.glyphForCharacter(it).index) }
            assertEquals(expected, a.commands.map { it.boundaryIndex })
            b.draw(program.drawer)
            // the shared map was not used for them
            assertEquals(0, "abcxyz".count { stable.glyphs.containsKey(face.hashCode() * 31 + face.glyphForCharacter(it).index) })
        } finally {
            GlyphMaps.transientBudget = budget
            GlyphMaps.setTransient(face, false)
        }
    }
}
