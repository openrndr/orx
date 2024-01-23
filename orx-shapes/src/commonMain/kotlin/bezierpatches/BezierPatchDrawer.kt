package org.openrndr.extra.shapes.bezierpatches

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

import org.openrndr.draw.ShadeStyleGLSL.Companion.drawerUniforms
import org.openrndr.draw.ShadeStyleGLSL.Companion.fragmentMainConstants
import org.openrndr.draw.ShadeStyleGLSL.Companion.vertexMainConstants
import org.openrndr.extra.shaderphrases.preprocess
import org.openrndr.extra.shapes.phrases.BezierPhraseBook
import org.openrndr.extra.color.phrases.ColorPhraseBook
import org.openrndr.extra.color.spaces.ColorOKLABa
import org.openrndr.math.Vector4
import kotlin.jvm.JvmName

class BezierPatchDrawer {
    private fun vsGenerator(structure: ShadeStructure): String {
        return """
            |// BezierPatchDrawer.kt / vsGenerator
            |${drawerUniforms()}
            |${structure.attributes.orEmpty()}
            |${structure.varyingOut.orEmpty()}
            |void main() {
            |   ${vertexMainConstants()}
            |   vec3 x_normal = vec3(0.0, 0.0, 1.0);
            |   vec3 x_position = a_position;
            |   ${structure.varyingBridge}
            |}""".trimMargin()
    }

    private fun fsGenerator(structure: ShadeStructure): String {
        return ("""
            |// BezierPatchDrawer.kt / fsGenerator            
            |${drawerUniforms()}
            |${structure.varyingIn.orEmpty()}

            |out vec4 o_color;
            |void main() {
            |   vec4 x_fill = u_fill * va_color;
            |   vec4 x_stroke = u_stroke;
            |   {
            |       ${structure.fragmentTransform.orEmpty()}
            |   }
            |   o_color = x_fill;
            |   o_color.rgb *= o_color.a;
            }""".trimMargin())
    }
    private fun fsGeneratorOKLab(structure: ShadeStructure): String {
        return ("""
            |// BezierPatchDrawer.kt / fsGeneratorOKLab            
            |${drawerUniforms()}
            |${ColorPhraseBook.oklabToLinearRgb.phrase}
            |${ColorPhraseBook.linearRgbToSRgb.phrase}
            |${structure.varyingIn.orEmpty()}
            |out vec4 o_color;
            |void main() {
            |   ${fragmentMainConstants(instance = "0")}
            |   vec4 x_fill = u_fill * va_color;
            |   vec4 x_stroke = u_stroke;
            |   {
            |       ${structure.fragmentTransform.orEmpty()}
            |   }
            |   o_color = linear_rgb_to_srgb(oklab_to_linear_rgb(x_fill));
            |   o_color.rgb *= o_color.a;
            |}""".trimMargin())
    }
    private fun tseGenerator(structure: ShadeStructure): String {
        BezierPhraseBook.register()
        return """
            |
            |#pragma import beziers.bezier_patch42
            |#pragma import beziers.bezier_patch43
            |#pragma import beziers.bezier_patch44
            |
            |${drawerUniforms()}
            |layout(quads, equal_spacing, ccw) in;
            |
            |in vec3 cva_position[gl_MaxPatchVertices];
            |in vec4 cva_color[gl_MaxPatchVertices];
            |in vec2 cva_texCoord0[gl_MaxPatchVertices];
            |
            |${structure.varyingOut.orEmpty()}
            |
            |void main() {
            |   va_position = bezier_patch43(cva_position, gl_TessCoord.xy);
            |   va_color = bezier_patch44(cva_color, gl_TessCoord.xy);
            |   va_texCoord0 = bezier_patch42(cva_texCoord0, gl_TessCoord.xy);
            |   gl_Position = u_projectionMatrix * u_viewMatrix * u_modelMatrix * vec4(va_position,1.0);
            }""".trimMargin().preprocess()
    }

