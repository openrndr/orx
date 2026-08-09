package org.openrndr.extra.textoncontour

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.slug.SlugCommand
import org.openrndr.draw.slug.SlugDrawer
import org.openrndr.draw.slug.SlugGlyphMap
import org.openrndr.draw.slug.StrokeMode
import org.openrndr.draw.slug.TextSpan
import org.openrndr.draw.slug.TextStyle
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.transform

fun RectifiedContour.positionOnlyPose(t: Double): Matrix44 {
    return Matrix44.fromColumnVectors(Vector4.UNIT_X * -1.0, Vector4.UNIT_Y * -1.0, Vector4.UNIT_Z, position(t).xy01)
}

fun slugTextOnContour(
    drawer: Drawer,
    slugDrawer: SlugDrawer,
    slugGlyphMap: SlugGlyphMap,
    baseStyle: TextStyle,
    spans: List<TextSpan>,
    contour: RectifiedContour,
    startT: Double = 0.0
) {
    val fixOrientation = Matrix44.fromColumnVectors(
        Vector4(-1.0, 0.0, 0.0, 0.0),
        Vector4(0.0, -1.0, 0.0, 0.0),
        Vector4.UNIT_Z,
        Vector4.UNIT_W
    )

    val shaper = TextShapingDriver.instance

    var textWidth = 0.0

    class WordInfo(val word: String, val width: Double, val shaped: List<ShapeResult>, val style: TextStyle)

    val items = mutableListOf<WordInfo>()


    for (span in spans) {
        val spanStyle = baseStyle.cascade(span.style)
        val face = spanStyle.face ?: error("face not set in span")

        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (ch in span.text) {
            when (ch) {
                ' ', '\t' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add(" ")
                }

                '\n' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add("\n")
                }

                else -> {
                    currentWord.append(ch)
                }
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        val spaceGlyph = face.glyphForCharacter(' ')
        val spaceWidth = spaceGlyph.advanceWidth() * spanStyle.textWidthFactor!! * spanStyle.sizeInEm!!

        for (word in words) {
            when (word) {
                " ", "\t" -> {
                    items.add(WordInfo(" ", spaceWidth, emptyList(), spanStyle))
                    textWidth += spaceWidth
                }

                else -> {
                    val features = spanStyle.features

                    val shaped = shaper.shape(face, word, features)
                    var wordWidth = 0.0
                    for (sr in shaped) {
                        wordWidth += sr.advance.x * spanStyle.textWidthFactor!! * spanStyle.sizeInEm!!
                    }

                    items.add(WordInfo(word, wordWidth, shaped, spanStyle))
                    textWidth += wordWidth
                }
            }
        }
        var cursorT = startT

        val dt = 1.0 / contour.contour.length

        val commands = mutableListOf<SlugCommand>()
        for (item in items) {
            if (item.word == " ") {
                cursorT += item.width * dt
            } else {

                val itemTransform = transform {
                    scale(item.style.sizeInEm!! * item.style.textWidthFactor!!, item.style.sizeInEm!!)
                }

                val hscale = item.style.sizeInEm!! * item.style.textWidthFactor!!
                for (shape in item.shaped) {
                    val slugIndex = slugGlyphMap.getSlugForGlyphIndex(item.style.face!!, shape.glyphIndex)

                    val t =
                        if (contour.contour.closed) (cursorT + shape.offset.x * hscale * dt).mod(1.0) else cursorT + shape.offset.x * hscale * dt

                    val pose =
                        contour.pose(t) * itemTransform * fixOrientation

                    val cmd = SlugCommand(
                        slugIndex,
                        pose,
                        item.style.fill ?: ColorRGBa.TRANSPARENT,
                        item.style.stroke ?: ColorRGBa.TRANSPARENT,
                        strokeWeight = item.style.strokeWeight ?: 0.0,
                        (item.style.strokeMode ?: StrokeMode.CENTER).mode
                    )
                    commands.add(cmd)
                    cursorT += shape.advance.x * hscale * dt
                }
            }
        }
        slugDrawer.prepare(slugGlyphMap.slugMap, commands)
        slugDrawer.draw(drawer, slugGlyphMap.slugMap)
    }
}