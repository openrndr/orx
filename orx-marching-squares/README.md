# orx-marching-squares

Tools for extracting contours from functions

## How to use it?

`orx-marching-squares` provides the `findContours()` function

```kotlin
fun f(v: Vector2) = v.distanceTo(drawer.bounds.center) - 200.0
val segments = findContours(::f, drawer.bounds, 16.0)
drawer.lineSegments(segments)
```

With a small adjustment to the given function one can use `findContours` to find iso contours. The trick is to add a cosine over the distance function.

```kotlin
fun f(v: Vector2) = cos((v.distanceTo(drawer.bounds.center) / 100.0) * 2 * PI)
val segments = findContours(::f, drawer.bounds.offsetEdges(32.0), 16.0)
drawer.lineSegments(segments)
```