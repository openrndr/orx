package org.openrndr.extra.shaderphrases.annotations

enum class ShaderPhraseLanguage {
    GLSL
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FIELD)
annotation class ShaderPhrase(val exports: Array<String>,
                              val imports: Array<String> = emptyArray(),
                              val language: ShaderPhraseLanguage = ShaderPhraseLanguage.GLSL)


@Target(AnnotationTarget.FILE)
annotation class ShaderPhrases(val exports: Array<String>)