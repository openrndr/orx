# orx-shader-phrases

A library that provides a `#pragma import` statement for shaders.

## Usage

Work in progress.


We can use the `preprocessShader()` function to resolve `#pragma import` statements.

```kotlin
    val preprocessedSource = preprocessShader(originalSource)
```

Alternatively loading and preprocessing can be combined in a single function call.

```kotlin
    val preprocessedSource = preprocessShaderFromUrl(resourceUrl("/some-shader.frag"))
```

