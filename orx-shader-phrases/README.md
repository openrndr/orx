# orx-shader-phrases

A library that provides a `#pragma import` statement for shaders.

## Usage

*Work in progress.*


We can use the `preprocessShader()` function to resolve `#pragma import` statements.

```kotlin
    val preprocessedSource = preprocessShader(originalSource)
```

Alternatively loading and preprocessing can be combined in a single function call.

```kotlin
    val preprocessedSource = preprocessShaderFromUrl(resourceUrl("/some-shader.frag"))
```

## Example

```kotlin
import org.openrndr.application
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.shaderphrases.ShaderPhrase
import org.openrndr.extra.shaderphrases.ShaderPhraseBook
import org.openrndr.extra.shaderphrases.preprocessShader

// 1. Define GLSL functions to reuse in multiple files or programs.
// Typically these will be larger blocks of code.
// Note that the Kotlin variable name do not matter, but the used GLSL function name must match the #pragma import.
class ColorShaderPhrases : ShaderPhraseBook("colors") {
    val fRed = ShaderPhrase(
        "vec3 red() { return vec3(1.0, 0.0, 0.0); }"
    )

    val fGreen = ShaderPhrase(
        "vec3 green() { return vec3(0.0, 1.0, 0.0); }"
    )

    val fBlue = ShaderPhrase(
        "vec3 blue() { return vec3(0.0, 0.0, 1.0); }"
    )
}

fun main() = application {
    program {
        // 2. Make defined GLSL functions available
        ColorShaderPhrases().register()

        extend {
            drawer.shadeStyle = shadeStyle {
                // 3. Import the GLSL functions needed in this program
                fragmentPreamble = preprocessShader("""
                    #pragma import colors.red
                    #pragma import colors.blue
                """.trimIndent())

                // 4. Make use of the available GLSL functions
                fragmentTransform = """
                    x_stroke.rgb = red();
                    x_fill.rgb = blue();
                """.trimIndent()
            }
            drawer.circle(drawer.bounds.center, 100.0)
        }
    }
}
```
