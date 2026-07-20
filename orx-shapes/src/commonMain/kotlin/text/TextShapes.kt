package org.openrndr.extra.shapes.text

import org.openrndr.draw.font.Face
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import org.openrndr.shape.Shape

/**
 * Generates a list of shapes representing the given text with the specified font face, size, and position.
 *
 * @param face The font face used to render the text.
 * @param text The text content to be converted into shapes.
 * @param size The font size to be applied to the shapes.
 * @param position The starting position for rendering the text, defaulting to the origin vector.
 * @param scaler A function that scales the font face. By default, it uses `fontHeightScaler`.
 * @return A list of shapes representing the rendered text.
 */
fun shapesFromText(
    face: Face,
    text: String,
    position: Vector2 = Vector2.ZERO,
): List<Shape> {
    var cursor = position
    return text.windowed(2, 1, partialWindows = true) {

        if (it[0] == '\n') {
            cursor = Vector2(position.x, cursor.y + face.lineGap)
            Shape.EMPTY
        } else {

            val glyph = face.glyphForCharacter(it.first())
            val shape = glyph.shape().transform(buildTransform {
                translate(cursor)
            })
            if (it.length == 2) {
                cursor += Vector2(face.kernAdvance(it[0], it[1]), 0.0)
            }
            cursor += Vector2(glyph.advanceWidth(), 0.0)
            shape
        }
    }.filter { !it.empty }
}

