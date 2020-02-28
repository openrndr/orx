# orx-glslify

Easily use glslify compatible shaders found on [npm](https://www.npmjs.com/search?q=glslify).

### Caveats

Some glslify shaders have their own imports. When this happens we print a message to the console,
so you can proceed to import them. These need to be imported in the shader file on top of the main import.

There's also a mapping functionality that glslify provides that we don't support. This can be easily solved
by doing as the following example (based on `glsl-raytrace` package):

```glsl
const int steps = 50;
vec2 map(vec3 p);

#pragma import shaders.RayMarching.*
```

## Example

Shader Phrases file:
```kotlin
@file:JvmName("Checkers")
@file:ShaderPhrases

package shaders

import org.openrndr.extra.glslify.glslify
import org.openrndr.extra.shaderphrases.annotations.ShaderPhrases

val periodic by lazy { glslify("glsl-noise/classic/3d", "perlin") }
val checker by lazy { glslify("glsl-checker") }
val easings by lazy { glslify("glsl-easings/cubic-in-out", "easing")}
```

Shader file:

```glsl
#version 330

in vec2 v_texCoord0;

uniform sampler2D tex0;
uniform float uTime;

out vec4 o_color;

#pragma import shaders.Checkers.*

void main() {
    vec2 uv = v_texCoord0;
    float n = perlin(vec3(uv * 2.5 + uTime * 0.01, uTime * 0.2)) * 0.5 + 0.5;

    float patt = checker(uv * easing(n), 6.0);

    vec3 col = mix(vec3(0.173, 0.216, 0.278),vec3(0.792, 0.282, 0.478), vec3(patt)) * (n + 0.1);

    o_color = vec4(col, 1.0);
}
```