    private fun tscGenerator(structure: ShadeStructure): String {
        return """
            |uniform int u_subdivisions;
            |${drawerUniforms()}
            |layout(vertices = 16) out; // 16 points per patch
            |
            |in vec3 va_position[];
            |in vec4 va_color[];
            |in vec2 va_texCoord0[];
            |
            |out vec3 cva_position[];
            |out vec4 cva_color[];
            |out vec2 cva_texCoord0[];
            |
            |void main() {
            |   cva_position[gl_InvocationID] = va_position[gl_InvocationID];
            |   cva_color[gl_InvocationID] = va_color[gl_InvocationID];
            |   cva_texCoord0[gl_InvocationID] = va_texCoord0[gl_InvocationID];
            |
            |   if (gl_InvocationID == 0) {
            |       gl_TessLevelOuter[0] = u_subdivisions;
            |       gl_TessLevelOuter[1] = u_subdivisions;
            |       gl_TessLevelOuter[2] = u_subdivisions;
            |       gl_TessLevelOuter[3] = u_subdivisions;
            |       gl_TessLevelInner[0] = u_subdivisions;
            |       gl_TessLevelInner[1] = u_subdivisions;
            |   }
            |}""".trimMargin()
    }

    val shadeStyleManager by lazy {
        ShadeStyleManager.fromGenerators(
            name = "bezier-patches",
            vsGenerator = ::vsGenerator,
            tscGenerator = ::tscGenerator,
            tseGenerator = ::tseGenerator,
            fsGenerator = ::fsGenerator
        )
    }

    val shadeStyleManagerOKLab by lazy {
        ShadeStyleManager.fromGenerators(
            name = "bezier-patches-oklab",
            vsGenerator = ::vsGenerator,
            tscGenerator = ::tscGenerator,
            tseGenerator = ::tseGenerator,
            fsGenerator = ::fsGeneratorOKLab
        )
    }

    var vertices =
            vertexBuffer(
                vertexFormat {
                    position(3)
                    color(4)
                    textureCoordinate(2)
                }, 16, session = Session.root)


