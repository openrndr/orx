# orx-noise

Randomness for every type of person: Perlin, uniform, value, simplex, fractal and many other types of noise.

## Uniform random numbers

```kotlin
val sua = Double.uniform()
val sub = Double.uniform(-1.0, 1.0)

val v2ua = Vector2.uniform()
val v2ub = Vector2.uniform(-1.0, 1.0)
val v2uc = Vector2.uniform(Vector2(0.0, 0.0), Vector2(1.0, 1.0))
val v2ur = Vector2.uniformRing(0.5, 1.0)

val v3ua = Vector3.uniform()
val v3ub = Vector3.uniform(-1.0, 1.0)
val v3uc = Vector3.uniform(Vector3(0.0, 0.0, 0.0), Vector3(1.0, 1.0, 1.0))
val v3ur = Vector3.uniformRing(0.5, 1.0)

val v4ua = Vector4.uniform()
val v4ub = Vector4.uniform(-1.0, 1.0)
val v4uc = Vector4.uniform(Vector4(0.0, 0.0, 0.0, 0.0), Vector4(1.0, 1.0, 1.0, 1.0))
val v4ur = Vector4.uniformRing(0.5, 1.0)

val ringSamples = List(500) { Vector2.uniformRing() }
```

## Multi-dimensional noise

These are a mostly straight port from FastNoise-Java but have a slightly different interface.

### Perlin noise
```
// -- 1d
val v0 = perlinLinear(seed, x)
val v1 = perlinQuintic(seed, x)
val v2 = perlinHermite(seed, x)

// -- 2d
val v3 = perlinLinear(seed, x, y)
val v4 = perlinQuintic(seed, x, y)
val v5 = perlinHermite(seed, x, y)

// -- 3d
val v6 = perlinLinear(seed, x, y, z)
val v7 = perlinQuintic(seed, x, y, z)
val v8 = perlinHermite(seed, x, y, z)
```

### Value noise
```
// -- 1d
val v0 = valueLinear(seed, x)
val v1 = valueQuintic(seed, x)
val v2 = valueHermite(seed, x)

// -- 2d
val v2 = valueLinear(seed, x, y)
val v3 = valueQuintic(seed, x, y)
val v4 = valueHermite(seed, x, y)

// -- 3d
val v5 = valueLinear(seed, x, y, z)
val v6 = valueQuintic(seed, x, y, z)
val v7 = valueHermite(seed, x, y ,z)
```

### Simplex noise
```
// -- 1d
val v0 = simplex(seed, x)

// -- 2d
val v1 = simplex(seed, x, y)

// -- 3d
val v2 = simplex(seed, x, y, z)

// -- 4d
val v3 = simplex(seed, x, y, z, w)
```

### Cubic noise
```
// -- 1d
val v0 = cubic(seed, x, y)
val v1 = cubicQuintic(seed, x, y)
val v2 = cubicHermite(seed, x, y)

// -- 2d
val v0 = cubic(seed, x, y)
val v1 = cubicQuintic(seed, x, y)
val v2 = cubicHermite(seed, x, y)

// -- 3d
val v3 = cubic(seed, x, y, z)
val v4 = cubicQuintic(seed, x, y, z)
val v5 = cubicHermite(seed, x, y ,z)
```

### Fractal noise

The library provides 3 functions with which fractal noise can be composed.

#### Fractal brownian motion (FBM)

```
// 1d
val v0 = fbm(seed, x, ::perlinLinear, octaves, lacunarity, gain)
val v1 = fbm(seed, x, ::simplexLinear, octaves, lacunarity, gain)
val v2 = fbm(seed, x, ::valueLinear, octaves, lacunarity, gain)

// 2d
val v3 = fbm(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v4 = fbm(seed, x, y, ::simplexLinear, octaves, lacunarity, gain)
val v5 = fbm(seed, x, y, ::valueLinear, octaves, lacunarity, gain)

// 3d
val v6 = fbm(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v7 = fbm(seed, x, y, z, ::simplexLinear, octaves, lacunarity, gain)
val v8 = fbm(seed, x, y, z, ::valueLinear, octaves, lacunarity, gain)
```

#### Rigid

```
// 1d
val v0 = rigid(seed, x, ::perlinLinear, octaves, lacunarity, gain)
val v1 = rigid(seed, x, ::simplexLinear, octaves, lacunarity, gain)
val v2 = rigid(seed, x, ::valueLinear, octaves, lacunarity, gain)

// 2d
val v2 = rigid(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v3 = rigid(seed, x, y, ::simplexLinear, octaves, lacunarity, gain)
val v4 = rigid(seed, x, y, ::valueLinear, octaves, lacunarity, gain)

// 3d
val v3 = rigid(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v4 = rigid(seed, x, y, z, ::simplexLinear, octaves, lacunarity, gain)
val v5 = rigid(seed, x, y, z, ::valueLinear, octaves, lacunarity, gain)
```

#### Billow

```
// 1d
val v0 = billow(seed, x, ::perlinLinear, octaves, lacunarity, gain)
val v1 = billow(seed, x, ::perlinLinear, octaves, lacunarity, gain)
val v2 = billow(seed, x, ::perlinLinear, octaves, lacunarity, gain)

// 2d
val v3 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v4 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v5 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)

// 3d
val v6 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v7 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v8 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
```
<!-- __demos__ >
# Demos
[DemoGradientPerturb2DKt](src/demo/kotlin/DemoGradientPerturb2DKt.kt
![DemoGradientPerturb2DKt](https://github.com/openrndr/orx/blob/media/orx-noise/images/DemoGradientPerturb2DKt.png
[DemoGradientPerturb3DKt](src/demo/kotlin/DemoGradientPerturb3DKt.kt
![DemoGradientPerturb3DKt](https://github.com/openrndr/orx/blob/media/orx-noise/images/DemoGradientPerturb3DKt.png
[DemoPoissonDiskSamplingKt](src/demo/kotlin/DemoPoissonDiskSamplingKt.kt
![DemoPoissonDiskSamplingKt](https://github.com/openrndr/orx/blob/media/orx-noise/images/DemoPoissonDiskSamplingKt.png
<!-- __demos__ -->
## Demos
### DemoGradientPerturb2D
[source code](src/demo/kotlin/DemoGradientPerturb2D.kt)

![DemoGradientPerturb2DKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-noise/images/DemoGradientPerturb2DKt.png)

### DemoGradientPerturb3D
[source code](src/demo/kotlin/DemoGradientPerturb3D.kt)

![DemoGradientPerturb3DKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-noise/images/DemoGradientPerturb3DKt.png)

### DemoSimplex01
[source code](src/demo/kotlin/DemoSimplex01.kt)

![DemoSimplex01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-noise/images/DemoSimplex01Kt.png)
