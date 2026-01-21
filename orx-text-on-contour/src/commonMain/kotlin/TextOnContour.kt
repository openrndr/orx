package org.openrndr.extra.textoncontour

import org.openrndr.draw.*
import org.openrndr.extra.shapes.rectify.RectifiedContour
import org.openrndr.internal.Driver
import org.openrndr.internal.GlyphRectangle
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.buildTransform
import kotlin.math.round

private fun Vector2.transform(m: Matrix44): Vector2 {
    return (m * this.xy01).xy
}

private class OnContourImageMapDrawer {
    var lastPos = Vector2.ZERO

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "font-image-map",
        vsGenerator = Driver.instance.shaderGenerators::fontImageMapVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::fontImageMapFragmentShader
    )

    private val maxQuads = 20_000

    private val vertices = VertexBuffer.createDynamic(VertexFormat().apply {
        textureCoordinate(2)
        attribute("bounds", VertexElementType.VECTOR4_FLOAT32)
        position(3)
        attribute("instance", VertexElementType.FLOAT32)
    }, 6 * maxQuads)

    private var quadCount = 0

    fun drawTextOnContour(
        contour: RectifiedContour,
        context: DrawContext,
        drawStyle: DrawStyle,
        text: String,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        tracking: Double = 0.0,
        scale: Double = 1.0
    ) = drawTextsOnContours(
        contour,
        context,
        drawStyle,
        listOf(text),
        listOf(Vector2(offsetX, offsetY)),
        tracking,
        scale
    )

    class SetResult(val cursorT: List<Double>, val glyphRectangles: List<List<GlyphRectangle>>)

    fun drawTextsOnContours(
        contour: RectifiedContour,
        context: DrawContext,
        drawStyle: DrawStyle,
        texts: List<String>,
        positions: List<Vector2>,
        tracking: Double = 0.0,
        scale: Double = 1.0
    ): SetResult {
        val fontMap = drawStyle.fontMap as? FontImageMap
        val cursorTs = mutableListOf<Double>()

        if (fontMap != null) {
            var instance = 0

            val textAndPositionPairs = texts.zip(positions)
            for ((text, position) in textAndPositionPairs) {
                var cursorX = position.x
                val cursorY = 0.0

                val bw = vertices.shadow.writer()
                bw.position = vertices.vertexFormat.size * quadCount * 6

                var lastChar: Char? = null
                text.forEach {
                    val lc = lastChar
                    if (drawStyle.kerning == KernMode.METRIC) {
                        cursorX += if (lc != null) fontMap.kerning(lc, it) else 0.0
                    }
                    val metrics = fontMap.glyphMetrics[it] ?: fontMap.glyphMetrics.getValue(' ')
                    val (dx, _) = insertCharacter(
                        contour,
                        fontMap,
                        bw,
                        it,
                        cursorX,
                        position.y + cursorY,
                        instance,
                        drawStyle.textSetting,
                        scale
                    )
                    cursorX += metrics.advanceWidth + dx + tracking
                    lastChar = it
                }
                cursorTs.add(cursorX)
                instance++
            }
            flush(context, drawStyle)
        }
        return SetResult(cursorTs, emptyList())
    }

    var queuedInstances = 0

    fun flush(context: DrawContext, drawStyle: DrawStyle) {
        if (quadCount > 0) {
            vertices.shadow.uploadElements(0, quadCount * 6)
            val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
            shader.begin()
            context.applyToShader(shader)

            Driver.instance.setState(drawStyle)
            drawStyle.applyToShader(shader)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(vertices),
                DrawPrimitive.TRIANGLES,
                0,
                quadCount * 6,
                verticesPerPatch = 0
            )
            shader.end()
            quadCount = 0
        }
        queuedInstances = 0
    }

    private fun insertCharacter(
        contour: RectifiedContour,
        fontMap: FontImageMap,
        bw: BufferWriter,
        character: Char,
        cx: Double,
        cy: Double,
        instance: Int,
        textSetting: TextSettingMode,
        scale: Double
    ): Pair<Double, GlyphRectangle?> {

        val rectangle = fontMap.map[character] ?: fontMap.map[' ']
        val targetContentScale = RenderTarget.active.contentScale

        val x = if (textSetting == TextSettingMode.PIXEL) round(cx * targetContentScale) / targetContentScale else cx

        val metrics =
            fontMap.glyphMetrics[character] ?: fontMap.glyphMetrics[' '] ?: error("glyph or space substitute not found")

        val glyphRectangle =
            if (rectangle != null) {
                val pad = 2.0f
                val ushift = 0.0f
                val xshift = (metrics.xBitmapShift / fontMap.contentScale).toFloat()
                val yshift = (metrics.yBitmapShift / fontMap.contentScale).toFloat()

                val u0 = (rectangle.x.toFloat() - pad) / fontMap.texture.effectiveWidth + ushift
                val u1 =
                    (rectangle.x.toFloat() + rectangle.width.toFloat() + pad) / fontMap.texture.effectiveWidth + ushift
                val v0 = (rectangle.y.toFloat() - pad) / fontMap.texture.effectiveHeight
                val v1 = v0 + (pad * 2 + rectangle.height.toFloat()) / fontMap.texture.effectiveHeight


                val x0 = -pad / fontMap.contentScale.toFloat() + xshift
                val x1 =
                    (rectangle.width.toFloat() / fontMap.contentScale.toFloat()) + pad / fontMap.contentScale.toFloat() + xshift


                val t = (x + (x0 + x1) / 2.0) / (contour.contour.length / scale)

                if (t >= 1.0) {
                    null
                } else {
                    val pose = contour.pose(t)

                    val y0 = -pad / fontMap.contentScale.toFloat() + yshift
                    val y1 =
                        rectangle.height.toFloat() / fontMap.contentScale.toFloat() + pad / fontMap.contentScale.toFloat() + yshift

                    val transform = buildTransform {
                        multiply(
                            Matrix44(
                                pose.c0r0, pose.c1r0, pose.c2r0, pose.c3r0 / scale,
                                pose.c0r1, pose.c1r1, pose.c2r1, pose.c3r1 / scale,
                                pose.c0r2, pose.c1r2, pose.c2r2, pose.c3r2 / scale,
                                pose.c0r3, pose.c1r3, pose.c2r3, pose.c3r3,
                            )
                        )
                        multiply(
                            Matrix44(
                                -1.0, 0.0, 0.0, 0.0,
                                0.0, -1.0, 0.0, 0.0,
                                0.0, 0.0, 1.0, 0.0,
                                0.0, 0.0, 0.0, 1.0
                            )
                        )
                        translate(-(x0 + x1) / 2.0, 0.0)
                    }

                    val p00 = Vector2(x0.toDouble(), y0.toDouble())
                    val p01 = Vector2(x0.toDouble(), y1.toDouble())
                    val p10 = Vector2(x1.toDouble(), y0.toDouble())
                    val p11 = Vector2(x1.toDouble(), y1.toDouble())

                    val t00 = p00.transform(transform)
                    val t01 = p01.transform(transform)
                    val t10 = p10.transform(transform)
                    val t11 = p11.transform(transform)

                    lastPos = t00

                    val s0 = 0.0f
                    val t0 = 0.0f
                    val s1 = 1.0f
                    val t1 = 1.0f

                    val w = (x1 - x0)
                    val h = (y1 - y0)
                    val z = quadCount.toFloat()

                    val floatInstance = instance.toFloat()

                    if (quadCount < maxQuads) {
                        bw.apply {
                            write(u0, v0); write(s0, t0, w, h); write(t00.x.toFloat(), t00.y.toFloat(), z); write(
                            floatInstance
                        )
                            write(u1, v0); write(s1, t0, w, h); write(t10.x.toFloat(), t10.y.toFloat(), z); write(
                            floatInstance
                        )
                            write(u1, v1); write(s1, t1, w, h); write(t11.x.toFloat(), t11.y.toFloat(), z); write(
                            floatInstance
                        )
                            write(u0, v0); write(s0, t0, w, h); write(t00.x.toFloat(), t00.y.toFloat(), z); write(
                            floatInstance
                        )
                            write(u0, v1); write(s0, t1, w, h); write(t01.x.toFloat(), t01.y.toFloat(), z); write(
                            floatInstance
                        )
                            write(u1, v1); write(s1, t1, w, h); write(t11.x.toFloat(), t11.y.toFloat(), z); write(
                            floatInstance
                        )
                        }
                        quadCount++
                    }
                    GlyphRectangle(character, x0.toDouble(), y0.toDouble(), (x1 - x0).toDouble(), (y1 - y0).toDouble())
                }
            } else {
                null
            }
        return Pair(x - cx, glyphRectangle)
    }
}

private val onContourImageMapDrawer by lazy { OnContourImageMapDrawer() }

/**
 * Draws a given text along the given contour
 *
 * @param text The string to be rendered along the contour.
 * @param contour The rectified contour along which the text will be rendered.
 * @param offsetX Optional horizontal offset to shift the starting point of the text along the contour. Default value is 0.0.
 */
fun Drawer.textOnContour(text: String, contour: RectifiedContour, offsetX: Double = 0.0) {
    onContourImageMapDrawer.drawTextOnContour(contour, context, drawStyle, text, offsetX)
}
