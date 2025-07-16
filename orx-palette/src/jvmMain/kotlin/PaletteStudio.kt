package org.openrndr.extra.palette

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.*
import org.openrndr.extra.noise.Random
import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorRGBa.Companion.BLACK
import org.openrndr.color.ColorRGBa.Companion.GREEN
import org.openrndr.color.ColorRGBa.Companion.PINK
import org.openrndr.color.ColorRGBa.Companion.RED
import org.openrndr.color.ColorRGBa.Companion.YELLOW
import org.openrndr.color.ColorRGBa.Companion.fromHex
import java.io.File
import java.net.URL
import kotlin.math.max
import kotlin.math.min

internal val logger = KotlinLogging.logger {}

class Palette(
        val background: ColorRGBa,
        val foreground: ColorRGBa,
        val colors: List<ColorRGBa>,
        val colors2: List<ColorRGBa>
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
    var palettes: MutableList<List<ColorRGBa>> = mutableListOf()
    var palette: Palette = defaultPalette

    var randomPaletteKey = 'l'
    var randomizeKey = 'k'

    private var paletteIndex: Int = 0
    var nextColorIndex: Int = 0
    get() {
        return field++ % colors.size
    }
    var nextColor2Index: Int = 0
    get() {
        return field++ % colors2.size
    }

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

    val background: ColorRGBa
        get() {
            return palette.background
        }

    val foreground: ColorRGBa
        get() {
            return palette.foreground
        }

    val colors: List<ColorRGBa>
        get() {
            return palette.colors
        }

    val colors2: List<ColorRGBa>
        get() {
            return palette.colors2
        }

    init {
        if (loadDefault) {
            loadCollection(collection)
        }
    }

    private var onChangeListener = {}

    fun loadCollection(newCollection: Collections) {
        val collectionPath: URL = collectionsResource.getValue(newCollection)
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
            )

            for (p in clipData) {
                val palette = p.map { fromHex(it) }
                palettes.add(palette)
            }
        } catch (ex: JsonParseException) {
            error("Only JSON files with Array<Array<String>> structure can be loaded using this method")
        }
    }

    private fun loadFromURL(url: URL): Unit = load(url.readText())

    private fun assemblePalette(clrs: List<ColorRGBa>): Palette {
        val background = clrs.first()
        val foreground = clrs
                .takeLast(clrs.size - 1)
                .map { getContrast(background, it) to it }
                .maxByOrNull { it.first }!!
                .second

        var constraint = clrs.size
        var constraint2 = clrs.size

        if (colorCountConstraint > 0 && colorCountConstraint < clrs.size) {
            constraint = colorCountConstraint
            constraint2 = colorCountConstraint + 1
        }

        val colors1 = clrs.slice(0 until constraint)
        val colors2 = clrs.slice(1 until constraint2)

        return Palette(background, foreground, colors1, colors2)
    }

    private fun createPalette(colors: List<ColorRGBa>): Palette {
        val sortedColors = when (sortBy) {
            SortBy.DARKEST -> colors.sortedBy {
                getLuminance(it)
            }
            SortBy.BRIGHTEST -> colors.sortedByDescending {
                getLuminance(it)
            }
            SortBy.NO_SORTING -> colors
        }

        return assemblePalette(sortedColors)
    }

    fun onChange(fn: () -> Unit) {
        onChangeListener = fn
    }

    @JvmName("addColorRGBaList")
    fun add(newPalette: List<ColorRGBa>) {
        palette = createPalette(newPalette)

        palettes.add(newPalette)
        paletteIndex = palettes.lastIndex
    }

    @JvmName("addHexList")
    fun add(hexColors: List<String>) {
        val newPalette = hexColors.map { ColorRGBa.fromHex(it) }
        palette = createPalette(newPalette)

        palettes.add(newPalette)
        paletteIndex = palettes.lastIndex
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

    val nextColor: ColorRGBa
    get() = colors[nextColorIndex]

    val nextColor2: ColorRGBa
    get() = colors2[nextColor2Index]

    fun randomize() {
        palette = assemblePalette(colors.shuffled())
    }

    fun randomPalette() {
        val comparison = palette.colors.toMutableList()
        @Suppress("UNCHECKED_CAST") val colors = Random.pick(palettes, comparison) as MutableList<ColorRGBa>

        paletteIndex = palettes.indexOf(colors)
        palette = createPalette(colors)
    }

    fun newCollection() {
        palettes.clear()
    }

    private fun registerKeybindings(keyboard: KeyEvents) {
        keyboard.keyDown.listen {
            if (!it.propagationCancelled) {
                if (it.name == "$randomPaletteKey") {
                    randomPalette()
                    onChangeListener()
                }
                if (it.name == "$randomizeKey") {
                    randomize()
                    onChangeListener()
                }
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
