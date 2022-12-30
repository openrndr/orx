package org.openrndr.extra.gcode

import org.openrndr.math.Vector2
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

typealias Command = String
typealias Commands = List<Command>

fun Command.asCommands(): Commands = listOf(this)

fun Commands.toGcode(): String = StringBuilder()
    .also { sb -> this.forEach { sb.appendLine(it) } }
    .toString()

/**
 * Consecutive identical commands are removed, ignoring comments.
 * Comments are lines starting with ";" or "(".
 */
fun Commands.withoutDuplicates(): Commands {
    var lastCommand = 0
    return this.filterIndexed { i, c ->
        when (i) {
            0 -> true
            else -> {
                if (c.startsWith(';') || c.startsWith('(')) {
                    true
                } else {
                    !c.contentEquals(this[lastCommand]).also { lastCommand = i }
                }
            }
        }
    }
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

/**
 * Generates g-code for defined operations.
 * All operations are empty command lists on default and have to be defined explicitly.
 */
data class Generator(

    /**
     * Setup code at the beginning of a file.
     */
    val setup: Commands = emptyList(),

    /**
     * A move operation to the given location.
     */
    val moveTo: (Vector2) -> Commands = { emptyList() },

    /**
     * Start drawing sequence. Pen down, laser on etc.
     */
    val preDraw: Commands = emptyList(),

    /**
     * A draw operation to the given location.
     */
    val drawTo: (Vector2) -> Commands = { emptyList() },

    /**
     * End draw sequence. Lift pen, turn laser off.
     * Called after a draw action before a move is performed.
     */
    val postDraw: Commands = emptyList(),

    /**
     * End of file sequence.
     */
    val end: Commands = emptyList(),

    /**
     * Insert a comment.
     */
    val comment: (String) -> Commands = { _ -> emptyList() }
)

/**
 * Creates a [Generator] to be used by grbl controlled pen plotters.
 * [drawRate] sets the feed rate used for drawing operations.
 * Moves are performed with G0. When [moveRate] is set, moves are instead
 * done with G1 and the given rate as feedRate.
 * Can be customized by overwriting individual fields with *.copy*.
 */
fun basicGrblSetup(
    drawRate: Double = 500.0,
    moveRate: Double? = null,
) = Generator(
    setup = listOf(
        "G21", // mm
        "G90", // Absolute positioning
    ),
    moveTo = when (moveRate) {
        null -> { p -> "G0 X${p.x.roundedTo()} Y${p.y.roundedTo()}".asCommands() }
        else -> { p -> "G1 X${p.x.roundedTo()} Y${p.y.roundedTo()} F$drawRate".asCommands() }
    },
    preDraw = listOf("M3 S255"),
    drawTo = { p -> "G1 X${p.x.roundedTo()} Y${p.y.roundedTo()} F$drawRate".asCommands() },
    postDraw = listOf("M3 S0"),
    end = listOf(
        "G0 X0 Y0",
        "G90",
    ),
    comment = { c -> listOf(";$c") }
)