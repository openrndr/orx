package org.openrndr.extra.svg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.math.*
import org.openrndr.shape.*

private val logger = KotlinLogging.logger {}

/** a command of SVG path data, its operator and its numbers */
internal class Command(val op: String, vararg val operands: Double) {
    fun asVectorList(): List<Vector2>? {
        return if (operands.size % 2 == 0) {
            operands.asList().chunked(2) { Vector2(it[0], it[1]) }
        } else {
            null
        }
    }
}

// For evaluating elliptical arc arguments according to the SVG spec
internal fun Double.toBoolean(): Boolean? = when (this) {
    0.0 -> false
    1.0 -> true
    else -> null
}

/**
 * Creates a [Shape] from SVG path data, the value of the d attribute of a path element, like
 * `svgShape("M 10 10 h 80 v 80 h -80 z")`. All commands are supported, absolute and relative: M, L, H, V, C, S, Q, T,
 * A and Z. Every subpath becomes a contour, closed when it ends with Z. Path data that is not valid is read up to
 * the first error, as SVG does.
 *
 * @param svgPath the path data
 */
fun svgShape(svgPath: String): Shape {
    if (svgPath.isBlank() || svgPath.trim() == "none") return Shape.EMPTY
    return SVGPathParser.shape(SVGPathParser.commands(svgPath))
}

/**
 * The parser of SVG path data: the commands of path data, and the shape they describe
 */
internal object SVGPathParser {
    private val numbers = Regex("[-+]?[0-9]*[.]?[0-9]+(?:[eE][-+]?[0-9]+)?")

    /**
     * The commands of the path data [pathValue]. The flags of arcs can be written without separators, as minified
     * paths do: "a5 5 0 105 5" has the flags 1 and 0.
     */
    fun commands(pathValue: String): List<Command> {
        val rawCommands = pathValue.split("(?=[MmZzLlHhVvCcSsQqTtAa])".toRegex()).map(String::trim)
        val commands = mutableListOf<Command>()

        for (rawCommand in rawCommands) {
            if (rawCommand.isNotEmpty()) {
                val operands = mutableListOf<Double>()
                val isArc = rawCommand[0] == 'a' || rawCommand[0] == 'A'
                var position = 1
                while (position < rawCommand.length) {
                    // the fourth and fifth argument of every arc are flags of a single digit
                    if (isArc && operands.size % 7 in 3..4) {
                        val c = rawCommand[position]
                        if (c == '0' || c == '1') {
                            operands.add(if (c == '1') 1.0 else 0.0)
                            position++
                            continue
                        }
                        if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r') {
                            position++
                            continue
                        }
                        // anything else where a flag is expected is an error
                        break
                    }
                    val match = numbers.find(rawCommand, position) ?: break
                    operands.add(match.value.toDouble())
                    position = match.range.last + 1
                }
                commands += Command(rawCommand[0].toString(), *(operands.toDoubleArray()))
            }
        }

