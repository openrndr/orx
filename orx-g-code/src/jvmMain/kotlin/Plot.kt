package org.openrndr.extra.gcode

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.Extension
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.composition.Composition
import org.openrndr.extra.composition.CompositionDrawer
import org.openrndr.extra.composition.composition
import org.openrndr.extra.composition.drawComposition
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min


enum class RenderMode {
    BEFORE,
    AFTER,
    MANUAL,
}

enum class LayerMode {
    SINGLE_FILE, MULTI_FILE,
}

enum class Origin {
    BOTTOM_LEFT, TOP_LEFT, CENTER
}

typealias DrawFunction = CompositionDrawer.() -> Unit

private val logger = KotlinLogging.logger {}

/**
 * Configuration:
 * When [manualRedraw] is true, the programs presentation mode is set to Manual on startup.
 * "r" to trigger redraw.
 * When [renderMode] is set to manual, the plot will not be rendered to the programms drawer.
 * Then [render] has to be called to draw the plot. [origin]
 */
class Plot(
    // Document
    dimensions: Vector2, // Document size in mm
    var name: String? = null,
    val origin: Origin = Origin.BOTTOM_LEFT,

    // G-code
    var generator: Generator = noopGenerator(),
    var distanceTolerance: Double = 0.5,
    var layerMode: LayerMode = LayerMode.SINGLE_FILE,

    // Rendering Properties
    var defaultDrawColor: ColorRGBa = ColorRGBa.BLACK,
    var defaultPenWeight: Double = 1.0, // In mm
    var backgroundColor: ColorRGBa = ColorRGBa.WHITE,
    val manualRedraw: Boolean = true,
    var renderMode: RenderMode = RenderMode.AFTER,

    // Key Binds. Set null to disable
    val gCodeBind: String? = "g",
    val redrawBind: String? = "r",

    /**
     * The folder where the g-code will be saved to. Default value is "gcode", saves in current working
     * directory when set to null.
     */
    var folder: String? = "gcode",

    ) : Extension {

    override var enabled: Boolean = true

    val docBounds = when (origin) {
        Origin.CENTER -> Rectangle(-dimensions.times(.5), dimensions.x, dimensions.y)
        Origin.BOTTOM_LEFT,
        Origin.TOP_LEFT,
            -> Rectangle(0.0, 0.0, dimensions.x, dimensions.y)
    }

    val layers: MutableMap<String, Composition> = mutableMapOf()
    private var order: List<String> = listOf()

    private var scale = 1.0

    private var program: Program? = null


    override fun setup(program: Program) {
        this.program = program
        // Scale to fit in viewport
        scale = min(program.width / docBounds.width, program.height / docBounds.height)

        if (name == null) {
            name = program.name
        }

        if (!enabled) {
            return
        }

        if (manualRedraw) {
            program.window.presentationMode = PresentationMode.MANUAL
            program.window.requestDraw()
        }

        program.keyboard.keyUp.listen { event ->

            if (redrawBind != null && event.name == redrawBind) {
                program.window.requestDraw()
            }

            if (gCodeBind != null && event.name == gCodeBind) {
                writeGcode()
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (enabled && renderMode == RenderMode.BEFORE) {
            render(drawer)
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (enabled && renderMode == RenderMode.AFTER) {
            render(drawer)
        }
    }

    /**
     * Draws to the "default" layer. See [layer].
     */
    fun draw(drawFunction: DrawFunction) = layer("default", drawFunction)

    /**
     * Draws to the given layer.
     * If the layer with given name already exists, it is replaced.
     */
    fun layer(name: String, drawFunction: DrawFunction) {

        val composition = layers[name]

        composition?.clear()

        // Append new layers to order
        order = order.filter { it != name } + name

        layers[name] = drawComposition(composition = composition) {
            this.composition.clear()
            // Set default
            fill = ColorRGBa.TRANSPARENT
            strokeWeight = defaultPenWeight
            stroke = defaultDrawColor
            drawFunction(this)
        }
    }

    /**
     * Executes block once for each layer in the order they were added.
     */
    fun forEachLayer(block: (layer: String, composition: Composition) -> Unit) = order.forEach { name ->
        layers[name]?.also { layer ->
            block(name, layer)
        }
    }

    /**
     * Renders this plot to the given drawer.
     */
    fun render(drawer: Drawer) = scaled(drawer) { scaled ->

        // Draw the background surface
        drawer.isolated {
            strokeWeight = 0.0
            fill = backgroundColor
            rectangle(docBounds)
        }

        // Draw layers
        forEachLayer { _, composition ->
            scaled.composition(composition)
        }
    }


    /**
     * Drawer scaled to document space, to fit to the window.
     */
    fun scaled(drawer: Drawer, drawFunction: (Drawer) -> Unit) = drawer.isolated {
        when (origin) {
            Origin.BOTTOM_LEFT -> {
                // Scale to fit screen and flip y-axis
                translate(0.0, scaled(docBounds.height))
                scale(scale, -scale)
            }

            Origin.TOP_LEFT -> {
                // Scale to fit screen
                scale(scale, scale)
            }

            Origin.CENTER -> {
                translate(.5 * scaled(docBounds.width), .5 * scaled(docBounds.height))
                scale(scale, -scale)
            }
        }
        drawFunction(this)
    }

    /**
     * Scales and translates the given position from screen space to document space.
     * Can be used to translate mouse events to draw to the plot.
     */
    fun toDocumentSpace(p: Vector2): Vector2 {
        val s = 1.0 / scale
        return when (origin) {
            Origin.BOTTOM_LEFT -> Vector2(p.x * s, docBounds.height - p.y * s)
            Origin.TOP_LEFT -> p.times(s)
            Origin.CENTER -> p.times(Vector2(s, -s)).plus(Vector2(-docBounds.width, docBounds.height) * .5)
        }
    }

    /**
     * Double [v] scaled from document space to screen space.
     */
    fun scaled(v: Double) = v * scale

    /**
     * Vector [v] scaled from document space to screen space.
     */
    fun scaled(v: Vector2) = v * scale

    /**
     * Scale from document space to screen space.
     */
    fun scale() = scale

    /**
     * Writes the gcode to file(s) based on
     * [layerMode], [name] and [folder].
     *
     */
    fun writeGcode() = when (layerMode) {
        LayerMode.SINGLE_FILE -> {
            writeAllLayersToSingleFile()
        }

        LayerMode.MULTI_FILE -> {
            writeEachLayerToFile()
        }
    }

    /**
     * Combines the gcode of all layers.
     */
    fun toCombinedGcode() = generator.file {
        forEachLayer { layer, composition ->
            render(layer, composition)
        }
    }.toGcode()

    /**
     * Writes all layers to a single file.
     */
    fun writeAllLayersToSingleFile() = writeFile(name ?: "plot") {
        toCombinedGcode()
    }

    /**
     * Returns a map of layer names to gcode.
     */
    fun toSplitGcode() = mutableMapOf<String, String>().apply {
        forEachLayer { layer, composition ->
            val gcode = generator.file { render(layer, composition) }.toGcode()
            put(layer, gcode)
        }
    }

    /**
     * Writes each layer to a separate file.
     */
    fun writeEachLayerToFile() =
        toSplitGcode().forEach {
            (layerName, gcode) -> writeFile("${name ?: ""}-${layerName}") { gcode }
        }

    /**
     * Writes [content] to file "[folder]/timestamp-[name].[extension]"
     */
    fun writeFile(name: String, extension: String = "gcode", content: () -> String) {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmSS")
        val timestamp = formatter.format(LocalDateTime.now())
        val fileName = "$timestamp-$name.$extension"

        File(File(folder ?: "."), fileName)
            .also { logger.info { "💾Writing g-code $name to ${it.path}" } }
            .writeText(content())
    }
}