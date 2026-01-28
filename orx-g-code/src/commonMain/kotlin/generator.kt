package org.openrndr.extra.gcode

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.extra.composition.Composition
import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour

/**
 * Simple factory for [GeneratorContext] instances.
 */
fun interface Generator {
    fun createContext(): GeneratorContext
}

/**
 * Opens a new [GeneratorContext] and calls [block] with it.
 * The context is initialized with [GeneratorContext.beginFile] and finalized with [GeneratorContext.endFile].
 * @return all commands generated in the context.
 */
fun Generator.file(block: GeneratorContext.() -> Unit): Commands = with(createContext()) {
    beginFile()
    block()
    endFile()
    commands()
}

/**
 * Generates g-code commands for defined operations.
 */
interface GeneratorContext {

    /**
     * Distance tolerance used for [org.openrndr.shape.Segment2D.adaptivePositions] when approximating curves.
     */
    val distanceTolerance: Double

    /**
     * Minimum distance for two points to be considered distinct.
     * Consecutive points with a distance smaller than this are considered duplicates and skipped.
     */
    val minSquaredDistance: Double

    /**
     * @return all commands added in this context.
     */
    fun commands(): Commands

    /**
     * Called at the beginning of a new file.
     *
     * Should perform machine setup, such as homing, setting up units or positioning mode.
     */
    fun beginFile()

    /**
     * Called at the beginning of a new layer.
     *
     * @param layer name of the layer.
     * @param composition composition of the layer.
     *
     * Implementation is optional.
     */
    fun beginLayer(layer: String, composition: Composition) = Unit

    /**
     * Called at the beginning of a new shape.
     *
     * Implementation is optional.
     */
    fun beginShape(shape: Shape) = Unit

    /**
     * Called at the beginning of a new contour.
     *
     * Expected to **move to the first point** of the contour and **enable the drawing tool**,
     * for example, pen down, laser on, etc.
     *
     * @param start start position of the contour.
     * @param contour contour that is about to be drawn.
     */
    fun beginContour(start: Vector2, contour: ShapeContour)

    /**
     * Drawing move from the current to the given position.
     * Called for each point of a contour, excluding the start point.
     *
     * - For linear segments: It is called with the end of each segment.
     * - For bezier segments: Called for all points of the curve, approximated with adaptivepositions using [distanceTolerance].
     */
    fun drawTo(p: Vector2)

    /**
     * Called at the end of a contour.
     * Expected to disable the drawing tool, for example, lift pen, turn laser off, etc.
     */
    fun endContour(contour: ShapeContour)

    /**
     * Called at the end of a shape.
     *
     * Implementation is optional.
     */
    fun endShape(shape: Shape) = Unit

    /**
     * Called at the end of a composition.
     *
     * @param layer name of the layer.
     * @param composition composition of the layer.
     *
     * Implementation is optional.
     */
    fun endLayer(layer: String, composition: Composition) = Unit

    /**
     * Called at the end of a file.
     *
     * Should perform final operations, such as returning to the home position, turning off the laser, etc.
     */
    fun endFile()
}

/**
 * Base implementation of [GeneratorContext] that collects commands in a list.
 */
abstract class BaseGeneratorContext(
    val deduplicateCommands: Boolean = true,
) : GeneratorContext {
    private val commands = mutableListOf<Command>()

    fun add(command: Command) {
        commands.add(command)
    }

    fun add(commands: Commands) {
        this.commands.addAll(commands)
    }

    fun add(vararg commands: Command) {
        this.commands.addAll(commands)
    }

    override fun commands(): Commands = if (deduplicateCommands) {
        commands.withoutDuplicates()
    } else {
        commands
    }
}

/**
 * NOOP GCode Generator used as default.
 */
fun noopGenerator() = Generator {
    object : GeneratorContext {

        val logger = KotlinLogging.logger {}

        override val distanceTolerance: Double = 0.0
        override val minSquaredDistance: Double = 0.0
        override fun commands(): Commands = emptyList()
        override fun beginFile() {
            logger.warn { "Using NOOP generator. No g-code will be generated." }
        }
        override fun beginContour(start: Vector2, contour: ShapeContour) {}

        override fun drawTo(p: Vector2) {}

        override fun endContour(contour: ShapeContour) {}

        override fun endFile() = Unit
    }
}