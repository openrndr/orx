package org.openrndr.extra.gcode

import org.openrndr.extra.composition.Composition
import org.openrndr.math.Vector2
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

class GrblGeneratorContext(
    val drawRate: Double?,
    val moveRate: Double?,
    val setup: Commands,
    val preDraw: Commands,
    val postDraw: Commands,
    val comment: (String) -> Commands,
    val end: Commands,
    override val distanceTolerance: Double,
    override val minSquaredDistance: Double,
    deduplicateCommands: Boolean = true,
) : BaseGeneratorContext(deduplicateCommands) {

    override fun beginFile() = add(setup)

    override fun beginLayer(layer: String, composition: Composition) {
        addComment("begin layer: $layer")
    }

    override fun beginShape(shape: Shape) {
        addComment("begin shape")
    }

    override fun beginContour(start: Vector2, contour: ShapeContour) {
        moveTo(start)
        add(preDraw)
    }

    fun moveTo(p: Vector2) = if (moveRate == null) {
        add("G0 X${p.x.roundedTo()} Y${p.y.roundedTo()}")
    } else {
        add("G0 X${p.x.roundedTo()} Y${p.y.roundedTo()} F$moveRate")
    }
    
    override fun endFile() = add(end)

    override fun drawTo(p: Vector2) = if (drawRate == null) {
        add("G1 X${p.x.roundedTo()} Y${p.y.roundedTo()}")
    } else {
        add("G1 X${p.x.roundedTo()} Y${p.y.roundedTo()} F$drawRate")
    }

    override fun endContour(contour: ShapeContour) {
        add(postDraw)
    }

    override fun endShape(shape: Shape) {
        addComment("end shape")
    }

    override fun endLayer(layer: String, composition: Composition) {
        addComment("end layer: $layer")
    }
    
    fun addComment(comment: String) = add(comment(comment))
}

data class BasicGrblGenerator(

    val drawRate: Double? = 500.0,

    val moveRate: Double? = null,

    /**
     * Setup code at the beginning of a file.
     */
    val setup: Commands = listOf(
        "G21", // mm
        "G90", // Absolute positioning
    ),
    /**
     * Commands to be executed before drawing each contour.
     * Enables the drawing tool.
     */
    val preDraw: Commands = listOf("M3 S255"),

    /**
     * Commands to be executed after drawing each contour.
     * Disables the drawing tool.
     */
    val postDraw: Commands = listOf("M3 S0"),

    /**
     * Function to convert a comment string to gcode commands.
     */
    val comment: (String) -> Commands = { listOf(";$it") },
    
    /**
     * Commands to be executed at the end of a file.
     */
    val end: Commands = listOf(
        "G0 X0 Y0",
        "G90",
    ),
    val deduplicateCommands: Boolean = true,
    val distanceTolerance: Double = 0.5,
    val minSquaredDistance: Double = 0.5,
) : Generator {
    override fun createContext(): GeneratorContext = GrblGeneratorContext(
        drawRate,
        moveRate,
        setup,
        preDraw,
        postDraw,
        comment,
        end,
        distanceTolerance,
        minSquaredDistance,
        deduplicateCommands
    )
}

/**
 * Double to String rounded to absolute value of [decimals].
 * Helper to be used in generator functions.
 */
fun Double.roundedTo(decimals: Int = 3): String {
    val f = 10.0.pow(decimals.absoluteValue)
    return when {
        decimals != 0 -> "${this.times(f).roundToInt().div(f)}"
        else -> "${this.roundToInt()}"
    }
}