        return commands
    }

    /** [commands] split into subpaths, every subpath starts with a moveto */
    private fun compounds(commands: List<Command>): List<List<Command>> {
        val starts = commands.indices.filter { commands[it].op == "M" || commands[it].op == "m" }
        return starts.mapIndexed { index, start ->
            commands.subList(start, if (index + 1 < starts.size) starts[index + 1] else commands.size)
        }
    }

        fun shape(commands: List<Command>): Shape {
            var cursor = Vector2.ZERO
            var anchor = Vector2.ZERO
            // Still problematic
            var prevCubicCtrlPoint: Vector2? = null
            var prevQuadCtrlPoint: Vector2? = null

            val contours = compounds(commands).map { compoundCommands ->
                val segments = mutableListOf<Segment2D>()
                var closed = false
                // If an argument is invalid, an error is logged,
                // further interpreting is stopped and compound is returned as-is.
                compoundCommands.forEach { command ->

                    if (command.op !in listOf("z", "Z") && command.operands.isEmpty()) {
                        logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                        return@forEach
                    }

                    val points = command.asVectorList()

                    // TODO: Rethink this check
                    if (points == null && command.op.lowercase() !in listOf("a", "h", "v")) {
                        logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                        return@forEach
                    }

                    // smooth curves only reflect the control point of a preceding curve of the same kind
                    if (command.op.lowercase() !in listOf("c", "s")) prevCubicCtrlPoint = null
                    if (command.op.lowercase() !in listOf("q", "t")) prevQuadCtrlPoint = null

                    when (command.op) {
                        "A", "a" -> {
                            // If size == step, only the last window can be partial
                            // Special case as it also has boolean values
                            val contours = command.operands.toList().windowed(7, 7, true).map m@{
                                if (it.size != 7) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val rx = it[0]
                                    val ry = it[1]
                                    val xAxisRot = it[2]
                                    val largeArcFlag = it[3].toBoolean()
                                    val sweepFlag = it[4].toBoolean()

                                    if (largeArcFlag == null || sweepFlag == null || rx == 0.0 || ry == 0.0) {
                                        logger.error { "Invalid values provided for: ${command.op}" }
                                        return@forEach
                                    }

                                    val end = Vector2(it[5], it[6]).let { v ->
                                        if (command.op == "a") {
                                            v + cursor
                                        } else {
                                            v
                                        }
                                    }

                                    contour {
                                        moveTo(cursor)
                                        arcTo(rx, ry, xAxisRot, largeArcFlag, sweepFlag, end)
                                        cursor = end
                                    }
                                }
                            }

                            // I don't know why we can't just have segments from the above map,
                            // but this is the only way this works.
                            segments += contours.flatMap { it.segments }
                        }

                        "M" -> {
                            // TODO: Log an error when this nulls
                            cursor = points!!.firstOrNull() ?: return@forEach
                            anchor = cursor

                            // Following points are implicit lineto arguments
                            segments += points.drop(1).map {
                                Segment2D(cursor, it).apply {
                                    cursor = it
                                }
                            }
                        }

                        "m" -> {
                            // TODO: Log an error when this nulls
                            cursor += points!!.firstOrNull() ?: return@forEach
                            anchor = cursor

                            // Following points are implicit lineto arguments
                            segments += points.drop(1).map {
                                Segment2D(cursor, cursor + it).apply {
                                    cursor += it
                                }
                            }
                        }

                        "L" -> {
                            segments += points!!.map {
                                Segment2D(cursor, it).apply {
                                    cursor = it
                                }
                            }
                        }

                        "l" -> {
                            segments += points!!.map {
                                Segment2D(cursor, cursor + it).apply {
                                    cursor += it
                                }
                            }
                        }

                        "H" -> {
                            segments += command.operands.map {
                                val target = Vector2(it, cursor.y)
                                Segment2D(cursor, target).apply {
                                    cursor = target
                                }
                            }
                        }

                        "h" -> {
                            segments += command.operands.map {
                                val target = cursor + Vector2(it, 0.0)
                                Segment2D(cursor, target).apply {
                                    cursor = target
                                }
                            }
                        }

                        "V" -> {
                            segments += command.operands.map {
                                val target = Vector2(cursor.x, it)
                                Segment2D(cursor, target).apply {
                                    cursor = target
                                }
                            }
                        }

                        "v" -> {
                            segments += command.operands.map {
                                val target = cursor + Vector2(0.0, it)
                                Segment2D(cursor, target).apply {
                                    cursor = target
                                }
                            }
                        }

                        "C" -> {
                            segments += points!!.windowed(3, 3, true).map {
                                if (it.size != 3) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val (cp1, cp2, target) = it
                                    Segment2D(cursor, cp1, cp2, target).also {
                                        cursor = target
                                        prevCubicCtrlPoint = cp2
                                    }
                                }
                            }
                        }

                        "c" -> {
                            segments += points!!.windowed(3, 3, true).map {
                                if (it.size != 3) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val (cp1, cp2, target) = it.map { v -> cursor + v }
                                    Segment2D(cursor, cp1, cp2, target).apply {
                                        cursor = target
                                        prevCubicCtrlPoint = cp2
                                    }
                                }
                            }
                        }

                        "S" -> {
                            segments += points!!.windowed(2, 2, true).map {
                                if (it.size != 2) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val cp1 = 2.0 * cursor - (prevCubicCtrlPoint ?: cursor)
                                    val (cp2, target) = it
                                    Segment2D(cursor, cp1, cp2, target).also {
                                        cursor = target
                                        prevCubicCtrlPoint = cp2
                                    }
                                }
                            }
                        }

                        "s" -> {
                            segments += points!!.windowed(2, 2, true).map {
                                if (it.size != 2) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val cp1 = 2.0 * cursor - (prevCubicCtrlPoint ?: cursor)
                                    val (cp2, target) = it.map { v -> cursor + v }
                                    Segment2D(cursor, cp1, cp2, target).also {
                                        cursor = target
                                        prevCubicCtrlPoint = cp2
                                    }
                                }
                            }
                        }

                        "Q" -> {
                            segments += points!!.windowed(2, 2, true).map {
                                if (it.size != 2) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val (cp, target) = it
                                    Segment2D(cursor, cp, target).also {
                                        cursor = target
                                        prevQuadCtrlPoint = cp
                                    }
                                }
                            }
                        }

                        "q" -> {
                            segments += points!!.windowed(2, 2, true).map {
                                if (it.size != 2) {
                                    logger.error { "Invalid amount of arguments provided for: ${command.op}" }
                                    return@forEach
                                } else {
                                    val (cp, target) = it.map { v -> cursor + v }
                                    Segment2D(cursor, cp, target).also {
                                        cursor = target
                                        prevQuadCtrlPoint = cp
                                    }
                                }
                            }
                        }

                        "T" -> {
                            points!!.forEach {
                                val cp = 2.0 * cursor - (prevQuadCtrlPoint ?: cursor)
                                segments += Segment2D(cursor, cp, it)
                                cursor = it
                                prevQuadCtrlPoint = cp
                            }
                        }

                        "t" -> {
                            points!!.forEach {
                                val cp = 2.0 * cursor - (prevQuadCtrlPoint ?: cursor)
                                val target = cursor + it
                                segments += Segment2D(cursor, cp, target)
                                cursor = target
                                prevQuadCtrlPoint = cp
                            }
                        }

                        "Z", "z" -> {
                            if ((cursor - anchor).length >= 0.001) {
                                segments += Segment2D(cursor, anchor)
                            }
                            cursor = anchor
                            closed = true
                        }

                        else -> {
                            // The spec declares we should still attempt to render
                            // the path up until the erroneous command as to visually
                            // signal the user where the error occurred.
                            logger.error { "Invalid path operator: ${command.op}" }
                            return@forEach
                        }
                    }
                }
                ShapeContour(segments, closed, YPolarity.CW_NEGATIVE_Y)
            }
            return Shape(contours)
        }
}
