package org.openrndr.extra.gcode

/**
 * A single G-code command.
 */
typealias Command = String

/**
 * A list of G-code commands.
 */
typealias Commands = List<Command>

fun Command.asCommands(): Commands = listOf(this)

/**
 * Converts a list of commands to a single string.
 * Newlines separate the commands.
 */
fun Commands.toGcode(): String = StringBuilder()
    .also { sb -> forEach { sb.appendLine(it) } }
    .toString()

/**
 * Consecutive identical commands are removed, ignoring comments.
 * Comments are lines starting with ";" or "(".
 */
fun Commands.withoutDuplicates(): Commands {
    var lastCommand = 0
    return filterIndexed { i, c ->
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