# orx-shader-phrases

A library that provides a `#pragma import` statement for shaders by using the JVM class loader.

## Usage

Given a shader source:

````glsl
#version 330
// -- this imports all phrases in Dummy 
#pragma import org.openrndr.extra.shaderphrases.phrases.Dummy.*

void main() {
    float a = dummy();
}
````

We can use the `preprocessShader()` function to resolve `#pragma import` statements.

```kotlin
    val preprocessedSource = preprocessShader(originalSource)
```

Alternatively loading and preprocessing can be combined in a single function call.

```kotlin
    val preprocessedSource = preprocessShaderFromUrl(resourceUrl("/some-shader.frag"))
```

To create importable shader phrases one creates a Kotlin class and adds the `ShaderPhrases` annotation.
For example the `dummy` phrase in our example is made available as follows:

```kotlin
// -- force the class name to be Dummy on the JVM
@file:JvmName("Dummy")
@file:ShaderPhrases
package org.openrndr.extra.shaderphrases.phrases
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

// -- the shader phrase
const val dummy = """
float dummy() {
    return 0.0;    
}
"""
``` 