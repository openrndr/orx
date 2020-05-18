# orx-glslify

Load glslify compatible shaders from [npm](https://www.npmjs.com/search?q=glslify).

### Caveats

There's also a mapping functionality that glslify provides that we don't support. This can be easily solved
by doing as the following example (based on `glsl-raytrace` package):

```glsl
const int steps = 50;
vec2 map(vec3 p);

#pragma glslify: raytrace = require(glsl-raytrace)
```

## Example

Shader file:

```glsl
#version 330

in vec2 v_texCoord0;

uniform sampler2D tex0;
uniform float uTime;

out vec4 o_color;

#pragma glslify: checker = require(glsl-checker)
#pragma glslify: perlin = require(glsl-noise/classic/3d)
#pragma glslify: easing = require(glsl-easings/cubic-in-out)

void main() {
    vec2 uv = v_texCoord0;
    float n = perlin(vec3(uv * 2.5 + uTime * 0.01, uTime * 0.2)) * 0.5 + 0.5;

    float patt = checker(uv * easing(n), 6.0);

    vec3 col = mix(vec3(0.173, 0.216, 0.278),vec3(0.792, 0.282, 0.478), vec3(patt)) * (n + 0.1);

    o_color = vec4(col, 1.0);
}
```

Then preprocess it with either: 

```kotlin
preprocessGlslifyFromUrl(resourceUrl("/shaders/ray-marching.glsl"))
```

or

```kotlin
preprocessGlslify("""version #330 ...""")
```
