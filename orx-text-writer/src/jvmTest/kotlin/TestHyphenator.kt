import org.openrndr.draw.font.internal.Script
import org.openrndr.extra.textwriter.Hyphenator
import org.openrndr.extra.textwriter.hyphenate
import org.openrndr.extra.textwriter.hyphenation.hyphenationPatternSets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestHyphenator {
    private fun hyphenated(word: String, language: String, script: Script? = null) =
        Hyphenator.forLanguage(language, script)!!.hyphenateWord(word, '-')

    @Test
    fun english() {
        assertEquals("hy-phen-ation", hyphenated("hyphenation", "en-us"))
        assertEquals("Hy-phen-ation", hyphenated("Hyphenation", "en"))
        // from the exception list
        assertEquals("as-so-ciate", hyphenated("associate", "en"))
    }

    @Test
    fun otherLanguages() {
        assertEquals("Sil-ben-tren-nung", hyphenated("Silbentrennung", "de"))
        assertEquals("af-bre-ken", hyphenated("afbreken", "nl"))
        assertEquals("ко-ло-ко-ло", hyphenated("колоколо", "uk"))
        assertEquals("αλ-φά-βη-το", hyphenated("αλφάβητο", "el"))
    }

    @Test
    fun minimumLengthsAreRespected() {
        val hyphenator = Hyphenator.forLanguage("en")!!
        assertEquals(2, hyphenator.leftMin)
        assertEquals(3, hyphenator.rightMin)
        assertTrue(hyphenator.hyphenationPoints("ab").isEmpty())
        for (word in listOf("hyphenation", "computer", "algorithm", "typography")) {
            for (point in hyphenator.hyphenationPoints(word)) {
                assertTrue(point >= 2 && word.length - point >= 3, "$word $point")
            }
        }
    }

    @Test
    fun textGetsSoftHyphens() {
        val result = hyphenate("The hyphenation algorithm, well-known.", "en")
        assertEquals("The hy\u00ADphen\u00ADation al\u00ADgo\u00ADrithm, well-known.", result)
        // hyphenating twice does not add more hyphens
        assertEquals(result, hyphenate(result, "en"))
    }

    @Test
    fun unknownLanguagesAreLeftUnchanged() {
        assertEquals("hyphenation", hyphenate("hyphenation", "xx"))
        assertNull(Hyphenator.findPatternSet("xx"))
    }

    @Test
    fun languageTagsAreResolved() {
        assertEquals("en-gb", Hyphenator.findPatternSet("en_GB")?.tag)
        assertEquals("en-gb", Hyphenator.findPatternSet("en-AU")?.tag)
        assertEquals("de-1996", Hyphenator.findPatternSet("de-AT")?.tag)
        assertEquals("de-1901", Hyphenator.findPatternSet("de-1901")?.tag)
        assertEquals("fr", Hyphenator.findPatternSet("fr-CA")?.tag)
        assertEquals("nb", Hyphenator.findPatternSet("no")?.tag)
        assertEquals("mul-ethi", Hyphenator.findPatternSet("am")?.tag)
    }

    @Test
    fun scriptSelectsPatternSet() {
        assertEquals("mn-cyrl", Hyphenator.findPatternSet("mn", Script.CYRILLIC)?.tag)
        assertNull(Hyphenator.findPatternSet("mn", Script.LATIN))
        assertEquals("zh-latn-pinyin", Hyphenator.findPatternSet("zh", Script.LATIN)?.tag)
        assertEquals("zh-latn-pinyin", Hyphenator.findPatternSet("zh-Latn")?.tag)
        assertNull(Hyphenator.findPatternSet("zh"))
        // English patterns are not used for Cyrillic text
        assertNull(Hyphenator.findPatternSet("en", Script.CYRILLIC))
        // Sanskrit patterns cover multiple scripts
        assertEquals("sa", Hyphenator.findPatternSet("sa", Script.DEVANAGARI)?.tag)
    }

    @Test
    fun allPatternSetsLoad() {
        for (set in hyphenationPatternSets) {
            val hyphenator = Hyphenator(set)
            assertTrue(set.notice.isNotEmpty(), set.tag)
            hyphenator.hyphenate("lorem ipsum")
        }
    }

    @Test
    fun lpplLanguages() {
        for ((language, word) in listOf(
            "ru" to "гипертекстовый", "sv" to "avstavningsprogram", "tr" to "bilgisayarlar",
            "ca" to "independentment", "hr" to "rastavljanje", "is" to "orðskiptingar"
        )) {
            val hyphenator = Hyphenator.forLanguage(language)
            assertTrue(hyphenator != null && hyphenator.hyphenationPoints(word).isNotEmpty(), language)
        }
    }

    @Test
    fun serbianUsesTheScript() {
        assertEquals("sh-cyrl", Hyphenator.findPatternSet("sr")?.tag)
        assertEquals("sh-latn", Hyphenator.findPatternSet("sr", Script.LATIN)?.tag)
        assertEquals("sh-latn", Hyphenator.findPatternSet("sr-Latn")?.tag)
        assertEquals("sh-cyrl", Hyphenator.findPatternSet("bs", Script.CYRILLIC)?.tag)
    }
}
