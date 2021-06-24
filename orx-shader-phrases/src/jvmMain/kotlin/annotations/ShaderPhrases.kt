package org.openrndr.extra.shaderphrases.annotations

enum class ShaderPhraseLanguage {
    GLSL_330
}

@Target(AnnotationTarget.FILE)
annotation class ShaderPhrases(val exports: Array<String> = emptyArray())

