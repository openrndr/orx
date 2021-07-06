package org.openrndr.extra.shapes.phrases

import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook

object BezierPhraseBook: ShaderPhraseBook("beziers") {

    val bezier22 = ShaderPhrase("""
        |vec2 bezier22(vec2 a, vec2 b, float t) {
        |   return mix(a, b, t);
        |}""".trimMargin())

    val bezier32 = ShaderPhrase("""
        |#pragma import $bookId.bezier22
        |vec2 bezier32(vec2 a, vec2 b, vec2 c, float t) {
        |   return mix(bezier22(a, b, t), bezier22(b, c, t), t);
        |}""".trimMargin())

    val bezier42 = ShaderPhrase("""
        |#pragma import $bookId.bezier32
        |vec2 bezier42(vec2 a, vec2 b, vec2 c, vec2 d, float t) {
        |   return mix(bezier32(a, b, c, t), bezier32(b, c, d, t), t);
        |}""".trimMargin())

    val bezier23 = ShaderPhrase("""
        |vec3 bezier23(vec3 a, vec3 b, float t) {
        |   return mix(a, b, t);
        |}""".trimMargin())

    val bezier33 = ShaderPhrase("""
        |#pragma import $bookId.bezier23
        |vec3 bezier33(vec3 a, vec3 b, vec3 c, float t) {
        |   return mix(bezier23(a, b, t), bezier23(b, c, t), t);
        |}""".trimMargin())

    val bezier43 = ShaderPhrase("""
        |#pragma import $bookId.bezier33
        |vec3 bezier43(vec3 a, vec3 b, vec3 c, vec3 d, float t) {
        |   return mix(bezier33(a, b, c, t), bezier33(b, c, d, t), t);
        |}""".trimMargin())

    val bezier24 = ShaderPhrase("""
        |vec4 bezier24(vec4 a, vec4 b, float t) {
        |   return mix(a, b, t);
        |}""".trimMargin())

    val bezier34 = ShaderPhrase("""
        |#pragma import $bookId.bezier24
        |vec4 bezier34(vec4 a, vec4 b, vec4 c, float t) {
        |   return mix(bezier24(a, b, t), bezier24(b, c, t), t);
        |}""".trimMargin())

    val bezier44 = ShaderPhrase("""
        |#pragma import $bookId.bezier34
        |vec4 bezier44(vec4 a, vec4 b, vec4 c, vec4 d, float t) {
        |   return mix(bezier34(a, b, c, t), bezier34(b, c, d, t), t);
        |}""".trimMargin())

    val bezierPatch42 = ShaderPhrase("""
        |#pragma import $bookId.bezier42
        |vec2 bezier_patch42(in vec2[gl_MaxPatchVertices] cps, vec2 uv) {
        |   vec2 p0 = bezier42(cps[0], cps[1], cps[2], cps[3], uv.x);
        |   vec2 p1 = bezier42(cps[4], cps[5], cps[6], cps[7], uv.x);
        |   vec2 p2 = bezier42(cps[8], cps[9], cps[10], cps[11], uv.x);
        |   vec2 p3 = bezier42(cps[12], cps[13], cps[14], cps[15], uv.x);
        |   return bezier42(p0, p1, p2, p3, uv.y);
        |}""".trimMargin())

    val bezierPatch43 = ShaderPhrase("""
        |#pragma import $bookId.bezier43
        |vec3 bezier_patch43(in vec3[gl_MaxPatchVertices] cps, vec2 uv) {
        |   vec3 p0 = bezier43(cps[0], cps[1], cps[2], cps[3], uv.x);
        |   vec3 p1 = bezier43(cps[4], cps[5], cps[6], cps[7], uv.x);
        |   vec3 p2 = bezier43(cps[8], cps[9], cps[10], cps[11], uv.x);
        |   vec3 p3 = bezier43(cps[12], cps[13], cps[14], cps[15], uv.x);
        |   return bezier43(p0, p1, p2, p3, uv.y);
        |}""".trimMargin())

    val bezierPatch44 = ShaderPhrase("""
        |#pragma import $bookId.bezier44
        |vec4 bezier_patch44(in vec4[gl_MaxPatchVertices] cps, vec2 uv) {
        |   vec4 p0 = bezier44(cps[0], cps[1], cps[2], cps[3], uv.x);
        |   vec4 p1 = bezier44(cps[4], cps[5], cps[6], cps[7], uv.x);
        |   vec4 p2 = bezier44(cps[8], cps[9], cps[10], cps[11], uv.x);
        |   vec4 p3 = bezier44(cps[12], cps[13], cps[14], cps[15], uv.x);
        |   return bezier44(p0, p1, p2, p3, uv.y);
        |}""".trimMargin())
}