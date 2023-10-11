package org.openrndr.extra.shapes.text

import org.openrndr.draw.font.Face
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Shape

fun shapesFromText(face: Face, text: String, size: Double, position: Vector2 = Vector2.ZERO): List<Shape> {
    var cursor = position
    return text.windowed(2, 1, partialWindows = true) {

        if (it[0] == '\n') {
            cursor = Vector2(position.x, cursor.y + face.lineSpace(size))
            Shape.EMPTY
        } else {

            val glyph = face.glyphForCharacter(it.first())
            val shape = glyph.shape(size).transform(buildTransform {
                translate(cursor)
            })
            if (it.length == 2) {
                cursor += Vector2(face.kernAdvance(size, it[0], it[1]), 0.0)
            }
            cursor += Vector2(glyph.advanceWidth(size), 0.0)
            shape
        }
    }.filter { !it.empty }
}