    internal fun ensureVertexCount(count: Int) {
        if (vertices.vertexCount < count) {
            vertices.destroy()
            vertices = vertexBuffer(
                vertexFormat {
                    position(3)
                    color(4)
                    textureCoordinate(2)
                }, count, session = Session.root)
        }
    }
    fun drawBezierPatches(
        context: DrawContext,
        drawStyle: DrawStyle,
        bezierPatches: List<BezierPatchBase<ColorRGBa>>,
        subdivisions: Int = 32
    ) {
        ensureVertexCount(bezierPatches.size * 16)
        val shader = shadeStyleManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            emptyList()
        )
        vertices.put {
            for (bezierPatch in bezierPatches) {
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        write(bezierPatch.points[j][i].xy0)
                        if (bezierPatch.colors.isEmpty()) {
                            write(ColorRGBa.WHITE)
                        } else {
                            write(bezierPatch.colors[j][i])
                        }
                        write(Vector2(i / 3.0, j / 3.0))
                    }
                }
            }
        }
        shader.begin()
        shader.uniform("u_subdivisions", subdivisions)
        context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.PATCHES,
            0,
            16 * bezierPatches.size,
            16
        )
        shader.end()
    }

    @JvmName("drawBezierPatchesOKLab")
    fun drawBezierPatches(
        context: DrawContext,
        drawStyle: DrawStyle,
        bezierPatches: List<BezierPatchBase<ColorOKLABa>>,
        subdivisions: Int = 32
    ) {
        ensureVertexCount(bezierPatches.size * 16)
        val shader = shadeStyleManagerOKLab.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            emptyList()
        )

        vertices.put {
            for(bezierPatch in bezierPatches) {
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        write(bezierPatch.points[j][i].xy0)
                        if (bezierPatch.colors.isEmpty()) {
                            write(ColorRGBa.WHITE)
                        } else {
                            write(bezierPatch.colors[j][i].let {
                                Vector4(it.l, it.a, it.b, it.alpha)
                            })
                        }
                        write(Vector2(i / 3.0, j / 3.0))
                    }
                }
            }
        }
        shader.begin()
        shader.uniform("u_subdivisions", subdivisions)
        context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.PATCHES,
            0,
            16 * bezierPatches.size,
            16
        )
        shader.end()
    }

    @JvmName("drawBezierPatches3D")
    fun drawBezierPatches(
        context: DrawContext,
        drawStyle: DrawStyle,
        bezierPatches: List<BezierPatch3DBase<ColorRGBa>>,
        subdivisions: Int = 32
    ) {
        ensureVertexCount(bezierPatches.size * 16)
        val shader = shadeStyleManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            emptyList()
        )
        vertices.put {
            for (bezierPatch in bezierPatches) {
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        write(bezierPatch.points[j][i])
                        if (bezierPatch.colors.isEmpty()) {
                            write(ColorRGBa.WHITE)
                        } else {
                            write(bezierPatch.colors[j][i])
                        }
                        write(Vector2(i / 3.0, j / 3.0))
                    }
                }
            }
        }
        shader.begin()
        shader.uniform("u_subdivisions", subdivisions)
        context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.PATCHES,
            0,
            16 * bezierPatches.size,
            16
        )
        shader.end()
    }

    @JvmName("drawBezierPatches3DOKLab")
    fun drawBezierPatches(
        context: DrawContext,
        drawStyle: DrawStyle,
        bezierPatches: List<BezierPatch3DBase<ColorOKLABa>>,
        subdivisions: Int = 32
    ) {
        ensureVertexCount(bezierPatches.size * 16)
        val shader = shadeStyleManagerOKLab.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            emptyList()
        )

        vertices.put {
            for(bezierPatch in bezierPatches) {
                for (j in 0 until 4) {
                    for (i in 0 until 4) {
                        write(bezierPatch.points[j][i])
                        if (bezierPatch.colors.isEmpty()) {
                            write(ColorRGBa.WHITE)
                        } else {
                            write(bezierPatch.colors[j][i].let {
                                Vector4(it.l, it.a, it.b, it.alpha)
                            })
                        }
                        write(Vector2(i / 3.0, j / 3.0))
                    }
                }
            }
        }
        shader.begin()
        shader.uniform("u_subdivisions", subdivisions)
        context.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(
            shader,
            listOf(vertices),
            DrawPrimitive.PATCHES,
            0,
            16 * bezierPatches.size,
            16
        )
        shader.end()
    }
}

private val Drawer.bezierPatchDrawer: BezierPatchDrawer by lazy { BezierPatchDrawer() }

@JvmName("bezierPatchRGBa")
fun Drawer.bezierPatch(bezierPatch: BezierPatchBase<ColorRGBa>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, listOf(bezierPatch), subdivisions)
}

@JvmName("bezierPatchesRGBa")
fun Drawer.bezierPatches(bezierPatch: List<BezierPatchBase<ColorRGBa>>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, bezierPatch, subdivisions)
}

@JvmName("bezierPatchOKLAB")
fun Drawer.bezierPatch(bezierPatch: BezierPatchBase<ColorOKLABa>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, listOf(bezierPatch), subdivisions)
}

@JvmName("bezierPatchesOKLAB")
fun Drawer.bezierPatches(bezierPatch: List<BezierPatchBase<ColorOKLABa>>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, bezierPatch, subdivisions)
}

@JvmName("bezierPatch3DRGBa")
fun Drawer.bezierPatch(bezierPatch: BezierPatch3DBase<ColorRGBa>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, listOf(bezierPatch), subdivisions)
}

@JvmName("bezierPatches3DRGBa")
fun Drawer.bezierPatches(bezierPatch: List<BezierPatch3DBase<ColorRGBa>>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, bezierPatch, subdivisions)
}

@JvmName("bezierPatch3DOKLAB")
fun Drawer.bezierPatch(bezierPatch: BezierPatch3DBase<ColorOKLABa>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, listOf(bezierPatch), subdivisions)
}

@JvmName("bezierPatches3DOKLAB")
fun Drawer.bezierPatches(bezierPatch: List<BezierPatch3DBase<ColorOKLABa>>, subdivisions: Int = 32) {
    bezierPatchDrawer.drawBezierPatches(context, drawStyle, bezierPatch, subdivisions)
}
