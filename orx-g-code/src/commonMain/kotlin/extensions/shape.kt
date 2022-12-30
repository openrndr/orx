package org.openrndr.extra.gcode.extensions

import org.openrndr.extra.gcode.Command
import org.openrndr.extra.gcode.Commands
import org.openrndr.extra.gcode.Generator
import org.openrndr.shape.Composition
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour

fun Composition.toCommands(generator: Generator, distanceTolerance: Double): Commands {

    val sequence = generator.comment("begin composition").toMutableList()

    this.findShapes().forEachIndexed { index, shapeNode ->
        sequence += generator.comment("begin shape: $index")
        shapeNode.shape.contours.forEach {
            sequence += it.toCommands(generator, distanceTolerance)
        }
        sequence += generator.comment("end shape: $index")
    }

    sequence += generator.comment("end composition")

    return sequence
}

fun ShapeContour.toCommands(generator: Generator, distanceTolerance: Double): Commands {

    val sequence = mutableListOf<Command>()

    val isDot =
        this.segments.size == 1 && this.segments.first().start.squaredDistanceTo(this.segments.first().end) < distanceTolerance
    if (isDot) {
        sequence += generator.moveTo(this.segments.first().start)
        sequence += generator.preDraw
        sequence += generator.comment("dot")
        sequence += generator.postDraw
        return sequence
    }

    this.segments.forEachIndexed { i, segment ->

        // On first segment, move to beginning of segment and tool down
        if (i == 0) {
            sequence += generator.moveTo(segment.start)
            sequence += generator.preDraw
        }

        // Draw segment
        sequence += segment.toCommands(generator, distanceTolerance)
    }

    // Close after last segment
    if (this.closed) {
        generator.drawTo(this.segments.first().start)
    }

    sequence += generator.postDraw

    return sequence.toList()
}

fun Segment.toCommands(generator: Generator, distanceTolerance: Double): Commands {
    return if (this.control.isEmpty()) {
        generator.drawTo(this.end)
    } else {
        this.adaptivePositions(distanceTolerance).flatMap {
            generator.drawTo(it)
        }
    }
}