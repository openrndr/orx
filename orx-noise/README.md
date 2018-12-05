# orx-noise

A collection of noisy functions

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
// -- 2d
val v0 = perlinLinear(seed, x, y)
val v1 = perlinQuintic(seed, x, y)
val v2 = perlinHermite(seed, x, y)

// -- 3d
val v3 = perlinLinear(seed, x, y, z)
val v4 = perlinQuintic(seed, x, y, z)
val v5 = perlinHermite(seed, x, y, z)
```

### Value noise
```
// -- 2d
val v0 = valueLinear(seed, x, y)
val v1 = valueQuintic(seed, x, y)
val v2 = valueHermite(seed, x, y)

// -- 3d
val v3 = valueLinear(seed, x, y, z)
val v4 = valueQuintic(seed, x, y, z)
val v5 = valueHermite(seed, x, y ,z)
```

### Simplex noise
```
// -- 2d
val v0 = simplexLinear(seed, x, y)
val v1 = simplexQuintic(seed, x, y)
val v2 = simplexHermite(seed, x, y)

// -- 3d
val v3 = simplexLinear(seed, x, y, z)
val v4 = simplexQuintic(seed, x, y, z)
val v5 = simplexHermite(seed, x, y ,z)
```

### Cubic noise
```
// -- 2d
val v0 = cubicLinear(seed, x, y)
val v1 = cubicQuintic(seed, x, y)
val v2 = cubicHermite(seed, x, y)

// -- 3d
val v3 = cubicLinear(seed, x, y, z)
val v4 = cubicQuintic(seed, x, y, z)
val v5 = cubicHermite(seed, x, y ,z)
```

### Fractal noise

The library provides 3 functions with which fractal noise can be composed.

#### Fractal brownian motion (FBM)

```
val v0 = fbm(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v1 = fbm(seed, x, y, ::simplexLinear, octaves, lacunarity, gain)
val v2 = fbm(seed, x, y, ::valueLinear, octaves, lacunarity, gain)

val v3 = fbm(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v4 = fbm(seed, x, y, z, ::simplexLinear, octaves, lacunarity, gain)
val v5 = fbm(seed, x, y, z, ::valueLinear, octaves, lacunarity, gain)
```

#### Rigid

```
val v0 = rigid(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v1 = rigid(seed, x, y, ::simplexLinear, octaves, lacunarity, gain)
val v2 = rigid(seed, x, y, ::valueLinear, octaves, lacunarity, gain)

val v3 = rigid(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v4 = rigid(seed, x, y, z, ::simplexLinear, octaves, lacunarity, gain)
val v5 = rigid(seed, x, y, z, ::valueLinear, octaves, lacunarity, gain)
```

#### Billow

```
val v0 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v1 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)
val v2 = billow(seed, x, y, ::perlinLinear, octaves, lacunarity, gain)

val v3 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v4 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
val v5 = billow(seed, x, y, z, ::perlinLinear, octaves, lacunarity, gain)
```