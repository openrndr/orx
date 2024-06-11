# orx-compute-shaders

Tools easing the work with compute shaders.

## Overview

`orx-compute-shaders` is designed to simplify the execution of compute shaders, especially for 2D data. This can include
images, grids of values, or organized point clouds.

## Executing Compute Shaders for 2D Data

When working with 2-dimensional data, particularly if the dimensions are unknown, certain assumptions must be made about
the dimensions of the computation. Below is an example of how to specify the layout in the shader and execute it.

### Shader Layout
In the shader itself, specify the layout as follows:

```glsl
#version 430
layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    // Prevent computation outside the resolution range
    if (coord.x >= resolution.x || coord.y >= resolution.y) {
        return;
    }

    // Desired computation
    // ...
}
```

### Execution in Kotlin

Now, you can execute such a shader in Kotlin with the following code:

```kotlin
val shader = ComputeShader.fromCode(shaderCode)
val executeDimensions = computeShaderExecuteDimensions(
    resolution,
    localSizeX = 8,
    localSizeY = 8
)
shader.execute(executeDimensions)
```

Note: Ensure the local size matches between the GLSL and Kotlin code for each respective dimension.
