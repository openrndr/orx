package org.openrndr.extra.shapes.text

import org.openrndr.draw.font.Face
import org.openrndr.draw.font.fontHeightScaler
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Shape

fun shapesFromText(
    face: Face,
    text: String,
    size: Double,
    position: Vector2 = Vector2.ZERO,
    scaler: (Face) -> Double = ::fontHeightScaler
): List<Shape> {
    var cursor = position
    val scale = size * scaler(face)
    return text.windowed(2, 1, partialWindows = true) {

        if (it[0] == '\n') {
            cursor = Vector2(position.x, cursor.y + face.lineSpace(scale))
            Shape.EMPTY
        } else {

            val glyph = face.glyphForCharacter(it.first())
            val shape = glyph.shape(scale).transform(buildTransform {
                translate(cursor)
            })
            if (it.length == 2) {
                cursor += Vector2(face.kernAdvance(scale, it[0], it[1]), 0.0)
            }
            cursor += Vector2(glyph.advanceWidth(scale), 0.0)
            shape
        }
    }.filter { !it.empty }
}

