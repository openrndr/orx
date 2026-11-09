import org.junit.jupiter.api.Assumptions.assumeTrue
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.GlyphMaps
import org.openrndr.fontdriver.freetype.FontDriverFreetype
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// which fonts are transient needs a font driver but no window
class TestGlyphMapsChurn {
    @Test
    fun aVariableFontWhoseMastersKeepChangingBecomesTransient() {
        val path = System.getProperty("user.home") + "/IdeaProjects/openrndr/data/fonts/Platypi-Variable.ttf"
        assumeTrue(java.io.File(path).exists(), "Platypi-Variable.ttf of the openrndr repository is not there")
        if (FontDriver.driver == null) FontDriver.driver = FontDriverFreetype()
        val face = loadFace(path)
        val axis = face.master.ranges.keys.first()
        val range = face.master.ranges.getValue(axis)
        assertFalse(GlyphMaps.use(face))
        var transient = false
        for (i in 1..GlyphMaps.churnMasters + 1) {
            val master = face.master.also { it[axis] = range.start + (range.endInclusive - range.start) * i / 20.0 }
            transient = GlyphMaps.use(face.withMaster(master))
        }
        assertTrue(transient)
        assertTrue(GlyphMaps.isTransient(face))
        GlyphMaps.setTransient(face, false)
        assertFalse(GlyphMaps.isTransient(face))
    }
}
