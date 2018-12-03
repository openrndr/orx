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