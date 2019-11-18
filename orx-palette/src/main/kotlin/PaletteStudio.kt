package org.openrndr.extra.palette

import mu.KotlinLogging
import com.google.gson.GsonBuilder
import org.openrndr.Extension
import org.openrndr.Keyboard
import org.openrndr.Program
import org.openrndr.extra.noise.Random
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.color.ColorRGBa.Companion.GREEN
import org.openrndr.color.ColorRGBa.Companion.PINK
import org.openrndr.color.ColorRGBa.Companion.RED
import org.openrndr.color.ColorRGBa.Companion.WHITE
import org.openrndr.color.ColorRGBa.Companion.YELLOW
import org.openrndr.resourceUrl
import java.io.File
import java.net.URL
import kotlin.math.max
import kotlin.math.min

internal val logger = KotlinLogging.logger {}

internal typealias Colors = List<ColorRGBa>

data class Palette(
        val background: ColorRGBa,
        val foreground: ColorRGBa,
        val colors: Colors,
        val colors2: Colors
)

internal val defaultPalette: Palette = Palette(
        BLACK,
        PINK,
        listOf(BLACK, PINK, YELLOW, RED, GREEN),
        listOf(PINK, YELLOW, RED, GREEN)
)

internal fun getCollPath(n: String) = URL(resourceUrl("/org/openrndr/extra/palette/collections/collection-$n.json"))

class PaletteStudio(
        loadDefault: Boolean = true,
        val sortBy: SortBy = SortBy.NO_SORTING,
        collection: Collections = Collections.ONE,
        val colorCountConstraint: Int = 0
) : Extension {
    var palettes: MutableList<MutableList<ColorRGBa>> = mutableListOf()
    var palette: Palette = defaultPalette

    private var paletteIndex: Int = 0

    enum class SortBy {
        NO_SORTING, DARKEST, BRIGHTEST
    }

    enum class Collections {
        ONE, TWO, THREE
    }

    private val collectionsResource = mapOf(
        Collections.ONE to getCollPath("1"),
        Collections.TWO to getCollPath("2"),
        Collections.THREE to getCollPath("3")
    )

    var background: ColorRGBa = defaultPalette.background
        get() {
            return palette.background
        }
    var foreground: ColorRGBa = defaultPalette.foreground
        get() {
            return palette.foreground
        }
    var colors: Colors = defaultPalette.colors
        get() {
            return palette.colors
        }
    var colors2: Colors = defaultPalette.colors2
        get() {
            return palette.colors2
        }

    init {
        if (loadDefault) {
            loadCollection(collection)
        }
    }

    private fun loadCollection(newCollection: Collections) {
        val collectionPath: URL = collectionsResource[newCollection]!!

        palettes = mutableListOf()

        loadFromURL(collectionPath)

        val choice = Random.pick(palettes)

        palette = createPalette(choice)
        paletteIndex = palettes.indexOf(choice)
    }

    private fun load(contents: String) {
        try {
            val gson = GsonBuilder().create()

            val clipData = gson.fromJson(
                    contents, Array<Array<String>>::class.java
            ).toList()

            for (p in clipData) {
                val palette = mutableListOf<ColorRGBa>()

                for (colorStr in p.toList()) {
                    val colorInt = fromHex(colorStr)

                    palette.add(colorInt)
                }

                palettes.add(palette)
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Error: Could not load palettes" }
            logger.info { "Only JSON files with Array<Array<String>> structure can be loaded using this method" }
        }
    }

    private fun loadFromURL(url: URL): Unit = load(url.readText())

    private fun createPalette(colors: MutableList<ColorRGBa>) : Palette {
        when(sortBy) {
            SortBy.DARKEST -> {
                val darkest = Comparator<ColorRGBa> { c1: ColorRGBa, c2: ColorRGBa -> (getLuminance(c1) - getLuminance(c2)).toInt() }
                colors.sortWith(darkest)
            }
            SortBy.BRIGHTEST -> {
                val brightest = Comparator<ColorRGBa> { c1: ColorRGBa, c2: ColorRGBa -> (getLuminance(c2) - getLuminance(c1)).toInt() }
                colors.sortWith(brightest)
            }
            SortBy.NO_SORTING -> {}
        }

        val background = colors.first()

        val foreground = colors
                .takeLast(colors.size - 1)
                .map { getContrast(background, it) to it }
                .maxBy { it.first }!!
                .second

        var constraint =  colors.size
        var constraint2 = colors.size

        if (colorCountConstraint > 0 && colorCountConstraint < colors.size) {
            constraint = colorCountConstraint
            constraint2 = colorCountConstraint + 1
        }

        val colors1 = colors.slice(0 until constraint)
        val colors2 = colors.slice(1 until constraint2)

        return Palette(background, foreground, colors1, colors2)
    }

    fun loadExternal(filePath: String) {
        palettes = mutableListOf()

        load(File(filePath).readText())

        val choice = Random.pick(palettes)

        palette = createPalette(choice)
        paletteIndex = palettes.indexOf(choice)
    }

    fun select(index: Int = 0) {
        paletteIndex = max(min(index, palettes.size - 1), 0)
        palette = createPalette(palettes[paletteIndex])
    }

    fun getIndex(): Int {
        return paletteIndex
    }

    fun randomize() {
        palette = createPalette(Random.pick(palette.colors, count = palette.colors.size))
    }

    fun randomPalette() {
        val comparison = palette.colors.toMutableList()
        val colors= Random.pick(palettes, comparison) as MutableList<ColorRGBa>

        paletteIndex = palettes.indexOf(colors)
        palette = createPalette(colors)
    }

    fun changeCollection(newCollection: Collections) {
        loadCollection(newCollection)
    }

    private fun registerKeybindings(keyboard: Keyboard) {
        keyboard.keyDown.listen {
            if (it.name == "l") {
                randomPalette()
            }
            if (it.name == "k") {
                randomize()
            }
        }
    }

    /*
     * EXTENSION
     */
    override var enabled: Boolean = true

    override fun setup(program: Program) {
        registerKeybindings(program.keyboard)
    }
}