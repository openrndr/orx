package org.openrndr.extra.gcode

import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.gcode.extensions.toCommands
import org.openrndr.math.Vector2
import org.openrndr.shape.Composition
import org.openrndr.shape.CompositionDrawer
import org.openrndr.shape.Rectangle
import org.openrndr.shape.drawComposition
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

/**
 * Configuration:
 * When [manualRedraw] is true, the programs presentation mode is set to Manual on startup.
 * "r" to trigger redraw.
 */
class Plot(
    // Document
    dimensions: Vector2, // Document size in mm
    var origin: Origin = Origin.BOTTOM_LEFT,

    // G-code
    var generator: Generator = Generator(),
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
    var folder: String? = "gcode"

) : Extension {
    companion object {
        val logger = KotlinLogging.logger {}
    }

    override var enabled: Boolean = true

    val docBounds = Rectangle(0.0, 0.0, dimensions.x, dimensions.y)

    val layers: MutableMap<String, Composition> = mutableMapOf()
    private var order: List<String> = listOf()

    private var scale = 1.0

    private var program: Program? = null

    override fun setup(program: Program) {
        this.program = program

        // Scale to fit in viewport
        scale = min(program.width / docBounds.width, program.height / docBounds.height)

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
                when (layerMode) {
                    LayerMode.SINGLE_FILE -> {
                        writeFile("plot", toCombinedGcode())
                    }

                    LayerMode.MULTI_FILE -> {
                        toSplitGcode().forEach {
                            writeFile(it.key, it.value)
                        }
                    }
                }
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
            strokeWeight = scaled(defaultPenWeight)
            stroke = defaultDrawColor
            drawFunction(this)
        }
    }

    /**
     * Renders this plot to the given drawer.
     */
    fun render(drawer: Drawer) = scaled(drawer) { scaled ->

        // Draw background surface
        drawer.isolated {
            strokeWeight = 0.0
            fill = backgroundColor
            when (origin) {
                Origin.CENTER -> rectangle(docBounds.dimensions * -.5, docBounds.width, docBounds.height)
                else -> rectangle(0.0, 0.0, docBounds.width, docBounds.height)
            }
        }

        // Layers
        order.mapNotNull { layers[it] }
            .forEach { scaled.composition(it) }
    }

    /**
     * Drawer scaled to document space.
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
     * Double [v] scaled from screen space to document space.
     */
    fun scaled(v: Double) = v.times(scale)

    /**
     * Converts all layers to a single g-code string in the order they were added.
     */
    fun toCombinedGcode(): String = order
        .mapNotNull { l -> layers[l] }
        .toCommands(generator, distanceTolerance).withoutDuplicates()
        .toGcode()

    /**
     * Converts each layer to a g-code string.
     */
    fun toSplitGcode(): Map<String, String> = layers.mapValues { e ->
        val commands = e.value.toCommands(generator, distanceTolerance).withoutDuplicates()
        (generator.setup + commands + generator.end).toGcode()
    }

    /**
     * Writes [content] to file "[folder]/timestamp-[name].[extension]"
     */
    fun writeFile(name: String, content: String, extension: String = "gcode") {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm-SS")
        val timestamp = formatter.format(LocalDateTime.now())
        val fileName = "$timestamp-$name.$extension"

        File(File(folder ?: "."), fileName)
            .also { logger.info { "💾Writing g-code $name to ${it.path}" } }
            .writeText(content)
    }
}